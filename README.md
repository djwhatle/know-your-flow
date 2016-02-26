# know-your-flow

## About

Know Your Flow (later renamed to Know UR Shower) came out of a weekend-long Make-A-Thon at NC State University.

Our goal was to create a device that would provide both live and historical data on showering habits in order to raise awareness and help people form better water usage habits over time. 

While in the shower, users see immediate information on their showering habits through a strip of ~10 WS2812 RGB LED's. The user sets up a water budget for their shower session (e.g. 10 gallons per shower) and then receives live feedback through the LED's. 

As the water budget is depleted over the course of the shower, the topmost LED's on the strip turn off one by one, similar to how a bucket with a hole in the bottom of it would lower in water level from top to bottom. 

The device designed is egg shaped and gets screwed in between the spigot and shower head. This design allows for charging of the internal battery via residual water pressure pushing a combination turbine/flowmeter. The strip of WS2812 LED's encased in a waterproof enclosure with a light diffusive window to soften the harshness of the LED's, and mounted to the wall of the shower with miniature suction cups.


The user is able to change the water output of the shower head by turning a ring on the exterior of a flow meter.

## What's in the repo?

This repository contains the Arduino code for the demo, as well as code for an Android application which can communicate with the hardware via Bluetooth SPP at 9600 baud.

## What components do I need to build this?

We used the following components in our design:

- 5V Arduino Micro
- Adafruit 16x2 RGB Negative LCD Display
- 10K Potentiometer (flow rate setting)
- 4.7k Potentiometer (screen contrast trimming)
- Adafruit Bluefruit EZ-Link
- WS2812B 1 meter strip with 144 LED's


## Credits

This code was created as a collaboration between myself and Michael Meli (mjmeli).
The overall project was created by Michael Meli, Brian Iezzi, Brian Murphy, and myself.
