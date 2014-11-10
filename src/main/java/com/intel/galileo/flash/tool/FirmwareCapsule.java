/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.intel.galileo.flash.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to represent firmware content for a Galileo board.
 */
public class FirmwareCapsule {
    
    FirmwareCapsule(URL u, File cacheDir) {
        location = u;
        this.cacheDir = cacheDir;
    }

    /**
     * MD5 sum for the capsule file contents.  This is needed to verify the
     * capsule made it to the board properly.
     * @return 
     */
    public final String getMD5() {
        checkCacheAndLoadIfNeeded();
        return md5sum;
    }
    
    /**
     * The location of the capsule file content.  This could be in the
     * jar file of the application, an http or ftp server somewhere on the
     * web, or a file on the machine running the application.
     * @return 
     */
    public final URL getLocation() {
        return location;
    }
    
    /**
     * Fetch the version of the firmware.  If this method returns 
     * null, the URL does not represent a valid firmware file.
     * @return 
     */
    public final GalileoVersion getVersion() {
        checkCacheAndLoadIfNeeded();
        return version;
    }
    
    /**
     * The cached capsule file located in a temporary file.
     * @return 
     */
    public final File getCache() {
        checkCacheAndLoadIfNeeded();
        return cache;
    }
    
    private static final String CAP_SUFFIX = ".cap";
    
    private synchronized void checkCacheAndLoadIfNeeded() {
        if (! cacheLoaded) {
            try {
                cache = new File(cacheDir, toString());
                MessageDigest md = MessageDigest.getInstance("MD5");
                InputStream is = location.openStream();
                OutputStream out = new FileOutputStream(cache);

                byte[] buffer = new byte[1024];
                int num;
                do {
                    num = is.read(buffer);
                    if (num > 0) {
                        md.update(buffer, 0, num);
                        out.write(buffer, 0, num);
                    }
                } while (num != -1);
                is.close();
                out.flush();
                out.close();
                byte[] digest = md.digest();
                BigInteger bi = new BigInteger(1, digest);

                String v = getImageVersion(cache);
                version = GalileoVersion.ofTargetString(v);
                md5sum = String.format("%0" + (digest.length << 1) + "X", bi);
            } catch (Exception e) {
                Logger.getLogger(FirmwareCapsule.class.getName())
                        .log(Level.SEVERE, "Invalid capsule", e);
            }
            cacheLoaded = true;
        }
    }
    
    private String getImageVersion(File f) throws IOException {
        final String FL_IMG_VER = "[Flash Image Version]";
        final String FL_IMG_VAL = "value=";
        final String FL_IMG_EOL = "\r\n";

        StringBuilder fileData = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(f));
        try{
	        char[] buf = new char[1024];
	        int numRead;
	        while ((numRead = reader.read(buf)) != -1) {
	            String readData = String.valueOf(buf, 0, numRead);
	            fileData.append(readData);
	        }
        } catch (Exception e) {
        	e.printStackTrace();
        } finally {
        	try {
        		reader.close();
        	} catch(Exception e) {
        		e.printStackTrace();
        	}
        }
        int index_FL_IMG_VER = fileData.indexOf(FL_IMG_VER);
        int index_FL_IMG_VAL = FL_IMG_VAL.length()
                + fileData.indexOf(FL_IMG_VAL, index_FL_IMG_VER);
        int index_FL_IMG_EOL = fileData.indexOf(FL_IMG_EOL, index_FL_IMG_VAL); 
        String v = fileData.substring(index_FL_IMG_VAL, index_FL_IMG_EOL);

        return v;
    }
    
    @Override
    public final String toString() {
        String fname = location.getPath();
        int index = fname.lastIndexOf("/");
        fname = (index > 0) ? fname.substring(index+1) : fname;
        return fname;
    }
    
    private boolean cacheLoaded = false;
    private String md5sum;
    private GalileoVersion version;
    private File cache;
    private final File cacheDir;
    private final URL location;
}
