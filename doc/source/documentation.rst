Waterlevel Monitoring
---------------------

A flownexus Example Application
...............................

This documentation shows a system that is compatible with Zephyr OS and
flownexus and reports the captured data to the flownexus server. The goal is to
build a system that monitors the level in a water tank and make that
measurement visible on a website.

The water level as well as additional sensor values like temperature and
humidity are collected and sent via Lightweight M2M (LwM2M) to a server that
runs flownexus.

Features
........

* The sytem has to be able to run on a battery for at least 1 year.
* Capable of being update via FOTA upates from flownexus.
* Samples values every 1 minute. Each sample has a timestamp and a value.
* Sending sampled values every 1 hour via SenML over LwM2M to the server.

Sensors
.......

The system is equpped with sensors that can measure the following values:

* Pressure in the water tank (water level)
* Air Pressure
* Humidity
* Temperature
* Air Quality


Firmware
--------

Build and Flash
...............

As device management protocol LwM2M is used. Zephyr offers a LwM2M client at
``subsys/net/lib/lwm2m``. This LwM2M client sample application implements the
LwM2M library and establishes a connection to an LwM2M server. The example can
be build with the following command:

.. code-block:: console

  host:~$ west build -b nrf9161dk_nrf9160_ns fw_test/lwm2m_client -p
  host:~$ west flash --recover


Hardware
--------

Overview
........

The IoT device is based on the Nordic Thingy:91 with a connected pressure
sensor (ST LPS28DFW). The nRF9160 is a low power LTE-M and NB-IoT module that
runs Zephyr OS.

.. figure:: images/hardware_assembled.jpg
  :width: 70%

  Assembled system

Pressure Sensor
...............

The pressure sensor comes encased in a waterproof housing, ensuring that the
device can reliably function even when immersed in water. This sensor connects
to a system through its cable, which facilitates communication via the I2C
protocol with the help of an SDA and an SDL line.

The table states how the pressure sensor is connected to the cable. The wires
of the 5 pin cable are named PE and numbers ranging form 1-4.

.. table:: Pinout Pressure Sensor Cable

  +---------+--------------+
  | **Pin** | **Function** |
  +=========+==============+
  | PE      | GND          |
  +---------+--------------+
  | 1       | I2C SDA      |
  +---------+--------------+
  | 2       | I2C SDL      |
  +---------+--------------+
  | 3       | Interrupt    |
  +---------+--------------+
  | 4       | VCC 1V8      |
  +---------+--------------+

.. figure:: images/pressure_sensor_housing.jpg
  :width: 25%

  Pressure Sensor with waterproof housing

.. figure:: images/pressure_sensor_cable.jpg
  :width: 25%

  Pressure Sensor cable connection

Mounting
........

.. figure:: images/hardware_mounted.jpg
  :width: 50%

  assembled system, ready to be mounted
