Waterlevel Monitor
==================

Update west
###########

Make sure to configure west to reference the manifest file (west.yml) and
update all references with::

  $ west update

Build and Flash the Firmware
############################

Build and flash the firmware application with::

  $ west build app -p
  $ west flash

Documentation
#############

See documentation in doc for a more details.
