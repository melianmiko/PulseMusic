package com.hardcodecoder.pulsemusic.activities.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.hardcodecoder.pulsemusic.Preferences;
import com.hardcodecoder.pulsemusic.R;
import com.hardcodecoder.pulsemusic.fragments.main.ControlsFragment;
import com.hardcodecoder.pulsemusic.fragments.nowplaying.screens.EdgeNowPlayingScreen;
import com.hardcodecoder.pulsemusic.fragments.nowplaying.screens.LandscapeModeNowPlayingScreen;
import com.hardcodecoder.pulsemusic.fragments.nowplaying.screens.ModernNowPlayingScreen;
import com.hardcodecoder.pulsemusic.fragments.nowplaying.screens.StylishNowPlayingScreen;
import com.hardcodecoder.pulsemusic.themes.ThemeColors;
import com.hardcodecoder.pulsemusic.utils.AppSettings;
import com.hardcodecoder.pulsemusic.utils.DimensionsUtil;

public abstract class DraggableNowPlayingSheetActivity extends ControllerActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private View mMainContent;
    private FrameLayout mPeekingFrame;
    private FrameLayout mExpandedFrame;
    private BottomSheetBehavior<FrameLayout> mBehaviour;
    private BottomSheetBehavior.BottomSheetCallback mBehaviourCallback;
    private Fragment mPeekingFragment = null;
    private Fragment mExpandedFragment = null;
    private BottomNavigationView mBottomNavBar;
    private boolean mPendingUpdateExpandedFragment = false;
    private int mCurrentOrientation;
    private int mDefaultSystemUiVisibility = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(null);
        setContentView(R.layout.activity_draggable_now_playing);
        ViewGroup root = findViewById(R.id.draggable_activity_root);
        mMainContent = LayoutInflater.from(this).inflate(getContentLayout(), root, false);
        root.addView(mMainContent, 0);
        onViewCreated(mMainContent, savedInstanceState);
        setUpBottomNavigationView();
    }

    private void setUpBottomNavigationView() {
        mBottomNavBar = findViewById(R.id.bottom_nav_bar);
        ColorStateList colorStateList = ThemeColors.getEnabledSelectedColorStateList();
        mBottomNavBar.setItemIconTintList(colorStateList);
        mBottomNavBar.setItemTextColor(colorStateList);
        mBottomNavBar.setItemRippleColor(ThemeColors.getBottomNavigationViewRippleColor());
        mBottomNavBar.setOnNavigationItemSelectedListener(menuItem -> {
            mBottomNavBar.postOnAnimation(() -> onNavigationItemSelected(menuItem));
            return true;
        });
    }

    private void initializeBottomSheet() {
        FrameLayout draggableFrame = findViewById(R.id.draggable_frame);
        draggableFrame.setVisibility(View.VISIBLE);

        mBehaviour = BottomSheetBehavior.from(draggableFrame);
        final int peekHeight = DimensionsUtil.getDimensionPixelSize(this, 104);
        mBehaviour.setPeekHeight(peekHeight, true);
        mBehaviour.setHideable(true);

        mPeekingFrame = findViewById(R.id.peeking_content_frame);
        mExpandedFrame = findViewById(R.id.expanded_content_frame);

        if (null == mPeekingFragment) {
            mPeekingFragment = ControlsFragment.getInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.peeking_content_frame, mPeekingFragment, ControlsFragment.TAG)
                    .commit();
            mPeekingFrame.setOnClickListener(v -> expandBottomSheet());
        }

        if (null == mExpandedFragment) {
            createExpandedFragment(false);
            getSharedPreferences(Preferences.NOW_PLAYING_SCREEN_STYLE_KEY, Context.MODE_PRIVATE)
                    .registerOnSharedPreferenceChangeListener(this);
            mExpandedFrame.setVisibility(View.GONE);
        }

        final float bottomNavBarHeight = DimensionsUtil.getDimension(this, 56);
        mBehaviourCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        mPeekingFrame.setVisibility(View.GONE);
                        if (needsLightStatusBarIcons()) setLightStatusBarIcons(true);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        mExpandedFrame.setVisibility(View.GONE);
                        updateBottomBarElevation(true);
                        if (mDefaultSystemUiVisibility != -1) setLightStatusBarIcons(false);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        mRemote.stop();
                        updateBottomBarElevation(false);
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                        if (mExpandedFrame.getVisibility() != View.VISIBLE)
                            mExpandedFrame.setVisibility(View.VISIBLE);
                        if (mPeekingFrame.getVisibility() != View.VISIBLE)
                            mPeekingFrame.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_HALF_EXPANDED:
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                final float inverseAlpha = 1 - slideOffset;
                mExpandedFrame.setAlpha(slideOffset);
                mPeekingFrame.setAlpha(inverseAlpha);
                mBottomNavBar.setAlpha(inverseAlpha);
                mBottomNavBar.setTranslationY(bottomNavBarHeight * Math.max(slideOffset, 0));
            }
        };
        mBehaviour.addBottomSheetCallback(mBehaviourCallback);
    }

    private void createExpandedFragment(boolean force) {
        if ((null != mExpandedFragment && !force) || null == mBehaviour) return;
        String tag;
        mCurrentOrientation = getResources().getConfiguration().orientation;
        if (mCurrentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mExpandedFragment = LandscapeModeNowPlayingScreen.getInstance();
            tag = LandscapeModeNowPlayingScreen.TAG;
        } else {
            int id = AppSettings.getNowPlayingScreenStyle(this);
            switch (id) {
                case Preferences.NOW_PLAYING_SCREEN_STYLISH:
                    mExpandedFragment = StylishNowPlayingScreen.getInstance();
                    tag = StylishNowPlayingScreen.TAG;
                    break;
                case Preferences.NOW_PLAYING_SCREEN_EDGE:
                    mExpandedFragment = EdgeNowPlayingScreen.getInstance();
                    tag = EdgeNowPlayingScreen.TAG;
                    break;
                case Preferences.NOW_PLAYING_SCREEN_MODERN:
                default:
                    mExpandedFragment = ModernNowPlayingScreen.getInstance();
                    tag = ModernNowPlayingScreen.TAG;
            }
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.expanded_content_frame, mExpandedFragment, tag)
                .commit();
    }

    private void updateBottomBarElevation(boolean isPeeking) {
        mBottomNavBar.setElevation(isPeeking ? 0f : DimensionsUtil.getDimensionPixelSize(this, 4));
    }

    /**
     * Changes the status bar icon colors to a light color
     * to account for a darker status bar background
     *
     * @param setLightIcons If true change the status bar icon colors to light
     *                      else we revert the system ui visibility to account
     *                      for dark and light themes
     */
    private void setLightStatusBarIcons(boolean setLightIcons) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window w = getWindow();
            if (setLightIcons) {
                mDefaultSystemUiVisibility = w.getDecorView().getSystemUiVisibility();
                w.getDecorView().setSystemUiVisibility(mDefaultSystemUiVisibility & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                // Instead of toggling back the View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                // We revert to default system ui visibility to account for light and dark themes
                w.getDecorView().setSystemUiVisibility(mDefaultSystemUiVisibility);
                mDefaultSystemUiVisibility = -1;
            }
        }
    }

    private void updateMainContentBottomPadding(int bottomPadding) {
        mMainContent.setPadding(
                mMainContent.getPaddingLeft(),
                mMainContent.getPaddingTop(),
                mMainContent.getPaddingRight(),
                bottomPadding);
    }

    private boolean needsLightStatusBarIcons() {
        return null != mExpandedFragment && mExpandedFragment instanceof EdgeNowPlayingScreen;
    }

    protected void updateDraggableSheet(boolean show) {
        if (show) {
            if (null == mBehaviour) initializeBottomSheet();
            mBehaviour.setState(BottomSheetBehavior.STATE_COLLAPSED);
            int bottomPaddingWhenPeeking = DimensionsUtil.getDimensionPixelSize(this, 104);
            updateMainContentBottomPadding(bottomPaddingWhenPeeking);
        } else {
            mBehaviour.setState(BottomSheetBehavior.STATE_HIDDEN);
            int bottomNavBarHeight = DimensionsUtil.getDimensionPixelSize(this, 56);
            updateMainContentBottomPadding(bottomNavBarHeight);
        }
    }

    public void collapseBottomSheet() {
        if (null != mBehaviour) mBehaviour.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mPeekingFrame.setVisibility(View.VISIBLE);
    }

    public void expandBottomSheet() {
        if (null != mBehaviour) mBehaviour.setState(BottomSheetBehavior.STATE_EXPANDED);
        mExpandedFrame.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPendingUpdateExpandedFragment) createExpandedFragment(true);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation != mCurrentOrientation) {
            createExpandedFragment(true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @NonNull String key) {
        if (key.equals(Preferences.NOW_PLAYING_SCREEN_STYLE_KEY))
            mPendingUpdateExpandedFragment = true;
    }

    @Override
    protected void onDestroy() {
        getSharedPreferences(Preferences.NOW_PLAYING_SCREEN_STYLE_KEY, Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this);
        if (null != mBehaviour && null != mBehaviourCallback)
            mBehaviour.removeBottomSheetCallback(mBehaviourCallback);
        super.onDestroy();
    }

    @LayoutRes
    public abstract int getContentLayout();

    public abstract void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState);

    public abstract void onNavigationItemSelected(@NonNull MenuItem menuItem);
}