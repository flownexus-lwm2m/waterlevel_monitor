/* Copyright (c) 2024 Phytec Messtechnik GmbH
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>
#include <zephyr/devicetree.h>
#include <zephyr/drivers/sensor.h>

#include "water_pressure.h"

LOG_MODULE_REGISTER(water_pressure, CONFIG_MULTI_SERVICE_LOG_LEVEL);

static const struct device *press_sensor = DEVICE_DT_GET(DT_ALIAS(water_press_sensor));

int get_water_pressure(double *press)
{
	int err;
	struct sensor_value data = {0};

	/* Fetch all data the sensor supports. */
	err = sensor_sample_fetch(press_sensor);
	if (err) {
		LOG_ERR("Failed to sample pressure sensor, error %d", err);
		return -ENODATA;
	}

	/* Pick out the ambient temperature data. */
	err = sensor_channel_get(press_sensor, SENSOR_CHAN_PRESS, &data);
	if (err) {
		LOG_ERR("Failed to read pressure, error %d", err);
		return -ENODATA;
	}

	*press = sensor_value_to_double(&data);

	return 0;
}
