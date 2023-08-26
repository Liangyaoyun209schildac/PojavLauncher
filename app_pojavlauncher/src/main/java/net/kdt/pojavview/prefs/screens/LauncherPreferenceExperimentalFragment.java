package net.kdt.pojavview.prefs.screens;

import android.os.Bundle;

import net.kdt.pojavview.R;

public class LauncherPreferenceExperimentalFragment extends LauncherPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_experimental);
    }
}
