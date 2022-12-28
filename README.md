# TM1637
*A simple Java library to control 4-digit-displays using TM1637 on a Raspberry Pi.*

## Currently supported
* Display 4 characters
* Includes default mappings that can be edited
* Enable/disable double point
* Set the brightness level

## Usage example
```java
// Supports BROADCOM, WIRING_PI and PHYSICAL
NumberingScheme numberingScheme = NumberingScheme.BROADCOM;

// Clock and data i/o pins
int clkPin = 21;
int dioPin = 20;
TM1637.setup(numberingScheme, clkPin, dioPin);
TM1637 tm1637 = TM1637.getInstance();
		
// Whether the double point should be activated
// autoFlush on: immediately update the display using the last 4 chars
// autoFlush off: wait until next call of show(char[])
boolean showDoublePoint = false;
boolean autoFlush = false;
tm1637.setShowDoublePoint(showDoublePoint, autoFlush);

// 4 bit brightness values: 0-15
// Some units (including the one this library was tested with)
// only support brightness levels between 8 and 15
int brightness = 15;
tm1637.setBrightness(brightness, autoFlush);

// display 4 characters
tm1637.show(new char[] {'1', '2', '3', '4'});
		
// optional:
// characters included: '0'...'9', 'A'...'F', '-', '_', ' '
// you may add additional mappings using this scheme:
// Set segments A-G with: 0bGFEDCBA
tm1637.addCharacter('L', (byte) 0b0111000);
```
Tested with:
* https://www.az-delivery.de/products/4-digit-display?ls=de
* Raspberry Pi Model B+ v1.2
* Pi4j-core v1.2

## Dependencies
This library requires [pi4j](http://pi4j.com) in order to work.


## License
This library is a Java rewrite of this [Python project](https://github.com/johnlr/raspberrypi-tm1637)
 licensed under the MIT license (credits go to [johnlr](https://github.com/johnlr)).
 The copyright statement is included in `src/TM1637.java`.
