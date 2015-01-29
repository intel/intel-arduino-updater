package com.intel.galileo.flash.tool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * User interface for updating firmware on Intel Galileo boards.
 * 
 */
public class FirmwareUpdateTool extends JFrame {

		
	private static void parseVersions() {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = FirmwareUpdateTool.class.getClassLoader().getResourceAsStream("versions.properties");
			if (input != null)
			{
			   prop.load(input);	
			}
			else
			{
				throw new FileNotFoundException("Not able to find the versions file");
			}
			appVersion = prop.getProperty("APP_VER");
		} catch (Exception io) {
			io.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}			
	}
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
        

        parseVersions();        
	    if (appVersion.length() == 0){
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
        this.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/application.png")));
        Dimension ss = Toolkit.getDefaultToolkit ().getScreenSize ();
        Dimension frameSize = new Dimension ( 500, 300 );
        this.setBounds ( ss.width / 2 - frameSize.width / 2, ss.height / 2 - frameSize.height / 2, frameSize.width, frameSize.height );
        flasher = new GalileoFirmwareUpdater();
        status = new UpdateStatusPanel();
        preferences = new PreferencesPanel(flasher, (new FirmwareUpdateAction(flasher,status)));
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setJMenuBar(createMenubar());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add("Center", preferences);
        getContentPane().add("South", status);
        pack();
        
        preferences.setFrame(this);
        
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
    
    public void paint(Graphics g) {
        setTitle(title + " " + FirmwareUpdateTool.capVersion);
    	super.paint(g);
    }
    
    private final GalileoFirmwareUpdater flasher;
    private final PreferencesPanel preferences;
    private final UpdateStatusPanel status;
    
    static String capVersion = "";  // temporary value
    static String appVersion = "";  // temporary value
    private static String title = "Galileo Firmware Update ";

}
