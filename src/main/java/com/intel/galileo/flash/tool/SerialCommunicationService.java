/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.intel.galileo.flash.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * Implementation of the CommunicationLink service to communicate with a 
 * Galileo board connected via a serial connection in a platform independent 
 * manner.  At the time of creation for this class, this is the default way to
 * connect to the board.  A zmodem program (lsz) is used as the transfer 
 * mechanism.  It is a native program that is unpacked into a temporary file
 * appropriate for the platform at runtime.
 */
public class SerialCommunicationService extends CommunicationService {

    @Override
    public String getServiceName() {
        return "Serial Connection Service";
    }

    @Override
    public String getConnectionLabel() {
        return "Serial Ports";
    }

    @Override
    public boolean isConnectionOpen() {
        return (port != null) && port.isOpened();
    }

    
    @Override
    public List<String> getAvailableConnections() {
        String[] names;   
        if (isMacOS()) {
            Pattern p = Pattern.compile("cu.usbmodem*");
            names = SerialPortList.getPortNames(p);
        } else {
            names = SerialPortList.getPortNames();
        }
        return Arrays.asList(names);
    }
    
    @Override
    public boolean openConnection(String portName) {
        port = new SerialPort(portName);
        try {
            zmodem = getZmodemProgram();
            boolean ok = port.openPort() && (zmodem != null);
            return ok;
        } catch (SerialPortException ex) {
            getLogger().log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    @Override
    public void closeConnection() {
        if (port != null) {
            try {
                port.closePort();
            } catch (SerialPortException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
            port = null;
        }
    }
    
    @Override
    public String sendCommand(String remoteCommand) throws Exception {
        progress = null;
        List<String> args = new LinkedList<>();
        args.add("--escape");
        args.add("--verbose");
        args.add("-c");
        args.add(remoteCommand);
        return zmodemOperation(args);
    }
    
    private String zmodemOperation(List<String> args) throws Exception {
        List<String> cmd = new LinkedList<>();
        cmd.add(zmodem.getCanonicalPath());
        cmd.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        quit = false;
        final Process p = pb.start();
        RemoteOutputPipe outputReader = new RemoteOutputPipe(p.getErrorStream());
        Thread serialOut = new Thread(new SerialOutputPipe(p.getInputStream()));
        serialOut.setName("serial-output");
        serialOut.start();
        Thread serialIn = new Thread(new SerialInputPipe(p.getOutputStream()));
        serialIn.setName("serial-input");
        serialIn.start();
        Thread t = new Thread(outputReader);
        t.setName("output-sucker");
        t.start();
        int exit = p.waitFor();
        quit = true;
        if (exit != 0) {
            String msg = String.format("Remote command exited with %d\n", exit);
            getLogger().severe(msg);
            getLogger().log(Level.INFO, "Output was: {0}", outputReader.getOutput());
            throw new Exception(msg);
        }
        return outputReader.getOutput();
    }

    @Override
    public void sendFile(File f, FileProgress p) throws Exception {
        progress = p;
        List<String> args = new LinkedList<>();
        args.add("--escape");
        args.add("--binary");
        args.add("--overwrite");
        args.add("--verbose");
        args.add(f.getCanonicalPath());
        zmodemOperation(args);
    }
    
    Logger getLogger() {
        return Logger.getLogger(SerialCommunicationService.class.getName());
    }
    
    private File getZmodemProgram() {
        try {
            String prefix = "lsz";
            String suffix = isWindows() ? "exe" : "";
            try (InputStream zstream = getZmodemResource()) {
                File ztmp = copyResourceToTmpFile(zstream, prefix, suffix);
                ztmp.setExecutable(true);
                return ztmp;
            }
        } catch (IOException ioe) {
            getLogger().log(Level.SEVERE, null, ioe);
            // fall through to return null
        }
        return null;
    }
    
    private File copyResourceToTmpFile(InputStream res, String prefix, String suffix)
            throws IOException {

        File tmp = File.createTempFile(prefix, suffix);
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            byte buff[] = new byte[4096];
            for (int n = res.read(buff); n >= 0; n = res.read(buff)) {
                out.write(buff, 0, n);
            }
            
            out.flush();
            out.close();
            res.close();
        }
        tmp.deleteOnExit();
        return tmp;

    }
    
    private InputStream getZmodemResource() {        
        String path = getOSResourcePath() + getOSZmodemFilename();
        return getClass().getResourceAsStream(path);
    }
    
    private String getOSZmodemFilename() {
        if (isWindows()) {
            return "lsz.exe";
        }
        return "lsz";
    }
    
    private String getOSResourcePath() {
        String prefix = "/os/";
        if (isMacOS()) {
            return prefix + "macosx/";
        } else if (isWindows()) {
            return prefix + "windows/";
        } else if (isLinux()) {
            String arch = System.getProperty("os.arch");
            if (arch.contains("64")) {
             return prefix + "linux64/";               
            }
            return prefix + "linux32/";
        }
        throw new IllegalArgumentException("Unsupported OS");
    }
    
    private static final String OS_PROPERTY_KEY = "os.name";
    
    private static boolean isMacOS() {
        return System.getProperty(OS_PROPERTY_KEY).toLowerCase()
                .contains("mac");
    }

    private static boolean isWindows() {
        return System.getProperty(OS_PROPERTY_KEY).toLowerCase()
                .contains("windows");
    }

    private static boolean isLinux() {
        return System.getProperty(OS_PROPERTY_KEY).toLowerCase()
                .contains("linux");
    }

    boolean quit = false;
    private SerialPort port;
    private File zmodem;
    private FileProgress progress;
    
    private class RemoteOutputPipe implements Runnable {

        private final InputStream es;
        private final StringBuffer output;
        
        RemoteOutputPipe(InputStream es) {
            this.es = es;
            this.output = new StringBuffer();
        }

        public String getOutput() {
            return output.toString().trim();
        }
        
        @Override
        public void run() {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(es));
                
                String s;
                for (s = r.readLine(); s != null; s = r.readLine()) {
                    output.append(s);
                    output.append("\n");
                }
            } catch (IOException ioe) {
                getLogger().log(Level.SEVERE, null, ioe);
            }
        }
    }
    
    private class SerialOutputPipe implements Runnable {

        private final InputStream in;
        
        SerialOutputPipe(InputStream in) {
            this.in = in;
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
            } catch (IOException | SerialPortException e) {
                getLogger().log(Level.SEVERE, null, e);
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
                
            } catch (SerialPortException | IOException ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }

        }
        
    }
}
