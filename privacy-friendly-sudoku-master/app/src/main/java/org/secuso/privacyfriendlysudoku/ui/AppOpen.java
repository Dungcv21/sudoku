package org.secuso.privacyfriendlysudoku.ui;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.secuso.privacyfriendlysudoku.SudokuApp;

import java.util.Date;

public class AppOpen implements LifecycleObserver,Application.ActivityLifecycleCallbacks {

    private Activity currentActivity;
    Activity context;
    private static final String LOG_TAG = "AppOpenManager";
    private String AD_UNIT_ID = "";
    private AppOpenAd appOpenAd = null;
    private static boolean isShowingAd = false;
    FirebaseRemoteConfig mFirebaseRemoteConfig;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;

    private final SudokuApp myApplication;
    private long loadTime = 0;

    /** Constructor */
    public AppOpen(SudokuApp myApplication) {
        this.myApplication = myApplication;
        this.myApplication.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        MobileAds.initialize(myApplication, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(60)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        //mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(myApplication.getMainExecutor(), new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            AD_UNIT_ID = mFirebaseRemoteConfig.getString("id_admob_open");
                        } else {
                            Toast.makeText(myApplication, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        showAdIfAvailable();
    }
    public void showAdIfAvailable() {
        if (!isShowingAd && isAdAvailable()) {
            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            AppOpen.this.appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {}

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                        }
                    };
            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);
        } else {
            fetchAd();
        }
    }
    public void fetchAd() {
        if (isAdAvailable()) {
            return;
        }

        loadCallback =
                new AppOpenAd.AppOpenAdLoadCallback() {
                    @Override
                    public void onAdLoaded(AppOpenAd ad) {
                        AppOpen.this.appOpenAd = ad;
                        AppOpen.this.loadTime = (new Date()).getTime();
                    }
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        // Handle the error.
                    }
                };
        AdRequest request = getAdRequest();
        AppOpenAd.load(
                myApplication, AD_UNIT_ID, request,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    }
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }
    public boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
    }
    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
    }
    @Override
    public void onActivityStopped(Activity activity) {}
    @Override
    public void onActivityPaused(Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}
    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
    }
}
