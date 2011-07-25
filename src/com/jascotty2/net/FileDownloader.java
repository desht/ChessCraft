/**
 * Programmer: Jacob Scott
 * Program Name: FileDownloader
 * Description:
 * Date: Jul 22, 2011
 */

package com.jascotty2.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author jacob
 */
public class FileDownloader {
    
    protected static boolean cancelled;

    public static boolean goodLink(String location) {
        try {
            //HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection.setFollowRedirects(true);
            // note : you may also need HttpURLConnection.setInstanceFollowRedirects(false)
            HttpURLConnection con = (HttpURLConnection) new URL(location).openConnection();
            con.setRequestMethod("HEAD");
            //System.out.println(con.getResponseCode());
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK
                    || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP);
        } catch (Exception e) {
            //System.out.println(e.getMessage());
            //e.printStackTrace();
            return false;
        }
    }

    public static synchronized void download(String location, String filename) throws IOException {
        cancelled = false;
        URLConnection connection = new URL(location).openConnection();
        connection.setUseCaches(false);
        //lastModified = connection.getLastModified();
        //int filesize = connection.getContentLength();
        File parentDirectory = new File(filename).getParentFile();

        if (parentDirectory != null) {
            parentDirectory.mkdirs();
        }

        InputStream in = connection.getInputStream();
        OutputStream out = new FileOutputStream(filename);

        byte[] buffer = new byte[65536];
        //int currentCount = 0;
        for (int count; !cancelled && (count = in.read(buffer)) >= 0;) {
            out.write(buffer, 0, count);
            //currentCount += count;
        }

        in.close();
        out.close();
    }

    public synchronized static void cancel() {
        cancelled = true;
    }

} // end class FileDownloader
