package net.kdt.pojavlaunch;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;

import net.kdt.pojavlaunch.multirt.MultiRTConfigDialog;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;

public class LauncherActivity extends BaseActivity {
    public static final String SETTING_FRAGMENT_TAG = "SETTINGS_FRAGMENT";

    private final int REQUEST_STORAGE_REQUEST_CODE = 1024;
    private final Object mLockStoragePerm = new Object();
    private ProgressLayout mProgressLayout;
    private ProgressServiceKeeper mProgressServiceKeeper;

    /* Listener for the back button in settings */
    private final ExtraListener<String> mBackPreferenceListener = (key, value) -> {
        if (value.equals("true")) onBackPressed();
        return false;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pojav_launcher);
        getWindow().setBackgroundDrawable(null);
        bindViews();
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));
        requestStoragePermission(); // Will wait here

        ProgressKeeper.addTaskCountListener(mProgressLayout);
        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);

        mProgressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT);
        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);
        mProgressLayout.observe(ProgressLayout.AUTHENTICATE_MICROSOFT);
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);
    }

    @Override
    public boolean setFullscreen() {
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(mProgressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == Tools.RUN_MOD_INSTALLER && data != null) {
            Tools.launchModInstaller(this, data);
            return;
        }
        if (requestCode == MultiRTConfigDialog.MULTIRT_PICK_RUNTIME && data != null) {
            Tools.installRuntimeFromUri(this, data.getData());
        }
        if (requestCode == REQUEST_STORAGE_REQUEST_CODE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            synchronized (mLockStoragePerm) {
                mLockStoragePerm.notifyAll();
            }
        }
    }

    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
    }

    @SuppressWarnings("unused")
    private Fragment getVisibleFragment(int id) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(id);
        if (fragment != null && fragment.isVisible()) {
            return fragment;
        }
        return null;
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 先判断有没有权限
            if (Environment.isExternalStorageManager()) {

            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getBaseContext().getPackageName()));
                startActivityForResult(intent, REQUEST_STORAGE_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 先判断有没有权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_REQUEST_CODE);
            }
        } else {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_REQUEST_CODE) {
            synchronized (mLockStoragePerm) {
                mLockStoragePerm.notifyAll();
            }
        }
    }

    /**
     * Stuff all the view boilerplate here
     */
    private void bindViews() {
        mProgressLayout = findViewById(R.id.progress_layout);
    }
}
