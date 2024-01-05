/* Copyright (c) 2024 Phytec Messtechnik GmbH
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#ifndef _WATER_PRESSURE_H_
#define _WATER_PRESSURE_H_

/**
 * @brief Take a pressure sample.
 *
 * @param[out] temp - Pointer to the double to be filled with the taken pressure
 * sample.
 * @return int - 0 on success, otherwise, negative error code.
 */
int get_water_pressure(double *press);

#endif /* _WATER_PRESSURE_H_ */
