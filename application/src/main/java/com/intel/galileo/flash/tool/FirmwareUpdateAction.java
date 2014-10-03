/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.galileo.flash.tool;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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

    static final String DIALOG_TITLE = "Galileo Firmware Update";

    public FirmwareUpdateAction(GalileoFirmwareUpdater galileo,
            UpdateStatusPanel status) {
        super("Update Firmware");
        this.galileo = galileo;
        this.status = status;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        int windowReturn;

        JComponent parent = (JComponent) e.getSource();

        // the command should not have been enabled until this returns true.
        if (!galileo.isReadyForUpdate()) {
            JOptionPane.showMessageDialog(parent,
                    "Preferences not yet properly set",
                    DIALOG_TITLE,
                    ERROR_MESSAGE);
            return;
        }

        windowReturn = JOptionPane.showConfirmDialog(parent,
                "Intel Galileo firmware update requires using the external power supply.",
                DIALOG_TITLE, JOptionPane.OK_CANCEL_OPTION);

        if (JOptionPane.CANCEL_OPTION == windowReturn || JOptionPane.CLOSED_OPTION == windowReturn) {
            galileo.getLogger().warning("Update canceled by user");
            return;
        }

        FirmwareCapsule cap = galileo.getUpdate();
        GalileoVersion ready_version_id = cap.getVersion();

        GalileoVersion target_version_id = galileo.getCurrentBoardVersion();
        String windowDescription = "Target firmware is version '"
                + target_version_id.toPresentationString() + "' now.\n\nDo you wish to ";

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

    private final GalileoFirmwareUpdater galileo;
    private final UpdateStatusPanel status;
    private FirmwareUpdateTask task;

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
                status.setVisible(false);
                status.revalidate();
                status.repaint();
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
