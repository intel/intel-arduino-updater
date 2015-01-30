/*
AbstractZmodemService.java this class is part of Galileo Firmware Update tool 
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the CommunicationLink service to communicate with a 
 * Galileo board connected via a serial connection in a platform independent 
 * manner.  At the time of creation for this class, this is the default way to
 * connect to the board.  A zmodem program (lsz) is used as the transfer 
 * mechanism.  It is a native program that is unpacked into a temporary file
 * appropriate for the platform at runtime.
 * <p>
 * The lsz program communicates through stdio.  It is run as a child process
 * and the input/output is piped to a serial transport.  For commands, the 
 * output is emitted via stderr by lsz.  This is piped to a string buffer for
 * later processing if desired.
 */
public abstract class AbstractZmodemService extends CommunicationService {

        
    @Override
    public String getServiceName() {
        return "Serial Connection Service";
    }

    @Override
    public String getConnectionLabel() {
        return "Port:";
    }

    @Override
    public final boolean isConnectionOpen() {
        return (zmodemDir != null) && (zmodem != null) && isSerialTransportOpen();
    }

    @Override
    public void setFileLocation(File dir) {
        zmodemDir = dir;
        try {
            zmodem = installResources();
        } catch (IOException ex) {
            Logger.getLogger(AbstractZmodemService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Implemented to create a temporary directory for the native zmodem
     * support.  Subclasses must actually populate it.
     * 
     * @param portName
     * @return 
     */
    @Override
    public final boolean openConnection(String portName) {
        try {
            if (zmodemDir == null) {
                File f = File.createTempFile("bogus", "");
                File tmpDir = f.getParentFile();
                f.delete();
                zmodemDir = new File(tmpDir, "zmodem");
                zmodemDir.mkdir();
                zmodemDir.deleteOnExit();
                zmodem = installResources();
            }
            return openSerialTransport(portName);
        } catch (IOException ex) {
            zmodemDir = null;
            Logger.getLogger(AbstractZmodemService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    /**
     * Subclasses should implement to install the zmodem program and any 
     * support files needed to the zmodemDir temporary directory.
     * 
     * @return  the location of the native zmodem program to run
     * @throws IOException 
     */
    protected abstract File installResources() throws IOException;
        
    @Override
    public final void closeConnection() {
        closeSerialTransport();
    }

    /**
     * Run the native zmodem program which uses stdio for communication and 
     * pipe its input/output to the given serial device.
     * 
     * @param remoteCommand  The remote command to execute
     * @return  The output of the command.
     * @throws Exception 
     */
    @Override
    public final String sendCommand(String remoteCommand) throws Exception {
        List<String> cmd = new LinkedList<String>();
        cmd.add(zmodem.getAbsolutePath());
        cmd.add("--escape");
        cmd.add("--verbose");
        cmd.add("-c");
        cmd.add(remoteCommand);
        return zmodemOperation(cmd, null);
    }
    
    /**
     * Run the native zmodem program which uses stdio for communication and 
     * pipe its input/output to the given serial device.
     * 
     * @param remoteCommand  The remote command to execute
     * @return  The output of the command.
     * @throws Exception 
     */
    @Override
    public final String sendCommandWithTimeout(String remoteCommand, int timeout) throws Exception {
        List<String> cmd = new LinkedList<String>();
        cmd.add(zmodem.getAbsolutePath());
        cmd.add("--escape");
        cmd.add("--verbose");
        cmd.add("-c");
        cmd.add(remoteCommand);
        return zmodemOperationWithTimeout(cmd, null, timeout);
    }

    @Override
    public void sendFile(File f, FileProgress p) throws Exception {
        List<String> cmd = new LinkedList<String>();
        cmd.add(zmodem.getPath().replace("\\", "/"));
        cmd.add("--escape");
        cmd.add("--binary");
        cmd.add("--overwrite");
        cmd.add("--verbose");
        cmd.add(f.getName());
        getLogger().info(cmd.toString());
        zmodemOperation(cmd, p);
    }

    @Override
    public boolean isProgressSupported() {
        return true;
    }
    
    /**
     * Create a Runnable that can be placed in a thread to pipe output from the
     * lsz program to the serial output transport.  If progress isn't null, 
     * updates should be provided through that interface.
     * 
     * @param stdout  the standard output of the lsz program.
     * @param progress
     * @return 
     */
    protected abstract Runnable createSerialOutputPipe(InputStream stdout, FileProgress progress);
    
    protected abstract Runnable createSerialInputPipe(OutputStream stdin);
    
    protected abstract boolean openSerialTransport(String portName);
    
    protected abstract void closeSerialTransport();
    
    protected abstract boolean isSerialTransportOpen();
    
    protected String zmodemOperation(List<String> cmd, FileProgress progress) throws Exception {
        ProcessBuilder pb = createProcessBuilder(cmd);
        quit = false;
        final Process p = pb.start();
        RemoteOutputPipe outputReader = new RemoteOutputPipe(p.getErrorStream());
        Thread serialOut = new Thread(createSerialOutputPipe(p.getInputStream(), progress));
        serialOut.setName("serial-output");
        serialOut.start();
        Thread serialIn = new Thread(createSerialInputPipe(p.getOutputStream()));
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
    
    protected String zmodemOperationWithTimeout(List<String> cmd, FileProgress progress, int timeout) throws Exception {
    	System.out.println("zmodemOperationWithTimeout");
        ProcessBuilder pb = createProcessBuilder(cmd);
        quit = false;
        Process p = pb.start();
        
        RemoteOutputPipe outputReader = new RemoteOutputPipe(p.getErrorStream());
        Thread serialOut = new Thread(createSerialOutputPipe(p.getInputStream(), progress));
        serialOut.setName("serial-output");
        serialOut.start();
        Thread serialIn = new Thread(createSerialInputPipe(p.getOutputStream()));
        serialIn.setName("serial-input");
        serialIn.start();
        Thread t = new Thread(outputReader);
        t.setName("output-sucker");
        t.start();
        
        try{
	        ProcessManager pm = new ProcessManager(p);
	        Thread thread = new Thread(pm);
	        thread.start();
	        pm.waitForOrKill(timeout);
        }
        catch(Exception e){
        	System.out.println(e.getMessage());
        }
        System.out.println("Output was: " + outputReader.getOutput());

        return outputReader.getOutput();
    }
    
    
    /**
     * Create the ProcessBuilder used to create a child process to run the lsz
     * program.  This is a hook for subclasses to customize the creation of the
     * process.
     * 
     * @param cmd
     * @return 
     */
    protected ProcessBuilder createProcessBuilder(List<String> cmd) {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(zmodemDir);
        return pb;
    }

    protected Logger getLogger() {
        return Logger.getLogger(AbstractZmodemService.class.getName());
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
    
    /**
     * Opens a stream to fetch the given resource name.  The name should 
     * not include the OS as that gets added by this method.
     * 
     * @param name  path of the resource (below the OS part of the path).
     * @return  The stream to read the resource, or null if it can't be read.
     */
    private InputStream getZmodemResource(String name) {        
        String path = getOSResourcePath() + name;
        return getClass().getResourceAsStream(path);
    }
    
    /**
     * Copy a resource to the zmodem temporary directory.
     * 
     * @param name
     * @return
     * @throws IOException 
     */
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
    
    protected volatile boolean quit = false;
    
    /**
     * Temporary directory where the native zmodem program and its supporting
     * files are installed.
     */
    protected File zmodemDir;
    
    /**
     * Location of the actual zmodem executable (lsz).
     */
    private File zmodem;
    
    /**
     * Runnable to capture the output of remote commands 
     * (basically capture stderr from the lsz program).
     */
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
    
}
