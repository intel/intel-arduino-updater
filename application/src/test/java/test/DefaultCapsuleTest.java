/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import com.intel.galileo.flash.tool.FirmwareCapsule;
import com.intel.galileo.flash.tool.GalileoFirmwareUpdater;
import com.intel.galileo.flash.tool.GalileoVersion;
import java.io.File;
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
public class DefaultCapsuleTest {
    
    public DefaultCapsuleTest() {
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
     * Perform some minimal sanity checks on the default capsule.
     * This is available without being connected to a board.
     */
    @Test
    public void testCapsule() {
        GalileoFirmwareUpdater galileo = new GalileoFirmwareUpdater();
        List<FirmwareCapsule> avail = galileo.getAvailableFirmware();
        assertNotNull(avail);
        assertFalse(avail.isEmpty());
        FirmwareCapsule cap = galileo.getUpdate();
        assertNotNull(cap);
        GalileoVersion vers = galileo.getUpdateVersion();
        assertNotNull(vers);
        assertFalse(vers.toPresentationString().isEmpty());
        File cache = cap.getCache();
        assertTrue(cache.exists());
        assertFalse(cap.getMD5().isEmpty());
        assertTrue(cap.toString().endsWith(".cap"));
    }
    
    /**
     * Upload the default capsule.  This requires a board.
     * @throws java.lang.Exception if the firmware update throws.
     */
    //@Test
    public void testCapsuleUpload() throws Exception {
        GalileoFirmwareUpdater galileo = new GalileoFirmwareUpdater();
        GalileoVersion boardVersion = galileo.getCurrentBoardVersion();
        assertNotNull(boardVersion);
        galileo.updateFirmwareOnBoard(null);
    }
}
