package com.hilmarzech.mobileaat;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

/**
 * This class stores information about trials, including information about what to display and information about the reaction.
 */
public class Trial {
    public static final int NO_RESPONSE = 0;
    public static final int PUSH_RESPONSE = 1;
    public static final int PULL_RESPONSE = 2;

    // Setup
    public int correctResponse;
    public String imageResource;
    public String imageName;
    public String group;
    //public String blockName;
    public int blockNumber;

    // Display
    public int fixationTime;
    public boolean giveFeedback;
    public boolean onlyNegativeFeedback; //200524

    // Response
    public boolean respondedCorrectly;
    public Integer reactionTime;
    public int respondedWith;
    public int trialNumber;
    public HashMap<String, Float> acceleration;
    public HashMap<String, Float> acceleration_x;
    public HashMap<String, Float> acceleration_y;
    public HashMap<String, Float> gyro_x;
    public HashMap<String, Float> gyro_y;
    public HashMap<String, Float> gyro_z;

    //public JsonObject jsonAcceleration;

    public Long drawnAtUnix;
    public Long drawnAt;
    public Long displayedAt;
    public Long reactedToAt;
    public Long clockOffset;

    public Trial(String imageName,
                 String group,
                 int correctResponse,
                 int fixation_time,
                 boolean giveFeedback,
                 boolean onlyNegativeFeedback,
                 Context context)
    {
        // Hilmar 120609
        //int imageResource = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());
        //if (imageResource == 0) { // Crash if resource is missing
        //    throw new RuntimeException("Resource not found" + imageName);
        //}
        this.acceleration = new HashMap<>();
        this.acceleration_x = new HashMap<>();
        this.acceleration_y = new HashMap<>();
        this.gyro_x = new HashMap<>();
        this.gyro_y = new HashMap<>();
        this.gyro_z = new HashMap<>();

        //this.jsonAcceleration = new JsonObject();

        this.clockOffset = null;
        this.correctResponse = correctResponse;
        this.fixationTime = fixation_time;
        this.giveFeedback = giveFeedback;
        this.onlyNegativeFeedback = onlyNegativeFeedback;
        this.group = group;
        this.imageName = imageName;
        this.imageResource = imageResource;

        //Trial trial = new Trial(imageResource, drawableName, group_name, correctResponse, this.fixation_time, giveFeedback);
    }


    public void setRespondedWith(int respondedWith) {
        this.respondedWith = respondedWith;
        respondedCorrectly = (respondedWith == correctResponse);
        if (respondedWith != Trial.NO_RESPONSE) {
            reactionTime = (int) (this.reactedToAt - this.displayedAt);
        } else {
            reactionTime = null;
        }
    }

    public void addAcceleration(long time, float x, float y, float z) {
        String string_time = Long.toString(time-this.drawnAt);
        this.acceleration.put(string_time, z);
        //this.acceleration_x.put(string_time, x);
        //this.acceleration_y.put(string_time, y);
    }

    public void addGyro(long time, float x, float y, float z) {
        String string_time = Long.toString(time-this.drawnAt);
        //this.gyro_x.put(string_time, x);
        //this.gyro_y.put(string_time, y);
        //this.gyro_z.put(string_time, z);
    }

    public JsonObject toJsonObject() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.add("acceleration",new Gson().toJsonTree(acceleration,new TypeToken<Map<String, Float>>(){}.getType()));
        jsonObject.add("acceleration_x",new Gson().toJsonTree(acceleration_x,new TypeToken<Map<String, Float>>(){}.getType()));
        jsonObject.add("acceleration_y",new Gson().toJsonTree(acceleration_y,new TypeToken<Map<String, Float>>(){}.getType()));
        jsonObject.add("gyro_x",new Gson().toJsonTree(gyro_x,new TypeToken<Map<String, Float>>(){}.getType()));
        jsonObject.add("gyro_y",new Gson().toJsonTree(gyro_y,new TypeToken<Map<String, Float>>(){}.getType()));
        jsonObject.add("gyro_z",new Gson().toJsonTree(gyro_z,new TypeToken<Map<String, Float>>(){}.getType()));
        jsonObject.addProperty("blockNumber", blockNumber);
        jsonObject.addProperty("correctResponse", correctResponse);
        jsonObject.addProperty("displayedAt", displayedAt);
        jsonObject.addProperty("drawnAt", drawnAt);
        jsonObject.addProperty("drawnAtUnix", drawnAtUnix);
        jsonObject.addProperty("fixationTime", fixationTime);
        jsonObject.addProperty("giveFeedback", giveFeedback);
        jsonObject.addProperty("group", group);
        jsonObject.addProperty("imageName", imageName);
        jsonObject.addProperty("imageResource", imageResource);
        jsonObject.addProperty("reactedToAt", reactedToAt);
        jsonObject.addProperty("reactionTime", reactionTime);
        jsonObject.addProperty("respondedCorrectly", respondedCorrectly);
        jsonObject.addProperty("respondedWith", respondedWith);
        jsonObject.addProperty("trialNumber", trialNumber);
        return jsonObject;
    }

    public String toString() {
        String response = (correctResponse == Trial.PULL_RESPONSE) ? "Pull" : "Push";
        return String.format("%s (%s): %s", imageName, group, response);
    }
}