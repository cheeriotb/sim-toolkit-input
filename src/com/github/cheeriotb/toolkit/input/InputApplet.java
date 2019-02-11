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
import sim.toolkit.ToolkitConstants;
import sim.toolkit.ToolkitException;
import sim.toolkit.ToolkitInterface;
import sim.toolkit.ToolkitRegistry;

public class InputApplet extends Applet implements ToolkitInterface, ToolkitConstants {

    private static byte[] MENU_ENTRY = new byte[] { 'G', 'e', 't', ' ', 'I', 'n', 'p', 'u', 't' };

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
        if (event == EVENT_MENU_SELECTION) {
            byte[] TEST = new byte[] { 'T', 'e', 's', 't' };
            displayText(TEST);
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
