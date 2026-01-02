package com.khata4u;

import android.app.Application;
import android.content.Context;
import androidx.annotation.NonNull;

public class App extends Application {

    // volatile to ensure visibility across threads
    private static volatile App instance;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @NonNull
    public static App getInstance() {
        if (instance == null) {
            // In case this is called before onCreate (shouldn't happen), create a fallback or throw
            throw new IllegalStateException("Application instance is not initialized yet.");
        }
        return instance;
    }

    @NonNull
    @SuppressWarnings("unused") // Used by code that needs a global app context; keeps IDE quiet
    public static Context getAppContext() {
        return getInstance().getApplicationContext();
    }
}
