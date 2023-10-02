package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Architecture.ARCH_X86;
import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_VIRTUAL_MOUSE_START;
import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;
import static org.lwjgl.glfw.CallbackBridge.windowHeight;
import static org.lwjgl.glfw.CallbackBridge.windowWidth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.kdt.LoggerView;

import net.kdt.pojavlaunch.customcontrols.*;
import net.kdt.pojavlaunch.customcontrols.keyboard.LwjglCharSender;
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput;
import net.kdt.pojavlaunch.forgedisplay.SocketDisplay;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.services.GameService;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.LocaleUtils;
import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import org.lwjgl.glfw.CallbackBridge;

import java.io.File;
import java.io.IOException;

public class MainActivity extends BaseActivity implements ControlButtonMenuListener, EditorExitable {
    public static volatile ClipboardManager GLOBAL_CLIPBOARD;

    volatile public static boolean isInputStackCall;

    public static TouchCharInput touchCharInput;
    private MinecraftGLSurface minecraftGLView;
    private static Touchpad touchpad;
    private LoggerView loggerView;
    private DrawerLayout drawerLayout;
    private ListView navDrawer;
    private View mDrawerPullButton;
    private GyroControl mGyroControl = null;
    public static ControlLayout mControlLayout;

    MinecraftProfile minecraftProfile;

    private ArrayAdapter<String> gameActionArrayAdapter;
    private AdapterView.OnItemClickListener gameActionClickListener;
    public ArrayAdapter<String> ingameControlsEditorArrayAdapter;
    public AdapterView.OnItemClickListener ingameControlsEditorListener;

    private SocketDisplay socketDisplay;

    private LinearLayout forgeDisplay;
    private TextView forgeMem;
    private TextView forgeP1;
    private TextView forgeP2;
    private TextView forgeP3;
    private ProgressBar forgeMemP;
    private ProgressBar forgeP1P;
    private ProgressBar forgeP2P;
    private ProgressBar forgeP3P;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PojavApplication.init(this);

        minecraftProfile = new MinecraftProfile();
        Intent intent = getIntent();
        minecraftProfile.gameDir = intent.getStringExtra("GAME_DIR");
        minecraftProfile.jvmArgs = intent.getStringArrayExtra("JVM_ARGS");
        minecraftProfile.gameArgs = intent.getStringArrayExtra("GAME_ARGS");
        minecraftProfile.version = intent.getStringExtra("GAME_VERSION");
        minecraftProfile.time = intent.getStringExtra("GAME_TIME");
        minecraftProfile.jvmVersion = Integer.parseInt(intent.getStringExtra("JVM_VERSION"));
        minecraftProfile.mainclass = intent.getStringExtra("MAINCLASS");
        minecraftProfile.classpath = intent.getStringExtra("CLASSPATH");
        minecraftProfile.javaDir = intent.getStringExtra("JAVA_DIR");
        minecraftProfile.v2 = intent.getBooleanExtra("GAME_V2", false);

        if("1.12.2".equals(minecraftProfile.version)) {
            socketDisplay = new SocketDisplay(this::forgeUpdate);
        }

        MCOptionUtils.load(minecraftProfile.gameDir);
        GameService.startService(this);
        initLayout(R.layout.activity_basemain);
        CallbackBridge.addGrabListener(touchpad);
        CallbackBridge.addGrabListener(minecraftGLView);
        if(LauncherPreferences.PREF_ENABLE_GYRO) mGyroControl = new GyroControl(this);

        // Enabling this on TextureView results in a broken white result
        if(PREF_USE_ALTERNATE_SURFACE) getWindow().setBackgroundDrawable(null);
        else getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        // Set the sustained performance mode for available APIs
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            getWindow().setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE);

        ingameControlsEditorArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.menu_customcontrol));
        ingameControlsEditorListener = (parent, view, position, id) -> {
            switch(position) {
                case 0: mControlLayout.addControlButton(new ControlData("New")); break;
                case 1: mControlLayout.addDrawer(new ControlDrawerData()); break;
                case 2: mControlLayout.addJoystickButton(new ControlJoystickData()); break;
                case 3: mControlLayout.openLoadDialog(); break;
                case 4: mControlLayout.openSaveDialog(this); break;
                case 5: mControlLayout.openSetDefaultDialog(); break;
                case 6: mControlLayout.openExitDialog(this);
            }
        };

        // Recompute the gui scale when options are changed
        MCOptionUtils.MCOptionListener optionListener = MCOptionUtils::getMcScale;
        MCOptionUtils.addMCOptionListener(optionListener);
        mControlLayout.setModifiable(false);
    }

    @SuppressLint("SetTextI18n")
    private void forgeUpdate(int type, boolean have, String title, String message, int step,
                             int steps, int maxMemory, int totalMemory, int freeMemory) {
        Tools.runOnUiThread(() -> {
            if (type == 5) {
                forgeDisplay.setVisibility(View.GONE);
                socketDisplay.close();
                return;
            }

            if (forgeDisplay.getVisibility() != View.VISIBLE) {
                forgeDisplay.setVisibility(View.VISIBLE);
            }
            if (type == 0) {
                int usedMemory = totalMemory - freeMemory;
                float usedMemoryPercent = usedMemory / (float) maxMemory;

                forgeMem.setText(usedMemory + "MB / " + maxMemory + "MB");
                forgeMemP.setProgress((int) (usedMemoryPercent * 100));

            } else if (type == 1) {
                if (!have) {
                    forgeP1.setVisibility(View.GONE);
                    forgeP1P.setVisibility(View.GONE);
                } else {
                    forgeP1.setVisibility(View.VISIBLE);
                    forgeP1P.setVisibility(View.VISIBLE);

                    String text = title + " - " + message;
                    if (text.length() > 50) {
                        text = text.substring(0, 50);
                    }

                    forgeP1.setText(text);
                    forgeP1P.setMax(steps);
                    forgeP1P.setProgress(step);
                }
            } else if (type == 2) {
                if (!have) {
                    forgeP2.setVisibility(View.GONE);
                    forgeP2P.setVisibility(View.GONE);
                } else {
                    forgeP2.setVisibility(View.VISIBLE);
                    forgeP2P.setVisibility(View.VISIBLE);

                    String text = title + " - " + message;
                    if (text.length() > 50) {
                        text = text.substring(0, 50);
                    }

                    forgeP2.setText(text);
                    forgeP2P.setMax(steps);
                    forgeP2P.setProgress(step);
                }
            } else if (type == 3) {
                if (!have) {
                    forgeP3.setVisibility(View.GONE);
                    forgeP3P.setVisibility(View.GONE);
                } else {
                    forgeP3.setVisibility(View.VISIBLE);
                    forgeP3P.setVisibility(View.VISIBLE);

                    String text = title + " - " + message;
                    if (text.length() > 50) {
                        text = text.substring(0, 50);
                    }

                    forgeP3.setText(text);
                    forgeP3P.setMax(steps);
                    forgeP3P.setProgress(step);
                }
            }
        });
    }

    protected void initLayout(int resId) {
        setContentView(resId);
        bindValues();
        mControlLayout.setMenuListener(this);

        mDrawerPullButton.setOnClickListener(v -> onClickedMenu());
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        try {
            String file = getIntent().getStringExtra("LOG_FILE");
            File latestLogFile = new File(file);
            if(!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw new IOException("Failed to create a new log file");
            Logger.begin(latestLogFile.getAbsolutePath());
            // FIXME: is it safe for multi thread?
            GLOBAL_CLIPBOARD = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            touchCharInput.setCharacterSender(new LwjglCharSender());

            setTitle("Minecraft " + minecraftProfile.version);

            // Minecraft 1.13+
            isInputStackCall = minecraftProfile.v2;
            CallbackBridge.nativeSetUseInputStackQueue(isInputStackCall);

            Tools.getDisplayMetrics(this);
            windowWidth = Tools.getDisplayFriendlyRes(currentDisplayMetrics.widthPixels, 1f);
            windowHeight = Tools.getDisplayFriendlyRes(currentDisplayMetrics.heightPixels, 1f);

            // Menu
            gameActionArrayAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.menu_ingame));
            gameActionClickListener = (parent, view, position, id) -> {
                switch(position) {
                    case 0: dialogForceClose(MainActivity.this); break;
                    case 1: openLogOutput(); break;
                    case 2: dialogSendCustomKey(); break;
                    case 3: adjustMouseSpeedLive(); break;
                    case 4: adjustGyroSensitivityLive(); break;
                    case 5: openCustomControls(); break;
                }
                drawerLayout.closeDrawers();
            };
            navDrawer.setAdapter(gameActionArrayAdapter);
            navDrawer.setOnItemClickListener(gameActionClickListener);
            drawerLayout.closeDrawers();

            final String finalVersion = minecraftProfile.version;
            minecraftGLView.setSurfaceReadyListener(() -> {
                try {
                    // Setup virtual mouse right before launching
                    if (PREF_VIRTUAL_MOUSE_START) {
                        touchpad.post(() -> touchpad.switchState());
                    }

                    runCraft(finalVersion);
                }catch (Throwable e){
                    Tools.showError(getApplicationContext(), e, true);
                }
            });

            minecraftGLView.start();
        } catch (Throwable e) {
            Tools.showError(this, e, true);
        }
    }

    private void loadControls() {
        try {
            // Load keys
            mControlLayout.loadLayout(
                    minecraftProfile.controlFile == null
                            ? LauncherPreferences.PREF_DEFAULTCTRL_PATH
                            : Tools.CTRLMAP_PATH + "/" + minecraftProfile.controlFile);
        } catch(IOException e) {
            try {
                Log.w("MainActivity", "Unable to load the control file, loading the default now", e);
                mControlLayout.loadLayout(Tools.CTRLDEF_FILE);
            } catch (IOException ioException) {
                Tools.showError(this, ioException);
            }
        } catch (Throwable th) {
            Tools.showError(this, th);
        }
        mDrawerPullButton.setVisibility(mControlLayout.hasMenuButton() ? View.GONE : View.VISIBLE);
        mControlLayout.toggleControlVisible();
    }

    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
        loadControls();
    }

    /** Boilerplate binding */
    private void bindValues() {
        mControlLayout = findViewById(R.id.main_control_layout);
        minecraftGLView = findViewById(R.id.main_game_render_view);
        touchpad = findViewById(R.id.main_touchpad);
        drawerLayout = findViewById(R.id.main_drawer_options);
        navDrawer = findViewById(R.id.main_navigation_view);
        loggerView = findViewById(R.id.mainLoggerView);
        mControlLayout = findViewById(R.id.main_control_layout);
        touchCharInput = findViewById(R.id.mainTouchCharInput);
        mDrawerPullButton = findViewById(R.id.drawer_button);

        forgeDisplay = findViewById(R.id.main_forge_display);
        forgeMem = findViewById(R.id.main_forge_mem);
        forgeP1 = findViewById(R.id.main_forge_p1);
        forgeP2 = findViewById(R.id.main_forge_p2);
        forgeP3 = findViewById(R.id.main_forge_p3);
        forgeMemP = findViewById(R.id.main_forge_mem1);
        forgeP1P = findViewById(R.id.main_forge_p11);
        forgeP2P = findViewById(R.id.main_forge_p21);
        forgeP3P = findViewById(R.id.main_forge_p31);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mGyroControl != null) mGyroControl.enable();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1);
    }

    @Override
    protected void onPause() {
        if(mGyroControl != null) mGyroControl.disable();
        if (CallbackBridge.isGrabbing()){
            sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
        }
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 1);
    }

    @Override
    protected void onStop() {
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 0);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CallbackBridge.removeGrabListener(touchpad);
        CallbackBridge.removeGrabListener(minecraftGLView);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if(mGyroControl != null) mGyroControl.updateOrientation();
        Tools.updateWindowSize(this);
        minecraftGLView.refreshSize();
        runOnUiThread(() -> mControlLayout.refreshControlButtonPositions());
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(minecraftGLView != null)  // Useful when backing out of the app
            Tools.MAIN_HANDLER.postDelayed(() -> minecraftGLView.refreshSize(), 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            // Reload PREF_DEFAULTCTRL_PATH
            LauncherPreferences.loadPreferences(getApplicationContext());
            try {
                mControlLayout.loadLayout(LauncherPreferences.PREF_DEFAULTCTRL_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void fullyExit() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static boolean isAndroid8OrHigher() {
        return Build.VERSION.SDK_INT >= 26;
    }

    private void runCraft(String versionId) throws Throwable {
        if(Tools.LOCAL_RENDERER == null) {
            Tools.LOCAL_RENDERER = LauncherPreferences.PREF_RENDERER;
        }
        if(!Tools.checkRendererCompatible(this, Tools.LOCAL_RENDERER)) {
            Tools.RenderersList renderersList = Tools.getCompatibleRenderers(this);
            String firstCompatibleRenderer = renderersList.rendererIds.get(0);
            Log.w("runCraft","Incompatible renderer "+Tools.LOCAL_RENDERER+ " will be replaced with "+firstCompatibleRenderer);
            Tools.LOCAL_RENDERER = firstCompatibleRenderer;
            Tools.releaseRenderersCache();
        }
        Logger.appendToLog("--------- beginning with launcher debug");
        printLauncherInfo(versionId);
        JREUtils.redirectAndPrintJRELog();
        int res = Tools.launchMinecraft(this, minecraftProfile, socketDisplay == null ? 0 : socketDisplay.port);
        Intent intent = new Intent();
        intent.putExtra("res", res);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void printLauncherInfo(String gameVersion) {
        Logger.appendToLog("Info: Architecture: " + Architecture.archAsString(Tools.DEVICE_ARCHITECTURE));
        Logger.appendToLog("Info: Device model: " + Build.MANUFACTURER + " " + Build.MODEL);
        Logger.appendToLog("Info: API version: " + Build.VERSION.SDK_INT);
        Logger.appendToLog("Info: Selected Minecraft version: " + gameVersion);
    }

    private void checkVulkanZinkIsSupported() {
        if (Tools.DEVICE_ARCHITECTURE == ARCH_X86
                || Build.VERSION.SDK_INT < 25
                || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL)
                || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
            Logger.appendToLog("Error: Vulkan Zink renderer is not supported!");
            throw new RuntimeException(getString(R.string. mcn_check_fail_vulkan_support));
        }
    }

    private void dialogSendCustomKey() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.control_customkey);
        dialog.setItems(EfficientAndroidLWJGLKeycode.generateKeyName(), (dInterface, position) -> EfficientAndroidLWJGLKeycode.execKeyIndex(position));
        dialog.show();
    }

    boolean isInEditor;
    private void openCustomControls() {
        if(ingameControlsEditorListener == null || ingameControlsEditorArrayAdapter == null) return;

        mControlLayout.setModifiable(true);
        navDrawer.setAdapter(ingameControlsEditorArrayAdapter);
        navDrawer.setOnItemClickListener(ingameControlsEditorListener);
        mDrawerPullButton.setVisibility(View.VISIBLE);
        isInEditor = true;
    }

    private void openLogOutput() {
        loggerView.setVisibility(View.VISIBLE);
    }

    public static void toggleMouse(Context ctx) {
        if (CallbackBridge.isGrabbing()) return;

        Toast.makeText(ctx, touchpad.switchState()
                        ? R.string.control_mouseon : R.string.control_mouseoff,
                Toast.LENGTH_SHORT).show();
    }

    public static void dialogForceClose(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setMessage(R.string.mcn_exit_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (p1, p2) -> {
                    try {
                        fullyExit();
                    } catch (Throwable th) {
                        Log.w(Tools.APP_NAME, "Could not enable System.exit() method!", th);
                    }
                }).show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(isInEditor) {
            if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) mControlLayout.askToExit(this);
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        boolean handleEvent;
        if(!(handleEvent = minecraftGLView.processKeyEvent(event))) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && !touchCharInput.isEnabled()) {
                if(event.getAction() != KeyEvent.ACTION_UP) return true; // We eat it anyway
                sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
                return true;
            }
        }
        return handleEvent;
    }

    public static void switchKeyboardState() {
        if(touchCharInput != null) touchCharInput.switchKeyboardState();
    }


    int tmpMouseSpeed;
    public void adjustMouseSpeedLive() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.mcl_setting_title_mousespeed);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_live_mouse_speed_editor,null);
        final SeekBar sb = v.findViewById(R.id.mouseSpeed);
        final TextView tv = v.findViewById(R.id.mouseSpeedTV);
        sb.setMax(275);
        tmpMouseSpeed = (int) ((LauncherPreferences.PREF_MOUSESPEED*100));
        sb.setProgress(tmpMouseSpeed-25);
        tv.setText(getString(R.string.percent_format, tmpMouseSpeed));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tmpMouseSpeed = i+25;
                tv.setText(getString(R.string.percent_format, tmpMouseSpeed));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        b.setView(v);
        b.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            LauncherPreferences.PREF_MOUSESPEED = ((float)tmpMouseSpeed)/100f;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("mousespeed",tmpMouseSpeed).apply();
            dialogInterface.dismiss();
            System.gc();
        });
        b.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            System.gc();
        });
        b.show();
    }

    int tmpGyroSensitivity;
    public void adjustGyroSensitivityLive() {
        if(!LauncherPreferences.PREF_ENABLE_GYRO) {
            Toast.makeText(this, R.string.toast_turn_on_gyro, Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.preference_gyro_sensitivity_title);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_live_mouse_speed_editor,null);
        final SeekBar sb = v.findViewById(R.id.mouseSpeed);
        final TextView tv = v.findViewById(R.id.mouseSpeedTV);
        sb.setMax(275);
        tmpGyroSensitivity = (int) ((LauncherPreferences.PREF_GYRO_SENSITIVITY*100));
        sb.setProgress(tmpGyroSensitivity -25);
        tv.setText(getString(R.string.percent_format, tmpGyroSensitivity));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                tmpGyroSensitivity = i+25;
                tv.setText(getString(R.string.percent_format, tmpGyroSensitivity));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        b.setView(v);
        b.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            LauncherPreferences.PREF_GYRO_SENSITIVITY = ((float) tmpGyroSensitivity)/100f;
            LauncherPreferences.DEFAULT_PREF.edit().putInt("gyroSensitivity", tmpGyroSensitivity).apply();
            dialogInterface.dismiss();
            System.gc();
        });
        b.setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
            dialogInterface.dismiss();
            System.gc();
        });
        b.show();
    }

    private static void setUri(Context context, String input, Intent intent) {
        if(input.startsWith("file:")) {
            int truncLength = 5;
            if(input.startsWith("file://")) truncLength = 7;
            input = input.substring(truncLength);
            Log.i("MainActivity", input);
            boolean isDirectory = new File(input).isDirectory();
            if(isDirectory) {
                intent.setType(DocumentsContract.Document.MIME_TYPE_DIR);
            }else{
                String type = null;
                String extension = MimeTypeMap.getFileExtensionFromUrl(input);
                if(extension != null) type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if(type == null) type = "*/*";
                intent.setType(type);
            }
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setData(DocumentsContract.buildDocumentUri(
                    context.getString(R.string.storageProviderAuthorities), input
            ));
            return;
        }
        intent.setDataAndType(Uri.parse(input), "*/*");
    }

    public static void openLink(String link) {
        Context ctx = touchpad.getContext(); // no more better way to obtain a context statically
        ((Activity)ctx).runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                setUri(ctx, link, intent);
                ctx.startActivity(intent);
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }
    @SuppressWarnings("unused") //TODO: actually use it
    public static void openPath(String path) {
        Context ctx = touchpad.getContext(); // no more better way to obtain a context statically
        ((Activity)ctx).runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(DocumentsContract.buildDocumentUri(ctx.getString(R.string.storageProviderAuthorities), path), "*/*");
                ctx.startActivity(intent);
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }

    public static void querySystemClipboard() {
        Tools.runOnUiThread(()->{
            ClipData clipData = GLOBAL_CLIPBOARD.getPrimaryClip();
            if(clipData == null) {
                AWTInputBridge.nativeClipboardReceived(null, null);
                return;
            }
            ClipData.Item firstClipItem = clipData.getItemAt(0);
            //TODO: coerce to HTML if the clip item is styled
            CharSequence clipItemText = firstClipItem.getText();
            if(clipItemText == null) {
                AWTInputBridge.nativeClipboardReceived(null, null);
                return;
            }
            AWTInputBridge.nativeClipboardReceived(clipItemText.toString(), "plain");
        });
    }

    public static void putClipboardData(String data, String mimeType) {
        Tools.runOnUiThread(()-> {
            ClipData clipData = null;
            switch(mimeType) {
                case "text/plain":
                    clipData = ClipData.newPlainText("AWT Paste", data);
                    break;
                case "text/html":
                    clipData = ClipData.newHtmlText("AWT Paste", data, data);
            }
            if(clipData != null) GLOBAL_CLIPBOARD.setPrimaryClip(clipData);
        });
    }

    @Override
    public void onClickedMenu() {
        drawerLayout.openDrawer(navDrawer);
        navDrawer.requestLayout();
    }

    @Override
    public void exitEditor() {
        try {
            MainActivity.mControlLayout.loadLayout((CustomControls)null);
            MainActivity.mControlLayout.setModifiable(false);
            System.gc();
            MainActivity.mControlLayout.loadLayout(
                    minecraftProfile.controlFile == null
                            ? LauncherPreferences.PREF_DEFAULTCTRL_PATH
                            : Tools.CTRLMAP_PATH + "/" + minecraftProfile.controlFile);
            mDrawerPullButton.setVisibility(mControlLayout.hasMenuButton() ? View.GONE : View.VISIBLE);
        } catch (IOException e) {
            Tools.showError(this,e);
        }
        //((MainActivity) this).mControlLayout.loadLayout((CustomControls)null);
        navDrawer.setAdapter(gameActionArrayAdapter);
        navDrawer.setOnItemClickListener(gameActionClickListener);
        isInEditor = false;
    }
}
