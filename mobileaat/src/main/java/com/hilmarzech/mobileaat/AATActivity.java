package com.hilmarzech.mobileaat;

import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Handler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.json.JSONException;

/**
 * This is the core of the AAT app.  It handles the user input and stimulus display during the AAT.  It extends AccelerometerActivity, which handles sensor input and uses RTView which handles display and timing of stimuli.
 * TODO: ACCELERATION NOT SAVED AT TIMEOUT
 */
public class AATActivity extends AccelerometerActivity implements RtView.RtViewCallback {
    static final String TAG = "AATActivity";
    final int STALE_TIME_MS = 1200000000; // Times out after 20 minutes
    private RtView rtView;
    private TextView instructionsTextView;
    private FloatingActionButton actionButton;
    private Trial currentTrial;
    private Trial trialToSave;
    private Block currentBlock;
    boolean fixationVisible;
    boolean instructionsVisible;
    private Handler trackingHandler;
    private AAT aat;
    private String session_id;
    private String task_id;
    private boolean completed;
    // TODO: These should be defined in json
    //private final boolean repeat_practice_error = true;
    private final int max_practice_trials = 20;

    /**
     * Setting up the main view.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate");
        enableImmersiveMode();
        setContentView(com.hilmarzech.mobileaat.R.layout.activity_aat);
        this.completed = false; // TODO: Shouldn't this be an AAT field?
        this.aat = null; // TODO: Is this redundant?
        this.rtView = (RtView) findViewById(com.hilmarzech.mobileaat.R.id.imageView);
        this.rtView.callback = this;
        this.instructionsTextView = (TextView) findViewById(com.hilmarzech.mobileaat.R.id.instructionsTextView);
        this.actionButton = (FloatingActionButton) findViewById(com.hilmarzech.mobileaat.R.id.instruction_button);
        this.trackingHandler = new Handler();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.w(TAG, "onRestart: ");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.w(TAG, "onStart: ");

    }

    /**
     * This function creates the AAT.  It does so by getting the AAT object from the Database, checking if it is outdated, and calling makeBlocks on the AAT object.
     * TODO: This function needs refactoring.  It is calling checkOutdated twice and it might have to be placed in a different spot than onResume.
     */
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: Resuming");
        Intent intent = getIntent();
        if (intent != null) {
            this.task_id = getIntent().getStringExtra(SessionActivity.TASK_ID_EXTRA_NAME);
            this.session_id = getIntent().getStringExtra("session_id");
            try {
                Log.d(TAG, "onResume: Getting AAT");
                this.aat = DatabaseHelper.getAAT(this.session_id, this.task_id, this);
                //checkOutdated(); Should be removed
                Log.d(TAG, "onResume: Making blocks");
                this.aat.makeBlocks(this);
                // TODO: This still has to be tested
                Log.d(TAG, "onResume: Current stim:"+aat.current_stim_index );
                Log.d(TAG, "onResume: Current stim2: "+aat.blocks.get(aat.current_block_index).current_stim_index);
                for (Block block : this.aat.blocks) {
                    Log.w("BlockOutput", block.name);
                    for (Trial trial : block.trials) {
                        Log.w("BlockOutput", "\t"+trial.imageName);
                    }
                }
                //aat.blocks.get(aat.current_block_index).current_stim_index = aat.current_stim_index;
                startExperiment();
            } catch (JSONException e) {
                Log.e(TAG, "onCreate: We did not manage to get the aat" + e.toString());
            }
        }
        //checkOutdated(); // Should be removed
        super.onResume();
    }

    /**
     * This function interrupts the trial and stores the AAT state if the activity is paused.
     * TODO: There might be times in which this function does not work properly.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.w(TAG, "onPause: ");
        // If we're interrupted before the task is finished, save the state

        if (!this.completed && !this.aat.outdated)
        {
            trackingAcceleration = false;
            waitingForResponse = false;
            rtView.interruptTrial();
            // We start over with the beginning of the block
            this.aat.current_stim_index = 0;


//            if (currentTrial!= null) {
//                if (currentTrial.giveFeedback) {
//                    // If we're in a practice block set the current stimulus index to 0
//                    this.aat.current_stim_index = 0;
//                } else {
//                    // TODO: This is not totally tested
//                    this.aat.current_stim_index = currentBlock.current_stim_index;
//                }
//            }


            Log.w(TAG, "onPause: saving aat state" + new DateTime().getMillis());
            DatabaseHelper.saveAATState(this.session_id, this.aat, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.w(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.w(TAG, "onDestroy: ");
    }

    /**
     * Checks if the AAT is outdated (participants have to complete AATs within 20 minutes of starting.
     * TODO: This timeout should be specified in the AAT in the tasks.json.
     * TODO: This could be depreciated (it's not used often, can be checked for in the data, and makes code complex)
     */
    private void checkOutdated() {
        Long current_time = new DateTime().getMillis();
        // If AAT is outdated, move back to session, set session as outdated, and move to home
        Log.w(TAG, "checkOutdated: "+this.aat.saved_at+ " "+current_time );
        if (this.aat.saved_at != 0L && (current_time - aat.saved_at) > this.STALE_TIME_MS) {
            Log.w(TAG, "checkOutdated: outdated");
            this.aat.outdated = true; // Hack to get rid of outdated AATs
            displayInstructions(this.aat.out_of_date_instruction);
        }
    }

    /**
     * This function starts the AAT.
     * TODO: should be called startAAT.
     */
    private void startExperiment() {
        currentBlock = this.aat.blocks.get(this.aat.current_block_index);
        Log.w(TAG, "startExperiment: " + currentBlock.instructions );
        displayInstructions(currentBlock.instructions);
    }

    /**
     * This function enables immersive mode, which means that menu bar etc are hidden.
     */
    private void enableImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
        }
    }

    /**
     * This function moves to the next block.
     */
    private void moveToNextBlock() {
        // Increment block
        this.aat.current_block_index ++;
        // If it's the last block show experiment finished instructions
        if (this.aat.current_block_index >= this.aat.blocks.size()) {
            displayInstructions(this.aat.end_of_experiment_instruction);
            // Otherwise get the next block and display the instructions
        } else {
            currentBlock = this.aat.blocks.get(this.aat.current_block_index);
            displayInstructions(currentBlock.instructions);
        }
    }

    /**
     * This function starts the trial sequence within a block.
     * TODO: This function might need some refactoring.
     */
    private void startTrialSequence() {
        Log.d(TAG, "startTrialSequence: starting trial sequence");
        rtView.setVisibility(View.VISIBLE);
        actionButton.setVisibility(View.GONE);
        instructionsTextView.setVisibility(View.GONE);
        currentTrial = currentBlock.trials.get(currentBlock.current_stim_index);
        // TODO: Code repitition
        String stimulusBitmapPath = OfflineHelper.getStimulusBitmapPath(currentTrial.imageName, this);
        rtView.displayTrial(currentTrial, stimulusBitmapPath);
        fixationVisible = true;
    }

    /**
     * This function moves to the next trial.
     */
    private void moveToNextTrial() {
        currentBlock.current_stim_index ++;
        if (currentBlock.current_stim_index >= currentBlock.trials.size() ) {
            moveToNextBlock();
        } else {
            currentTrial = currentBlock.trials.get(currentBlock.current_stim_index);
            String stimulusBitmapPath = OfflineHelper.getStimulusBitmapPath(currentTrial.imageName, this);
            rtView.displayTrial(currentTrial, stimulusBitmapPath);
            fixationVisible = true;
        }
    }

    /**
     * This function displays instructions.
     * @param instructions
     */
    private void displayInstructions(String instructions) {
        instructionsVisible = true;
        rtView.setVisibility(View.GONE);
        // From html is depreciated but should work for a while
        instructionsTextView.setText(Html.fromHtml(instructions));
        instructionsTextView.setVisibility(View.VISIBLE);
        // TODO: This should be set visible only when we're done tracking acceleration
        actionButton.setVisibility(View.VISIBLE);
    }


    /**
     * This function saves trials to the database.
     */
    private void saveTrial() {
        DatabaseHelper.saveTrial(this.session_id,this.trialToSave,this);
    }

    /**
     * This function handles responses, storing of responses, and feedback.
     * @param pushPull
     * @param time
     */
    private void handleResponse(int pushPull, float time) {
        Log.d(TAG, "handleResponse: ");
        // Stop waiting for response
        waitingForResponse = false;
        // Track another second after response is given
        trackingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                trackingAcceleration = false;
                // Save trial when collecting accelerations is complete
                saveTrial();
            }
        }, 1000 );
        // Logging reation time
        currentTrial.reactedToAt = (long)time;
        // Logging response and accuracy
        if (pushPull == AccelerometerActivity.PUSH_RESPONSE) {
            currentTrial.setRespondedWith(Trial.PUSH_RESPONSE);
        } else if (pushPull == AccelerometerActivity.PULL_RESPONSE) {
            currentTrial.setRespondedWith(Trial.PULL_RESPONSE);
        }
        // Add incorrect practice trials back to the block
        if (currentTrial.giveFeedback && !currentTrial.respondedCorrectly && currentBlock.repeat_practice_errors && currentTrial.trialNumber < max_practice_trials) {
            currentBlock.addTrial(currentTrial, this);
        }
        giveFeedback();
    }

    /**
     * This function handles display of feedback.
     */
    private void giveFeedback() {
        int feedback = RtView.NO_FEEDBACK;
        if (currentTrial.giveFeedback) {
            if (currentTrial.respondedCorrectly & !currentTrial.onlyNegativeFeedback) {
                feedback = RtView.POSITIVE_FEEDBACK;
            } else if (!currentTrial.respondedCorrectly) {
                feedback = RtView.NEGATIVE_FEEDBACK;
            }
        }
        rtView.completeTrial(feedback);
        //rtView.completeTrial(currentTrial.giveFeedback ? (currentTrial.respondedCorrectly ? RtView.POSITIVE_FEEDBACK : RtView.NEGATIVE_FEEDBACK) : RtView.NO_FEEDBACK);
    }

    /**
     * This is an accelerometerActivity callback handling a response detection.
     * @param pushPull
     * @param time
     */
    @Override
    public void onPushPullDetected(int pushPull, float time) {
        if (waitingForResponse) {
            Log.d(TAG, "onPushPullDetected "+pushPull);
            handleResponse(pushPull, time);
        }
    }

    /**
     * This is an accelerometerActivity callback handling sensor input.
     * @param sensorType
     * @param time
     * @param x
     * @param y
     * @param z
     */
    @Override
    public void onSensorData(int sensorType, long time, float x, float y, float z) {
        if (trackingAcceleration) {
            this.trialToSave = currentTrial;
            //this.trialToSave.blockName = currentBlock.name;
            this.trialToSave.blockNumber = this.aat.current_block_index;
            // We therefore save to the last trial number
            if (fixationVisible) {
                this.trialToSave = currentBlock.trials.get(currentBlock.current_stim_index-1);
                // If instructions are visible we moved on to the next block
                // We therefore save to the last trial of the last block
            } else if  (instructionsVisible) {
                if (this.aat.current_block_index!=0) {
                    Block lastBlock = this.aat.blocks.get(this.aat.current_block_index - 1);
                    this.trialToSave = lastBlock.trials.get(lastBlock.trials.size() - 1);
                    this.trialToSave.blockNumber = this.aat.current_block_index - 1;
                }
            }
            if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                this.trialToSave.addAcceleration(time, x, y, z);
            } else if (sensorType == Sensor.TYPE_GYROSCOPE) {
                this.trialToSave.addGyro(time, x, y, z);
            }
        }
    }

    /**
     * This is an accelerometerActivity callback handling the detecting of the sensor type.
     * @param sensorType
     */
    @Override
    public void onSensorTypeDetected(int sensorType) {
        String sensorTypeString = (sensorType == AccelerometerActivity.LINEAR_ACCELERATION) ? "linear" : "simple";
        DatabaseHelper.saveSensorType(sensorTypeString, this);
    }

    /**
     * This function handles the RT-View callback that an image was drawn.
     * @param time
     */
    @Override
    public void onImageDrawn(long time) {
        if (currentTrial != null) {
            currentTrial.drawnAt = time;
            currentTrial.drawnAtUnix = new DateTime().getMillis();
            // AAT activity method
            stopMeasuringOffset();
        }
    }

    /**
     * This function handles the RT-View callback that an image is visible.
     * @param time
     */
    @Override
    public void onImageVisible(long time) {
        if (currentTrial != null) {
            currentTrial.displayedAt = time - currentTrial.drawnAt;
            fixationVisible = false;
            // AAT activity variables
            trackingAcceleration = true;
            waitingForResponse = true;
        }
    }

    /**
     * This function handles the RT-View callback that a trial has timed out.
     */
    @Override
    public void onTimeout() {
        currentTrial.setRespondedWith(Trial.NO_RESPONSE);
        // AAT activity variables
        waitingForResponse = false;
        trackingAcceleration = false;
        // Save trial
        this.trialToSave = this.currentTrial;
        saveTrial();
    }

    /**
     * This function handles the RT-View callback that the trial has ended.
     */
    @Override
    public void onTrialEnded() {
        moveToNextTrial();
    }

    /**
     * This function handles the RT-View callback that an image is about to be presented.
     */
    @Override
    public void onStimulusAboutToBePresented() {
        startMeasuringOffset();
    }

    /**
     * This function moves to the next task (via session activity.
     */
    private void moveToNextActivity() {
        this.completed = true;
        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra(SessionActivity.SESSION_ID_EXTRA_NAME, session_id);
        intent.putExtra(SessionActivity.TASK_ID_EXTRA_NAME, this.task_id);
        startActivity(intent);
        finish();
    }

    /**
     * This function handles the action button click at the end of the experiment.
     * @param view
     */
    public void actionButtonClick(View view) {
        if (this.aat.outdated) {
            Log.d(TAG, "actionButtonClick: AAT outdated" );
            Intent intent = new Intent(this, SessionActivity.class);
            intent.putExtra(SessionActivity.SESSION_ID_EXTRA_NAME, session_id);
            intent.putExtra(SessionActivity.SESSION_OUTDATED_EXTRA_NAME, true);
            startActivity(intent);
        } else {
            if (!trackingAcceleration) {
                // Are we at the end of the block
                if (currentBlock.current_stim_index >= currentBlock.trials.size()) {
                    Log.d(TAG, "actionButtonClick: end of block" );
                    // Is this the last block
                    if (this.aat.current_block_index >= this.aat.blocks.size()) {
                        Log.w(TAG, "actionButtonClick: end of experiment" );
                        moveToNextActivity();
                    }
                } else {
                    Log.d(TAG, "actionButtonClick: Not end of block" );
                    instructionsVisible = false;
                    startTrialSequence();
                }
            }
        }
    }
}