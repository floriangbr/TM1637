/**
 * TM1637 - A simple Java library to control 4-digit-displays using TM1637 on a Raspberry Pi
 *
 * This file is a Java rewrite of the following Python project licensed under the MIT license:
 *                 https://github.com/johnlr/raspberrypi-tm1637
 * The original license declaration:
 *
 * MIT License
 *
 * Copyright (c) 2016 John La Rooy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.pi4j.wiringpi.Gpio;

import java.util.HashMap;

public class TM1637 {

    /**
     * Static instance
     */
    private static final TM1637 instance = new TM1637();

    /**
     * Fixed byte values for communication
     */
    private static final byte I2C_COMM1 = (byte) 0x40;
    private static final byte I2C_COMM2 = (byte) 0xC0;
    private static final byte I2C_COMM3 = (byte) 0x80;

    /**
     * Mapped characters
     */
    private static final HashMap<Character, Byte> charToSegment = new HashMap<>();

    /**
     * Clock and data i/o pins
     */
    private int clk = 21;
    private int dio = 20;

    /**
     * Brightness
     */
    private byte brightness = 0x0f;

    /**
     * Whether the double point should be displayed
     */
    private boolean showDoublePoint = false;

    /**
     * The last sent characters
     */
    private char[] lastData = new char[]{'-', '-', '-', '-'};

    /**
     * Private constructor to initialize
     */
    private TM1637() {
    }

    /**
     * Outputs the segments
     *
     * @param segments the four new segments
     */
    private void setSegments(byte[] segments) {
        // Write COMM1
        start();
        writeByte(I2C_COMM1);
        stop();

        // Write COMM2 + segments
        start();
        writeByte(I2C_COMM2); // not using start address
        for (int i = 0; i < 4; i++) {
            writeByte(segments[i]);
        }
        stop();

        // Write COMM3 + brightness
        start();
        writeByte((byte) (I2C_COMM3 + brightness));
        stop();
    }

    /**
     * Start routine
     */
    private void start() {
        Gpio.pinMode(dio, Gpio.OUTPUT);
        delay();
    }

    /**
     * Stop routine
     */
    private void stop() {
        Gpio.pinMode(dio, Gpio.OUTPUT);
        delay();
        Gpio.pinMode(clk, Gpio.INPUT);
        delay();
        Gpio.pinMode(dio, Gpio.INPUT);
        delay();
    }

    /**
     * Writes a single byte to the display
     *
     * @param b the byte to write
     */
    private void writeByte(byte b) {
        // Write the 8 bits
        for (int i = 0; i < 8; i++) {
            Gpio.pinMode(clk, Gpio.OUTPUT);
            delay();

            // Change to in/out depending on the bits value
            Gpio.pinMode(dio, (b & 1) == 1 ? Gpio.INPUT : Gpio.OUTPUT);
            delay();

            Gpio.pinMode(clk, Gpio.INPUT);
            delay();
            b >>= 1;
        }

        // Wait for ACK response
        Gpio.pinMode(clk, Gpio.OUTPUT);
        delay();
        Gpio.pinMode(clk, Gpio.INPUT);
        delay();
        Gpio.pinMode(clk, Gpio.OUTPUT);
        delay();
    }

    /**
     * Displays four new characters
     *
     * @param data the four characters
     */
    public void show(char[] data) {
        if (data == null || data.length != 4)
            throw new IllegalArgumentException("Char array must contain 4 elements!");

        byte[] bytes = new byte[data.length];
        for (int i = 0; i < bytes.length; i++) {
            char c = data[i];
            if (!charToSegment.containsKey(c))
                throw new IllegalArgumentException(
                        "The char '" + c + "' is not supported! You need to add it beforehand.");
            bytes[i] = charToSegment.get(c);
        }
        lastData = data;

        // Enable 8th bit on second segment for double point
        if (showDoublePoint)
            bytes[1] += 0x80;
        setSegments(bytes);
    }

    /**
     * Activates/Deactivates the double point
     *
     * @param showDoublePoint whether the double point should be activated
     * @param autoFlush       whether the segments should be updated
     */
    public void setShowDoublePoint(boolean showDoublePoint, boolean autoFlush) {
        this.showDoublePoint = showDoublePoint;
        if (autoFlush)
            show(lastData);
    }

    /**
     * Get whether the double point is activated
     *
     * @return whether the double point is activated
     */
    public boolean isShowDoublePoint() {
        return showDoublePoint;
    }

    /**
     * Sets a new brightness value
     *
     * @param brightness the new brightness value
     * @param autoFlush  whether the segments should be updated
     */
    public void setBrightness(int brightness, boolean autoFlush) {
        if (brightness < 0 || brightness > 15)
            throw new IllegalArgumentException("Illegal brightness '" + brightness + "' for range 0-15!");

        this.brightness = (byte) brightness;
        if (autoFlush)
            show(lastData);
    }

    /**
     * Get the current brightness value
     *
     * @return the current brightness value
     */
    public int getBrightness() {
        return brightness;
    }

    /**
     * Add a new character to the character mapping
     *
     * @param character the character
     * @param value     the set segments in byte form (0b0GFEDCBA)
     */
    public void addCharacter(char character, byte value) {
        charToSegment.put(character, value);
    }

    /**
     * Adds a 1ms delay
     */
    private void delay() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Whether the controller is ready
     */
    private static boolean ready = false;

    /**
     * Initializes the controller
     *
     * @param numberingScheme the numbering scheme to use
     * @param clkPin          the clock pin
     * @param dioPin          the data i/o pin
     * @return whether the setup was successful
     */
    public static boolean setup(NumberingScheme numberingScheme, int clkPin, int dioPin) {
        if (ready)
            throw new IllegalArgumentException("Controller is ready!");
        if (numberingScheme == null)
            throw new IllegalArgumentException("Numbering scheme may not be null!");

        instance.clk = clkPin;
        instance.dio = dioPin;
        initMap();

        int success = 0;
        if (numberingScheme == NumberingScheme.BROADCOM) {
            success = Gpio.wiringPiSetupGpio();
        } else if (numberingScheme == NumberingScheme.WIRING_PI) {
            success = Gpio.wiringPiSetup();
        } else if (numberingScheme == NumberingScheme.PHYSICAL) {
            success = Gpio.wiringPiSetupPhys();
        }

        if (success != 0)
            return false;

        Gpio.pinMode(clkPin, Gpio.INPUT);
        Gpio.pinMode(dioPin, Gpio.INPUT);
        Gpio.digitalWrite(clkPin, Gpio.LOW);
        Gpio.digitalWrite(dioPin, Gpio.LOW);

        return ready = true;
    }

    /**
     * Returns the instance
     *
     * @return the instance
     */
    public static TM1637 getInstance() {
        if (!ready)
            throw new IllegalStateException(
                    "Call TM1637.setup(numberingScheme, clkPin, dioPin) before accessing the TM1637 controller!");
        return instance;
    }

    /**
     * Initializes the character map
     */
    private static void initMap() {
        charToSegment.put('0', (byte) 0b0111111);
        charToSegment.put('1', (byte) 0b0000110);
        charToSegment.put('2', (byte) 0b1011011);
        charToSegment.put('3', (byte) 0b1001111);
        charToSegment.put('4', (byte) 0b1100110);
        charToSegment.put('5', (byte) 0b1101101);
        charToSegment.put('6', (byte) 0b1111101);
        charToSegment.put('7', (byte) 0b0000111);
        charToSegment.put('8', (byte) 0b1111111);
        charToSegment.put('9', (byte) 0b1101111);

        charToSegment.put('A', (byte) 0b1110111);
        charToSegment.put('B', (byte) 0b1111111);
        charToSegment.put('C', (byte) 0b0111001);
        charToSegment.put('D', (byte) 0b0111111);
        charToSegment.put('E', (byte) 0b1111001);
        charToSegment.put('F', (byte) 0b1110001);

        charToSegment.put(' ', (byte) 0b0000000);
        charToSegment.put('-', (byte) 0b1000000);
        charToSegment.put('_', (byte) 0b0001000);
    }

}