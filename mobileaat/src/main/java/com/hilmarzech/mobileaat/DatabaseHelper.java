package com.hilmarzech.mobileaat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * This class coordinates data handling at a high level. The version of the app is supported by Firebase which
 * handles most upstream data saving Lower level data operations are handled by OfflineHelper, JsonHelper, and FirebaseHelper.
 */
public final class DatabaseHelper {
    static final String TAG = "DatabaseHelper";

    private DatabaseHelper(){} // private constructor (all methods are abstract)

    public static void downloadResources(String experiment_name, FirebaseHelper.ResourcesDownloadedListener listener, Context context) {
        File resourcesZipFile = OfflineHelper.getResourcesZipFile(context);
        FirebaseHelper.downloadResources(experiment_name,resourcesZipFile, listener);
    }

    public static void saveExperiment(String experiment, Context context) {
        Log.w(TAG, "saveExperiment: Saving experiment "+experiment);
        getDefaultSharedPreferences(context).edit().putString("experiment", experiment).apply();
    }

    public static String getExperiment(Context context) {
        Log.w(TAG, "getExperiment: Getting experiment.");
        return getDefaultSharedPreferences(context).getString("experiment", null);
    }

    // TODO: Make a shared preferences helper? Might make sense.
    public static String addParticipant(Context context) {
        // Create a participant id based on UNIX milliseconds (safe) in base36 (short)
        DateTime now = new DateTime();
        String participantId = Long.toString(now.getMillis(), 36);
        String publicParticipantId = Long.toString(now.getMillis(), 35);
        // Save it to FireBase
        //FirebaseHelper.saveParticipant(participantId);
        // Save participant to sharedPrefs
        getDefaultSharedPreferences(context).edit().putString("participantId", participantId).apply();
        getDefaultSharedPreferences(context).edit().putString("publicParticipantId", publicParticipantId).apply();
        Log.d(TAG, "addParticipant: private" + participantId);
        Log.d(TAG, "addParticipant: public" + publicParticipantId);

        return participantId;
    }

    public static void saveAnswer(String sessionId, Answer answer, Context context) {
        String participantId = getParticipantID(context);
        String experiment_name = getExperiment(context);
        sessionId = checkSessionCompletionId(sessionId, context);
        if (!answer.answers.isEmpty()) {
            answer.answer = TextUtils.join(", ", answer.answers);
        }
        FirebaseHelper.saveAnswer(experiment_name, participantId,sessionId,answer.questionnaireID,answer.questionID,answer.answer);
        try {
            OfflineHelper.saveAnswer(participantId,sessionId, answer.questionnaireID, answer.questionID, answer.answer, context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void deletePreferences(Context context) {
        getDefaultSharedPreferences(context).edit().clear().commit();
    }


    public static String getParticipantID(Context context) {
        return getDefaultSharedPreferences(context).getString("participantId", null);
    }

    public static String getPublicParticipantID(Context context) {
        String public_id = getDefaultSharedPreferences(context).getString("publicParticipantId", null);
        Log.d(TAG, "getPublicParticipantID: "+public_id);
        return public_id;
    }

    public static void saveCompletion(boolean completion, final Context context) {
        FirebaseHelper.saveCompletion(getExperiment(context), getParticipantID(context), completion);
    }

    /**
     * Choose condition with least sign ups (not based on completed conditions)
     */
    public static void chooseCondition(final FirebaseHelper.ConditionChosenListener listener, final Context context) {
        FirebaseHelper.saveParticipant(getExperiment(context), getParticipantID(context));
        FirebaseHelper.chooseCondition(getExperiment(context), JsonHelper.getConditionNames(context), listener, context);
    }

    public static void saveModel(Context context, String model) {
        FirebaseHelper.saveModel(getExperiment(context), getParticipantID(context), model);
    }

    public static void saveConditionName(String conditionName, Context context) {
        getDefaultSharedPreferences(context).edit().putString("condition_name", conditionName).apply();
        FirebaseHelper.saveCondition(getExperiment(context), getParticipantID(context),conditionName);
    }

    public static String getConditionName(Context context) {
        return getDefaultSharedPreferences(context).getString("condition_name", null);
    }

    public static Boolean checkTaskCompletion(String sessionId, String taskId, Context context) {
        return getDefaultSharedPreferences(context).getBoolean(sessionId+taskId, false);
    }

    public static void saveTaskCompletion(String sessionId, String taskId, boolean completed, Context context) {
        Log.d(TAG, "saveTaskCompletion: saving");
        getDefaultSharedPreferences(context).edit().putBoolean(sessionId+taskId, completed).apply();
    }

    public static Long checkSessionCompletion(String sessionId, Context context) {
        return getDefaultSharedPreferences(context).getLong(sessionId, 0L);
    }

    public static void saveSessionCompletion(String sessionId, Context context) {
        Long time = new DateTime().getMillis();
        // We only log the completion date once
        if (checkSessionCompletion(sessionId, context) == 0L) {
            getDefaultSharedPreferences(context).edit().putLong(sessionId, time).apply();
        }
        incrementSessionCompletionCount(sessionId, context); // Incrementing session completion count if session is repeating
    }

    public static Long checkSessionCompletionCount(String sessionId, Context context) {
        return getDefaultSharedPreferences(context).getLong(sessionId+"_count", 1L);
    }

    public static String checkSessionCompletionId(String sessionId, Context context) {
        Long sessionCompletionCount = checkSessionCompletionCount(sessionId, context);
        if (sessionCompletionCount > 1) {
            sessionId = String.format("%s_%d", sessionId, sessionCompletionCount);
        }
        return sessionId;
    }

    public static void incrementSessionCompletionCount(String sessionId, Context context) {
        Long sessionCompletionCount = checkSessionCompletionCount(sessionId, context);
        sessionCompletionCount += 1;
        getDefaultSharedPreferences(context).edit().putLong(sessionId+"_count", sessionCompletionCount).apply();
    }

    public static Boolean checkSessionTimeout(String sessionId, Context context) {
        return getDefaultSharedPreferences(context).getBoolean(sessionId+"time_out", false);
    }

    public static void saveSessionTimeOut(String sessionId, Context context) {
        getDefaultSharedPreferences(context).edit().putBoolean(sessionId+"time_out", true).apply();
    }

    public static Boolean checkSessionReminderSet(String sessionId, Context context) {
        return getDefaultSharedPreferences(context).getBoolean(sessionId+"reminder", false);
    }

    public static void saveSessionReminderSet(String sessionId, Context context) {
        getDefaultSharedPreferences(context).edit().putBoolean(sessionId+"reminder", true).apply();
    }


    public static ArrayList<Session> getSessions(String condition, Context context) throws JSONException {
        ArrayList<Session> sessions = new ArrayList<>();
        JSONObject jsonCondition = JsonHelper.getUpdatedJsonObject(condition, "conditions.json", context);

        JSONArray sessionIds = null;
        if (jsonCondition != null) {
            sessionIds = jsonCondition.getJSONArray("sessions");
        } else {
            Log.w(TAG, "getSessions: Can't get sessions from condition='null'" );
        }
        if (sessionIds != null) {
            for (int i = 0; i < sessionIds.length(); i++) {
                Log.wtf(TAG, "getSessions: Get Session");
                Session session = getSession(sessionIds.getString(i), context);
                Log.wtf(TAG, "getSessions: Got Session");

                Long session_completed = checkSessionCompletion(session.id, context);
                //Log.wtf(TAG, "getSessions: checked session completion "+session.name+session_completed);
                if (session_completed != 0L) {
                    session.completedAt = session_completed;
                }
                if (checkSessionTimeout(sessionIds.getString(i), context)) {
                    //session.timed_out = true;
                    session.timed_out = false;
                }
                if (checkSessionReminderSet(sessionIds.getString(i),context)) {
                    session.reminder_scheduled = true;
                }
                sessions.add(session);
            }
        } else {
            Log.w(TAG, "getSessions: Could not get sessions from condition "+jsonCondition);
        }
        return sessions;
    }

    public static void saveSensorType(String sensorType, Context context) {
        FirebaseHelper.saveSensorType(getExperiment(context), getParticipantID(context),sensorType);
    }

    public static void savePictureRating(String session_id, String picture_rating_id, String picture_name, int rating, Context context) {
        String participantId = getParticipantID(context);

        FirebaseHelper.savePictureRating(
                getExperiment(context),
                participantId,
                checkSessionCompletionId(session_id, context),
                picture_rating_id,
                picture_name,
                rating);
        try {
            OfflineHelper.savePictureRating(participantId, session_id, picture_rating_id, picture_name, rating, context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void saveTrial(String session_id, Trial trial, Context context) {
        String participantId = getParticipantID(context);

        FirebaseHelper.saveTrial(
                getExperiment(context),
                getParticipantID(context),
                checkSessionCompletionId(session_id, context),
                trial
        );
        try {
            OfflineHelper.saveTrial(participantId,session_id,trial,context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String getString(String string, Context context) {
        return string.replace("PARTICIPANT_ID", DatabaseHelper.getParticipantID(context));
    }

    public static Questionnaire getQuestionnaire(String questionnaireID, Context context) throws JSONException {
        JSONObject jsonQuestionnaire = JsonHelper.getRelationalJsonObject(questionnaireID, "tasks.json", context);
        return new Questionnaire(jsonQuestionnaire, questionnaireID);
    }

    public static Task getTask(String taskId, Context context) throws JSONException {
        Log.wtf(TAG, "getTask: Session getting task");
        JSONObject jsonTask = JsonHelper.getUpdatedJsonObject(taskId, "tasks.json", context);
        Log.wtf(TAG, "getTask: Session Creating task");
        Task task = new Task(jsonTask, taskId);
        Log.wtf(TAG, "getTask: Session Created task");

        return task;
    }

    public static Session getSession(String sessionID, Context context) throws JSONException {
        Log.wtf(TAG, "getSession: getSession: "+sessionID);
        JSONObject jsonSession = JsonHelper.getUpdatedJsonObject(sessionID, "sessions.json", context);
        Log.wtf(TAG, "getSession: gotSession: "+sessionID);
        Session session = new Session(sessionID,jsonSession);
        Long session_completed = checkSessionCompletion(session.id, context);
        if (session_completed != 0L) {
            session.completedAt = session_completed;
        }
        return session;
    }

    public static PictureRating getPictureRating(String sessionId, String pictureRatingID, Context context) throws JSONException {
        JSONObject jsonPictureRating = JsonHelper.getRelationalJsonObject(pictureRatingID, "tasks.json", context);
        JSONObject stimulus_sets = JsonHelper.loadJsonFromResource("stimulus_sets.json", context);
        Long random_seed = getDefaultSharedPreferences(context).getLong(String.format("%s_%s_seed",sessionId, pictureRatingID), 0L);
        PictureRating pictureRating;
        if (random_seed != 0L) {
            pictureRating = new PictureRating(jsonPictureRating, pictureRatingID, stimulus_sets, random_seed);
            Integer current_stim = getDefaultSharedPreferences(context).getInt(String.format("%s_%s_stim",sessionId, pictureRatingID), 0);
            pictureRating.setCurrentStimIndex(current_stim);
        } else {
            pictureRating = new PictureRating(jsonPictureRating, pictureRatingID, stimulus_sets, null);
        }
        return pictureRating;
    }

    public static void savePictureRatingState(PictureRating pictureRating, String sessionId, Context context) {
        SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
        editor.putLong(String.format("%s_%s_seed",sessionId, pictureRating.id), pictureRating.random_seed).apply();
        editor.putInt(String.format("%s_%s_stim",sessionId, pictureRating.id), pictureRating.current_stim_index).apply();
    }

    public static String getAATTimeKey(String sessionId, String aatID) {
        return String.format("%s_%s_time", sessionId, aatID);
    }

    public static String getAATSeedKey(String sessionId, String aatID) {
        return String.format("%s_%s_seed", sessionId, aatID);
    }

    public static String getAATBlockKey(String sessionId, String aatID) {
        return String.format("%s_%s_block", sessionId, aatID);
    }

    public static String getAATStimulusKey(String sessionId, String aatID) {
        return String.format("%s_%s_stim", sessionId, aatID);
    }

    public static void saveAATState(String sessionID, AAT aat, Context context)
    {
        // Check if we paused at completion (while end of experiment instructions visible)
        if (aat.current_block_index >= aat.blocks.size()) {
            // Save task as completed
            // If paused at completion save task completion
            saveTaskCompletion(sessionID,aat.id, true, context);
            try {
                // Check if the session is completed
                Session session = getSession(sessionID, context);
                // TODO: This is copyed one to one from session
                boolean sessionCompleted = true;
                for (String taskId : session.tasks) {
                    if (!DatabaseHelper.checkTaskCompletion(sessionID, taskId,context)) {
                        sessionCompleted = false;
                        break;
                    }
                }
                // If the session is completed save it as completed
                if (sessionCompleted) {
                    saveSessionCompletion(sessionID, context);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            Long time = new DateTime().getMillis();
            SharedPreferences.Editor editor = getDefaultSharedPreferences(context).edit();
            editor.putLong(getAATTimeKey(sessionID, aat.id), time).apply();
            editor.putLong(getAATSeedKey(sessionID, aat.id), aat.random_seed).apply();
            Log.w(TAG, "saveAATState: "+aat.current_stim_index );
            editor.putInt(getAATBlockKey(sessionID, aat.id), aat.current_block_index).apply();
            editor.putInt(getAATStimulusKey(sessionID, aat.id), aat.current_stim_index).apply();
        }
    }


    // This function returns null if a stale AAT is requested
    public static AAT getAAT(String sessionID, String aatID, Context context) throws JSONException {
        // Create the AAT
        Log.wtf(TAG, "getAAT: Getting jsonAAT");
        JSONObject jsonAAT = JsonHelper.getRelationalJsonObject(aatID, "tasks.json", context);
        Log.wtf(TAG, "getAAT: Loading stimuli");
        JSONObject stimulus_sets = JsonHelper.loadJsonFromResource("stimulus_sets.json", context);
        // Loading hierarchical blocks separately
        HashMap<String, JSONObject> blocks_specifications = JsonHelper.getUpdatedJsonObjects("blocks.json", context);
        Log.w(TAG, "getAAT: "+stimulus_sets.toString() );
        Log.wtf(TAG, "getAAT: Creating AAT");
        AAT aat = new AAT(jsonAAT, aatID, stimulus_sets, blocks_specifications);
        // Check if AAT is being recreated. If it's not stale
        Long time = getDefaultSharedPreferences(context).getLong(getAATTimeKey(sessionID, aatID), 0L);
        Log.wtf(TAG, "getAAT: Adjusting AAT");
        aat.saved_at = time;
        if (time != 0L) {
            // If it was recently interrupted (< 30 minutes), start it were it was left of
            aat.random_seed = getDefaultSharedPreferences(context).getLong(getAATSeedKey(sessionID, aatID), 0L);
            aat.current_block_index = getDefaultSharedPreferences(context).getInt(getAATBlockKey(sessionID, aatID), 0);
            aat.current_stim_index = getDefaultSharedPreferences(context).getInt(getAATStimulusKey(sessionID, aatID), 0);
        }
        Log.w(TAG, "getAAT: "+aat.current_stim_index );
        return aat;
    }
}
