package com.hilmarzech.mobileaat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;


/**
 * This function handles the display of stimuli.
 */
public class RtView extends androidx.appcompat.widget.AppCompatImageView implements LoadImageTask.Listener {
    static final String TAG = "RtView";
    static final int NO_FEEDBACK = 0;
    static final int POSITIVE_FEEDBACK = 1;
    static final int NEGATIVE_FEEDBACK = 2;
    // This should start with true to avoid onImageVisible to get triggered before first trial
    // imageDisplayed (accidentally) makes sure that onImageVisible gets triggered when the
    // fixation cross is displayed
    private boolean imageDisplayed = true;
    private boolean imageDrawn;
    private Choreographer choreographer;
    private Choreographer.FrameCallback frameCallback;
    private Handler stimulusHandler;
    private Handler timeoutHandler;
    public final int timeoutResource = com.hilmarzech.mobileaat.R.drawable.alarm;
    public final int timeoutTime = 2000;
    public final int feedBackTime = 1500;
    public RtViewCallback callback;
    public boolean feedbackVisible = false;
    private LoadImageTask loadImageTask;
    private Trial trial;
    private long startLoading;

    /**
     * Callbacks which have to be handled by AATActivity.
     */
    public interface RtViewCallback {
        void onImageDrawn(long time);
        void onImageVisible(long time);
        void onTimeout();
        void onTrialEnded();
        void onStimulusAboutToBePresented();
    }

    /**
     * RtView constructor.
     * @param context
     * @param attrs
     */
    public RtView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupRtView();
    }

    /**
     * RTView constructor.
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public RtView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupRtView();
    }

    /**
     * RTView constructor.
     * @param context
     */
    public RtView(Context context) {
        super(context);
        setupRtView();
    }

    /**
     * Removes callbacks if a trial gets interrupted (e.g. by phone call)
     * TODO: Interruptions should be logged and stored.
     */
    public void interruptTrial() {
        timeoutHandler.removeCallbacksAndMessages(null);
        stimulusHandler.removeCallbacksAndMessages(null);
        choreographer.removeFrameCallback(frameCallback);
    }

    /**
     * Displays feedback once a (practice) trial is completed.
     * @param feedback
     * TODO: It should be possible to choose in the JSON specification whether feedback should be displayed.
     */
    public void completeTrial(int feedback) {
        Log.d(TAG, "displayTrial: completing trial." );
        // Stopping timeout callback
        timeoutHandler.removeCallbacksAndMessages(null);
        // giving feedback
        if (feedback == NO_FEEDBACK) {
            callback.onTrialEnded();
        } else {
            if (feedback == NEGATIVE_FEEDBACK) {
                feedbackVisible = true;
                setImageResource(R.drawable.incorrect);
            } else if (feedback == POSITIVE_FEEDBACK) {
                feedbackVisible = true;
                setImageResource(R.drawable.correct);
            }
            stimulusHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    feedbackVisible = false;
                    callback.onTrialEnded();
                }
            }, feedBackTime);
        }
    }


    /**
     * Sets up the RT view.  Calculates offset between different clocks.  Sets all clocks to millisecond scale.
     */
    private void setupRtView() {
        frameCallback = new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                // Making sure displayedAt is only set once during trial
                if (!imageDisplayed) {
                    // TODO: Change everything to milliseconds
                    long offset = SystemClock.elapsedRealtime() *1000000 - SystemClock.uptimeMillis() * 1000000;
                    imageDisplayed = true;
                    callback.onImageVisible((frameTimeNanos + offset)/1000000);
                }
            }
        };
        stimulusHandler = new Handler();
        timeoutHandler = new Handler();
        choreographer = Choreographer.getInstance();
    }

    /**
     * Displays the fixation cross, starts loading the next stimulus.
     * @param trial
     * @param stimulusBitmapPath
     */
    public void displayTrial(Trial trial, final String stimulusBitmapPath) {
        this.trial = trial;
        Log.d(TAG, "displayTrial: full fixation " + trial.fixationTime);
        // Show fixation cross
        setBackgroundColor(Color.WHITE);
        Log.w(TAG, "displayTrial: Displaying fixation.");
        Log.w(TAG, "displayTrial: stimulus " + trial.imageName);
        Log.w(TAG, "displayTrial: path " + stimulusBitmapPath );
        setImageResource(com.hilmarzech.mobileaat.R.drawable.black_dot);
        this.startLoading = SystemClock.uptimeMillis();
        loadImageTask = new LoadImageTask(this);
        loadImageTask.execute(stimulusBitmapPath);
    }

    /**
     * Once the image is decoded, the remaining fixation time is re-calculated and the stimulus display is scheduled.  Schedules additional timers, such as the timeout timer, a timer warning AATActivity that the stimulus is about to be displayed, and a timer handling the length of the stimulus display.
     * @param bitmap
     */
    @Override
    public void onImageDownloaded(final Bitmap bitmap) {
        long now = SystemClock.uptimeMillis();
        long loadingTime = now - this.startLoading;
        long fixationLeft = trial.fixationTime - loadingTime;
        if (fixationLeft < 250) {
            fixationLeft = 250;
        }
        long pictureDisplayedAt = now + fixationLeft;
        trial.fixationTime = (int)(loadingTime + fixationLeft);
        Log.w(TAG, "onImageDownloaded: fixation" + trial.fixationTime );


        // Give heads up that stimulus is about to be presented (200 ms before stimulus presentation)
        stimulusHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                callback.onStimulusAboutToBePresented();
            }
        }, pictureDisplayedAt - 200);

        // Show stimulus after fixation is presented for fixationTime
        stimulusHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                // This triggers onDraw
                imageDrawn = false;
                imageDisplayed = false;
                setImageBitmap(bitmap);
            }
        }, pictureDisplayedAt);
        // Schedule timeout
        long timeoutAt = pictureDisplayedAt + timeoutTime;
        timeoutHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                callback.onTimeout();
                setImageResource(timeoutResource);
            }
        }, timeoutAt);
        // Schedule end of trial after feedback is displayed
        timeoutHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                callback.onTrialEnded();
            }
        }, timeoutAt + feedBackTime);

    }

    /**
     * A callback from the Android system which informs us that a new screen was drawn.  This time is later used to calculate reaction times, as it is the closest time we can get to the actual display of the stimulus.
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!imageDrawn && !feedbackVisible) {
            imageDrawn = true;
            callback.onImageDrawn(SystemClock.elapsedRealtime());
            //Choreographer choreographer = Choreographer.getInstance();
            choreographer.postFrameCallback(frameCallback);
        }
    }
}
