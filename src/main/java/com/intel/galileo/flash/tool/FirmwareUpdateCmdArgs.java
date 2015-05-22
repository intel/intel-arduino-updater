package com.intel.galileo.flash.tool;

import java.io.File;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class FirmwareUpdateCmdArgs
{
    public List<String> queryPorts(){
         // List all the known ports
        List<String> result = new LinkedList<String>();
        List<CommunicationService> services;
        services = CommunicationService.getCommunicationServices();
        if (!services.isEmpty()) 
        {
          CommunicationService service = services.get(0);
          result = service.getAvailableConnections();
        } // if
        return result;
    } // _queryPorts
    
    public FirmwareUpdateCmdArgs(){
        _init();
    } // FirmwareUpdateCmdArgs
    
    private void _init()
    {
        // Initialize the default values
        try
        {
          _url = null;
        } // try
        catch(Exception e)
        {
          e.printStackTrace();
        } // catch     
        _port       = "NULL";
        _debug      = false;
        _nargs      = -1;
        _silentInstall = false;
		_verbose = false;
    } // _init
    
    public boolean isVerbose()
    {
        return _verbose;
    }
    public boolean isSilentInstall()
    {
        return _silentInstall;
    }
    
    public int getNumArgs()
    {
        return _nargs;
    } // getNumArgs
    
    public URL getCapURL(){
        return _url;
    } // getCapURL
    
    public String getSerialPort(){
        return _port;
    } // getSerialPort
    
    public void setDebug(boolean aFlag){
        _debug = aFlag;
    } // enableDebug

    private boolean _setFileURI(String aValue){
        boolean result = false;
        
        try
        {    
            if(_debug) System.out.print("  setFileURI,"+aValue);
            
            // Determine if the file physically exists
            File file = new File(aValue);
            if(file.exists() == true)
            {
                // Convert the path to a URI format
                _url = file.toURI().toURL();
                result = true;
                if(_debug) System.out.print(",FileExists");
            }
            else
            {
                if(_debug) System.out.print(",FileDoesNotExists");
            }
            if(_debug) System.out.println("");
        } 
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return result;
    } // _setFileURI
    
    public boolean setUserPort(String aValue) {
        boolean result = false;
        
        if(_debug) System.out.print("  setUserPort,"+aValue);
        List<String> ports = queryPorts();
        
        // Do we have ports?
        if(ports.size() > 0)
        {
            // Yes. Match the string with known ports
            for(int j=0; j<ports.size(); j++)
            {
              if(_debug) System.out.print(","+ports.get(j)+"("+j+")");
              if(aValue.equals(ports.get(j)))
              {
                  _port  = ports.get(j);
                  result = true;
                  if(_debug) System.out.println(",PortExists");
                  break;
              } // if
            } // for
        } // if
    
        if(result == false  && _debug ) System.out.println(",PortDoesNotExists");
        return result;
    } // _setUserPort
    
    public int ParseArguments(String[] aArgs) {
        int result = FirmwareUpdateTool.E_BADARG;
    
        // Arguments are optional
         if(aArgs.length == 0)
         {
           _nargs = 0;
          return FirmwareUpdateTool.E_SUCCESS; // 0 for success
         }
         
         // If passed, arguments shall be in cmd/value pairs
//        if((aArgs.length % 2) != 0)
 //       {
//          return FirmwareUpdateTool.E_BADARG;
//        }
         
        // Arguments shall be in <cmd> <value> form
        result       = FirmwareUpdateTool.E_BADARG;
        int index    = 0;
        int optsPort = 0; 
        int optsCap  = 0;
        for(String s:aArgs)
        {
          // Process only <cmd> arguments
//          if( (index % 2 ) == 0)
//          {
              if(_debug) System.out.print("Arg: "+s+","+index);
              
              // Commands shall be two characters
              if(s.length() >= 2  &&  s.startsWith("-", 0))
              {
                // Validate known <cmds>
                 if(s.equals("--file"))
                {
                    if(_setFileURI(aArgs[index+1]) == false)
                    {
                      _init(); // reset
                      return FirmwareUpdateTool.E_CAPFILE_NOTFOUND;
                    }
                    // Count how many times we have seen the command
                    optsCap++;
                    index += 2;
                    _silentInstall = true;
                }
                 else if(s.equals("--serial_port"))
                {
                    if(setUserPort(aArgs[index+1]) == false)
                    {
                      _init(); // reset
                      return FirmwareUpdateTool.E_BADCOMPORT;
                    }
                    // Count how many times we have seen the command
                    optsPort++;
                    index += 2;
                    _silentInstall = true;
                }
                else if(s.equals("--default_image"))
                {
                    // need to handle situation where they try to add a parameter after -default_image
                                        // Count how many times we have seen the command
                    optsCap++; // this would be the same as specifying a cap file
                    ++index;
                    _silentInstall = true;
                }                
                else if(s.equals("--v"))
                {
                    // need to handle situation where they try to add a parameter after -v RUSS
                    // don't really care if they specify this twice, we can add a check if needed
                    _verbose = true;
                    ++index;
                }
                else
                {
                    // Command is unknown
                    _init(); // reset
                    return FirmwareUpdateTool.E_BADARG;
                } // if
              } // if
          //} // mod
          //index=index+1;
        } // for
        
        // Command shall be specified once
        if(
                optsPort <= 1  &&  // User shall specify a one port
                (optsCap <= 1)     // User may   specify a cat file
          )
        {
          _nargs++; // Change -1 to zero
          if(optsPort > 0) _nargs++;
          if(optsCap  > 0) _nargs++;            
          result      = FirmwareUpdateTool.E_SUCCESS;
        }
        else
        {
          if (optsPort > 1) {
              result = FirmwareUpdateTool.E_COMCOUNT;
          }
          if (optsCap > 1) {
              result = FirmwareUpdateTool.E_CAPCOUNT;
          }
          _init(); // reset
        } // if
        
        return result;
    } // ParseArguments
    
    //-------------------------------------------------------------------------
    // Private Members
    URL          _url;
    String       _port;
    boolean      _debug;
    int          _nargs;
    boolean      _silentInstall;
    boolean      _verbose;	
} // class FirmwareUpdateCmdArgs