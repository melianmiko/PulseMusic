package com.hardcodecoder.pulsemusic.fragments.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.hardcodecoder.pulsemusic.GlideApp;
import com.hardcodecoder.pulsemusic.R;
import com.hardcodecoder.pulsemusic.fragments.settings.base.SettingsBaseFragment;

public class SettingsContributorsFragment extends SettingsBaseFragment {

    public static final String TAG = SettingsContributorsFragment.class.getSimpleName();

    @NonNull
    public static SettingsContributorsFragment getInstance() {
        return new SettingsContributorsFragment();
    }

    @Override
    public String getFragmentTag() {
        return TAG;
    }

    @Override
    public int getToolbarTitleForFragment() {
        return R.string.contributors;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_contributors, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.github_logo).setOnClickListener(v -> openLink(R.string.url_github));
        view.findViewById(R.id.facebook_logo).setOnClickListener(v -> openLink(R.string.url_facebook));
        view.findViewById(R.id.twitter_logo).setOnClickListener(v -> openLink(R.string.url_twitter));
        view.findViewById(R.id.telegram_logo).setOnClickListener(v -> openLink(R.string.url_telegram));

        GlideApp.with(view)
                .load(R.drawable.def_avatar)
                .circleCrop()
                .into((ImageView) view.findViewById(R.id.lead_developer_profile_icon));
    }

    private void openLink(@StringRes int linkId) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(getString(linkId)));
        startActivity(i);
    }
}