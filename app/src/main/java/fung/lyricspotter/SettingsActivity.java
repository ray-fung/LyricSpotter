package fung.lyricspotter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences mSharedPreferences;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private boolean quitApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSharedPreferences = this.getSharedPreferences("chatHead", 0);

        Button connectSpotify = findViewById(R.id.updateButton);
        Switch enableChatheads = findViewById(R.id.lyric_head_switch);

        boolean enableChatheadsPrev = mSharedPreferences.getBoolean("allow_lyric_head", false);
        enableChatheads.setChecked(enableChatheadsPrev);

        quitApp = true;

        connectSpotify.setOnClickListener(func -> {
            Intent newIntent = new Intent(SettingsActivity.this, SplashActivity.class);
            startActivity(newIntent);
        });

        enableChatheads.setOnCheckedChangeListener((func, bool) -> {
            // TODO save settings with SharedPreferences and start service
            if (bool) {
                SharedPreferences.Editor temp = mSharedPreferences.edit();
                temp.putBoolean("allow_lyric_head", true);
                // We use commit because this must be done immediately
                temp.commit();
                if (!Settings.canDrawOverlays(this)) {
                    //If the draw over permission is not available open the settings screen
                    //to grant the permission.
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                } else {
                    initializeChatHead();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            // Check if the permission is granted or not.
            // Settings activity never returns proper value so instead check with following method
            if (Settings.canDrawOverlays(this)) {
                initializeChatHead();
            } else { // Permission is not available
                Log.e("Permission: ", "NOT GRANTED");
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void initializeChatHead() {
        startService(new Intent(SettingsActivity.this, LyricHeadService.class));
        finish();
    }

    @Override
    public void onResume() {
        Log.e("onResume: Settings", "Being called!");
        quitApp = true;
        Long currTime = System.currentTimeMillis();
        if (mSharedPreferences == null) {
            mSharedPreferences = this.getSharedPreferences("SPOTIFY", 0);
        }
        Long sinceLastLogin = mSharedPreferences.getLong("time_since_last", currTime);

        if (sinceLastLogin != currTime && currTime - 3600000L > sinceLastLogin) {
            // It has been over an hour since the last login, which means that the
            // API token needs to be refreshed (since Spotify doesn't support
            // the Android SDK with refresh tokens)
            Intent newIntent = new Intent(SettingsActivity.this, SplashActivity.class);
            startActivity(newIntent);
        }
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.e("onStop: SettingsActivity", "Being called!");
        SharedPreferences temp = this.getSharedPreferences("chatHead", 0);
        boolean allowChatHead = temp.getBoolean("allow_lyric_head", false);
        if (allowChatHead && quitApp) {
            startService(new Intent(SettingsActivity.this, LyricHeadService.class));
            finish();
        }
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        Log.e("onBackPressed", "Called!");
        quitApp = false;
        super.onBackPressed();
    }

}
