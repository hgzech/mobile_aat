package com.hilmarzech.mobileaat;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * This function creates AAT objects from JSON specifications, which are later displayed by the AAT activity.  While the AAT runs it holds information about the current block and current stimulus.
 */
public class AAT extends Task {
    private static final String TAG = "AATObject";
    private static final String TYPE = "aat"; // TODO: This is also defined in json (not used)
    public String end_of_experiment_instruction;
    public String out_of_date_instruction;
    public int current_block_index;
    public int current_stim_index;
    public long saved_at;
    public boolean outdated;
    public ArrayList<Block> blocks;
    public Long random_seed;
    private ArrayList<String> block_sequence;
    private HashMap<String, ArrayList<String>> rep_dict;
    // Setting defaults
    private int fixation_time = 1500; //Fixation time defaults to 1500ms
    private boolean give_feedback = false; //Give feedback defaults to false
    private boolean only_negative_feedback = false; //Give feedback defaults to false
    private JSONObject stimulus_sets;
    private HashMap<String, JSONObject> block_specifications;
    private Random random;

    // TODO: Fix the scope (makeBlocks always gets called after AAT(), so they could be combined)
    /**
     * A constructor for the AAT, which turns JSON specifications into AAT Objects and sets current block and stimulus positions to zero.
     * @param json
     * @param id
     * @param stimulus_sets
     * @throws JSONException
     */
    public AAT(JSONObject json, String id, JSONObject stimulus_sets, HashMap<String, JSONObject> block_specifications) throws JSONException {
        // Creating a task of type aat
        super(id, TYPE);
        // Used to check whether AAT is outdated
        this.outdated = false;
        this.saved_at = 0L;
        // Used to keep track of AAT state to re-create it at the correct position after re-start
        this.current_block_index = 0;
        this.current_stim_index = 0;
        // Used to make sure randomization can be re-produced
        // TODO: Should be re-factored to setupRandomization
        this.random_seed = 0L;
        // The stimulus sets used by the AAT
        this.stimulus_sets = stimulus_sets;
        // Initiating blocks list
        this.block_specifications = block_specifications;
        this.blocks = new ArrayList<Block>();
        this.block_sequence = JsonHelper.arrayListFromJsonArray(json.getJSONArray("blocks"));
        this.out_of_date_instruction = json.getString("out_of_date_instruction");
        // TODO: This should be specified in blocks but needs changes in AATActivity
        this.end_of_experiment_instruction = json.getString("end_of_experiment_instruction");
        // Setting fixation time (can be overwritten by block specification)
        if (json.has("fixation_time")) {
            this.fixation_time = json.getInt("fixation_time");
        }
        // Setting give feedback (can be overwritten by block specification)
        if (json.has("give_feedback")) {
            this.give_feedback = json.getBoolean("give_feedback");
        }
        if (json.has("only_negative_feedback")) {
            this.only_negative_feedback = json.getBoolean("only_negative_feedback");
        }

    }

    private void setupRandomization() {
        // Setting up randomization
        if (this.random_seed == null || this.random_seed==0L) {
            this.random_seed = System.currentTimeMillis();
        }
        this.random = new Random(random_seed);
    }

    private void addTrials(Block block, JSONObject jsonBlock, String response, Context context) throws JSONException {
        if (jsonBlock.has("fixation_time")) {
            this.fixation_time = jsonBlock.getInt("fixation_time");
        }
        if (jsonBlock.has("give_feedback")) {
            this.give_feedback = jsonBlock.getBoolean("give_feedback");
        }
        if (jsonBlock.has("only_negative_feedback")) {
            this.only_negative_feedback = jsonBlock.getBoolean("only_negative_feedback");
        }


        JSONArray non_repeats = new JSONArray();
        JSONArray force_repeats = new JSONArray();

        if (jsonBlock.has("non_repeat")) {
            non_repeats = jsonBlock.getJSONArray("non_repeat");
        }
        if (jsonBlock.has("force_repeat")) {
            force_repeats = jsonBlock.getJSONArray("force_repeat");
        }
        // Adding a key to the rep_dict
        this.rep_dict.put(String.format("%s_%s", block.name, response), new ArrayList<String>());
        // Setting correct response
        int correct_response = (response == "pull") ? Trial.PULL_RESPONSE : Trial.PUSH_RESPONSE;
        // Getting the specifications on how to build trials for response
        Log.w(TAG, "addTrials: " + response );
        Log.w(TAG, "addTrials: " + jsonBlock);
        Iterator<String> keys = jsonBlock.keys();
        while (keys.hasNext()) {
            Log.w(TAG, "addTrials, key:" + keys.next());
        }

        JSONObject response_specifications = jsonBlock.getJSONObject(response);
        Log.w(TAG, "addTrials: response_specifications = " + response_specifications );
        // Collecting stimuli from other blocks that should not be repeated
        // TODO: non_repeats should be specifiable at block level
        ArrayList<String> rem_stimuli = new ArrayList<String>();
        ArrayList<String> rep_stimuli = new ArrayList<>();
        if (response_specifications.has("non_repeat")) {
            non_repeats = response_specifications.getJSONArray("non_repeat");
        }
        if (response_specifications.has("force_repeat")) {
            force_repeats = response_specifications.getJSONArray("force_repeat");
        }
        for (String non_repeat : JsonHelper.arrayListFromJsonArray(non_repeats)) {
            if (rep_dict.containsKey(non_repeat)) {
                rem_stimuli.addAll(rep_dict.get(non_repeat));
            }
        }
        for (String force_repeat : JsonHelper.arrayListFromJsonArray(force_repeats)) {
            if (rep_dict.containsKey(force_repeat)) {
                rep_stimuli.addAll(rep_dict.get(force_repeat));
            }
        }
        Log.d(TAG, "addTrials, rem_stimuli: " + rem_stimuli);
        // Getting picture set specifications
        JSONArray set_specifications = response_specifications.getJSONArray("stimuli");
        Log.d(TAG, "addTrials:, set_specifications" + set_specifications);
        // Looping through set specifications and making trials
        for (int j = 0; j < set_specifications.length(); j++) {
            JSONObject set_specification = set_specifications.getJSONObject(j);
            String from = set_specification.getString("from");
            Integer pick = set_specification.getInt("pick");
            Integer repeat = 1;
            if (set_specification.has("repeat")) {
                repeat = set_specification.getInt("repeat");
            }
            // Getting the set
            Log.d(TAG, "addTrials, from: " + from);
            ArrayList<String> stimulus_set = JsonHelper.arrayListFromJsonArray(this.stimulus_sets.getJSONArray(from));
            // Removing non-repeatable stimuli
            stimulus_set.removeAll(rem_stimuli);
            // Forcing repeats (this should first repeat things and then add new stimuli)
            ArrayList<String> first_half = new ArrayList<>(stimulus_set);
            first_half.retainAll(rep_stimuli);
            ArrayList<String> second_half = new ArrayList<>(stimulus_set);
            second_half.removeAll(rep_stimuli);
            Collections.shuffle(first_half, random);
            Collections.shuffle(second_half, random);
            first_half.addAll(second_half);
            stimulus_set = first_half;
            Log.d(TAG, "addTrials, stimulus_set" + stimulus_set);
            //stimulus_set.retainAll(rep_stimuli);
            // Randomizing stimulus set
            // Collections.shuffle(stimulus_set, random); // TODO: Check if shuffling works
            // Looping through stimuli and adding trials
            for (int k = 0; k < pick; k++) {
                String stimulus = stimulus_set.get(k);
                Log.d(TAG, "addTrials, stimulus:" + stimulus);
                rep_dict.get(String.format("%s_%s", block.name, response)).add(stimulus);
                rep_dict.get(String.format("%s", block.name)).add(stimulus);
                //Trial trial = new Trial(stimulus, from, response, )
                Trial trial = new Trial(stimulus, from, correct_response, this.fixation_time, this.give_feedback, this.only_negative_feedback, context);
                for (int l = 0; l < repeat; l ++) {
                    block.addTrial(trial, context);
                }
            }
        }
    }

    /**
     * This function gets called by AAT activity to get the blocks of this particular AAT
     * TODO: Perhaps this should happen during init?
     */
    //public ArrayList<Block> makeBlocks(Context context) throws JSONException {
    public void makeBlocks(Context context) throws JSONException {
        setupRandomization();
        // This dictionary keeps track of stimulus repetitions
        this.rep_dict = new HashMap<>();
        // Looping through block specifications and building blocks
        this.blocks = new ArrayList<>();
        for (String block_name : this.block_sequence) {
            // Creating a block and adding basic specifications (no trials, yet)
            JSONObject jsonBlock = block_specifications.get(block_name);
            Log.d(TAG, "makeBlocks: making block" + block_name);
            Block block = new Block(jsonBlock);
            // Adding a key to the non_repeat dict
            this.rep_dict.put(String.format("%s", block.name), new ArrayList<String>());
            // Adding trials
            Log.w(TAG, "makeBlocks: "+jsonBlock.toString() );
            addTrials(block, jsonBlock, "pull", context);
            addTrials(block, jsonBlock, "push", context);
            // Shuffling trials
            block.shuffleTrials(new Random(this.random.nextLong()));
            // Adding block
            this.blocks.add(block);
        }
        // TODO: Does this also handle current block index??
        // This line recreates an AAT if current stim index is given
        this.blocks.get(current_block_index).current_stim_index = this.current_stim_index;
        // Uncomment this for debugging
        log_block_overviews();

    }

    public void log_block_overviews() {
        for (Block test_block : this.blocks) {
            Log.wtf(TAG, "getBlocks Blockname: " + test_block.name);
            for (Trial test_trial : test_block.trials) {
                Log.wtf(TAG, "getBlocks: \t" + test_trial.imageName+","+test_trial.correctResponse+","+test_trial.giveFeedback);
            }
        }
    }
}
