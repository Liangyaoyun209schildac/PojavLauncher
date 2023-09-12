package net.kdt.pojavview;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import net.kdt.pojavview.tasks.AsyncAssetManager;
import net.kdt.pojavview.utils.LocaleUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class PojavApplication extends Application {
	public static final String CRASH_REPORT_TAG = "PojavCrashReport";
	public static final ExecutorService sExecutorService = new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS,  new LinkedBlockingQueue<>());
	
	@Override
	public void onCreate() {
		Thread.setDefaultUncaughtExceptionHandler((thread, th) -> {
			File crashFile = new File(Tools.DIR_GAME_HOME, "latestcrash.txt");
			try {
				// Write to file, since some devices may not able to show error
				File crashHome = crashFile.getParentFile();
				if (crashHome != null && !crashHome.exists() && !crashHome.mkdirs()) {
					throw new IOException("Failed to create crash log home");
				}
				PrintStream crashStream = new PrintStream(crashFile);
				crashStream.append("PojavLauncher crash report\n");
				crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n");
				crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL).append("\n");
				crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n");
				crashStream.append(" - Crash stack trace:\n");
				crashStream.append(Log.getStackTraceString(th));
				crashStream.close();
			} catch (Throwable throwable) {
				Log.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", throwable);
				Log.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th);
			}

			FatalErrorActivity.showError(PojavApplication.this, crashFile.getAbsolutePath(), true, th);
			MainActivity.fullyExit();
		});

		try {
			super.onCreate();
			Tools.APP_NAME = getResources().getString(R.string.app_short_name);

			Tools.DIR_GAME_HOME = getDir("files", MODE_PRIVATE).getParent();
			Tools.DIR_CACHE = getCacheDir();
			Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture();
			//Force x86 lib directory for Asus x86 based zenfones
			if (Architecture.isx86Device() && Architecture.is32BitsDevice()) {
				String originalJNIDirectory = getApplicationInfo().nativeLibraryDir;
				getApplicationInfo().nativeLibraryDir = originalJNIDirectory.substring(0,
								originalJNIDirectory.lastIndexOf("/"))
						.concat("/x86");
			}
			AsyncAssetManager.unpackComponents(getApplicationContext());
			AsyncAssetManager.unpackSingleFiles(getApplicationContext());
		} catch (Throwable throwable) {
			Intent ferrorIntent = new Intent(this, FatalErrorActivity.class);
			ferrorIntent.putExtra("throwable", throwable);
			ferrorIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
			startActivity(ferrorIntent);
		}
	}

	@Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLocale(this);
    }
}
