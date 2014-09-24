/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.galileo.flash.tool;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jssc.SerialPortList;

/**
 *
 */
public class WindowsZmodemService extends SerialCommunicationService {
    
    private String[] windowsResources = {
        "bash.exe",
        "cyggcc_s-1.dll",
        "cygiconv-2.dll",
        "cygintl-8.dll",
        "cygncurses++w-10.dll",
        "cygncursesw-10.dll",
        "cygreadline7.dll",
        "cygwin1.dll",
        "grep.exe",
        "lsz.exe",
        "md5sum.exe",
        "strings.exe",
        "upgrade.sh",
    };

    @Override
    public boolean isSupportedOnThisOS() {
                return System.getProperty(OS_PROPERTY_KEY).toLowerCase()
                .contains("windows");
    }

    @Override
    protected String getOSResourcePath() {
        return "os/windows/";
    }

    @Override
    public List<String> getAvailableConnections() {
        String[] names = SerialPortList.getPortNames();
        return Arrays.asList(names);
    }
    
    private String portName;
    private boolean resourcesInstalled = false;
    
    @Override
    public boolean openConnection(String portName) {
        if (super.openConnection(portName)) {
            try {
                if (! resourcesInstalled) {
                    for (String name : windowsResources) {
                        copyZmodemResource(name);
                    }
                    resourcesInstalled = true;
                }
                this.portName = portName;
            } catch (IOException ioe) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ioe);
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String sendCommand(String cmd) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void sendFile(File f, FileProgress p) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
