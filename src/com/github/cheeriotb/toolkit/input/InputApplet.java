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

    private static final byte[] MENU_ENTRY = new byte[] {
            'G', 'e', 't', ' ', 'I', 'n', 'p', 'u', 't'
    };

    private static final byte[] ITEM_SETTINGS = new byte[] {
            'S', 'e', 't', 't', 'i', 'n', 'g', 's'
    };
    private static final byte[] ITEM_REQUEST_SYNC = new byte[] {
            'R', 'e', 'q', 'u', 'e', 's', 't', ' ', '(', 's', 'y', 'n', 'c', ')'
    };
    private static final byte[] ITEM_REQUEST_ASYNC = new byte[] {
            'R', 'e', 'q', 'u', 'e', 's', 't', ' ', '(', 'a', 's', 'y', 'n', 'c', ')'
    };

    private static final byte[] DEFAULT_TEXT = new byte[] {
            'E', 'n', 't', 'e', 'r', ' ', 't', 'h', 'e', ' ', 'c', 'o', 'd', 'e'
    };
    private static final short MIN_TEST_SIZE = (short) 0x01;
    private static final short MAX_TEST_SIZE = (short) 0x80;

    private static final byte[] QUERY_DIGIT_ONLY = new byte[] {
            'D', 'i', 'g', 'i', 't', ' ', '(', '0', ' ', 't', 'o', ' ', '9', ',', ' ', '*', ',',
            ' ', '#', ' ', 'a', 'n', 'd', ' ', '+', ')', ' ', 'o', 'n', 'l', 'y', '?'
    };
    private static final byte[] QUERY_HIDE_INPUT = new byte[] {
            'U', 's', 'e', 'r', ' ', 'i', 'n', 'p', 'u', 't', ' ', 's', 'h', 'a', 'l', 'l', ' ',
            'n', 'o', 't', ' ', 'b', 'e', ' ', 'r', 'e', 'v', 'e', 'a', 'l', 'e', 'd', '?'
    };
    private static final byte[] QUERY_MIN = new byte[] {
            'M', 'i', 'n', 'i', 'm', 'u', 'm', ' ', 'l', 'e', 'n', 'g', 't', 'h', '?', ' ', '(',
            '1', ' ', '-', ' ', '9', ')'
    };
    private static final byte[] QUERY_MAX = new byte[] {
            'M', 'a', 'x', 'i', 'm', 'u', 'm', ' ', 'l', 'e', 'n', 'g', 't', 'h', '?', ' ', '(',
            '1', ' ', '-', ' ', '9', ')'
    };
    private static final byte[] QUERY_TEXT = new byte[] {
            'T', 'e', 'x', 't', '?'
    };

    private static final byte[] WAITING_TIMER = new byte[] {
            'W', 'a', 'i', 't', 'i', 'n', 'g', ' ', 'f', 'o', 'r', ' ', 't', 'h', 'e', ' ', 't',
            'i', 'm', 'e', 'o', 'u', 't'
    };
    private static final byte[] RESOURCE_UNAVAILABLE = new byte[] {
            'R', 'e', 's', 'o', 'u', 'r', 'c', 'e', ' ', 'u', 'n', 'a', 'v', 'a', 'i', 'l', 'a',
            'b', 'l', 'e'
    };

    private static final short MIN_LENGTH = (short) 0x01;
    private static final short MAX_LENGTH = (short) 0x09;

    private static final byte CMD_QUALIFIER_YES_NO = (byte) 0x04;

    private static final byte CMD_QUALIFIER_ALPHABET = (byte) 0x01;
    private static final byte CMD_QUALIFIER_HIDE = (byte) 0x04;

    private static final byte GET_INKEY_YES = (byte) 0x01;
    private static final byte GET_INKEY_NO = (byte) 0x00;
    private static final byte GET_INKEY_OTHER = (byte) 0xFF;

    private Object[] ITEMS = {
        ITEM_SETTINGS,
        ITEM_REQUEST_SYNC,
        ITEM_REQUEST_ASYNC
    };

    private byte mQualifier;
    private byte[] mText;
    private short mTextLength;
    private short mMin;
    private short mMax;

    private byte mTimerId = (byte) 0x00;
    private static byte[] TIMER_VALUE = new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0x30 };

    private static short POSITION_TIMER_VALUE = (short) 0x06;
    private byte[] mTimerAllocated = new byte[] { 'T', 'i', 'm', 'e', 'r', ' ', '0', ' ', 'a', 'l',
            'l', 'o', 'c', 'a', 't', 'e', 'd' };

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
        boolean stayAtSecondaryMenu = true;
        ProactiveResponseHandler response = ProactiveResponseHandler.getTheHandler();

        if (event == EVENT_MENU_SELECTION) {
            do {
                if (mTimerId == (byte) 0x00) {
                    selectItem();

                    if (response.getGeneralResult() != (byte) 0x00) {
                        break;
                    }

                    switch (response.getItemIdentifier()) {
                        case 1:  // ITEM_SETTINGS
                            configure();
                            break;
                        case 2:  // ITEM_REQUEST_SYNC
                            getInput();
                            break;
                        case 3:  // ITEM_REQUEST_ASYNC
                            try {
                                startTimer();
                            } catch (Exception e) {
                                displayText(RESOURCE_UNAVAILABLE);
                            }
                            stayAtSecondaryMenu = false;
                            break;
                        default:
                            break;
                    }
                } else {
                    displayText(WAITING_TIMER);
                    stayAtSecondaryMenu = false;
                }
            } while (stayAtSecondaryMenu);
        } else if (event == EVENT_TIMER_EXPIRATION) {
            ToolkitRegistry.getEntry().releaseTimer(mTimerId);
            mTimerId = (byte) 0x00;
            getInput();
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

    private void configure() {
        byte input = GET_INKEY_OTHER;
        byte qualifier = (byte) 0x00;

        input = getInkey(CMD_QUALIFIER_YES_NO, QUERY_DIGIT_ONLY);
        if (input == GET_INKEY_NO) {
            qualifier |= CMD_QUALIFIER_ALPHABET;
        } else if (input == GET_INKEY_OTHER) {
            return;
        }

        input = getInkey(CMD_QUALIFIER_YES_NO, QUERY_HIDE_INPUT);
        if (input == GET_INKEY_YES) {
            qualifier |= CMD_QUALIFIER_HIDE;
        } else if (input == GET_INKEY_OTHER) {
            return;
        }

        short min = MIN_LENGTH;
        input = getInkey((byte) 0x00, QUERY_MIN);
        input -= (byte) '0';
        if (!(input < MIN_LENGTH) && !(input > MAX_LENGTH)) {
            min = input;
        } else {
            return;
        }

        short max = MAX_LENGTH;
        if (min != MAX_LENGTH) {
            input = getInkey((byte) 0x00, QUERY_MAX);
            input -= (byte) '0';
            if (!(input < min) && !(input > MAX_LENGTH)) {
                max = input;
            } else {
                return;
            }
        } else {
            max = min;
        }

        getInput(CMD_QUALIFIER_ALPHABET, QUERY_TEXT, (short) QUERY_TEXT.length, MIN_TEST_SIZE,
                MAX_TEST_SIZE);

        ProactiveResponseHandler response = ProactiveResponseHandler.getTheHandler();
        if (response.getGeneralResult() == (byte) 0x00) {
            mTextLength = response.getTextStringLength();
            if (mTextLength > 0) {
                response.copyTextString(mText, (short) 0);
                mQualifier = qualifier;
                mMin = min;
                mMax = max;
            }
        }
    }

    private byte getInkey(byte qualifier, byte[] text) {
        /*
           Command Qualifier for GET INPUT

           bit 1: 0 = digit (0 to 9, *, # and L) only;
                  1 = alphabet set.
           bit 2: 0 = SMS default alphabet;
                  1 = UCS2 alphabet.
           bit 3: 0 = character sets defined by bit 1 and bit 2 are enabled;
                  1 = character sets defined by bit 1 and bit 2 are disabled
                      and the "Yes/No" response is requested.
           bit 4: 0 = user response shall be displayed.
                      The terminal may allow alteration and/or confirmation;
                  1 = an immediate digit response (0 to 9, * and #) is requested.
           bit 8: 0 = no help information available;
                  1 = help information available.
        */

        ProactiveHandler command = ProactiveHandler.getTheHandler();
        command.initGetInkey(qualifier, DCS_8_BIT_DATA, text, (short) 0,
                (short) text.length);
        command.send();

        ProactiveResponseHandler response = ProactiveResponseHandler.getTheHandler();
        if (response.getGeneralResult() == (byte) 0x00) {
            if (response.findTLV(TAG_TEXT_STRING, (byte) 1) == TLV_FOUND_CR_SET) {
                if (response.getValueLength() > 1) {
                    // Retrieve the second byte because the first byte indicates the DCS.
                    return response.getValueByte((short) 1);
                }
            }
        }

        return GET_INKEY_OTHER;
    }

    private void getInput() {
        getInput(mQualifier, mText, mTextLength, mMin, mMax);
    }

    private void getInput(byte qualifier, byte[] text, short textLength, short min, short max) {
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
        command.initGetInput(qualifier, DCS_8_BIT_DATA, text, (short) 0, (short) textLength, min,
            max);
        if (text != mText) {
            command.appendTLV((byte) (TAG_DEFAULT_TEXT | TAG_SET_CR), DCS_8_BIT_DATA, mText,
                    (short) 0, mTextLength);
        }
        command.send();
    }

    private void startTimer() throws ToolkitException {
        byte timerId = ToolkitRegistry.getEntry().allocateTimer();
        ProactiveHandler command = ProactiveHandler.getTheHandler();

        try {
            /*
               Command Qualifier for TIMER MANAGEMENT

               bit 1 to 2: 00 = start;
                           01 = deactivate;
                           10 = get current value.
            */
            command.init((byte) PRO_CMD_TIMER_MANAGEMENT, (byte) 0, DEV_ID_ME);
            command.appendTLV((byte) (TAG_TIMER_IDENTIFIER | TAG_SET_CR), timerId);
            command.appendTLV((byte) (TAG_TIMER_VALUE | TAG_SET_CR), TIMER_VALUE, (short) 0,
                    (short) TIMER_VALUE.length);
            command.send();

            mTimerId = timerId;
            mTimerAllocated[POSITION_TIMER_VALUE] = (byte) ('0' + timerId);
            displayText(mTimerAllocated);
        } catch (Exception e) {
            ToolkitRegistry.getEntry().releaseTimer(timerId);
        }
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
        command.appendTLV((byte) (TAG_IMMEDIATE_RESPONSE | TAG_SET_CR), buffer, (short) 0,
                (short) 0);
        command.send();
    }
}
