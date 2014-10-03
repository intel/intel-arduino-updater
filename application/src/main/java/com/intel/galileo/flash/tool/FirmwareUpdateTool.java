package com.intel.galileo.flash.tool;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;


//import processing.app.Base;
//import processing.app.Editor;
//import processing.app.Preferences;
//import processing.app.tools.Tool;

/**
 * User interface for updating firmware on Intel Galileo boards.
 * 
 */
public class FirmwareUpdateTool extends JFrame {

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(FirmwareUpdateTool.class.getName())
                    .log(java.util.logging.Level.SEVERE, null, ex);
        }

        // Create and display the Frame
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FirmwareUpdateTool().setVisible(true);
            }
        });

    }
	
    public FirmwareUpdateTool() {
        flasher = new GalileoFirmwareUpdater();
        preferences = new PreferencesPanel(flasher);
        status = new UpdateStatusPanel();
        setTitle(title);
        setJMenuBar(createMenubar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", preferences);
        getContentPane().add("South", status);

        pack();

    }
    
    /**
     * Create the menubar for the app.  
     * @return the menubar for the app
     */
    protected JMenuBar createMenubar() {
        JMenuBar mb = new JMenuBar();
        mb.add(createFileMenu());
        return mb;
    }
    
    JMenu createFileMenu() {
        JMenu m = new JMenu("File");
        m.add(new FirmwareUpdateAction(flasher,status));
        m.add(new AbstractAction("Quit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
            
        });
        return m;
    }

    private final GalileoFirmwareUpdater flasher;
    private final PreferencesPanel preferences;
    private final UpdateStatusPanel status;
    
    private static final String title = "Galileo Firmware Update";

}
