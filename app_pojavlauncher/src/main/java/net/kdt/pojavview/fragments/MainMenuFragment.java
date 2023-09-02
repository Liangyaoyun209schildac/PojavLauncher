package net.kdt.pojavview.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavview.CustomControlsActivity;
import net.kdt.pojavview.LauncherActivity;
import net.kdt.pojavview.R;
import net.kdt.pojavview.Tools;
import net.kdt.pojavview.prefs.screens.LauncherPreferenceFragment;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    public MainMenuFragment(){
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mNewsButton = view.findViewById(R.id.news_button);
        Button mCustomControlButton = view.findViewById(R.id.custom_control_button);

        mCustomControlButton.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));

        mNewsButton.setOnClickListener((v)->{
            Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, LauncherActivity.SETTING_FRAGMENT_TAG, true, null);
        });
    }
}
