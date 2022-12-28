package com.hilmarzech.mobileaat;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

/**
 * This class handles AAT blocks.
 * TODO: If "pick" is not specified, it should be set to the length of the stimulus set
 * TODO: If "repeat" is not specified, it should be set to 1
 */
public class Block {
    // Stimulus presentation
    public String instructions;
    public ArrayList<Trial> trials;
    // Data storage
    public String name;
    public int current_stim_index;
    public boolean repeat_practice_errors = true;
    public static String TAG = "Block";


    public Block(JSONObject jsonBlock) throws JSONException {
        current_stim_index = 0;
        this.trials = new ArrayList<>();
        // Getting the block name
        this.name = jsonBlock.getString("id"); // This should be set by AAT
        // Getting the block instructions
        this.instructions = jsonBlock.getString("instructions");

        if (jsonBlock.has("repeat_practice_errors")) {
            this.repeat_practice_errors = jsonBlock.getBoolean("repeat_practice_errors");
        }
        Log.w(TAG, "Block: " +  this.instructions);
    }

    /**
     * A function creating trial objects.
     * @param trial
     * @param context
     */
    public void addTrial(Trial trial, Context context) {
        Trial newTrial = new Trial(trial.imageName, trial.group, trial.correctResponse, trial.fixationTime, trial.giveFeedback, trial.onlyNegativeFeedback, context);
        newTrial.trialNumber = this.trials.size()+1;
        this.trials.add(newTrial);
    }

    /**
     * A function for shuffling trials.
     * @param random
     */
    public void shuffleTrials(Random random) {
        Collections.shuffle(this.trials, random);
        for (int i = 0; i < this.trials.size(); i++) {
            this.trials.get(i).trialNumber = i + 1;
        }
    }
}