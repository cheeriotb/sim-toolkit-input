/*
 *  Copyright (C) 2019 cheeriotb <cheerio.the.bear@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the MIT license.
 *  See the license information described in LICENSE file.
 */

package com.github.cheeriotb.toolkit.input;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.Util;

import sim.toolkit.ProactiveHandler;
import sim.toolkit.ProactiveResponseHandler;
import sim.toolkit.ToolkitConstants;
import sim.toolkit.ToolkitException;
import sim.toolkit.ToolkitInterface;
import sim.toolkit.ToolkitRegistry;

public class InputApplet extends Applet implements ToolkitInterface, ToolkitConstants {

    private static byte[] MENU_ENTRY = new byte[] { 'G', 'e', 't', ' ', 'I', 'n', 'p', 'u', 't' };

    private static byte[] ITEM_SETTINGS = new byte[] { 'S', 'e', 't', 't', 'i', 'n', 'g', 's' };
    private static byte[] ITEM_REQUEST_SYNC = new byte[] { 'R', 'e', 'q', 'u', 'e', 's', 't', ' ',
            '(', 's', 'y', 'n', 'c', ')' };

    private static byte[] DEFAULT_TEXT = new byte[] { 'E', 'n', 't', 'e', 'r', ' ', 't', 'h', 'e',
            ' ', 'c', 'o', 'd', 'e' };
    private static short MAX_TEST_SIZE = (short) 0x80;

    private Object[] ITEMS = {
        ITEM_SETTINGS,
        ITEM_REQUEST_SYNC
    };

    private byte mQualifier;
    private byte[] mText;
    private short mTextLength;
    private short mMin;
    private short mMax;

    private InputApplet() {
        ToolkitRegistry registry = ToolkitRegistry.getEntry();
        registry.initMenuEntry(MENU_ENTRY, (short) 0, (short) MENU_ENTRY.length,
                PRO_CMD_SELECT_ITEM,  // byte nextAction
                false,                // boolean helpSupported
                (byte) 0,             // byte iconQualifier
                (short) 0             // short iconIdentifier
                );

        // Set the default parameters for GET INPUT command
        mQualifier = (byte) 0x00;
        mText = new byte[MAX_TEST_SIZE];
        mTextLength = (short) DEFAULT_TEXT.length;
        Util.arrayCopy (DEFAULT_TEXT, (short) 0, mText, (short) 0 , mTextLength);
        mMin = mMax = 4;
    }

    public static void install(byte[] buffer, short offset, byte length) {
        InputApplet applet = new InputApplet();
        applet.register();
    }

    public void process(APDU arg0) throws ISOException {
        // So far there is nothing to do here.
    }

    public void processToolkit(byte event) throws ToolkitException {
        ProactiveResponseHandler response = ProactiveResponseHandler.getTheHandler();

        if (event == EVENT_MENU_SELECTION) {
            do {
                selectItem();

                if (response.getGeneralResult() != (byte) 0x00) {
                    break;
                }

                switch (response.getItemIdentifier()) {
                    case 1:  // ITEM_SETTINGS
                        break;
                    case 2:  // ITEM_REQUEST_SYNC
                        getInput();
                        break;
                    default:
                        break;
                }
            } while (true);
        }
    }

    private void selectItem() {
        ProactiveHandler command = ProactiveHandler.getTheHandler();

        /*
           Command Qualifier for SELECT ITEM

           bit 1: 0 = presentation type is not specified;
                  1 = presentation type is specified in bit 2.
           bit 2: 0 = presentation as a choice of data values if bit 1 = '1';
                  1 = presentation as a choice of navigation options if bit 1 is '1'.
           bit 3: 0 = no selection preference;
                  1 = selection using soft key preferred.
           bit 8: 0 = no help information available;
                  1 = help information available.
        */
        command.init((byte) PRO_CMD_SELECT_ITEM, (byte) 0, DEV_ID_ME);
        command.appendTLV((byte) (TAG_ALPHA_IDENTIFIER | TAG_SET_CR), MENU_ENTRY, (short) 0,
                (short) MENU_ENTRY.length);
        for (short index = 0; index < ITEMS.length; index++) {
            command.appendTLV((byte) (TAG_ITEM | TAG_SET_CR), (byte) (index + 1),
                    (byte[]) ITEMS[index], (short) 0, (short) ((byte[]) ITEMS[index]).length);
        }
        command.send();
    }

    private void getInput() {
        ProactiveHandler command = ProactiveHandler.getTheHandler();

        /*
           Command Qualifier for GET INPUT

           bit 1: 0 = digit (0 to 9, *, # and L) only;
                  1 = alphabet set.
           bit 2: 0 = SMS default alphabet;
                  1 = UCS2 alphabet.
           bit 3: 0 = terminal may echo user input on the display;
                  1 = user input shall not be revealed in any way (see note).
           bit 4: 0 = user input to be in unpacked format;
                  1 = user input to be in SMS packed format.
           bit 8: 0 = no help information available;
                  1 = help information available.
        */
        command.initGetInput(mQualifier, DCS_8_BIT_DATA, mText, (short) 0, mTextLength, mMin, mMax);
        command.send();
    }

    private void displayText(byte[] buffer) {
        ProactiveHandler command = ProactiveHandler.getTheHandler();

        /*
           Command Qualifier for DISPLAY TEXT

           bit 1: 0 = normal priority;
                  1 = high priority
           bit 8: 0 = clear message after a delay;
                  1 = wait for user to clear message.
        */
        command.initDisplayText((byte) 0, DCS_8_BIT_DATA, buffer, (short) 0, (short) buffer.length);
        command.send();
    }
}
