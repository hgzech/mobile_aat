package com.hilmarzech.mobileaat;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;


/**
 * This function handles sensor input during the AAT.
 */
public abstract class AccelerometerActivity extends AppCompatActivity implements SensorEventListener {
    public static final String TAG = "AccelerometerActivity";
    public static final int PUSH_RESPONSE = 0;
    public static final int PULL_RESPONSE = 1;
    public static final int LINEAR_ACCELERATION = 1;
    public static final int ACCELERATION = 0;
    protected boolean waitingForResponse = false;
    protected boolean trackingAcceleration = false;
    protected boolean measuringOffset = false;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private ArrayList<Float> accelerationOffsetList;
    private float accelerometerOffset;

    /**
     * A function that has to be handled by the superclass to handle responses.
     * @param pushPull
     * @param time
     */
    public abstract void onPushPullDetected(int pushPull, float time);

    /**
     * A function that has to be handled by the superclass to handle incoming sensor data.
     * @param sensorType
     * @param time
     * @param x
     * @param y
     * @param z
     */
    public abstract void onSensorData(int sensorType, long time, float x, float y, float z);

    /**
     * A function that has to be handled by the superclass to handle the detected sensor type.
     * @param sensorType
     */
    public abstract void onSensorTypeDetected(int sensorType);


    /**
     * This function registers to the accelerometers and (if available) gyroscopes.
     * @param savedInstanceState
     * TODO: This should be refactored (variable names are weird and hasAccelerometer is never used)
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initializing offset array
        accelerationOffsetList = new ArrayList<>();
        accelerometerOffset = 0;
        // Setting up sensor manager and registering listener
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        // Check if the phone's accelerometer corrects for gravity
        boolean hasLinearAccelerometer;
        hasLinearAccelerometer = senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        if (!hasLinearAccelerometer) {
            senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            boolean hasAccelerometer = senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            onSensorTypeDetected(AccelerometerActivity.ACCELERATION);
        } else {
            onSensorTypeDetected(AccelerometerActivity.LINEAR_ACCELERATION);
        }
        Sensor senGyro = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senSensorManager.registerListener(this, senGyro, SensorManager.SENSOR_DELAY_FASTEST);

    }

    /**
     * This function re-registers sensors if activity is resumed.
     */
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Registering listener." );
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * This function unregisters sensors if activity is paused.
     */
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onResume: Unregistering listener." );
        senSensorManager.unregisterListener(this);
    }

    /**
     * This function starts the offset measurement.
     */
    protected void startMeasuringOffset() {
        accelerometerOffset = 0;
        measuringOffset = true;
    }

    /**
     * This function ends the offset measurement.
     */
    protected void stopMeasuringOffset() {
        measuringOffset = false;
        calculateOffset();
    }

    /**
     * This function calculates the accelerometer offset. This is necessary in phones that do not have
     * a gyroscope as they cannot correct for biases of accelerometer values based on the tilt of the phone.
     * To counteract this problem, we measure the base acceleration at the beginning of the trial,
     * calculate the average of it, and subtract that from consecutive movements.
     */
    protected void calculateOffset() {
        // Calculating mean offset
        float sum = 0;
        if (!accelerationOffsetList.isEmpty()) {
            for (Float offset : accelerationOffsetList) {
                sum += offset;
            }
            accelerometerOffset = sum / accelerationOffsetList.size();
        } else {
            accelerometerOffset = 0;
        }
        accelerationOffsetList = new ArrayList<>();
    }


    /**
     * This method receives sensor updates.  It decides based on absolute cutoffs whether a trial is categorized as a pull trial (acceleration > 4 m/s^2) or a push trial (acceleration < -6.5 m/s^2.  It also subtracts the accelerometer offset from each trial.
     * TODO: It would be better to handle offset offline and store raw data.  Alternatively it would be useful to store the accelerometer offset.
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION ||
                mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Track the acceleration of the z-axis
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            // If the accelerometer does not correct for gravity, subtract offset from acceleration
            if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                z = z - accelerometerOffset;
            }
            // If we're tracking acceleration (controlled by AatActivity), handle acceleration input
            // (done in AatActivity), interpret movement direction, and handle direction (done in AatActivity).
            if (trackingAcceleration) {
                long time = sensorEvent.timestamp/1000000;
                onSensorData(Sensor.TYPE_ACCELEROMETER, time, x, y, z);
                if (z > 4) {
                    onPushPullDetected(AccelerometerActivity.PULL_RESPONSE, time);
                } else if (z < -6.5) {
                    onPushPullDetected(AccelerometerActivity.PUSH_RESPONSE, time);
                }
            } else {
                if (measuringOffset) {
                    accelerationOffsetList.add(z);
                }
            }
        } else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            if (trackingAcceleration) {
                long time = sensorEvent.timestamp / 1000000;
                onSensorData(Sensor.TYPE_GYROSCOPE, time, x, y, z);
            }

        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}