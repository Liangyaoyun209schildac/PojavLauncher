package net.kdt.pojavlaunch.prefs.screens;

import android.os.Bundle;

import net.kdt.pojavlaunch.R;

public class LauncherPreferenceJavaFragment extends LauncherPreferenceFragment {
    @Override
    public void onCreatePreferences(Bundle b, String str) {
        // Triggers a write for some reason
        addPreferencesFromResource(R.xml.pref_java);
    }
}
