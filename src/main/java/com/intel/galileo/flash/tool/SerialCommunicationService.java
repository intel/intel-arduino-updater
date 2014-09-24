/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.intel.galileo.flash.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;

/**
 * Implementation of the CommunicationLink service to communicate with a 
 * Galileo board connected via a serial connection in a platform independent 
 * manner.  At the time of creation for this class, this is the default way to
 * connect to the board.  A zmodem program (lsz) is used as the transfer 
 * mechanism.  It is a native program that is unpacked into a temporary file
 * appropriate for the platform at runtime.
 */
public abstract class SerialCommunicationService extends CommunicationService {

    protected File zmodemDir;
    
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
        return (zmodemDir != null);
    }
    
    /**
     * Implemented to create a temporary directory for the native zmodem
     * support.  Subclasses must actually populate it.
     * 
     * @param portName
     * @return 
     */
    @Override
    public boolean openConnection(String portName) {
        try {
            File f = File.createTempFile("bogus", "");
            File tmpDir = f.getParentFile();
            f.delete();
            zmodemDir = new File(tmpDir, "zmodem");
            zmodemDir.mkdir();
            zmodemDir.deleteOnExit();
        } catch (IOException ex) {
            zmodemDir = null;
            Logger.getLogger(SerialCommunicationService.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }
    
    @Override
    public void closeConnection() {
    }
        
    /**
     * Run the native zmodem program which uses stdio for communication and 
     * pipe its input/output to the given serial device.
     * 
     * @param zmodem  The path to the native zmodem program
     * @param remoteCommand  The remote command to execute
     * @param device  the serial device to use.
     * @return  The output of the command.
     * @throws Exception 
     */
    protected String zmodemSendCommand(File zmodem, String remoteCommand, File device) throws Exception {
        List<String> cmd = new LinkedList<String>();
        cmd.add(zmodem.getAbsolutePath());
        cmd.add("--escape");
        cmd.add("--verbose");
        cmd.add("-c");
        cmd.add(remoteCommand);
        return zmodemOperation(cmd, null, new FileOutputStream(device), new FileInputStream(device));
    }
    
    protected String zmodemOperation(List<String> cmd, FileProgress progress, OutputStream out, InputStream in) throws Exception {
        this.progress = progress;
        ProcessBuilder pb = new ProcessBuilder(cmd);
        quit = false;
        final Process p = pb.start();
        RemoteOutputPipe outputReader = new RemoteOutputPipe(p.getErrorStream());
        Thread serialOut = new Thread(new SerialOutputPipe(p.getInputStream(), out));
        serialOut.setName("serial-output");
        serialOut.start();
        Thread serialIn = new Thread(new SerialInputPipe(p.getOutputStream(), in));
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

    public void zmodemSendFile(File zmodem, File f, FileProgress p, File device) throws Exception {
        List<String> cmd = new LinkedList<String>();
        cmd.add(zmodem.getAbsolutePath());
        cmd.add("--escape");
        cmd.add("--binary");
        cmd.add("--overwrite");
        cmd.add("--verbose");
        cmd.add(f.getCanonicalPath());
        zmodemOperation(cmd, p, new FileOutputStream(device), new FileInputStream(device));
    }
    
    Logger getLogger() {
        return Logger.getLogger(SerialCommunicationService.class.getName());
    }
        
    private File copyResourceToTmpFile(InputStream res, File tmp)
            throws IOException {

        //File tmp = File.createTempFile(prefix, suffix);
        FileOutputStream out = new FileOutputStream(tmp);
        byte buff[] = new byte[4096];
        for (int n = res.read(buff); n >= 0; n = res.read(buff)) {
            out.write(buff, 0, n);
        }

        out.flush();
        out.close();
        res.close();

        tmp.deleteOnExit();
        return tmp;

    }
    
    private InputStream getZmodemResource(String name) {        
        String path = getOSResourcePath() + name;
        return getClass().getResourceAsStream(path);
    }
    
    File copyZmodemResource(String name) throws IOException {
        InputStream in = getZmodemResource(name);
        File tmp = new File(zmodemDir, name);
        return copyResourceToTmpFile(in, tmp);
    }
    
    /**
     * Fetch the resource path for resources to be used on this os.
     * @return 
     */
    protected abstract String getOSResourcePath();
    
    protected static final String OS_PROPERTY_KEY = "os.name";
    
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
            } finally {
                try {
                    es.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
    
    /**
     * Copies stdout from the process and sends it to the serial output.
     */
    private class SerialOutputPipe implements Runnable {

        private final InputStream in;
        private final OutputStream serialOut;
        
        SerialOutputPipe(InputStream stdout, OutputStream serialOut) {
            this.in = stdout;
            this.serialOut = serialOut;
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
                getLogger().log(Level.SEVERE, null, e);
            } finally {
                try {
                    in.close();
                    serialOut.close();
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
            byte[] buff = new byte[1024];
            try {
                for (int b = serialIn.read(); b != -1; b = serialIn.read()) {
                    out.write(b);                   
                    out.flush();
                }
           } catch (IOException e) {
                getLogger().log(Level.SEVERE, null, e);
            } finally {
                try {
                    out.close();
                    serialIn.close();
                } catch (IOException ignored) {
                }
            }

        }
        
    }
}
