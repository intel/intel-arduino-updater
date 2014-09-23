/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.intel.galileo.flash.tool;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that implements the functionality for updating the firmware on a 
 * Galileo board over some kind of communication service.  The intent is to be
 * able to use an instance of this class as a model for an user interface, or
 * use it directly for a headless firmware installation.
 */
public class GalileoFirmwareUpdater {

    // TODO - do something other than hardwared default
    static final String DEFAULT_CAPSULE = "/capsules/sysimage-galileo-1.0.2.cap";
    
    public GalileoFirmwareUpdater() {
        this(null,null);
    }
    
    /**
     * Construct a firmware updater.  The first communication service 
     * discovered will be 
     * set as the current service.  If there is only one connection discovered
     * on that service, it will be set as the current connection.
     * 
     * @param s the service to use, or null if the first available service is to
     * be used as a default.
     * @param c the connection to use, or null and if only one connection is
     * available it will be used as a default.
     */
    public GalileoFirmwareUpdater(CommunicationService s, String c) {
        URL u = getClass().getResource(DEFAULT_CAPSULE);
        update = new FirmwareCapsule(u);
        capsules = new ArrayList<FirmwareCapsule>();
        capsules.add(update);
        services = CommunicationService.getCommunicationServices();
        if (!services.isEmpty()) {
            // default service
            CommunicationService service = (s != null) ? s : services.get(0);
            setCommunicationService(service);
            if (c != null) {
                // connection was specified
                setCommunicationConnection(c);
            } else {
                List<String> connections = service.getAvailableConnections();
                if (connections.size() == 1) {
                    // only one connection, try to use it.
                    String connection = connections.get(0);
                    setCommunicationConnection(connection);
                }

            }
        }
    }
    
    /**
     * Fetch the list of communication services that have been discovered to
     * communicate with the board.  This is independent of the actual connection
     * to the board which is established with the communicationConnection
     * property.
     * 
     * @return 
     */
    public List<CommunicationService> getCommunicationServices() {
        return services;
    }
    
    /**
     * Fetch the current communication service.
     * @return 
     */
    public CommunicationService getCommunicationService() {
        return communicationService;
    }

    /**
     * Set the current communication service.
     * 
     * @param communicationService 
     */
    public final synchronized void setCommunicationService(
            CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    /**
     * Fetch the name of the current connection set on the current communication
     * service.
     * 
     * @return 
     */
    public String getCommunicationConnection() {
        return communicationConnection;
    }

    /**
     * Set the name of the current connection set on the current communication
     * service.
     * @param communicationConnection 
     */
    public final synchronized void setCommunicationConnection(String communicationConnection) {
        this.communicationConnection = communicationConnection;
    }
        
    private static final String VERSION_COMMAND =
        "cat /sys/firmware/board_data/flash_version";
    private static final String TRANSFER_COMPLETE = "Transfer complete";
    

    /**
     * Fetch the version of the firmware currently on the board.  This is a
     * potentially lengthy operation as the board must be queried over the 
     * communication link.
     * 
     * @return 
     */
    public synchronized final GalileoVersion getCurrentBoardVersion() {
        if (currentBoardVersion == null) {
            if (communicationService == null) {
                throw new IllegalArgumentException("No communication service");
            }
            if (communicationConnection == null) {
                throw new IllegalArgumentException("No connection to board specified");
            }
            if (!communicationService.isConnectionOpen()) {
                if (!communicationService.openConnection(communicationConnection)) {
                    throw new IllegalArgumentException("Unable to open communication");
                }
            }
            
            try {
                String rawVersion = communicationService.sendCommand(VERSION_COMMAND);
                int endIndex = rawVersion.indexOf(TRANSFER_COMPLETE);
                if (endIndex > 0) {
                    rawVersion = rawVersion.substring(0, endIndex).trim();
                    currentBoardVersion = GalileoVersion.ofTargetString(rawVersion);
                }
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, null, ex);
            }
            communicationService.closeConnection();
        }
        return currentBoardVersion;
    }

    /**
     * Fetch the version of the update firmware.  If this returns null, the 
     * selected update is not a valid firmware and will not be installed.
     * This is a potentially lengthy operation as firmware may be downloaded
     * over a network connection.
     * @return 
     */
    public final GalileoVersion getUpdateVersion() {
        if (update == null) {
            throw new IllegalArgumentException("No update firmware selected");
        }
        if (capsuleVersion == null) {
            capsuleVersion = update.getVersion();
        }
        return capsuleVersion;
    }
    
    public final List<FirmwareCapsule> getAvailableFirmware() {
        return capsules;
    }

    public final FirmwareCapsule getUpdate() {
        return update;
    }

    /**
     * Set the desired firmware package for update.
     * @param update 
     */
    public synchronized final void setUpdate(FirmwareCapsule update) {
        this.update = update;
        capsuleVersion = null;
    }
    
    /**
     * Determines if attempting a firmware update even makes sense yet without
     * performing any lengthy operations.  If the board version is known and
     * the update firmware version is known, then we should be ready to go.
     * Having the board version implies we can communicate with it.  Having the
     * update firmware version implies we have examined the firmware.
     * @return 
     */
    public boolean isReadyForUpdate() {
        return (capsuleVersion != null) && (currentBoardVersion != null);
    }
    
    public Logger getLogger() {
        return Logger.getLogger(GalileoFirmwareUpdater.class.getName());
    }

    /**
     * Upload the desired firmware to the board.  Once on the board an md5sum 
     * command is run on the board on the file uploaded and compared to the 
     * md5 hash computed on the host.  If everything is ok the file is 
     * positioned on the board for the actual upgrade.  Once the file is in
     * place a script is run to trigger the upgrade and the board is rebooted.
     * After waiting for the board to become available for communication again,
     * the version is read from the board and compared to the version of the
     * firmware capsule.  If there are any problems along the way an exception
     * will be thrown.
     * 
     * @param progress  progress update receiver.  This may be null if no 
     *   notification of updates is desired.
     * @return true if the firmware was updated and the version could be read
     * back and successfully compared to the version of the update capsule.  If
     * the versions don't compare, false is returned.
     * @throws java.lang.Exception  if there is a problem uploading the firmware
     * or the board cannot be communicated with after the update has started
     * (i.e. a timeout occurs).
     */
    public synchronized boolean updateFirmwareOnBoard(ProgressNotification progress) throws Exception {
        this.progress = progress;
        if (update == null) {
            throw new IllegalArgumentException("No update was specified");
        }
        if (update.getCache() == null) {
            throw new IllegalArgumentException("There firmware can't be loaded from the specified URL");
        }
        if (update.getVersion() == null) {
            throw new IllegalArgumentException("Update doesn't appear to be valid");
        }
        if (!communicationService.isConnectionOpen()) {
            if (!communicationService.openConnection(communicationConnection)) {
                throw new IllegalArgumentException("Unable to open communication");
            }
        }
        Logger log = getLogger();
        try {
            log.log(Level.INFO, "Uploading: {0}", update.getCache());
            if (progress != null) {
                progress.updateMessage("Loading new firmware onto target.");
            }
            communicationService.sendFile(update.getCache(), new UploadProgressRelay());
            
            String remoteFilename = "/" + update.getCache().getName();
            String cmd = "md5sum " + remoteFilename;
            log.log(Level.INFO, "Checking uploaded file: {0}", cmd);           
            if (progress != null) {
                progress.updateMessage("Checking new firmware on target.");
            }
            String output = communicationService.sendCommand(cmd);
            log.log(Level.INFO, "MD5 result: {0}", output);
            int index = output.indexOf("/");            
            String remoteMD5 = (index > 0) ? 
                output.substring(0, index-1).trim() : output;
            if (! update.getMD5().equalsIgnoreCase(remoteMD5)) {
                // oops, they differ
                log.log(Level.INFO, "Expected: {0}", update.getMD5());
                log.log(Level.INFO, "Found: {0}", remoteMD5);
                log.severe("Upload failed");
                throw new Exception("Failed upload integrity check");
            }
            String msg = "Integrity good, preparing for upgrade...";
            log.info(msg);
            if (progress != null) {
                progress.updateMessage(msg);
            }
            
            cmd = "mkdir -p /tmp/spi_upgrade";
            log.info(cmd);
            communicationService.sendCommand(cmd);
            
            cmd = "mkdir -p /lib/firmware/tmp/spi_upgrade";
            log.info(cmd);
            communicationService.sendCommand(cmd);
            
            cmd = "mv "+remoteFilename+" /tmp/spi_upgrade/galileo_firmware.bin";
            log.info(cmd);
            communicationService.sendCommand(cmd);
            
            cmd = "cp /tmp/spi_upgrade/galileo_firmware.bin " +
                    "/lib/firmware/tmp/spi_upgrade/galileo_firmware.bin";
            log.info(cmd);
            communicationService.sendCommand(cmd);
            
            msg = "Preparations complete!";
            log.info(msg);
            if (progress != null) {
                progress.updateMessage(msg);
            }
            if (progress != null) {
                progress.updateMessage(msg);
            }
            cmd = "./opt/cln/galileo/start_spi_upgrade.sh";
            log.info(cmd);
            communicationService.sendCommand(cmd);
            cmd = "reboot";
            log.info(cmd);
            communicationService.sendCommand(cmd);
            
            msg = "Updating firmware...This may take up to 5 minutes.";
            log.info(msg);
            if (progress != null) {
                progress.updateMessage(msg);
            }            
        } finally {
            communicationService.closeConnection();
        }
        
        // wait for the board to finish updating and check the firmware version
        currentBoardVersion = null;
        
        if (progress != null) {
            progress.updateProgress(0);
        }
        long startTime = System.currentTimeMillis();
        final long duration = 6 * 60 * 1000; //5 minutes
        final long start_polling = 3 * 60 * 1000;//3 minutes
        int i = 0;
        for (;;) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException unused) {
            }
            long time = System.currentTimeMillis() - startTime;
            if (time > duration) {
                throw new Exception("Timeout waiting for board");
            }
            if (progress != null) {
                int percent = (int)(100 * ((float)time / duration));
                progress.updateProgress(percent);
            }
            if (time >= start_polling) {
                log.info("poll ...  " + ++i);
                if (communicationService.openConnection(communicationConnection)) {
                    // we can talk to the board
                    GalileoVersion v = getCurrentBoardVersion();
                    communicationService.closeConnection();
                    boolean success = ((v != null) && 
                            (v.compareTo(update.getVersion()) == 0));
                    if (success) {
                        log.info("Update was successful!");
                    } else {
                        log.info("Update failed version check");
                    }
                    return success;
                }
            }

        }
    }
    
    private GalileoVersion capsuleVersion;
    private GalileoVersion currentBoardVersion;
    private List<CommunicationService> services;
    private String communicationConnection;
    private CommunicationService communicationService;
    private List<FirmwareCapsule> capsules;
    private FirmwareCapsule update;
    private ProgressNotification progress;
    
    private class UploadProgressRelay implements CommunicationService.FileProgress {

        public UploadProgressRelay() {
            nbytesTotal = update.getCache().length();
        }

        @Override
        public void bytesSent(int nsent) {
            try {
                // percentage is a rough estimate since bytes sent will not be 
                // the same as the file size
                int percent = (int) (100 * (nsent / nbytesTotal));
                percent = Math.min(percent, 100);
                percent = Math.max(percent, 0);

                if (progress != null) {
                    progress.updateProgress(percent);
                }
            } catch (Throwable unused) {
                // Don't let anything in the ProgressNotification implementation
                // stop the progress of the thread (which may be the 
                // communication thread).
            }
        }
    
        private final float nbytesTotal;
    }
    
    public interface ProgressNotification {
        
        public void updateMessage(String msg);
        
        public void updateProgress(int percent);
        
    }
}