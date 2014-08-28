/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.intel.galileo.flash.tool;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * A component to show the progress of a firmware update operation.
 */
public class UpdateStatusPanel extends JPanel {

      private static final String wlabel ="<html><font color='red'>Warning: do not interrupt by disconnecting power or USB, or by pressing any microswitches!</font></html>";
      
    public UpdateStatusPanel() {
        setLayout(new BorderLayout());
        progress = new JProgressBar(JProgressBar.HORIZONTAL);
        progress.setMinimum(0);
        progress.setMaximum(100);
        final JLabel wlbl = new JLabel(wlabel);
        message = new JLabel();
        wlbl.setForeground(Color.red);
        add(BorderLayout.SOUTH, progress);
        add(BorderLayout.NORTH, wlbl);
        add(BorderLayout.CENTER, message);
        setVisible(false);
    }
    
    public void updateMessage(String msg) {
        message.setText(msg);
        message.revalidate();
        repaint();
    }
    
    public void updateUploadProgress(int percent) {
        if (percent < 0) {
            progress.setIndeterminate(true);
        } else {
            progress.setValue(percent);
        }
        progress.revalidate();
        repaint();
    }

    private final JLabel message;
    private final JProgressBar progress;
    
}
