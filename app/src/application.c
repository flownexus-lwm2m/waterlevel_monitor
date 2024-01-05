/* Copyright (c) 2023 Nordic Semiconductor ASA
 *
 * SPDX-License-Identifier: LicenseRef-Nordic-5-Clause
 */

#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>
#include <zephyr/logging/log_ctrl.h>
#include <date_time.h>
#include <stdio.h>
#include <net/nrf_cloud.h>
#include <net/nrf_cloud_codec.h>
#include <net/nrf_cloud_log.h>
#include <net/nrf_cloud_alert.h>
#if defined(CONFIG_NRF_CLOUD_COAP)
#include <net/nrf_cloud_coap.h>
#endif
#include "application.h"
#include "temperature.h"
#include "water_pressure.h"
#include "cloud_connection.h"
#include "message_queue.h"
#include "led_control.h"

LOG_MODULE_REGISTER(application, CONFIG_MULTI_SERVICE_LOG_LEVEL);

/* Timer used to time the sensor sampling rate. */
static K_TIMER_DEFINE(sensor_sample_timer, NULL, NULL);

/* AT command request error handling */
#define AT_CMD_REQUEST_ERR_FORMAT "Error while processing AT command request: %d"
#define AT_CMD_REQUEST_ERR_MAX_LEN (sizeof(AT_CMD_REQUEST_ERR_FORMAT) + 20)
BUILD_ASSERT(CONFIG_AT_CMD_REQUEST_RESPONSE_BUFFER_LENGTH >= AT_CMD_REQUEST_ERR_MAX_LEN,
	     "Not enough AT command response buffer for printing error events.");

#define N_MSG 15

/**
 * @brief Construct a device message object with automatically generated timestamp
 *
 * The resultant JSON object will be conformal to the General Message Schema described in the
 * application-protocols repo:
 *
 * https://github.com/nRFCloud/application-protocols
 *
 * @param msg - The object to contain the message
 * @param appid - The appId for the device message
 * @param msg_type - The messageType for the device message
 * @return int - 0 on success, negative error code otherwise.
 */
static int create_timestamped_device_message(struct nrf_cloud_obj *const msg,
					     const char *const appid,
					     const char *const msg_type)
{
	int err;
	int64_t timestamp;

	/* Acquire timestamp */
	err = date_time_now(&timestamp);
	if (err) {
		LOG_ERR("Failed to obtain current time, error %d", err);
		return -ETIME;
	}

	/* Create message object */
	err = nrf_cloud_obj_msg_init(msg, appid,
				     IS_ENABLED(CONFIG_NRF_CLOUD_COAP) ? NULL : msg_type);
	if (err) {
		LOG_ERR("Failed to initialize message with appid %s and msg type %s",
			appid, msg_type);
		return err;
	}

	/* Add timestamp to message object */
	err = nrf_cloud_obj_ts_add(msg, timestamp);
	if (err) {
		LOG_ERR("Failed to add timestamp to data message with appid %s and msg type %s",
			appid, msg_type);
		nrf_cloud_obj_free(msg);
		return err;
	}

	return 0;
}

/**
 * @brief Transmit a collected sensor sample to nRF Cloud.
 *
 * @param sensor - The name of the sensor which was sampled.
 * @param value - The sampled sensor value.
 * @return int - 0 on success, negative error code otherwise.
 */
static int add_sensor_sample(const char *const sensor, double value)
{
	int ret;
	static int j;
	static struct nrf_cloud_obj msg_obj[N_MSG];

	/* Flush all data once the messages have been send to the cloud */
	if (!j) {
		for (int i = 0; i < N_MSG; ++i) {
			msg_obj[i].type = NRF_CLOUD_OBJ_TYPE_COAP_CBOR;
			msg_obj[i].coap_cbor = NULL;
			msg_obj[i].enc_src = NRF_CLOUD_ENC_SRC_NONE;
			msg_obj[i].encoded_data.ptr = NULL;
			msg_obj[i].encoded_data.len = 0;
		}
	}

	/* Create a timestamped message container object for the sensor sample. */
	ret = create_timestamped_device_message(&msg_obj[j], sensor,
						NRF_CLOUD_JSON_MSG_TYPE_VAL_DATA);
	if (ret) {
		return -EINVAL;
	}

	/* Populate the container object with the sensor value. */
	ret = nrf_cloud_obj_num_add(&msg_obj[j], NRF_CLOUD_JSON_DATA_KEY, value, false);
	if (ret) {
		LOG_ERR("Failed to append value to %s sample container object ",
			sensor);
		nrf_cloud_obj_free(&msg_obj[j]);
		return -ENOMEM;
	}
	LOG_INF("Enqueued message [%d/%d]", j+1, N_MSG);
	j++;

	/* Send the sensor sample container object as a device message. */
	if (j >= N_MSG) {
		while (j) {
			ret = send_device_message(&msg_obj[j-1]);
			if (ret < 0) {
				LOG_ERR("Failed to send msg to cloud");
				return ret;
			}
			j--;
		}
	}

	return 0;
}

void main_application_thread_fn(void)
{
	/* Wait for first connection before starting the application. */
	(void)await_cloud_ready(K_FOREVER);

	(void)nrf_cloud_alert_send(ALERT_TYPE_DEVICE_NOW_ONLINE, 0, NULL);

	/* Wait for the date and time to become known.
	 * This is needed both for location services and for sensor sample timestamping.
	 */
	LOG_INF("Waiting for modem to determine current date and time");
	if (!await_date_time_known(K_SECONDS(CONFIG_DATE_TIME_ESTABLISHMENT_TIMEOUT_SECONDS))) {
		LOG_WRN("Failed to determine valid date time. Proceeding anyways");
	} else {
		LOG_INF("Current date and time determined");
	}

	nrf_cloud_log_init();
	nrf_cloud_log_control_set(CONFIG_NRF_CLOUD_LOG_OUTPUT_LEVEL);
	/* Send a direct log to the nRF Cloud web portal indicating the sample has started up. */
	(void)nrf_cloud_log_send(LOG_LEVEL_INF,
				 "nRF Cloud multi-service sample has started, "
				 "version: %s, protocol: CoAP",
				 CONFIG_APP_VERSION);

	/* Begin sampling sensors. */
	while (true) {
		/* Start the sensor sample interval timer.
		 * We use a timer here instead of merely sleeping the thread, because the
		 * application thread can be preempted by other threads performing long tasks
		 * (such as periodic location acquisition), and we want to account for these
		 * delays when metering the sample send rate.
		 */
		k_timer_start(&sensor_sample_timer,
			K_SECONDS(CONFIG_SENSOR_SAMPLE_INTERVAL_SECONDS), K_FOREVER);

		if (IS_ENABLED(CONFIG_TEMP_TRACKING)) {
			double temp = -1, press = -1, hum = -1;

			if (get_temp_hum(&temp, &hum) == 0) {
				LOG_INF("Temperature %d C", (int)temp);
				add_sensor_sample(NRF_CLOUD_JSON_APPID_VAL_TEMP, temp);
				LOG_INF("Humidity: %d %%", (int)hum);
				add_sensor_sample(NRF_CLOUD_JSON_APPID_VAL_HUMID, hum);
			}

			if (get_water_pressure(&press) == 0) {
				LOG_INF("Pressure is %d kPA", (int)press);
				add_sensor_sample(NRF_CLOUD_JSON_APPID_VAL_AIR_PRESS, press);
			}
		}

		/* Wait out any remaining time on the sample interval timer. */
		k_timer_status_sync(&sensor_sample_timer);
	}
}
