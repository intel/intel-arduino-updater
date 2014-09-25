/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
        
    private SerialPort port;
    
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
                    port.writeByte((byte)b);
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
            byte[] buff = new byte[1024];
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
