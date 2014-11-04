/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.intel.galileo.flash.tool;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.ActionEvent;
import com.intel.galileo.flash.tool.FirmwareUpdateAction.FirmwareUpdateTask;


/**
 * A JPanel to select the preferences for driving the firmware update.
 */
public class PreferencesPanel extends javax.swing.JPanel {

    /**
     * Creates new form PreferencesPanel
     * @param galileo flashing functionality
     */
    public PreferencesPanel(GalileoFirmwareUpdater galileo) {
        
        this.galileo = galileo;
        this.updateAction = null;
        this.galileo.addPropertyChangeListener(changes);
        initComponents();
        initFirmware();
        List<CommunicationService> services = galileo.getCommunicationServices();
        if (! services.isEmpty()) {
            servicesComboBox.removeAllItems();
            for (CommunicationService s : services) {
                servicesComboBox.addItem(s);                        
            }
            populateConnections(galileo);
            String currentConnection = galileo.getCommunicationConnection();
            if (currentConnection != null) {
                connectionComboBox.setSelectedItem(currentConnection);
            }
            
        }

        updateFirmwareVersion();
    }
    
    public PreferencesPanel(GalileoFirmwareUpdater galileo, FirmwareUpdateAction action) {
        
        this.galileo = galileo;
        this.galileo.addPropertyChangeListener(changes);
        this.updateAction = action;
        initComponents();
        initFirmware();
        List<CommunicationService> services = galileo.getCommunicationServices();
        if (! services.isEmpty()) {
            servicesComboBox.removeAllItems();
            for (CommunicationService s : services) {
                servicesComboBox.addItem(s);                        
            }
            populateConnections(galileo);
            String currentConnection = galileo.getCommunicationConnection();
            if (currentConnection != null) {
                connectionComboBox.setSelectedItem(currentConnection);
            }
            
        }
        
        updateFirmwareVersion();
    }
    
    private void populateConnections(GalileoFirmwareUpdater galileo) {
        CommunicationService current = galileo.getCommunicationService();
        if (current != null) {
            servicesComboBox.setSelectedItem(current);
            jLabel2.setText(current.getConnectionLabel());
            List<String> connections = current.getAvailableConnections();
            connectionComboBox.removeAllItems();
            for (String connection : connections) {
                connectionComboBox.addItem(connection);
            }
            
        }
    }

    private void initFirmware() {
        List<FirmwareCapsule> available = galileo.getAvailableFirmware();
        if (! available.isEmpty()) {
            firmwareComboBox.removeAllItems();
            for (FirmwareCapsule fc : available) {
                firmwareComboBox.addItem(fc);
            }
        }
    }
    
    private SwingWorker boardVersionUpdater;
    private void updateBoardVersion() {
        boardVersion.setText("Unknown");
        if (galileo.getCommunicationConnection() != null) {
            boardVersionUpdater = new SwingWorker<GalileoVersion, Void>() {

                @Override
                protected GalileoVersion doInBackground() throws Exception {
                    return galileo.getCurrentBoardVersion();

                }

                @Override
                protected void done() {
                    try {
                        GalileoVersion vers = get();
                        if (vers != null) {
                            boardVersion.setText(vers.toPresentationString());
                            boardVersion.repaint();
                            boardVersion.revalidate();
                        }
                    } catch (InterruptedException unused) {
                    } catch (ExecutionException unused) {
                    }
                }

            };
            boardVersionUpdater.execute();
        }
    }
    
    
    private SwingWorker capsuleVersionUpdater;
    private void updateFirmwareVersion() {
        capsuleVersion.setText("Unknown");
        if (galileo.getUpdate() != null) {
            capsuleVersionUpdater = new SwingWorker<GalileoVersion,Void>() {
                
                @Override
                protected GalileoVersion doInBackground() throws Exception {
                    return galileo.getUpdateVersion();

                }

                @Override
                protected void done() {
                    try {
                        GalileoVersion vers = get();
                        if (vers != null) {
                            capsuleVersion.setText(vers.toPresentationString());
                            capsuleVersion.repaint();
                            capsuleVersion.revalidate();
                        }
                    } catch (InterruptedException unused) {
                    } catch (ExecutionException unused) {
                    }
                }
                
            };
            capsuleVersionUpdater.execute();
        }
    }
    
    private PropertyChangeListener changes = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            final Object o = evt.getNewValue();
            if ("updateVersion".equals(name)) {
                GalileoVersion v = (o != null) ? (GalileoVersion) o : null;
                final String updateVersion = (v != null) ? v.toPresentationString() : "Unknown";
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        capsuleVersion.setText(updateVersion);
                        capsuleVersion.repaint();
                        capsuleVersion.revalidate();
                                            
                    }
                });
            } else if ("currentBoardVersion".equals(name)) {
                GalileoVersion v = (o != null) ? (GalileoVersion) o : null;
                final String version = (v != null) ? v.toPresentationString() : "Unknown";
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        boardVersion.setText(version);
                        boardVersion.repaint();
                        boardVersion.revalidate();
                                            
                    }
                });
            }
        }
        
    };
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        servicesComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        connectionComboBox = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        firmwareComboBox = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        capsuleVersion = new javax.swing.JTextField();
        boardVersion = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        uploadFirmwareButton = new javax.swing.JButton("Upload Firmware");

        status = new UpdateStatusPanel();

        uploadFirmwareButton.setAction(updateAction);
        
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Service:");
        servicesComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None Available" }));	


        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Connection:");

        connectionComboBox.setEditable(true);
        connectionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None" }));
        connectionComboBox.setName("connection"); // NOI18N
        connectionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectionComboBoxActionPerformed(evt);
            }
        });

	connectionComboBox.addPopupMenuListener(new  PopupMenuListener() {

	    @Override
	    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		populateConnections(galileo);
	    }

	    @Override
	    public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {

	    }

	    @Override
	    public void popupMenuCanceled(PopupMenuEvent event) {

      	    }
	});
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Firmware:");

        firmwareComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None Available" }));
        firmwareComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                firmwareChoiceChanged(evt);
            }
        });

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setLabelFor(capsuleVersion);
        jLabel4.setText("Update Firmware Version:");

        capsuleVersion.setEditable(false);
        capsuleVersion.setText("Unknown");
        capsuleVersion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                capsuleVersionActionPerformed(evt);
            }
        });

        boardVersion.setEditable(false);
        boardVersion.setText("Unknown");

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("Current Board Firmware Version:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 84, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(servicesComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(connectionComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(firmwareComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(boardVersion, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
                            .addComponent(capsuleVersion)
                            .addComponent(uploadFirmwareButton, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(servicesComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(connectionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(firmwareComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))                    
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(capsuleVersion)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(boardVersion)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                		.addComponent(uploadFirmwareButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addContainerGap(128, Short.MAX_VALUE))
        );

        connectionComboBox.getAccessibleContext().setAccessibleName("");
    }// </editor-fold>//GEN-END:initComponents

    private void capsuleVersionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_capsuleVersionActionPerformed
        
        
    }//GEN-LAST:event_capsuleVersionActionPerformed

    private void connectionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectionComboBoxActionPerformed

        String connection = (String)connectionComboBox.getSelectedItem();
        galileo.setCommunicationConnection(connection);

	//uncomment when lsz issue is resolved
        //boardVersion.setText("");	
        //updateBoardVersion();
    }//GEN-LAST:event_connectionComboBoxActionPerformed

    private void firmwareChoiceChanged(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_firmwareChoiceChanged
        FirmwareCapsule cap = (FirmwareCapsule) firmwareComboBox.getSelectedItem();
        galileo.getLogger().info("Selected firmware: "+cap);
        galileo.setUpdate(cap);
    }//GEN-LAST:event_firmwareChoiceChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField boardVersion;
    private javax.swing.JTextField capsuleVersion;
    private javax.swing.JComboBox connectionComboBox;
    private javax.swing.JComboBox firmwareComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JButton uploadFirmwareButton;
    private javax.swing.JComboBox servicesComboBox;
    // End of variables declaration//GEN-END:variables

    private final GalileoFirmwareUpdater galileo;
    private final FirmwareUpdateAction updateAction;
    private UpdateStatusPanel status;
    

}
