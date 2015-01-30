/*
UpdateStatusPanel.java this class is part of Galileo Firmware Update tool 
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
