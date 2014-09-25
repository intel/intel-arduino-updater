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

/**
 * Implements a zmodem based serial connection service for Mac OS/X
 */
public class MacosxZmodemService extends JsscZmodemService {

    @Override
    public boolean isSupportedOnThisOS() {
        return System.getProperty(OS_PROPERTY_KEY).toLowerCase()
                .contains("mac");
    }

    @Override
    protected String getOSResourcePath() {
        return "/os/macosx/";
    }
    
    @Override
    public List<String> getAvailableConnections() {
        List<String> devices = new LinkedList<String>();
        File dev = new File("/dev");
        for (File f : dev.listFiles()) {
            String name = f.getName();
            if (name.startsWith("cu.usbmodem")) {
                devices.add(f.getAbsolutePath());
            }
        }
        return devices;
    }

    @Override
    protected File installResources() throws IOException {
        File z = copyZmodemResource("lsz");
        z.setExecutable(true);
        return z;
    }

}
