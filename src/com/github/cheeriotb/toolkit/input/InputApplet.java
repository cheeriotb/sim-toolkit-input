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

    private Object[] ITEMS = {
        ITEM_SETTINGS,
        ITEM_REQUEST_SYNC
    };

    private InputApplet() {
        ToolkitRegistry registry = ToolkitRegistry.getEntry();
        registry.initMenuEntry(MENU_ENTRY, (short) 0, (short) MENU_ENTRY.length,
                PRO_CMD_SELECT_ITEM,  // byte nextAction
                false,                // boolean helpSupported
                (byte) 0,             // byte iconQualifier
                (short) 0             // short iconIdentifier
                );
    }

    public static void install(byte[] buffer, short offset, byte length) {
        InputApplet applet = new InputApplet();
        applet.register();
    }

    public void processToolkit(byte event) throws ToolkitException {
        ProactiveHandler command = ProactiveHandler.getTheHandler();
        ProactiveResponseHandler response = ProactiveResponseHandler.getTheHandler();

        if (event == EVENT_MENU_SELECTION) {
            do {
                command.init((byte) PRO_CMD_SELECT_ITEM, (byte) 0, DEV_ID_ME);
                command.appendTLV((byte) (TAG_ALPHA_IDENTIFIER | TAG_SET_CR),
                        MENU_ENTRY, (short) 0, (short) MENU_ENTRY.length);
                for (short index = 0; index < ITEMS.length; index++) {
                    command.appendTLV((byte) (TAG_ITEM | TAG_SET_CR), (byte) (index + 1),
                            (byte[]) ITEMS[index], (short) 0,
                            (short) ((byte[]) ITEMS[index]).length);
                }
                command.send();

                if (response.getGeneralResult() != (byte) 0x00) {
                    break;
                }
            } while (true);
        }
    }

    public void process(APDU arg0) throws ISOException {
        // So far there is nothing to do here.
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
