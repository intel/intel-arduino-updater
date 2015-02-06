/*
FirmwareUpdateAction.java this class is part of Galileo Firmware Update tool 
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

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import javax.swing.SwingWorker;

/**
 * A command to perform a firmware update.
 */
public class FirmwareUpdateAction extends AbstractAction {

    static final String DIALOG_TITLE = "Intel Galileo Firmware Updater";
    private PreferencesPanel parentPanel;

    public FirmwareUpdateAction(GalileoFirmwareUpdater galileo,
            UpdateStatusPanel status) {
        super("Update Firmware");
        this.galileo = galileo;
        this.status = status;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        int windowReturn;
        FirmwareCapsule cap;
        JComponent parent = (JComponent) e.getSource();

		
        // first of all let's check if the file really exist
        // since the user has option to type the name
        try {
        	
        	// checking if it is a local cap file or from resource
        	if (galileo.getLocalCapFile() != null) {
    			new File(galileo.getLocalCapFile().getPath()).exists();        		
        	}
        		
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			JOptionPane.showMessageDialog(parent,
		            "Invalid cap file, make sure the file exists or you have valid permissions.",
		            DIALOG_TITLE,
		            ERROR_MESSAGE);
		    return;
		} 
        
		//get the current file typed if any thus we will fix the annoying error
        // "Preferences not yet properly set" always shown before
        GalileoVersion target_version_id = galileo.getCurrentBoardVersion();
	    
        

        
        // the command should not have been enabled until this returns true.
        if (!galileo.isReadyForUpdate()) {
            JOptionPane.showMessageDialog(parent,
                    "Preferences not yet properly set",
                    DIALOG_TITLE,
                    ERROR_MESSAGE);
			//temporary skip until lsz issue is fixed
            return;
        }

        windowReturn = JOptionPane.showConfirmDialog(parent,
                "Intel Galileo firmware update requires using the external power supply.",
                DIALOG_TITLE, JOptionPane.OK_CANCEL_OPTION);

        if (JOptionPane.CANCEL_OPTION == windowReturn || JOptionPane.CLOSED_OPTION == windowReturn) {
            galileo.getLogger().warning("Update canceled by user");
            return;
        }

        cap = galileo.getUpdate();
        GalileoVersion ready_version_id = cap.getVersion();

        String windowDescription = "Target firmware is version '"
                + target_version_id.toPresentationString() + "' now.\n\nDo you wish to ";
     
        // checking if there is some downgrade
        final String critical_version = "1.0.2";
        String v_b = target_version_id.toPresentationString();
        String v_candidate = ready_version_id.toPresentationString();
        
        int res = v_b.compareTo(critical_version);
        if (res >= 0) {

            // if the board is superior of 1.0.2 it is necessary
        	// to check if the candidate capsule file is superior 
        	// to 1.0.2
        	
        	res = v_candidate.compareTo(critical_version);
        	
        	// Note the comparation with "732" must be removed when decoder
        	// issue be resolved
        	if ((res < 0) || (v_candidate.compareTo("732") == 0)  ) {
        		 JOptionPane.showMessageDialog(parent,
                         "The current firmware version " + v_b + " does not accept be downgraded to " + v_candidate +
        		         ".The candidate capsule file must be " + critical_version + " or newer.",
                         DIALOG_TITLE,
                         ERROR_MESSAGE);
                 return;
        	}
        	
        }
                
        boolean isEquivalent = false;
        try {
            int d = ready_version_id.compareTo(target_version_id);
            if (d < 0) {
                windowDescription += "update with older";
            } else if (d > 0) {
                windowDescription += "update with newer";
            } else {
                windowDescription += "rewrite with equivalent";
                isEquivalent = true;
            }
        } catch (IllegalArgumentException iae) {
            windowDescription += "overwrite with";
        }

        
        if (!isEquivalent) {
            windowDescription += " version '"
                    + ready_version_id.toPresentationString() + "'";
        }

        windowDescription += " firmware?";

        windowReturn = JOptionPane.showConfirmDialog(null, windowDescription,
                DIALOG_TITLE, JOptionPane.YES_NO_OPTION);
        if (JOptionPane.NO_OPTION == windowReturn
                || JOptionPane.CLOSED_OPTION == windowReturn) {
            galileo.getLogger().warning("User canceled update");
            return;
        }

        task = new FirmwareUpdateTask();
        status.setVisible(true);
        task.execute();

    }

    public void setPreferencesPanel(PreferencesPanel panel)
    {
    	this.parentPanel = panel;
    }
    
    public boolean isRunning(){
    	return runState;
    }
    private final GalileoFirmwareUpdater galileo;
    private final UpdateStatusPanel status;
    private FirmwareUpdateTask task;
    private boolean runState = false;

    /**
     * Execute the firmware update on a separate thread with progress displayed
     * in a swing component (UpdateStatusPanel).
     */
    class FirmwareUpdateTask extends SwingWorker<Boolean, String> implements
            GalileoFirmwareUpdater.ProgressNotification {

        public FirmwareUpdateTask() {
            addPropertyChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if ("progress".equals(evt.getPropertyName())) {
                        status.updateUploadProgress((Integer) evt.getNewValue());
                    }
                }
            });
        }

        @Override
        protected void done() {
            try {
            	runState = false;
                status.setVisible(false);
                status.revalidate();
                status.repaint();
                parentPanel.enableUI();
                boolean success = get();
                // show user result
                String msgText = success
                        ? "Target firmware successfully updated."
                        : "Target firmware not updated.";
                int msgType = success
                        ? JOptionPane.PLAIN_MESSAGE
                        : JOptionPane.ERROR_MESSAGE;
                JOptionPane.showMessageDialog(
                        status, msgText, DIALOG_TITLE, msgType);

            } catch (InterruptedException ignore) {
            } catch (java.util.concurrent.ExecutionException e) {
                String why;
                Throwable cause = e.getCause();
                if (cause != null) {
                    why = cause.getMessage();
                } else {
                    why = e.getMessage();
                }
                JOptionPane.showMessageDialog(
                        null, why, DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
            }
        }

        /**
         * Called on the event thread with messages from the update that was on
         * another thread.
         *
         * @param chunks
         */
        @Override
        protected void process(List<String> chunks) {
            String lastMsg = chunks.get(chunks.size() - 1);
            status.updateMessage(lastMsg);
        }

        /**
         * Start the firmware update on the worker thread.
         *
         * @return
         * @throws Exception
         */
        @Override
        protected Boolean doInBackground() throws Exception {
        	runState = true;
        	parentPanel.disableUI();
            return galileo.updateFirmwareOnBoard(this);
        }

        @Override
        public void updateMessage(String msg) {
            publish(msg);
        }

        @Override
        public void updateProgress(int percent) {
            setProgress(percent);
        }

    }
}
