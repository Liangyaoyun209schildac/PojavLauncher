package net.kdt.pojavview;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

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

import net.kdt.pojavview.tasks.AsyncAssetManager;
import net.kdt.pojavview.utils.*;

public class PojavApplication {
	public static final ExecutorService sExecutorService = new ThreadPoolExecutor(4, 4, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

	public static void Init(Context context) {
		Tools.DIR_DATA = context.getDir("files", Context.MODE_PRIVATE).getParent();
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
