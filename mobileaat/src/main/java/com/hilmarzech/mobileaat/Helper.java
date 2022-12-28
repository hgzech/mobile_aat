package com.hilmarzech.mobileaat;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This helper class holds a couple of convenience functions.
 * TODO: This should be refactored.
 * TODO: This could be refactored as a method of Block or AAT
 */

public final class Helper {
    static final String TAG = "Helper";
    private Helper(){}

    /**
     * A function handling unzipping of downloaded resource files.
     * @param path
     * @param zipname
     * @return
     * @throws IOException
     */
    public static boolean unpackZip(String path, String zipname) throws IOException {
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(path + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();
                Log.w(TAG, "unpackZip: "+filename );

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    String canonicalPath = fmd.getCanonicalPath();
                    Log.w(TAG, "path: "+canonicalPath );
                    if (!canonicalPath.startsWith("/storage")) {
                        throw new SecurityException("Unsafe zip file.");
                    } else {
                        fmd.mkdirs();
                        continue;
                    }
                }

                FileOutputStream fout = new FileOutputStream(path + filename);

                while ((count = zis.read(buffer)) != -1)
                {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }
        Log.w(TAG, "unpackZip: "+"Zip unpacked" );

        return true;
    }

    /**
     * A function handling splitting of lists into equal sublists.  This is used for dividing stimuli over different blocks.
     * @param arrayList
     * @param splitInto
     * @param repeatable
     * @return
     */
    public static ArrayList<ArrayList<String>> splitArrayList(ArrayList<String> arrayList, int splitInto, boolean repeatable) {
        ArrayList<ArrayList<String>> splitList = new ArrayList<>();
        if (repeatable == false) {
            int partSize = arrayList.size() / splitInto;
            if (arrayList.size() % splitInto != 0) {
                Log.w(TAG, "splitArrayList: Can't split list with size " + arrayList.size() + " into " + splitInto + " chunks.");
            }
            for (int i = 0; i < splitInto; i++) {
                splitList.add(new ArrayList<>(arrayList.subList(i * partSize, (i + 1) * partSize)));
            }
        } else {
            splitInto = splitInto/2;
            int partSize = arrayList.size() / splitInto;
            if (arrayList.size() % splitInto != 0) {
                Log.w(TAG, "splitArrayList: Can't split list with size " + arrayList.size() + " into " + splitInto + " chunks.");
            }
            for (int i = 0; i < splitInto; i++) {
                splitList.add(new ArrayList<>(arrayList.subList(i * partSize, (i + 1) * partSize)));
            }
        }

        return splitList;
    }
}
