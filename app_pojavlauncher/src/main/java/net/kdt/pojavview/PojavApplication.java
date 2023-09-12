package net.kdt.pojavview;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.os.*;
import androidx.core.app.*;

import android.util.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;
import net.kdt.pojavview.tasks.AsyncAssetManager;
import net.kdt.pojavview.utils.*;

public class PojavApplication {

	private static final int REQUEST_STORAGE_REQUEST_CODE = 1;

	public static final ExecutorService sExecutorService = new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

	public static void Init(Context context) {
		Thread.setDefaultUncaughtExceptionHandler((thread, th) -> {
			File crashFile = new File(Tools.DIR_GAME_HOME, "latestcrash.txt");
			try {
				// Write to file, since some devices may not able to show error
				File crashHome = crashFile.getParentFile();
				if(crashHome != null && !crashHome.exists() && !crashHome.mkdirs()) {
					throw new IOException("Failed to create crash log home");
				}
				PrintStream crashStream = new PrintStream(crashFile);
				crashStream.append("Crash report\n");
				crashStream.append(" - Time: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n");
				crashStream.append(" - Device: ").append(Build.PRODUCT).append(" ").append(Build.MODEL).append("\n");
				crashStream.append(" - Android version: ").append(Build.VERSION.RELEASE).append("\n");
				crashStream.append(" - Crash stack trace:\n");
				crashStream.append(Log.getStackTraceString(th));
				crashStream.close();
			} catch (Throwable throwable) {
				Log.e("Pojav Crash", " - Exception attempt saving crash stack trace:", throwable);
				Log.e("Pojav Crash", " - The crash stack trace was:", th);
			}

			FatalErrorActivity.showError(context, crashFile.getAbsolutePath(), true, th);
			MainActivity.fullyExit();
		});

		Tools.DIR_CACHE = context.getCacheDir();
		Tools.DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture();
		//Force x86 lib directory for Asus x86 based zenfones
		if(Architecture.isx86Device() && Architecture.is32BitsDevice()){
			String originalJNIDirectory = context.getApplicationInfo().nativeLibraryDir;
			context.getApplicationInfo().nativeLibraryDir = originalJNIDirectory.substring(0,
							originalJNIDirectory.lastIndexOf("/"))
					.concat("/x86");
		}

		AsyncAssetManager.unpackComponents(context);
		AsyncAssetManager.unpackSingleFiles(context);
	}
}
