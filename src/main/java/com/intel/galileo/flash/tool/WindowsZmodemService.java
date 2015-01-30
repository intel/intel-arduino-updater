/*
WindowsZmodemService.java this class is part of Galileo Firmware Update tool 
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import jssc.SerialPortList;

/**
 * TODO - Finish implementing this class (It isn't currently functional).
 */
public class WindowsZmodemService extends JsscZmodemService {
    
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
        return "/os/windows/";
    }

    @Override
    public List<String> getAvailableConnections() {
        String[] names = SerialPortList.getPortNames();
        return Arrays.asList(names);
    }
    
    @Override
    protected File installResources() throws IOException {
        for (String name : windowsResources) {
            copyZmodemResource(name);
        }
        for (String name : windowsResources) {
            File f = new File(zmodemDir, name);
            f.setExecutable(true);
        }
        return new File(zmodemDir, "lsz.exe");
    }

    static final String PATH_KEY = "Path";
    
    @Override
    protected ProcessBuilder createProcessBuilder(List<String> cmd) {
        ProcessBuilder pb = super.createProcessBuilder(cmd);
        Map<String,String> env = pb.environment();
        String path = env.get(PATH_KEY);
        path = (path != null) ? zmodemDir.getAbsolutePath() + ";" + path : 
                zmodemDir.getAbsolutePath();
        path += ";.";
        env.put(PATH_KEY, path);
        getLogger().info("process path: "+path);
        return pb;
    }

    @Override
    public void sendFile(File f, FileProgress p) throws Exception {
        port.closePort();
        List<String> cmd = new LinkedList<String>();
        String path = zmodemDir.getPath().replace("\\", "/");
        cmd.add(path + "/bash.exe");
        cmd.add("--verbose");
        cmd.add("--noprofile");
        cmd.add(path+"/upgrade.sh");
        cmd.add(path);
        cmd.add(f.getName());
        cmd.add(port.getPortName());
        getLogger().info(cmd.toString());
        zmodemOperation(cmd, p);
        port.openPort();
    }

    @Override
    public boolean isProgressSupported() {
        return false;
    }

    
    
}
