package com.hilmarzech.mobileaat;

// TODO: Functions names in this file are a bit cryptic (also, I'm not sure all functions are actually used)
import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import static com.hilmarzech.mobileaat.Session.TAG;

/**
 * This class loads json resources needed to build experiments (e.g. tasks.json).
 * It also solves their parent-child relations and handles property inheritance.
 */
public final class JsonHelper {
    private JsonHelper() {} // Private constructor makes sure that JsonHelper is never instantiated (all methods are static)

    /**
     * This function gets all condition names specified in conditions.json.
     * @param context
     * @return The list of names of conditions specified in conditions.json.
     */
    public static ArrayList<String> getConditionNames(Context context) {
        ArrayList<String> conditionNames = new ArrayList<>();
        JSONObject jsonConditionNames = loadJsonFromResource("conditions.json", context);
        Iterator<String> conditionNamesIterator = jsonConditionNames.keys();
        while (conditionNamesIterator.hasNext()) {
            conditionNames.add(conditionNamesIterator.next());
        }
        return conditionNames;
    }

    /**
     * This function is no longer used and should be cleaned up.
     * @deprecated Use getUpdatedJsonObject instead.
     * @param objectId
     * @param resource_name
     * @param context
     * @return
     * @throws JSONException
     */
    public static JSONObject getRelationalJsonObject(String objectId, String resource_name, Context context) throws JSONException {
        Log.wtf(TAG, "getRelationalJsonObject: Session update");
        JSONObject jsonObject = getUpdatedJsonObject(objectId, resource_name, context);
        if (jsonObject == null) {
            return null;
        }
        Log.wtf(TAG, "getRelationalJsonObject: Session solving relation");
        // HILMAR changed this
        //jsonObject = solveJsonObjectRelation(jsonObject, context);
        Log.wtf(TAG, "getRelationalJsonObject: Session solved relation");

        return jsonObject;
    }

    /**
     * This function parses all (updated) objects from a json specification
     * TODO: At the moment this function also returns objects that only serve as parents (which might be redundant but keeps close cleaner)
     * @param resource_name The json file from which the objects should be parsed
     * @param context
     * @return A HashMap containing the JSONObjects and keys in the
     * @throws JSONException
     */
    public static HashMap<String, JSONObject> getUpdatedJsonObjects(String resource_name, Context context) throws JSONException {
        HashMap<String, JSONObject> objects = new HashMap<>(); // This also loads parents
        JSONObject jsonObjects = loadJsonFromResource(resource_name, context);
        Iterator<String> keys = jsonObjects.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (jsonObjects.get(key) instanceof JSONObject) {
                Log.d(TAG, "Getting object " + key);
                objects.put(key, getUpdatedJsonObject(key, resource_name, context));
            }
        }
        return objects;
    };

    /**
     * This function solves inheritance in JSON resources.  Objects in the resources, by default
     * take over properties of their parents.  These properties are overwritten by properties of the
     * child object.
     * @param objectId
     * @param resource_name
     * @param context
     * @return
     * @throws JSONException
     */
    public static JSONObject getUpdatedJsonObject(String objectId, String resource_name, Context context) throws JSONException {
        // Getting the json resource with all objects
        JSONObject objects = loadJsonFromResource(resource_name, context);
        if (!objects.has(objectId)) {
            Log.w(TAG, "getUpdatedJsonObject: Can not find json object "+objectId);
            return null;
        }
        // Checking hierarchy
        ArrayList<String> hierarchy = new ArrayList<>();
        // The requested object is the lowest in the hierarchy so we put it first
        hierarchy.add(objectId);
        // Checking the parents and adding them to the hierarchy

        // TODO: Experimental function for several parents (only works for one level of hierarchy)
        if (objects.getJSONObject(objectId).has("parents")) {
            JSONArray parents = objects.getJSONObject(objectId).getJSONArray("parents");
            for (int p = 0; p < parents.length(); p++) {
                hierarchy.add(parents.getString(p));
            }
        }

        // Adding parents to the object hierarchy
        while (objects.getJSONObject(objectId).has("parent")) {
            objectId = objects.getJSONObject(objectId).getString("parent");
            hierarchy.add(objectId);
        }
        // Creating an empty json object
        JSONObject finalObject = new JSONObject();
        // Moving through the hierarchy and updating the json object
        for (int i = hierarchy.size() - 1; i >= 0; i--) {
            // Getting the parent object
            JSONObject parent = objects.getJSONObject(hierarchy.get(i));
            // Looping through the keys and updating the final object
            Iterator<String> keys = parent.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                finalObject.put(key, parent.get(key));
            }
        }
        // Adding the object id to the object
        finalObject.put("id", objectId);
        return finalObject;
    }

    /**
     * This function loads JSON resource files (e.g. tasks.json)
     * @param resource_name
     * @param context
     * @return A JSON object containing the whole resource
     */
    public static JSONObject loadJsonFromResource(String resource_name, Context context) {
        JSONObject resource = null;
        File path = context.getExternalFilesDir(
                null);
        File f = new File(path, "Resources/"+resource_name);
        Log.d(TAG, "loadJsonFromResource: "+path);
        String str = OfflineHelper.readFile(f);
        try {
            resource = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resource;
    }

    /**
     * This is a helper function that turns a JSONArray into a java arrayList.
     * @param jsonArray
     * @return
     * @throws JSONException
     */
    public static ArrayList<String> arrayListFromJsonArray(JSONArray jsonArray) throws JSONException {
        ArrayList<String> arrayList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            arrayList.add(jsonArray.getString(i));
        }
        return arrayList;
    }
}
