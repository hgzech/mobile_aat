package com.hilmarzech.mobileaat;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * This class holds information about PictureRating tasks.
 */
public class PictureRating extends Task {
    //public ArrayList<String> stimuli;
    static final String TAG = "PictureRating";
    public ArrayList<PictureRatingTrial> trials;
    //public String question;
    public String min;
    public String max;
    public int current_stim_index;
    private static final String TYPE = "picture_rating";
    public PictureRatingTrial current_trial;
    public Long random_seed;



    public PictureRating(JSONObject jsonTask, String id, JSONObject stimulus_sets, Long random_seed) throws JSONException {
        super(id, TYPE);

        //this.question = jsonTask.getString("question");
        this.min = jsonTask.getString("min");
        this.max = jsonTask.getString("max");
        this.random_seed = random_seed;
        JSONArray questions = jsonTask.getJSONArray("questions");
        // Loop through stimulus sets and add pictures to stimuli arraylist
        this.trials = new ArrayList<>();
        for (int i = 0; i < questions.length(); i++) {
            JSONObject questionObject = questions.getJSONObject(i);
            String questionText = questionObject.getString("question");
            JSONArray stimulusSetIds = questionObject.getJSONArray("for_stimuli");
            for (int j=0; j < stimulusSetIds.length(); j++) {
                String stimulusSetId = stimulusSetIds.getString(j);
                JSONArray stimulusSet = stimulus_sets.getJSONArray(stimulusSetId);
                for (int k=0; k < stimulusSet.length(); k++) {
                    this.trials.add(new PictureRatingTrial(stimulusSet.getString(k), questionText));
                }
            }
        }


        if (this.random_seed == null) {
            this.random_seed = new Random().nextLong();
        }
        Collections.shuffle(this.trials, new Random(this.random_seed));
        setCurrentStimIndex(0);
    }

    public void setCurrentStimIndex(Integer current_stim_index) {
        this.current_stim_index = current_stim_index;
        this.current_trial = this.trials.get(current_stim_index);
    }

    public void moveToNextTrial() {
        this.current_stim_index ++;
        if (this.current_stim_index < this.trials.size()) {
            this.current_trial = this.trials.get(this.current_stim_index);
            Log.w(TAG, "moveToNextTrial: "+this.current_trial.stimulus+this.current_stim_index );
        } else {
            this.current_trial = null;
        }

    }
}
