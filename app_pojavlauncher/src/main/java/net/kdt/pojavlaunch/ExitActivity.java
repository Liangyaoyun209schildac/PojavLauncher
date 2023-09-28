package net.kdt.pojavlaunch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

@Keep
public class ExitActivity extends AppCompatActivity {

    @SuppressLint("StringFormatInvalid") //invalid on some translations but valid on most, cant fix that atm
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int code = -1;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            code = extras.getInt("code",-1);
        }

        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.mcn_exit_title,code))
                .setPositiveButton(R.string.main_open_logs, (p1, p2) -> {
                    Tools.openFile(this);

                    //MainActivity.fullyExit();
                })
                .setOnDismissListener(dialog -> ExitActivity.this.finish())
                .show();
    }

    public static void showExitMessage(Context ctx, int code) {
        Intent i = new Intent(ctx,ExitActivity.class);
        i.putExtra("code",code);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

}
