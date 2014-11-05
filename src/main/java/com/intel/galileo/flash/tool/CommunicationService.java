/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.intel.galileo.flash.tool;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Abstracts a connection to a board as a discoverable service.
 *
 */
public abstract class CommunicationService {

    /**
     * Fetch the list of services that are currently available on the classpath.
     * Services are discovered using the java.util.ServiceLoader mechanism.
     * See the javadoc for that class to figure out how to register new 
     * services.
     * 
     * @return 
     */
    public static List<CommunicationService> getCommunicationServices() {
        List<CommunicationService> services = new LinkedList<CommunicationService>();
        Class<CommunicationService> c = CommunicationService.class;
        ServiceLoader<CommunicationService> available = ServiceLoader.load(c);
        for (CommunicationService link : available) {
            if (link.isSupportedOnThisOS()) {
                services.add(link);
            }
        }
        return services;
    }
    
    /**
     * Name of the communication service.
     * @return 
     */
    public abstract String getServiceName();
    
    /**
     * Fetch the list of possible connections, if the connections can in some
     * way be discovered (or perhaps if they have been used before).
     * @return 
     */
    public abstract List<String> getAvailableConnections();
    
    /**
     * Fetch the label to use to describe the connections returned by 
     * getAvailableConnections().  This might be something line serial
     * ports, ip addresses, etc.
     * @return 
     */
    public abstract String getConnectionLabel();
    
    /**
     * Try the open the given connection.
     * @param connection
     * @return 
     */
    public abstract boolean openConnection(String connection);
    
    /**
     * Close the connection as its no longer needed. 
     */
    public abstract void closeConnection();
    
    /**
     * Is the connection currently open (and believed to be operating).
     * @return 
     */
    public abstract boolean isConnectionOpen();
    
    /**
     * Send a command to execute on the board.
     * @param cmd
     * @return The output of the command, or null if the command failed.
     * @throws java.lang.Exception problem sending the command.
     */
    public abstract String sendCommand(String cmd) throws Exception;
    
    /**
     * Send a command to execute on the board.
     * @param cmd
     * @return The output of the command, or null if the command failed.
     * @throws java.lang.Exception problem sending the command.
     */
    public abstract String sendCommandWithTimeout(String cmd, int timeout) throws Exception;
    
    /**
     * Send the given file to the board.
     * 
     * @param f  the file to send.
     * @param p  object to receive progress updates
     * @throws Exception 
     */
    public abstract void sendFile(File f, FileProgress p) throws Exception;
    
    @Override
    public String toString() {
        return getServiceName();
    }
    
    /**
     * Is the service supported on the os currently being run on.
     * 
     * @return 
     */
    public abstract boolean isSupportedOnThisOS();
    
    /**
     * Set the location for files used by the service.
     * 
     * @param dir 
     */
    public abstract void setFileLocation(File dir);
    
    /**
     * Is progress update supported with file transfers?
     * @return 
     */
    public abstract boolean isProgressSupported();
    
    public interface FileProgress {
        
        /**
         * Called periodically to indicate how many bytes were sent.
         * 
         * @param nsent 
         */
        public void bytesSent(int nsent);
    }
}
