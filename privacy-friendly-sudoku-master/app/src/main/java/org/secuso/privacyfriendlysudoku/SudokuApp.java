/*
 This file is part of Privacy Friendly Sudoku.

 Privacy Friendly Sudoku is free software:
 you can redistribute it and/or modify it under the terms of the
 GNU General Public License as published by the Free Software Foundation,
 either version 3 of the License, or any later version.

 Privacy Friendly Sudoku is distributed in the hope
 that it will be useful, but WITHOUT ANY WARRANTY; without even
 the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Privacy Friendly Sudoku. If not, see <http://www.gnu.org/licenses/>.
 */
package org.secuso.privacyfriendlysudoku;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.secuso.privacyfriendlysudoku.ui.AppOpen;

import java.util.Date;

public class SudokuApp extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver{

    public static final String CHANNEL_ID = "sudoku.0";
    private static AppOpen appOpenManager;

    AppOpenAdManager appOpenAdManager;
    Activity currentActivity;
    FirebaseRemoteConfig mFirebaseRemoteConfig;
    String check, id_admob_banner, id_admob_interstitial, id_admob_native, id_admob_open, id_admob_reward;
    String id_ads_banner, id_ads_interstitial, id_ads_native, id_ads_open, id_ads_reward;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        sharedPreferences = getSharedPreferences("dataID", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // channels
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Default", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        MobileAds.initialize(
                this,
                new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(InitializationStatus initializationStatus) {}
                });
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(60)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        //mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(getMainExecutor(), new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            check = mFirebaseRemoteConfig.getString("check");
                            id_admob_open = mFirebaseRemoteConfig.getString("id_admob_open");
                            id_admob_banner = mFirebaseRemoteConfig.getString("id_admob_banner");
                            id_admob_native = mFirebaseRemoteConfig.getString("id_admob_native");
                            id_admob_reward = mFirebaseRemoteConfig.getString("id_admob_reward");
                            id_admob_interstitial = mFirebaseRemoteConfig.getString("id_admob_interstitial");

                            id_ads_open = mFirebaseRemoteConfig.getString("id_ads_open");
                            id_ads_banner = mFirebaseRemoteConfig.getString("id_ads_banner");
                            id_ads_native = mFirebaseRemoteConfig.getString("id_ads_native");
                            id_ads_reward = mFirebaseRemoteConfig.getString("id_ads_reward");
                            id_ads_interstitial = mFirebaseRemoteConfig.getString("id_ads_interstitial");

                            editor.putString("check", check);
                            editor.putString("id_admob_open", id_admob_open);
                            editor.putString("id_admob_banner", id_admob_banner);
                            editor.putString("id_admob_native", id_admob_native);
                            editor.putString("id_admob_reward", id_admob_reward);
                            editor.putString("id_admob_interstitial", id_admob_interstitial);

                            editor.putString("id_ads_open", id_ads_open);
                            editor.putString("id_ads_banner", id_ads_banner);
                            editor.putString("id_ads_native", id_ads_native);
                            editor.putString("id_ads_reward", id_ads_reward);
                            editor.putString("id_ads_interstitial", id_ads_interstitial);
                            editor.commit();
                        } else {
                            Toast.makeText(SudokuApp.this, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        SharedPreferences msharedPreferences = getSharedPreferences("dataID", MODE_PRIVATE);
        //Toast.makeText(this, msharedPreferences.getString("check", ""), Toast.LENGTH_SHORT).show();
        String checkAd = msharedPreferences.getString("check", "");
        if(checkAd.equals("1")){
            appOpenManager = new AppOpen(this);
        }else{
            this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdManager = new AppOpenAdManager();
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        appOpenAdManager.showAdIfAvailable(currentActivity);
    }

    /** ActivityLifecycleCallback methods. */
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
    public void showAdIfAvailable(
            @NonNull Activity activity,
            @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        appOpenAdManager.showAdIfAvailable(activity, onShowAdCompleteListener);
    }
    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    private static class AppOpenAdManager {

        private static final String LOG_TAG = "AppOpenAdManager";
        private static final String AD_UNIT_ID = "/6499/example/app-open";

        private AppOpenAd appOpenAd = null;
        private boolean isLoadingAd = false;
        private boolean isShowingAd = false;

        /** Keep track of the time an app open ad is loaded to ensure you don't show an expired ad. */
        private long loadTime = 0;

        /** Constructor. */
        public AppOpenAdManager() {}

        /**
         * Load an ad.
         *
         * @param context the context of the activity that loads the ad
         */
        private void loadAd(Context context) {
            // Do not load ad if there is an unused ad or one is already loading.
            if (isLoadingAd || isAdAvailable()) {
                return;
            }

            isLoadingAd = true;
            AdManagerAdRequest request = new AdManagerAdRequest.Builder().build();
            AppOpenAd.load(
                    context,
                    AD_UNIT_ID,
                    request,
                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        /**
                         * Called when an app open ad has loaded.
                         *
                         * @param ad the loaded app open ad.
                         */
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
                            appOpenAd = ad;
                            isLoadingAd = false;
                            loadTime = (new Date()).getTime();

                            Log.d(LOG_TAG, "onAdLoaded.");
                            //Toast.makeText(context, "onAdLoaded", Toast.LENGTH_SHORT).show();
                        }

                        /**
                         * Called when an app open ad has failed to load.
                         *
                         * @param loadAdError the error.
                         */
                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            isLoadingAd = false;
                            Log.d(LOG_TAG, "onAdFailedToLoad: " + loadAdError.getMessage());
                            //Toast.makeText(context, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        /** Check if ad was loaded more than n hours ago. */
        private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
            long dateDifference = (new Date()).getTime() - loadTime;
            long numMilliSecondsPerHour = 3600000;
            return (dateDifference < (numMilliSecondsPerHour * numHours));
        }

        /** Check if ad exists and can be shown. */
        private boolean isAdAvailable() {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
        }
        private void showAdIfAvailable(@NonNull final Activity activity) {
            showAdIfAvailable(
                    activity,
                    new OnShowAdCompleteListener() {
                        @Override
                        public void onShowAdComplete() {
                        }
                    });
        }
        private void showAdIfAvailable(
                @NonNull final Activity activity,
                @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
            if (isShowingAd) {
                return;
            }
            if (!isAdAvailable()) {
                onShowAdCompleteListener.onShowAdComplete();
                loadAd(activity);
                return;
            }
            appOpenAd.setFullScreenContentCallback(
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            appOpenAd = null;
                            isShowingAd = false;
                            onShowAdCompleteListener.onShowAdComplete();
                            loadAd(activity);
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            appOpenAd = null;
                            isShowingAd = false;
                            onShowAdCompleteListener.onShowAdComplete();
                            loadAd(activity);
                        }
                        @Override
                        public void onAdShowedFullScreenContent() {
                        }
                    });
            isShowingAd = true;
            appOpenAd.show(activity);
        }
    }

}
