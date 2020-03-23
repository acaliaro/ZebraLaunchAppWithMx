package it.soluzione1.zebralaunchappwithmx;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;

public class MyCustomApplication extends Application {

    public static MyCustomApplication instance;
    private static Context context;

    // Called when the application is starting, before any other application objects have been created.
    // Overriding this method is totally optional!
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();


//        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
//            @Override
//            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
//
//                Crashlytics.logException(throwable);
//
//                Log.d("EVENTS", "CRASH!!!!!");
//
//                Utility.restartApp(getBaseContext(),throwable.getMessage(), throwable.getStackTrace(), false, true );
//
//                //System.exit(2);
//
//            }
//        });

    }

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public static MyCustomApplication getInstance() {
        return instance;
    }
    public static Context getAppContext() {
        return context;
    }
}
