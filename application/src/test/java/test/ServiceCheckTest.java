/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import com.intel.galileo.flash.tool.CommunicationService;
import com.intel.galileo.flash.tool.GalileoFirmwareUpdater;
import com.intel.galileo.flash.tool.GalileoVersion;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author tim
 */
public class ServiceCheckTest {
    
    public ServiceCheckTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Check to make sure that at least one service is available.  The tool 
     * isn't very useful if there are no services to connect to a board.
     */
    @Test
    public void checkServiceAvailable() {
        List<CommunicationService> services = 
          CommunicationService.getCommunicationServices();
        assertNotNull(services);
        assertFalse(services.isEmpty());
    }
    
    /**
     * This only works if there are actual connections to a board and if they
     * can be automatically discovered. Uncomment the test annotation to run
     * manually for diagnostics.
     */
    //@Test
    public void testVersionCommandOnServices() {
        List<CommunicationService> services
                = CommunicationService.getCommunicationServices();
        for (CommunicationService service : services) {
            if (service.isSupportedOnThisOS()) {
                System.out.println("Service: " + service.getServiceName());
                List<String> ports = service.getAvailableConnections();
                System.out.printf("There are %d connections:\n", ports.size());
                for (String port : ports) {
                    System.out.println(port);
                }
                if (!ports.isEmpty()) {
                    requestBoardVersion(service, ports.get(0));
                }
            }
        }
    }
    
    void requestBoardVersion(CommunicationService link, String connection) {
        System.out.printf("Requesting version using %s on %s\n", connection, 
                link.getServiceName());
        GalileoFirmwareUpdater updater = new GalileoFirmwareUpdater(link, connection);
        GalileoVersion vers = updater.getCurrentBoardVersion();
        assertNotNull(vers);
        System.out.println("Version: "+vers.toPresentationString());
        GalileoVersion uvers = updater.getUpdateVersion();
        assertNotNull(uvers);
        System.out.println("Update Version: "+uvers.toPresentationString());
    }
}
