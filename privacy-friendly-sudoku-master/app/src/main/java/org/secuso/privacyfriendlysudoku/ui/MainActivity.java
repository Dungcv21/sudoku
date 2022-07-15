package org.secuso.privacyfriendlysudoku.ui;

import static org.secuso.privacyfriendlysudoku.ui.TutorialActivity.ACTION_SHOW_ANYWAYS;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.ads.nativetemplates.TemplateView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.controller.GameStateManager;
import org.secuso.privacyfriendlysudoku.controller.NewLevelManager;
import org.secuso.privacyfriendlysudoku.controller.helper.GameInfoContainer;
import org.secuso.privacyfriendlysudoku.game.GameDifficulty;
import org.secuso.privacyfriendlysudoku.game.GameType;
import org.secuso.privacyfriendlysudoku.ui.listener.IImportDialogFragmentListener;
import org.secuso.privacyfriendlysudoku.ui.view.R;
import org.secuso.privacyfriendlysudoku.ui.view.databinding.DialogFragmentImportBoardBinding;

import java.util.LinkedList;
import java.util.List;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, IImportDialogFragmentListener{

    RatingBar difficultyBar;
    TextView difficultyText;
    SharedPreferences settings;
    ImageView arrowLeft, arrowRight;
    DrawerLayout drawer;
    NavigationView mNavigationView;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    FirebaseRemoteConfig mFirebaseRemoteConfig;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    long backPressedTime = 0;
    AdLoader adLoader;
    String id_admob_native, id_ads_native;
    String  id_admob_interstitial, id_ads_interstitial;
    Boolean checkid = false;
    String check;

    //inters
    InterstitialAd mInterstitialAd;
    AdManagerInterstitialAd mAdManagerInterstitialAd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        sharedPreferences = getSharedPreferences("dataID", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        settings = PreferenceManager.getDefaultSharedPreferences(this);
        if (settings.getBoolean("pref_dark_mode_setting", false )) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
        else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        //admob
        //-----------------------------------
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(60)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                    @Override
                    public void onComplete(@NonNull Task<Boolean> task) {
                        if (task.isSuccessful()) {
                            boolean updated = task.getResult();
                            check = mFirebaseRemoteConfig.getString("check");
                            editor.putString("check", check);
                            editor.commit();
                        } else {
                            Toast.makeText(MainActivity.this, "Fetch failed",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        SharedPreferences msharedPreferences = getSharedPreferences("dataID", MODE_PRIVATE);
        //Toast.makeText(this, msharedPreferences.getString("check", ""), Toast.LENGTH_SHORT).show();
        check = msharedPreferences.getString("check", "");
        id_admob_native = msharedPreferences.getString("id_admob_native", "");
        id_ads_native = msharedPreferences.getString("id_ads_native", "");
        id_admob_interstitial = msharedPreferences.getString("id_admob_interstitial", "");
        id_ads_interstitial = msharedPreferences.getString("id_ads_interstitial", "");
        if(check.equalsIgnoreCase("1")){
            loadInterstitial();
            admob();
        }else{
            loadInterstitialAds();
            ads();
        }
        NewLevelManager newLevelManager = NewLevelManager.getInstance(getApplicationContext(), settings);
        // check if we need to pre generate levels.
        newLevelManager.checkAndRestock();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.scroller);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        List<GameType> validGameTypes = GameType.getValidGameTypes();
        String lastChosenGameType = settings.getString("lastChosenGameType", GameType.Default_9x9.name());
        int index = validGameTypes.indexOf(Enum.valueOf(GameType.class, lastChosenGameType));
        mViewPager.setCurrentItem(index);
        arrowLeft = (ImageView)findViewById(R.id.arrow_left);
        arrowRight = (ImageView) findViewById(R.id.arrow_right);
        //care for initial postiton of the ViewPager
        arrowLeft.setVisibility((index==0)?View.INVISIBLE:View.VISIBLE);
        arrowRight.setVisibility((index==mSectionsPagerAdapter.getCount()-1)?View.INVISIBLE:View.VISIBLE);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }
            @Override
            public void onPageSelected(int position) {
                arrowLeft.setVisibility((position==0)?View.INVISIBLE:View.VISIBLE);
                arrowRight.setVisibility((position==mSectionsPagerAdapter.getCount()-1)?View.INVISIBLE:View.VISIBLE);
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        // Set the difficulty Slider to whatever was chosen the last time
        difficultyBar = (RatingBar)findViewById(R.id.difficultyBar);
        difficultyText = (TextView) findViewById(R.id.difficultyText);
        final LinkedList<GameDifficulty> difficultyList = GameDifficulty.getValidDifficultyList();
        difficultyBar.setNumStars(difficultyList.size());
        difficultyBar.setMax(difficultyList.size());
        difficultyBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                //createGameBar.setChecked(false);
                ((Button) findViewById(R.id.playButton)).setText(R.string.new_game);

                if (rating >= 1) {
                    difficultyText.setText(getString(difficultyList.get((int) ratingBar.getRating() - 1).getStringResID()));
                }
//                else {
//                    difficultyText.setText(R.string.difficulty_custom);
//                    ((Button)findViewById(R.id.playButton)).setText(R.string.create_game);
//                }
            }
        });
        String retrievedDifficulty = settings.getString("lastChosenDifficulty", "Moderate");
        GameDifficulty lastChosenDifficulty = GameDifficulty.valueOf(retrievedDifficulty);
                //retrievedDifficulty.equals("Custom")? GameDifficulty.Unspecified.toString() : retrievedDifficulty);
        if (lastChosenDifficulty == GameDifficulty.Unspecified) {
            difficultyBar.setRating(0);
        } else {
            difficultyBar.setRating(GameDifficulty.getValidDifficultyList().indexOf(lastChosenDifficulty) + 1);
        }
        if(Configuration.SCREENLAYOUT_SIZE_SMALL == (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)) {
            difficultyBar.setScaleX(0.75f);
            difficultyBar.setScaleY(0.75f);
        }
        // on first create always check for loadable levels!
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("savesChanged", true);
        editor.apply();
        refreshContinueButton();
        // set Nav_Bar
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout_main);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();
        mNavigationView = (NavigationView) findViewById(R.id.nav_view_main);
        mNavigationView.setNavigationItemSelectedListener(this);
        selectNavigationItem(R.id.nav_newgame_main);
        overridePendingTransition(0, 0);
    }
    private void admob() {
        adLoader = new AdLoader.Builder(MainActivity.this, id_admob_native)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd NativeAd) {
                        if (isDestroyed()) {
                            NativeAd.destroy();
                        }
                        NativeTemplateStyle styles = new
                                NativeTemplateStyle.Builder().withMainBackgroundColor(new ColorDrawable(Color.WHITE)).build();
                        TemplateView template = findViewById(R.id.my_template);
                        template.setStyles(styles);
                        template.setNativeAd(NativeAd);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .build())
                .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }
    private void ads() {
        AdLoader adLoaderr = new AdLoader.Builder(MainActivity.this, id_ads_native)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd NativeAd) {
                        if (isDestroyed()) {
                            NativeAd.destroy();
                        }
                        NativeTemplateStyle styles = new
                                NativeTemplateStyle.Builder().withMainBackgroundColor(new ColorDrawable(Color.WHITE)).build();
                        TemplateView template = findViewById(R.id.my_template);
                        template.setStyles(styles);
                        template.setNativeAd(NativeAd);
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .build())
                .build();
        adLoaderr.loadAd(new AdRequest.Builder().build());
    }
    public void onClick(View view) {
        Intent i = null;
        switch(view.getId()) {
            case R.id.arrow_left:
                mViewPager.arrowScroll(View.FOCUS_LEFT);
                break;
            case R.id.arrow_right:
                mViewPager.arrowScroll(View.FOCUS_RIGHT);
                break;
            case R.id.continueButton:
                if (mInterstitialAd != null) {
                    mInterstitialAd.show(MainActivity.this);
                    mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            mInterstitialAd = null;
                            Intent a = new Intent(MainActivity.this, LoadGameActivity.class);
                            a.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(a);
                        }
                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            mInterstitialAd = null;
                        }
                    });
                }else if(mAdManagerInterstitialAd != null){
                        mAdManagerInterstitialAd.show(MainActivity.this);
                        mAdManagerInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mAdManagerInterstitialAd = null;
                                Intent a = new Intent(MainActivity.this, LoadGameActivity.class);
                                a.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(a);
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                mAdManagerInterstitialAd = null;
                            }
                        });
                    } else {
                    i = new Intent(this, LoadGameActivity.class);
                }
                break;
            case R.id.playButton:
                GameType gameType = GameType.getValidGameTypes().get(mViewPager.getCurrentItem());
                int index = difficultyBar.getProgress()-1;
                GameDifficulty gameDifficulty = GameDifficulty.getValidDifficultyList().get(index < 0 ? 0 : index);
                NewLevelManager newLevelManager = NewLevelManager.getInstance(getApplicationContext(), settings);
                newLevelManager.checkAndRestock();
                if(newLevelManager.isLevelLoadable(gameType, gameDifficulty)) {
                    // save current setting for later
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("lastChosenGameType", gameType.name());
                    editor.putString("lastChosenDifficulty", gameDifficulty.name());
                    editor.apply();
                    if (mInterstitialAd != null) {
                        mInterstitialAd.show(MainActivity.this);
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mInterstitialAd = null;
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                mInterstitialAd = null;
                            }
                        });
                    } else {
                        i = new Intent(this, GameActivity.class);
                        i.putExtra("gameType", gameType.name());
                        i.putExtra("gameDifficulty", gameDifficulty.name());
                    }
                }
                break;
            default:
        }
        final Intent intent = i;
        if(intent != null) {
            View mainContent = findViewById(R.id.main_content);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startActivity(intent);
                }
            }, MAIN_CONTENT_FADEOUT_DURATION);
        }
    }
    private void loadInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, id_admob_interstitial, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        Toast.makeText(MainActivity.this, "admodFail" + " | " + id_admob_interstitial, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadInterstitialAds() {
        AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
        AdManagerInterstitialAd.load(this,id_ads_interstitial, adRequest,
                new AdManagerInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull AdManagerInterstitialAd interstitialAds) {
                        mAdManagerInterstitialAd = interstitialAds;
                    }
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mAdManagerInterstitialAd = null;
                        Toast.makeText(MainActivity.this, "adsFail" + " | " + id_ads_interstitial, Toast.LENGTH_SHORT).show();
                    }
                });
    }
    @Override
    public void onResume() {
        super.onResume();
        selectNavigationItem(R.id.nav_newgame_main);
        refreshContinueButton();
    }
    private void refreshContinueButton() {
        // enable continue button if we have saved games.
        Button continueButton = (Button)findViewById(R.id.continueButton);
        GameStateManager fm = new GameStateManager(getBaseContext(), settings);
        List<GameInfoContainer> gic = fm.loadGameStateInfo();
        if(gic.size() > 0 && !(gic.size() == 1 && gic.get(0).getID() == GameController.DAILY_SUDOKU_ID)) {
            continueButton.setEnabled(true);
            continueButton.setBackgroundResource(R.drawable.bgr_btn);
        } else {
            continueButton.setEnabled(false);
            continueButton.setBackgroundResource(R.drawable.bgr_nobtn);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        drawer.closeDrawer(GravityCompat.START);

        if(id == R.id.nav_newgame_main) {
            return true;
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                goToNavigationItem(id);
            }
        }, NAVDRAWER_LAUNCH_DELAY);

        View mainContent = findViewById(R.id.main_content);

        return true;
    }

    private void selectNavigationItem(int itemId) {
        for(int i = 0 ; i < mNavigationView.getMenu().size(); i++) {
            boolean b = itemId == mNavigationView.getMenu().getItem(i).getItemId();
            mNavigationView.getMenu().getItem(i).setChecked(b);
        }
    }

    private boolean goToNavigationItem(int id) {
        Intent intent;

        switch(id) {
            case R.id.menu_settings_main:
                //open settings
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;

            case R.id.nav_highscore_main:
                // see highscore list

                intent = new Intent(this, StatsActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;

            case R.id.menu_about_main:
                //open about page
                intent = new Intent(this,AboutActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;

            case R.id.menu_help_main:
                //open about page
                intent = new Intent(this,HelpActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;

            case R.id.menu_tutorial_main:
                intent = new Intent(this, TutorialActivity.class);
                intent.setAction(ACTION_SHOW_ANYWAYS);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;

            case R.id.nav_dailySudoku_main:
                intent = new Intent(this, DailySudokuActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;

            default:
        }
        return true;
    }

    public void onImportDialogPositiveClick(String input) {
        String inputSudoku = null;
        String prefix = "";
        StringBuilder errorMessage = new StringBuilder();

        // a valid input needs to contain exactly one of the valid prefixes
        for (int i = 0; i < GameActivity.validUris.size(); i++) {
            prefix = GameActivity.validUris.get(i).getHost().equals("") ?
                    GameActivity.validUris.get(i).getScheme() + "://" :
                    GameActivity.validUris.get(i).getScheme() + "://" + GameActivity.validUris.get(i).getHost() + "/";
            if (input.startsWith(prefix)) {
                inputSudoku = input.replace(prefix, "");
                break;
            }

            String endOfRecord = i == GameActivity.validUris.size() - 1 ? "" : ", ";
            errorMessage.append(prefix);
            errorMessage.append(endOfRecord);
        }

        if (inputSudoku == null) {
            Toast.makeText(MainActivity.this,
                    this.getString(R.string.menu_import_wrong_format_custom_sudoku) + " " + errorMessage.toString(), Toast.LENGTH_LONG).show();
            return;
        }

        double size = Math.sqrt(inputSudoku.length());
        boolean validSize = false;

        // check whether or not the size of the encoded sudoku is valid; if not, notify the user
        for (GameType type : GameType.getValidGameTypes()) {
            if (type.getSize() == size) {
                validSize = true;
                break;
            }
        }

        if (!validSize) {
            Toast.makeText(MainActivity.this, R.string.failed_to_verify_custom_sudoku_toast, Toast.LENGTH_LONG).show();
            return;
        }

        GameType gameType = Enum.valueOf(GameType.class, "Default_" + (int)size + "x" + (int)size);

        //check whether or not the sudoku is valid and has a unique solution
        boolean solvable = CreateSudokuActivity.verify(gameType, inputSudoku);

        // if the encoded sudoku is solvable, sent the code directly to the GameActivity; if not, notify the user
        if (solvable) {
            Toast.makeText(MainActivity.this, R.string.finished_verifying_custom_sudoku_toast, Toast.LENGTH_LONG).show();
            final Intent intent = new Intent(this, GameActivity.class);
            intent.setData(Uri.parse(prefix + inputSudoku));
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(MainActivity.this, R.string.failed_to_verify_custom_sudoku_toast, Toast.LENGTH_LONG).show();
        }
    }

    public void onDialogNegativeClick() {
        mNavigationView.setCheckedItem(R.id.nav_newgame_main);
    }

    public static class ImportBoardDialog extends DialogFragment {
        private LinkedList<IImportDialogFragmentListener> listeners = new LinkedList<>();

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            // Verify that the host activity implements the callback interface
            if(activity instanceof IImportDialogFragmentListener) {
                listeners.add((IImportDialogFragmentListener) activity);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            DialogFragmentImportBoardBinding binding = DialogFragmentImportBoardBinding.inflate(inflater);
            builder.setView(binding.getRoot());
            builder.setMessage(R.string.dialog_import_custom_sudoku);
            builder.setPositiveButton(R.string.dialog_import_custom_sudoku_positive_button, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    for(IImportDialogFragmentListener l : listeners) {
                        l.onImportDialogPositiveClick(binding.ver3ImportSudokuEditText.getText().toString());
                    }
                }
            })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for(IImportDialogFragmentListener l : listeners) {
                                l.onDialogNegativeClick();
                            }
                        }
                    });
            return builder.create();
        }

    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a GameTypeFragment (defined as a static inner class below).
            return GameTypeFragment.newInstance(position);
        }



        @Override
        public int getCount() {
            // Show 3 total pages.
            return GameType.getValidGameTypes().size();
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class GameTypeFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */


        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static GameTypeFragment newInstance(int sectionNumber) {
            GameTypeFragment fragment = new GameTypeFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_menu, container, false);

            GameType gameType = GameType.getValidGameTypes().get(getArguments().getInt(ARG_SECTION_NUMBER));

            ImageView imageView = (ImageView) rootView.findViewById(R.id.gameTypeImage);

            imageView.setImageResource(gameType.getResIDImage());


            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(gameType.getStringResID()));
            return rootView;
        }
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        }else{
            if (backPressedTime + 3000 > System.currentTimeMillis()) {
                super.onBackPressed();
                finish();
            } else {
                Toast.makeText(this, "Press back again to leave the app.", Toast.LENGTH_LONG).show();
            }
            backPressedTime = System.currentTimeMillis();
        }


    }
}
