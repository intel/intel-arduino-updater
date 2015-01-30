/*
FileDeviceZmodemService.java this class is part of Galileo Firmware Update tool 
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that uses regular java file i/o as a communication transport for the
 * zmodem protocol. This will only work on serial devices presented as a file in
 * the filesystem. The Galileo boards use USB CDC ACM so this works on Linux and
 * MacOSX.
 */
public abstract class FileDeviceZmodemService extends AbstractZmodemService {

    private File device;
    private InputStream serialIn;
    private OutputStream serialOut;

    @Override
    protected Runnable createSerialOutputPipe(InputStream stdout, FileProgress progress) {
        return new SerialOutputPipe(stdout, serialOut, progress);
    }

    @Override
    protected boolean openSerialTransport(String portName) {
        device = portToDevice(portName);
        if ((device != null) && (device.exists())) {
            try {
                serialIn = new FileInputStream(device);
                serialOut = new FileOutputStream(device);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FileDeviceZmodemService.class.getName()).log(Level.SEVERE, null, ex);
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isSerialTransportOpen() {
        return (serialIn != null) && (serialOut != null);
    }

    @Override
    protected void closeSerialTransport() {
        device = null;
        if (serialIn != null) {
            try {
                serialIn.close();
            } catch (IOException ignored) {
            }
            serialIn = null;
        }
        if (serialOut != null) {
             try {
                serialOut.close();
            } catch (IOException ignored) {
            }
            serialOut = null;
        }
    }

    @Override
    protected Runnable createSerialInputPipe(OutputStream stdin) {
        return new SerialInputPipe(stdin, serialIn);
    }

    /**
     * Map the port name to a device file.
     *
     * @param portName
     * @return
     */
    protected File portToDevice(String portName) {
        return new File(portName);

    }

    //new SerialInputPipe(p.getOutputStream(), in)
    /**
     * Copies standard output from the process and sends it to the serial
     * output.
     */
    private class SerialOutputPipe implements Runnable {

        private final InputStream in;
        private final OutputStream serialOut;
        private final FileProgress progress;

        SerialOutputPipe(InputStream stdout, OutputStream serialOut, FileProgress progress) {
            this.in = stdout;
            this.serialOut = serialOut;
            this.progress = progress;
        }

        @Override
        public void run() {
            int nsent = 0;
            try {
                for (int b = in.read(); b >= 0; b = in.read()) {
                    serialOut.write(b);
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
            } catch (IOException e) {
                String reason = e.getMessage();
                getLogger().severe(reason);
            } catch (Exception ex) {
            	String reason = ex.getMessage();
                getLogger().severe(reason);
            } finally {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }

        }

    }

    private class SerialInputPipe implements Runnable {

        private final OutputStream out;
        private final InputStream serialIn;

        SerialInputPipe(OutputStream out, InputStream serialIn) {
            this.out = out;
            this.serialIn = serialIn;
        }

        @Override
        public void run() {
            try {
                for (int b = serialIn.read(); b != -1; b = serialIn.read()) {
                    out.write(b);
                    out.flush();
                }
            } catch (IOException e) {
                String reason = e.getMessage();
                getLogger().severe(reason);
            } finally {
                try {
                    out.close();
                } catch (IOException ignored) {
                }
            }

        }

    }

}
