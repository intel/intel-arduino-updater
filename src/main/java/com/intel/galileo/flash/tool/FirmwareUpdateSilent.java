/*
SilentInstall.java this class is part of Galileo Firmware Update tool 
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

import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Class to represent firmware content for a Galileo board.
*/
public class FirmwareUpdateSilent {

   FirmwareUpdateSilent(FirmwareUpdateCmdArgs aCmdArgs){
       _cmdArgs        = aCmdArgs;
       _galileoflash   = new GalileoFirmwareUpdater();
       _capBaseVersion = "1.0.2";
       _systemExitCode = FirmwareUpdateTool.E_UNKNOWN;
   } // Constructor

   public String _compareBaseFwVer(String aFW)
   {
       String result       = "ERR";

       // Verify that the version is at lease 1.0.2
       if(aFW.equals("732"))
       {
           // This is an odd version that does not work with the compare below.
         result = "LT";
       }
       else
       {
           // In order for compare to work properly,
           // the FW shall be in the x.x.x format
           if(_isVersionForm(aFW) == false)
               return result;

           // Translate the string compare
           int r = aFW.compareTo(_capBaseVersion);
           if (r > 0)
               result = "GT";
           else if (r < 0)
               result = "LT";
           else if (r == 0)
               result = "EQ";
       } // if

       // EQ, same version
       // GT, upgrade
       // LT, downgrade
       // ERR, rejected
       return result;
   } // compareFirmwareVer


   private boolean _isVersionForm(String aFW)
   {
       boolean result = false;
       int idx;
       int idxLast;

       idx  = aFW.indexOf('.');
       if(idx == -1) return result; // First dot not found

       idxLast = idx+1;
       idx     = aFW.indexOf('.', idxLast);
       if(idx == -1) return result; // Second dot not found

       // TBD, need to check digits between '.' are digits

       result = true;
       return result;
   } // isVersionForm
   
   private boolean _setCapFile()
   {
     boolean result = false;

     URL chkUrl = null;
     URL usrUrl = null;
     GalileoVersion objFirmwareVer;
     String         strFirmwareVer;
     FirmwareCapsule capUser;
     FirmwareCapsule capDefault;
     FirmwareCapsule capCheck;

     // Validate the internal firmware
     capDefault     =_galileoflash.getUpdate();
     objFirmwareVer = capDefault.getVersion();
     if(objFirmwareVer == null)
     {
         System.out.println("Corrupted internal firmware. Terminate!");
         _systemExitCode = FirmwareUpdateTool.E_BADINTERNALFW;
         return result;
     }
     strFirmwareVer = objFirmwareVer.toPresentationString();
     System.out.println("Internal FW="+strFirmwareVer);

     try
     {
         // Did the user specify a URL to the firmware?
         usrUrl = _cmdArgs.getCapURL();
         if(usrUrl == null)
         {
             // No. Use internal firmware
             capUser = _galileoflash.getUpdate();
             _galileoflash.setUpdate(capUser);
         }
         else
         {
             // Yes. Pass the url to obtain the firmware
            _galileoflash.setLocalCapFile(usrUrl);
            chkUrl = _galileoflash.getLocalCapFile();
            if(chkUrl == null)
            {
                 return result; // the error code is set from caller
            }

            // The cap file is copied to a reserved directory
            File f = new File(System.getProperty("user.home"), ".galileo");
            f.mkdir();
            capUser = new FirmwareCapsule(_galileoflash.getLocalCapFile(), f);  // Kills our cap file too.

            // Verify the firmware in the reserved directory
            objFirmwareVer = capUser.getVersion();
            if(objFirmwareVer == null)
            {
                System.out.println("Corrupted user firmware. Terminate!");
                System.exit(FirmwareUpdateTool.E_BADCAPFILE);             
                return result;
            }
            strFirmwareVer = objFirmwareVer.toPresentationString();
            System.out.println("User FW    =" + strFirmwareVer);

            // Give the user's firmware to the flasher
            _galileoflash.setUpdate(capUser);
         } // if
     } // try
     catch (Exception ex)
     {
          Logger.getLogger(FirmwareUpdateTool.class.getName())
                  .log(java.util.logging.Level.SEVERE, null, ex);
     } // catch

     // For some reason, this is now expected to be null
     chkUrl = _galileoflash.getLocalCapFile();
     if(chkUrl != null)
     {
         return result;
     }

     // Get the flasher FW and verify again
     capCheck = _galileoflash.getUpdate();
     objFirmwareVer = capCheck.getVersion();
     if(objFirmwareVer == null)
     {
         System.out.println("Corrupted flasher firmware. Terminate!");
         return result;
     }
     strFirmwareVer = objFirmwareVer.toPresentationString();
     System.out.println("New FW     ="+strFirmwareVer);


     // Reject if the FW is below the base supported verison
     String cmp = _compareBaseFwVer(strFirmwareVer);
     if(cmp.equals("LT")  ||  cmp.equals("ERR"))
     {
         System.out.println("The lowest supported *.cap file is " + _capBaseVersion);
         System.out.println("Firmware version ("+ strFirmwareVer+ ") is not supported. Terminate!");
         System.exit(FirmwareUpdateTool.E_INVDOWNGRADE);
         return result;
     }

     result = true;
     return result;
   } // _selectCapfile

   private boolean _setSerialPort()
   {
     boolean result = false;

     GalileoVersion objFirmwareVer;
     String         strFirmwareVer;

     // Give the user serial port to the flasher
     _galileoflash.setCommunicationConnection(_cmdArgs.getSerialPort());

     // Query the port and obtain the firmware version.
     objFirmwareVer = _galileoflash.getCurrentBoardVersion();
     if(objFirmwareVer == null)
     {
         System.out.println("Unable to query board firmware. Terminate!");
         _systemExitCode = FirmwareUpdateTool.E_UNABLEQUERYBRDFW;
         return result;
     }
     strFirmwareVer = objFirmwareVer.toPresentationString();
     System.out.println("Board FW  ="+strFirmwareVer);

     // Reject if the board is an unknown version (LT, EQ, GT are good)
     if(_compareBaseFwVer(strFirmwareVer).equals("ERR"))
     {
         System.out.println("Board version ("+strFirmwareVer+") is not supported. Terminate!");
         _systemExitCode = FirmwareUpdateTool.E_INVDOWNGRADE;
         return result;
     }

     result = true;
     return result;
   } // _selectSerialPort

   public void runSilent(boolean aAllowFirmwareFlash)
    {

      // Set the flasher firmware
      if(_setCapFile() == false){
        // Something not right with the firmware
        _systemExitCode = FirmwareUpdateTool.E_BADCAPFILE;
        return;
      }

      // Set the flasher serial port
      if(_setSerialPort() == false){
         // Something not right with the serial port
         _systemExitCode = FirmwareUpdateTool.E_BADCOMPORT;
         System.exit(_systemExitCode);
      }

      // Verify that the flasher has the firmware and serial port
      if (!_galileoflash.isReadyForUpdate()) {
          // the port or cap is not set so should exit with error
          _systemExitCode = FirmwareUpdateTool.E_UPDATEFAILED;
      }

     if(aAllowFirmwareFlash) {
         try {
           if (_galileoflash.updateFirmwareOnBoard(new SilentConsole()) == true) {
               _systemExitCode = FirmwareUpdateTool.E_SUCCESS; // successful upgrade; 
           }
         } catch (Exception ex) {
           Logger.getLogger(FirmwareUpdateSilent.class.getName()).log(Level.SEVERE, null, ex);
         }
     }
     else {
         System.out.println("Firmware flash disabled. Is aAllowFirmwareFlash false?");
     }
     System.exit(_systemExitCode);
   } // runSilent

   protected Boolean flashnow() throws Exception {
                  return _galileoflash.updateFirmwareOnBoard(null);
   }
//=============================================================================
   class SilentConsole implements
           GalileoFirmwareUpdater.ProgressNotification {

       public SilentConsole() {
       } // Constructor

       public void updateMessage(String msg) {
         System.out.println(msg);
       } // updateMessage

       public void updateProgress(int percent) {
         System.out.println(percent+" %");
       } // updateProgress

   } // SilentConsole
//-----------------------------------------------------------------------------
   private final FirmwareUpdateCmdArgs  _cmdArgs;
   private final GalileoFirmwareUpdater _galileoflash;
   private final String                 _capBaseVersion;
   private       int                    _systemExitCode;
} // class FirmwareUpdateSlient
