package com.hilmarzech.mobileaat;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class handles communication with the online server.
 */
public final class FirebaseHelper {
    static final String PARTICIPANTS_ROOT = "pps";
    static final String SIGNED_UP_KEY = "signed_up";
    static final String COMPLETED_KEY = "completed";
    static final String TAG = "FirebaseHelper";


    private FirebaseHelper(){}


    // An interface for other classes which wait for the condition to be chosen.
    public interface ConditionChosenListener {
        void onConditionChosen(String conditionName);
        void onFailed();
    }

    public interface ResourcesDownloadedListener {
        void onResourcesDownloaded(String experiment_name);
        void onExperimentDoesNotExist();
        void onResourceDownloadFailed();
    }


    public static void downloadResources(final String experiment_name, final File resourcesZipFile, final ResourcesDownloadedListener listener) {


        StorageReference ref = FirebaseStorage.getInstance().getReference();
        StorageReference res = ref.child(String.format("%s/Resources.zip", experiment_name));
        res.getFile(resourcesZipFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                try {
                    Helper.unpackZip(resourcesZipFile.getParentFile().getPath()+'/', resourcesZipFile.getName());
                    File directory = new File(String.format("%s/Resources", resourcesZipFile.getParentFile().getPath()));
                    File[] files = directory.listFiles();
                    Log.w("Files", "Size: "+ files.length);
                    for (int i = 0; i < files.length; i++)
                    {
                        Log.w("Files", "FileName:" + files[i].getName());
                    }
                    listener.onResourcesDownloaded(experiment_name);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

                if (exception.getClass().equals(StorageException.class)) {
                    StorageException storageException = (StorageException)exception;
                    if (storageException.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        listener.onExperimentDoesNotExist();
                    }
                }


                Log.w(TAG, "onFailure: hilmar"+exception.getClass());
                // Handle any errors
            }
        });
    }


    // Convenience function to create time stamps
    public static String getTimeStamp() {
        DateTimeFormatter dtfOut = DateTimeFormat.forPattern("dd MMM yyyy HH:mm:ss");
        return dtfOut.print(new DateTime());
    }

    // Convenience function to save to firebase
    // TODO: Not essential single line function
    public static void saveToFirebase(String reference, Object value) {
        FirebaseDatabase.getInstance().getReference(reference).setValue(value);
    }

    // Convenience function to save participant
    public static void saveParticipant(String experiment_name, String participantId) {
        String key = String.format("%s/%s/%s/%s", experiment_name,PARTICIPANTS_ROOT, participantId, SIGNED_UP_KEY);
        FirebaseHelper.saveToFirebase(key, getTimeStamp());
        saveCompletion(experiment_name, participantId, false);
    }

    public static void saveModel(String experiment_name, String participantId, String modelId) {
        String key = String.format("%s/%s/%s/device", experiment_name, PARTICIPANTS_ROOT, participantId);
        FirebaseHelper.saveToFirebase(key, modelId);
    }

    public static void saveCondition(String experiment_name, String participantId, String conditionName) {
        String key = String.format("%s/%s/%s/condition", experiment_name, PARTICIPANTS_ROOT, participantId);
        FirebaseHelper.saveToFirebase(key, conditionName);
    }

    public static void saveCompletion(String experiment_name, String participantId, boolean completion) {
        String key = String.format("%s/%s/%s/completion", experiment_name, PARTICIPANTS_ROOT, participantId);
        FirebaseHelper.saveToFirebase(key, completion);
    }

    // Function that saves answers
    // TODO: It might be easier to make answer objects with time stamp and save the object to firebase
    public static void saveAnswer(String experiment_name, String participantId, String sessionId, String questionnaireId, String questionId, String answer) {
        String key = String.format("%s/%s/%s/%s/%s/%s/answer", experiment_name, PARTICIPANTS_ROOT,participantId, sessionId, questionnaireId, questionId);
        saveToFirebase(key, answer);
        String time_key = String.format("%s/%s/%s/%s/%s/%s/time", experiment_name, PARTICIPANTS_ROOT,participantId, sessionId, questionnaireId, questionId);
        saveToFirebase(time_key,getTimeStamp());
    }

    // Function that saves picture ratings
    // TODO: Picture ratings should perhaps be merged with questionnaires and ratings saved as answers
    public static void savePictureRating(String experiment_name,
                                         String participant_id,
                                         String session_id,
                                         String picture_rating_id,
                                         String picture_name,
                                         int picture_rating) {
        if (picture_name.contains(".")) {
            picture_name = picture_name.substring(0, picture_name.indexOf("."));
        }
        String key = String.format("%s/%s/%s/%s/%s/%s/rating", experiment_name, PARTICIPANTS_ROOT,participant_id,session_id,picture_rating_id,picture_name);
        saveToFirebase(key, picture_rating);
        String time_key = String.format("%s/%s/%s/%s/%s/%s/time", experiment_name, PARTICIPANTS_ROOT,participant_id, session_id,picture_rating_id, picture_name);
        saveToFirebase(time_key,getTimeStamp());
    }

    // Function that saves sensor type
    public static void saveSensorType(String experiment_name,
                                      String participant_id,
                                      String sensor_type) {
        String key = String.format("%s/%s/%s/sensor_type", experiment_name, PARTICIPANTS_ROOT,participant_id);
        saveToFirebase(key, sensor_type);
    }

    // Function that saves trial (structure determined by Trial class)
    public static void saveTrial(String experiment_name,
                                 String participant_id,
                                 String session_id,
                                 Trial trial) {
        String key = String.format("%s/%s/%s/%s/AAT/blocks/%d/%d", experiment_name, PARTICIPANTS_ROOT,participant_id, session_id,trial.blockNumber, trial.trialNumber);
        saveToFirebase(key, trial);
    }

    // The following three function find the least used condition and return them
    // TODO: Seems a bit complex, perhaps this could be handled by firebase?
    public static void initializeConditions(final String experiment_name, final ArrayList<String> conditionNames, final ConditionChosenListener listener, Context context) {
        // Create condition counts with first condition set to 1
        HashMap<String, Long> conditionCounts = new HashMap<>(); // Firebase translates int to long so to avoid confusion we're working with longs to begin with
        //final ArrayList<String> conditionNames = getConditionNames(context);
        Long count = 1L;
        for (String conditionName : conditionNames) {
            Log.d(TAG, "initializeConditions: "+conditionName);
            conditionCounts.put(conditionName, count);
            count = 0L;
        }
        // Initialize condition counts on Firebase and set first condition to 1
        DatabaseReference conditionCountsReference = FirebaseDatabase.getInstance().getReference(String.format("%s/condition_counts", experiment_name));
        conditionCountsReference.setValue(conditionCounts, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError==null) {
                    listener.onConditionChosen(conditionNames.get(0));
                } else {
                    listener.onFailed();
                }
            }
        });
    }

    public static void allocateCondition(final DataSnapshot conditionSnapshot, final ConditionChosenListener listener) {
        conditionSnapshot.getRef().setValue((Long)conditionSnapshot.getValue() + 1, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError==null) {
                    listener.onConditionChosen(conditionSnapshot.getKey());
                } else {
                    listener.onFailed();
                }
            }
        });
    }

    public static void chooseCondition(final String experiment_name, final ArrayList<String> conditionNames, final ConditionChosenListener listener, final Context context) {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(String.format("%s/condition_counts", experiment_name));
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (! dataSnapshot.hasChildren()) {
                    initializeConditions(experiment_name, conditionNames, listener, context);
                } else {
                    // Check least used condition
                    DataSnapshot leastUsedCondition = null;
                    ArrayList<DataSnapshot> leastUsedConditions = null;
                    for (DataSnapshot conditionSnapshot : dataSnapshot.getChildren()) {
                        if (leastUsedCondition == null || (Long)conditionSnapshot.getValue() < (Long)leastUsedCondition.getValue()) {
                            leastUsedCondition = conditionSnapshot;
                            leastUsedConditions = new ArrayList<>();
                            leastUsedConditions.add(leastUsedCondition);
                        } else if (leastUsedCondition != null && conditionSnapshot.getValue() == leastUsedCondition.getValue()) {
                            leastUsedConditions.add(conditionSnapshot);
                        }
                    }
                    Collections.shuffle(leastUsedConditions); // This is not super-necessary
                    leastUsedCondition = leastUsedConditions.get(0);
                    allocateCondition(leastUsedCondition, listener);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Firebase", databaseError.getMessage());
                listener.onFailed();
            }
        });
    }}
