package com.intel.galileo.flash.tool;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;

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
		 Map<String, String> env = System.getenv();
	        for (String envName : env.keySet()) {
	            if (envName.equals("APP_VER"))  {
	            	appVersion = env.get(envName);
	            } else if (envName.equals("CAP_VER"))  {
	            	capVersion = env.get(envName);
	            }
	        }				
	    if ((capVersion.length() == 0) || (appVersion.length() == 0)) {
           Logger.getLogger(FirmwareUpdateTool.class.getName())
           .log(java.util.logging.Level.SEVERE, null, "YOUR FORGOT THE VERSIONS IN ENV VARS!!!");  
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
        status = new UpdateStatusPanel();
        preferences = new PreferencesPanel(flasher, (new FirmwareUpdateAction(flasher,status)));
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        title += appVersion;
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
        mb.add(createAboutMenu());
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

    JMenu createAboutMenu() {
        JMenu m = new JMenu("About");
        m.add(new AbstractAction("Version") {
            @Override
            public void actionPerformed(ActionEvent e) {
        		About window = new About();
        		window.showMe(null);
           }
       });	
       return m;
    }
    private final GalileoFirmwareUpdater flasher;
    private final PreferencesPanel preferences;
    private final UpdateStatusPanel status;
    
    static String capVersion = "";  // temporary value
    static String appVersion = "";  // temporary value
    private static String title = "Galileo Firmware Update ";

}
