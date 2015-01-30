/*
JsscZmodemService.java this class is part of Galileo Firmware Update tool 
Copyright (C) 2015 Intel Corporation

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.intel.galileo.flash.tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 * Implements a zmodem based serial communication service for the Galileo using
 * the JSSC library for the serial transport.
 */
public abstract class JsscZmodemService extends AbstractZmodemService {

    @Override
    protected Runnable createSerialOutputPipe(InputStream stdout, FileProgress progress) {
        return new SerialOutputPipe(stdout, progress);
    }

    @Override
    protected Runnable createSerialInputPipe(OutputStream stdin) {
        return new SerialInputPipe(stdin);
    }

    @Override
    protected boolean openSerialTransport(String portName) {
        port = new SerialPort(portName);
        try {
            return port.openPort();
        } catch (SerialPortException ex) {
            getLogger().severe(ex.getMessage());
        }
        return false;
    }

    @Override
    protected void closeSerialTransport() {
        if (port != null) {
            try {
                port.closePort();
            } catch (SerialPortException ex) {
                getLogger().severe(ex.getMessage());
            }
            port = null;
        }
    }

    @Override
    protected boolean isSerialTransportOpen() {
        return (port != null) && port.isOpened();
    }

    protected SerialPort port;

    private class SerialOutputPipe implements Runnable {

        private final InputStream in;
        private final FileProgress progress;

        SerialOutputPipe(InputStream in, FileProgress progress) {
            this.in = in;
            this.progress = progress;
        }

        @Override
        public void run() {
            int nsent = 0;
            try {
                for (int b = in.read(); b >= 0; b = in.read()) {
                    port.writeByte((byte) b);
                    if (progress != null) {
                        nsent += 1;
                        if ((nsent % 1024) == 0) {
                            progress.bytesSent(nsent);
                        }
                    }
                }
                if (progress != null) {
                    progress.bytesSent(nsent);
                }
            } catch (IOException ioe) {
                getLogger().severe(ioe.getMessage());
            } catch (SerialPortException e) {
                getLogger().severe(e.getMessage());
            }
        }

    }

    private class SerialInputPipe implements Runnable {

        private final OutputStream out;

        SerialInputPipe(OutputStream out) {
            this.out = out;
        }

        @Override
        public void run() {
            try {
                for (byte[] bytes = port.readBytes(); !quit; bytes = port.readBytes()) {
                    if (bytes != null) {
                        out.write(bytes);
                        out.flush();
                    } else {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                            quit = true;
                        }
                    }
                }

            } catch (IOException ioe) {
                getLogger().severe(ioe.getMessage());
            } catch (SerialPortException e) {
                getLogger().severe(e.getMessage());
            }
        }

    }

}
