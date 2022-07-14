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
package org.secuso.privacyfriendlysudoku.ui.view;

import static org.secuso.privacyfriendlysudoku.ui.view.SudokuButtonType.Spacer;
import static org.secuso.privacyfriendlysudoku.ui.view.SudokuButtonType.getSpecialButtons;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import org.secuso.privacyfriendlysudoku.controller.GameController;
import org.secuso.privacyfriendlysudoku.game.listener.IHighlightChangedListener;
import org.secuso.privacyfriendlysudoku.ui.listener.IHintDialogFragmentListener;

import java.util.LinkedList;

/**
 * Created by TMZ_LToP on 17.11.2015.
 */
public class SudokuSpecialButtonLayout extends LinearLayout implements IHighlightChangedListener {


    SudokuSpecialButton[] fixedButtons;
    public int fixedButtonsCount = getSpecialButtons().size();
    GameController gameController;
    SudokuKeyboardLayout keyboard;
    Bitmap bitMap,bitResult;
    Canvas canvas;
    FragmentManager fragmentManager;
    Context context;
    float buttonMargin;
    FirebaseRemoteConfig mFirebaseRemoteConfig;
    String id_admod_reward, id_ads_reward;
    RewardedAd mRewardedAd, mRewardedAds;
    OnClickListener listener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v instanceof SudokuSpecialButton) {
                SudokuSpecialButton btn = (SudokuSpecialButton)v;

                //int row = gameController.getSelectedRow();
                //int col = gameController.getSelectedCol();
                MobileAds.initialize(getContext(), new OnInitializationCompleteListener() {
                    @Override
                    public void onInitializationComplete(InitializationStatus initializationStatus) {
                    }
                });
                ///6499/example/native
                mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
                FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(60)
                        .build();
                mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
                //mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config);
                mFirebaseRemoteConfig.fetchAndActivate()
                        .addOnCompleteListener((Activity) getContext(), new OnCompleteListener<Boolean>() {
                            @Override
                            public void onComplete(@NonNull Task<Boolean> task) {
                                if (task.isSuccessful()) {
                                    boolean updated = task.getResult();
                                    //Log.d("TAG", "Config params updated: " + updated);
                                    id_admod_reward = mFirebaseRemoteConfig.getString("id_admob_reward");
                                    id_ads_reward = mFirebaseRemoteConfig.getString("id_ads_reward");
                                    if(id_admod_reward != null){
                                        loadReward();
                                    }
                                    if(id_ads_reward != null){
                                        adsloadReward();
                                    }
//                            Log.d("Config id", id_admob_interstitial + " || " + id_ads_interstitial);
                                } else {
                                    Toast.makeText(getContext(), "Fetch failed",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                switch(btn.getType()) {
                    case Delete:
                        gameController.deleteSelectedCellsValue();
                        break;
                    case NoteToggle:
                        // rotates the Drawable
                        gameController.setNoteStatus(!gameController.getNoteStatus());
                        keyboard.updateNotesEnabled();
                        onHighlightChanged();
                        break;
                    case Do:
                        gameController.ReDo();
                        break;
                    case Undo:
                        gameController.UnDo();
                        break;
                    case Hint:
                        if(gameController.isValidCellSelected()) {
//                            if(gameController.getUsedHints() == 0 && !gameController.gameIsCustom()) {
//                                // are you sure you want to use a hint?
//                                HintConfirmationDialog hintDialog = new HintConfirmationDialog();
//                                hintDialog.show(fragmentManager, "HintDialogFragment");
//
//                            } else {
//                                gameController.hint();
//                            }
                            if (mRewardedAd != null) {
                                Activity activityContext = (Activity) getContext();
                                mRewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
                                    @Override
                                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                        gameController.hint();
                                        Toast.makeText(getContext(), "Admob", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else if(mRewardedAds!=null)  {
                                Activity activityContext = (Activity) getContext();
                                mRewardedAds.show(activityContext, new OnUserEarnedRewardListener() {
                                    @Override
                                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                                        gameController.hint();
                                        Toast.makeText(getContext(), "Ads", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }else {
                                Toast.makeText(getContext(), "Not qc", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Display a Toast that explains how to use the Hint function.
                            Toast t = Toast.makeText(getContext(), R.string.hint_usage, Toast.LENGTH_SHORT);
                            t.show();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private void adsloadReward() {
        AdManagerAdRequest adRequest = new AdManagerAdRequest.Builder().build();
        RewardedAd.load(getContext(), id_ads_reward,
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mRewardedAds = null;
                    }
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAds = rewardedAd;
                        mRewardedAds.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mRewardedAds = null;
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                mRewardedAds = null;
                            }
                        });
                    }
                });
    }

    private void loadReward() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(getContext(), id_admod_reward,
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        mRewardedAd = null;
                    }
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                        mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdClicked() {
                            }
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                mRewardedAd = null;
                            }
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                mRewardedAd = null;
                            }
                            @Override
                            public void onAdImpression() {
                            }
                            @Override
                            public void onAdShowedFullScreenContent() {
                            }
                        });
                    }
                });
    }


    public SudokuSpecialButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SudokuSpecialButtonLayout);
        buttonMargin = a.getDimension(R.styleable.SudokuSpecialButtonLayout_sudokuSpecialKeyboardMargin, 5f);
        a.recycle();

        setWeightSum(fixedButtonsCount);
        this.context = context;
    }

    public void setButtonsEnabled(boolean enabled) {
        for(SudokuSpecialButton b : fixedButtons) {
            b.setEnabled(enabled);
        }
    }

    public void setButtons(int width, GameController gc, SudokuKeyboardLayout key, FragmentManager fm, int orientation, Context cxt) {
        fragmentManager = fm;
        keyboard=key;
        gameController = gc;
        context = cxt;
        if(gameController != null) {
            gameController.registerHighlightChangedListener(this);
        }
        fixedButtons = new SudokuSpecialButton[fixedButtonsCount];
        LayoutParams p;
        int i = 0;
        //ArrayList<SudokuButtonType> type = (ArrayList<SudokuButtonType>) SudokuButtonType.getSpecialButtons();
        for (SudokuButtonType t : getSpecialButtons()){
            fixedButtons[i] = new SudokuSpecialButton(getContext(),null);
            if(orientation == LinearLayout.HORIZONTAL) {
                p = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1);
            } else {
                p = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1);
            }
            fixedButtons[i].setPadding((int)buttonMargin*5, 0, (int)buttonMargin*5, 0);
            p.setMargins((int)buttonMargin, (int)buttonMargin, (int)buttonMargin, (int)buttonMargin);

            //int width2 =width/(fixedButtonsCount);
            //p.width= width2-15;
            if(t == Spacer) {
                fixedButtons[i].setVisibility(View.INVISIBLE);
            }

            fixedButtons[i].setLayoutParams(p);
            fixedButtons[i].setType(t);
            fixedButtons[i].setImageDrawable(ContextCompat.getDrawable(context, fixedButtons[i].getType().getResID()));
            fixedButtons[i].setScaleType(ImageView.ScaleType.FIT_XY);
            fixedButtons[i].setAdjustViewBounds(true);
            fixedButtons[i].setOnClickListener(listener);
            fixedButtons[i].setBackgroundResource(R.drawable.numpad_highlighted_four);
            addView(fixedButtons[i]);

            i++;
        }

    }

    @Override
    public void onHighlightChanged() {
        for(int i = 0; i < fixedButtons.length; i++) {
            switch(fixedButtons[i].getType()) {
                case Undo:
                    fixedButtons[i].setBackgroundResource(gameController.isUndoAvailable() ?
                            R.drawable.numpad_highlighted_four : R.drawable.button_inactive);
                    break;
                case Do:
                    fixedButtons[i].setBackgroundResource(gameController.isRedoAvailable() ?
                            R.drawable.numpad_highlighted_four : R.drawable.button_inactive);
                    break;
                case NoteToggle:
                    Drawable drawable = ContextCompat.getDrawable(context, fixedButtons[i].getType().getResID());
                    // prepare canvas for the rotation of the note drawable
                    setUpVectorDrawable(drawable);

                    canvas.rotate(gameController.getNoteStatus() ? 45.0f : 0.0f, bitMap.getWidth()/2, bitMap.getHeight()/2);
                    canvas.drawBitmap(bitMap, 0, 0, null);
                    drawable.draw(canvas);

                    fixedButtons[i].setImageBitmap(bitResult);
                    fixedButtons[i].setBackgroundResource(gameController.getNoteStatus() ? R.drawable.numpad_highlighted_three : R.drawable.numpad_highlighted_four);

                    keyboard.updateNotesEnabled();

                    break;
                default:
                    break;
            }
        }
    }

    /*
    Set up the vector drawables so that they can be properly displayed despite using theme attributes for their fill color
     */
    private void setUpVectorDrawable(Drawable drawable) {
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        bitMap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        bitResult = Bitmap.createBitmap(bitMap.getWidth(), bitMap.getHeight(), Bitmap.Config.ARGB_8888);

        canvas = new Canvas(bitResult);
    }

    public static class HintConfirmationDialog extends DialogFragment {

        LinkedList<IHintDialogFragmentListener> listeners = new LinkedList<>();

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            // Verify that the host activity implements the callback interface
            if(activity instanceof IHintDialogFragmentListener) {
                listeners.add((IHintDialogFragmentListener) activity);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog);
            builder.setMessage(R.string.hint_confirmation)
                    .setPositiveButton(R.string.hint_confirmation_confirm, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            for(IHintDialogFragmentListener l : listeners) {
                                l.onHintDialogPositiveClick();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            return builder.create();
        }
    }
}
