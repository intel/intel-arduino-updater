/*
FirmwareUpdateTool.java this class is part of Galileo Firmware Update tool 
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
import javax.swing.WindowConstants;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.LogManager;

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
        final FirmwareUpdateCmdArgs cmdArgs = new FirmwareUpdateCmdArgs();
        int errorcode;
        if((errorcode = cmdArgs.ParseArguments(args)) > 0) // Non-Zero mean bad parsing
        {
            System.out.println("--serial_port SerialPort --file CapsFile.cap --default_image");
            System.out.println("With no arguments, GUI version will start.");
            System.exit(errorcode);
            return;
        }
        if (!cmdArgs.isVerbose()) {
            LogManager.getLogManager().reset();
            Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
            globalLogger.setLevel(java.util.logging.Level.OFF);
        }
		
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
                if (cmdArgs.isSilentInstall() == true) {
                    new FirmwareUpdateSilent(cmdArgs).runSilent(true); // Disallow actual flashing if false
                }
                else {
                    // if the exe is double clicked this makes sure that the console is not noisy
                    LogManager.getLogManager().reset();
                    Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
                    globalLogger.setLevel(java.util.logging.Level.OFF);
                    new FirmwareUpdateTool().setVisible(true);
                }
            }
        });

    }
	
    public FirmwareUpdateTool() {  
        this.setIconImage(java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/application.png")));
        Dimension ss = Toolkit.getDefaultToolkit ().getScreenSize ();
        Dimension frameSize = new Dimension ( 500, 300 );
        this.setBounds ( ss.width / 2 - frameSize.width / 2, ss.height / 2 - frameSize.height / 2, frameSize.width, frameSize.height );
        this.addWindowListener(new java.awt.event.WindowAdapter() {
        	@Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                
                if(preferences.isUpdateRunning()){
                	setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                }
                else {
                	setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                	System.out.println("Closing");
					System.exit(0);
				}
            }
        });
        flasher = new GalileoFirmwareUpdater();
        status = new UpdateStatusPanel();
        preferences = new PreferencesPanel(flasher, (new FirmwareUpdateAction(flasher,status)));
        preferences.setFirmwareUpdateTool(this);
        
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
        FirmwareUpdateAction action;
        action = new FirmwareUpdateAction(flasher,status);
        action.setPreferencesPanel(preferences);
        m.add(action);
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
    
    // errors returned to OS
    static final int E_SUCCESS              = 0;
    static final int E_INVDOWNGRADE         = 1;
    static final int E_CAPFILE_NOTFOUND     = 2;
    static final int E_BADCOMPORT           = 3;
    static final int E_UPDATEFAILED         = 4;
    static final int E_BADCAPFILE           = 5;
    static final int E_BADARG               = 6;
    static final int E_BADINTERNALFW        = 7;
    static final int E_UNABLEQUERYBRDFW     = 8;
    static final int E_UNKNOWN              = 9;
    static final int E_COMCOUNT             = 10; // must use one COM if commmand line args used
    static final int E_CAPCOUNT             = 11; // must use one or less cap if command line used
	static final int E_NOBOARD              = 12; // if no comport specified and search fails
    
    private static String title = "Intel\u00ae Galileo Firmware Updater ";
     

}
