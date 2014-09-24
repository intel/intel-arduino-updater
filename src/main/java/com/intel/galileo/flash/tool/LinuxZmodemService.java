/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.galileo.flash.tool;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class LinuxZmodemService extends SerialCommunicationService {

    @Override
    public boolean isSupportedOnThisOS() {
        return System.getProperty(OS_PROPERTY_KEY).toLowerCase()
                .contains("linux");
    }

    @Override
    protected String getOSResourcePath() {
        String prefix = "/os/";
        String arch = System.getProperty("os.arch");
        if (arch.contains("64")) {
            return prefix + "linux64/";
        }
        return prefix + "linux32/";
    }

        @Override
    public List<String> getAvailableConnections() {
        List<String> devices = new LinkedList<String>();
        File dev = new File("/dev");
        for (File f : dev.listFiles()) {
            String name = f.getName();
            if (name.startsWith("ttyACM")) {
                devices.add(name);
            }
        }
        return devices;
    }
    
    @Override
    public boolean openConnection(String portName) {
        if (super.openConnection(portName)) {
            try {
                if (zmodem == null) {
                    zmodem = copyZmodemResource("lsz");
                    zmodem.setExecutable(true);
                }
                device = new File("/dev", portName);
                if (device.exists()) {
                    return true;
                }
            } catch (IOException ioe) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ioe);
            }
        }
        return false;
    }

    @Override
    public boolean isConnectionOpen() {
        return (super.isConnectionOpen() &&(zmodem != null) && (device != null));
    }
    
    @Override
    public void closeConnection() {
        device = null;
    }

    @Override
    public String sendCommand(String remoteCommand) throws Exception {
        return zmodemSendCommand(zmodem, remoteCommand, device);
    }

    @Override
    public void sendFile(File f, FileProgress p) throws Exception {
        zmodemSendFile(zmodem, f, p, device);
    }
    
    private File device;
    private File zmodem;

}
