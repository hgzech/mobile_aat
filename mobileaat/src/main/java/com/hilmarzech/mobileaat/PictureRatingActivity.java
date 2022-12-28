package com.hilmarzech.mobileaat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.json.JSONException;

/**
 * This activity displays a picture rating task.
 */
public class PictureRatingActivity extends AppCompatActivity {
    private static final String TAG = "PictureRatingActivity";
    private String session_id;
    private String task_id;
    private PictureRating pictureRating;
    //private ArrayList<String> stimuli;
    //private ArrayList<PictureRatingTrial> trials;
    private RadioGroup radioGroup;
    private ImageView pictureRatingImageView;
    private TextView questionTextView;
    private TextView minTextView;
    private TextView maxTextView;
    //private int currentPictureIndex ;

    /**
     * A function creating the main view.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.hilmarzech.mobileaat.R.layout.activity_picture_rating);
        // Getting intent
        Intent intent = getIntent();
        if (intent != null) {
            this.task_id = getIntent().getStringExtra(SessionActivity.TASK_ID_EXTRA_NAME);
            this.session_id = getIntent().getStringExtra(SessionActivity.SESSION_ID_EXTRA_NAME);
        }
        // Setting up views
        this.minTextView = this.findViewById(com.hilmarzech.mobileaat.R.id.picture_rating_min_text_view);
        this.maxTextView = this.findViewById(com.hilmarzech.mobileaat.R.id.picture_rating_max_text_view);
        this.questionTextView = this.findViewById(com.hilmarzech.mobileaat.R.id.picture_rating_question_text_view);
        this.pictureRatingImageView = (ImageView) this.findViewById(com.hilmarzech.mobileaat.R.id.picture_rating_image_view);

        // Setting up radiogroup
        this.radioGroup = this.findViewById(com.hilmarzech.mobileaat.R.id.picture_rating_radio_group);
        this.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i != -1) {
                    RadioButton button = (RadioButton) radioGroup.findViewById(radioGroup.getCheckedRadioButtonId());
                    if (button.isPressed()) {
                        int index = radioGroup.indexOfChild(button);
                        pictureRated((String)radioGroup.getTag(),index+1);
                    }
                }
            }
        });

        //updateView();
    }

    /**
     * A function that saves the state of the picture rating activity when the application is paused.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (this.pictureRating.current_trial != null) {
            DatabaseHelper.savePictureRatingState(this.pictureRating, this.session_id, this);
        }
    }

    /**
     * A function that gets the picture rating state and moves to the correct picture when the activity is resumed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Getting Picture Rating from Database
            this.pictureRating = DatabaseHelper.getPictureRating(this.session_id,this.task_id, this);
            Log.w(TAG, "onResume: "+this.pictureRating );
            minTextView.setText(pictureRating.min);
            maxTextView.setText(pictureRating.max);
        } catch (JSONException e) {
            Log.e(TAG, "onCreate: We did not manage to get the picture rating");
        }
        updateView();
    }

    /**
     * This function updates the view (i.e. displays the next picture).
     */
    private void updateView() {
        // Unchecking radiogroup
        this.radioGroup.check(-1);
        this.questionTextView.setText(this.pictureRating.current_trial.question);
        //int imageResource = this.getResources().getIdentifier(this.pictureRating.current_trial.stimulus, "drawable", this.getPackageName());
        // Setting the ImageView
        //this.pictureRatingImageView.setImageResource(imageResource);
        // TODO: This should run async
        String imagePath = OfflineHelper.getStimulusBitmapPath(this.pictureRating.current_trial.stimulus, this);
        Bitmap bitmap =  BitmapFactory.decodeFile(imagePath);
        this.pictureRatingImageView.setImageBitmap(bitmap);
        this.radioGroup.setTag(this.pictureRating.current_trial.stimulus);
    }

    /**
     * This function saves the rating of a picture and moves to the next picture (or next activity if no pictures are left).
     * @param picture_name
     * @param rating
     */
    private void pictureRated(String picture_name, int rating) {
        // Save answer
        DatabaseHelper.savePictureRating(this.session_id, this.pictureRating.id, picture_name, rating, this);
        // Move to next picture
        this.pictureRating.moveToNextTrial();
        //currentPictureIndex ++;
        if (this.pictureRating.current_trial != null) {
            updateView();
        } else {
            moveToNextActivity();
        }
    }


    /**
     * This function moves to the next activity (by moving to SessionActivity)
     */
    private void moveToNextActivity() {
        //DatabaseHelper.saveTaskCompletion(session_id,this.task_id, this);
        Intent intent = new Intent(this, SessionActivity.class);
        intent.putExtra(SessionActivity.SESSION_ID_EXTRA_NAME, session_id);
        intent.putExtra(SessionActivity.TASK_ID_EXTRA_NAME, task_id);
        startActivity(intent);
        finish();
    }
}
