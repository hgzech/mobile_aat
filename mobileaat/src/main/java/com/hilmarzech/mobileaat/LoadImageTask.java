package com.hilmarzech.mobileaat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

/**
 * This class handles background loading of stimuli to improve timing in the AAT.
 */
public class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
    public Listener listener;

    /**
     * An interface informing RTView that the image was loaded.
     */
    public interface Listener {
        void onImageDownloaded(final Bitmap bitmap);
    }

    /**
     * Constructor.
     * @param listener
     */
    public LoadImageTask(final Listener listener) {
        this.listener = listener;
    }

    /**
     * Background loading and decoding of image.
     * @param paths
     * @return
     */
    @Override
    protected Bitmap doInBackground(String... paths) {
        String path = paths[0];
        Bitmap bitmap =  BitmapFactory.decodeFile(path);
        return bitmap;

    }

    /**
     * A function that calls the imagedownloaded listener.
     * @param result
     */
    @Override
    protected void onPostExecute(Bitmap result) {
        listener.onImageDownloaded(result);
    }
}



