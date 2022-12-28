package com.hilmarzech.mobileaat;
import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;
import net.danlew.android.joda.JodaTimeAndroid;


/**
 * This class is required to set some Application wide settings.
 */
public class MobileAAT extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        JodaTimeAndroid.init(this);
    }
}
