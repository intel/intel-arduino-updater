/*
MacosxZmodemService.java this class is part of Galileo Firmware Update tool 
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
