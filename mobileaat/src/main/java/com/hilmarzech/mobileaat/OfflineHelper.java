package com.hilmarzech.mobileaat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.google.gson.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This class handles offline storage.
 */
public final class OfflineHelper {

    static final String TAG = "DatabaseHelper";
    static final String RESOURCES_ZIP_NAME = "Resources.zip";

    public static File getResourcesZipFile(Context context) {
        File path = context.getExternalFilesDir(
                null);
        File resourcesZipFile = new File(path, RESOURCES_ZIP_NAME );
        return resourcesZipFile;
    }

    public static String getStimulusBitmapPath(String stimulusName, Context context) {
        String stimulusPath = context.getExternalFilesDir(
                String.format("Resources/stimuli/%s", stimulusName)).getPath();
        return stimulusPath;
    }


    public static void saveAnswer(String participantId, String sessionId, String questionnaireId, String questionId, String answer, Context context) throws JSONException {
       // String key = String.format("%s/%d/%s/%s.json", sessionId, repetition, questionnaireId, questionId);
        String path = String.format("Data/%s/%s/%s/", sessionId,participantId, questionnaireId);
        String fileName = String.format("%s.json", questionId);
        //key = key.replace("/", "-");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("answer",answer);
        //Log.d(TAG, "saveAnswer: "+key);
        saveJson(jsonObject.toString(),path,fileName,context);
    }


    public static void savePictureRating(String participantId, String sessionId, String picture_rating_id, String picture_name, int picture_rating, Context context) throws JSONException {
        // String key = String.format("%s/%d/%s/%s.json", sessionId, repetition, questionnaireId, questionId);
        String path = String.format("Data/%s/%s/%s/", sessionId,participantId, picture_rating_id);
        String fileName = String.format("%s.json", picture_name);
        //key = key.replace("/", "-");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("answer",picture_rating);
        //Log.d(TAG, "saveAnswer: "+key);
        saveJson(jsonObject.toString(),path,fileName,context);
    }

    public static void saveTrial(String participantId,
                                 String session_id,
                                 Trial trial,
                                 Context context) throws JSONException {
        String path = String.format("Data/%s/%s/AAT/blocks/%d/", session_id,participantId, trial.blockNumber);
        String fileName = String.format("%d.json", trial.trialNumber);
        String content = trial.toJsonObject().toString();
        Log.d(TAG, "saveTrial: "+content);
        saveJson(content,path, fileName, context);
    }

    public static void saveJson(String content, String folder, String fileName, Context context) {
        File path = context.getExternalFilesDir(
                null);
        path = new File(path, folder);
        path.mkdirs();
        File file = new File(path, fileName );
        FileOutputStream outputStream;
        try {
            file.createNewFile();
            outputStream = new FileOutputStream(file);
            outputStream.write(content.getBytes());
            outputStream.flush();
            outputStream.close();
            //TODO: Comment this out for final
            MediaScannerConnection.scanFile(context, new String[] { file.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });
        } catch (Exception e) {
            Log.d("problem", "saveToLocalStorage: ");
            e.printStackTrace();
        }
    }

    public static String readFile(File file){
        String line = null;

        try {
            FileInputStream fileInputStream = new FileInputStream (file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuilder stringBuilder = new StringBuilder();

            while ( (line = bufferedReader.readLine()) != null )
            {
                stringBuilder.append(line + System.getProperty("line.separator"));
            }
            fileInputStream.close();
            line = stringBuilder.toString();

            bufferedReader.close();
        }
        catch(FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }
        catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return line;
    }

    public static JsonObject traverse (File dir, JsonObject jdata) {


        //HashMap<String, String> data = new HashMap<>();

        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    traverse(file, jdata);
                } else {
                    Log.wtf(TAG, "traverse: "+file );

                    jdata.addProperty(file.getPath(), readFile(file));
                    //data.put(file.getPath(), readFile(file));
                    // do something here with the file
                }
            }
        }
        return jdata;
    }

    public static void makeBackupFile(Context context) {
        //File path = Environment.getExternalStoragePublicDirectory(
        //        Environment.DIRECTORY_DOWNLOADS);
        File path = context.getExternalFilesDir(
                null);
        path = new File(path, "Data");

        JsonObject jdata = new JsonObject();

        jdata = traverse(path, jdata);
        saveJson(jdata.toString(),"Backup",String.format("%s.aat", DatabaseHelper.getParticipantID(context)),context);

        MediaScannerConnection.scanFile(context, new String[] { path.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });

    }
}
