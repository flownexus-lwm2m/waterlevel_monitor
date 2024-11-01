LPS28DFW: Temperature and Humidity Monitor
##########################################

Overview
********
This sample periodically reads pressure from the LPS28DFW MEMS pressure
sensor and displays it on the console.


Requirements
************

This sample uses the LPS22HB sensor controlled using the I2C interface.

Building and Running
********************

This project outputs sensor data to the console. It requires an LPS22HB
sensor, which is present on the disco_l475_iot1 board.

.. zephyr-app-commands::
   :zephyr-app: samples/sensor/lps22hb
   :board: disco_l475_iot1
   :goals: build
   :compact:

Sample Output
=============

.. code-block:: console

   Observation:1
   Pressure:98.7 kPa
   Temperature:22.5 C
   Observation:2
   Pressure:98.7 kPa
   Temperature:22.5 C
   Observation:3
   Pressure:98.7 kPa
   Temperature:22.5 C

   <repeats endlessly every 2 seconds>
