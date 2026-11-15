package com.gathin.tvhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class QuickSettingsActivity extends Activity {
    private MediaController player;
    private TextView nowTitle;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radius));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private void glass(View v, int radius) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, shape(0x6655E6E6, 0xFFFFFFFF, radius));
        s.addState(new int[]{}, shape(0x2AFFFFFF, 0x44FFFFFF, radius));
        v.setBackground(s);
        v.setFocusable(true);
        v.setClickable(true);
    }

    private TextView text(String t, int sp) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(0xFFFFFFFF);
        v.setSingleLine(true);
        return v;
    }

    private TextView button(String t, Runnable r) {
        TextView b = text(t, 16);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        glass(b, 18);
        b.setOnClickListener(v -> r.run());
        return b;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setOnClickListener(v -> finish());
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(shape(0xE60B2F3A, 0x6674E0E0, 28));
        panel.setOnClickListener(v -> { });
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(dp(440), -2, Gravity.TOP | Gravity.END);
        pp.topMargin = dp(90);
        pp.setMarginEnd(dp(48));
        root.addView(panel, pp);

        LinearLayout now = new LinearLayout(this);
        now.setGravity(Gravity.CENTER_VERTICAL);
        nowTitle = text("Nothing playing", 15);
        now.addView(nowTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        now.addView(button("<<", () -> { if (player != null) player.getTransportControls().skipToPrevious(); }));
        now.addView(button("||", this::toggle));
        now.addView(button(">>", () -> { if (player != null) player.getTransportControls().skipToNext(); }));
        panel.addView(now);

        TextView volLabel = text("Volume", 13);
        volLabel.setPadding(0, dp(14), 0, dp(4));
        panel.addView(volLabel);
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        SeekBar vol = new SeekBar(this);
        vol.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        vol.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC));
        vol.setFocusable(true);
        vol.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) am.setStreamVolume(AudioManager.STREAM_MUSIC, p, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        panel.addView(vol);

        Object[][] tiles = {
            {"Network", R.drawable.ic_wifi, SystemSettings.intent(this, Settings.ACTION_WIFI_SETTINGS)},
            {"Bluetooth", R.drawable.ic_settings, SystemSettings.intent(this, Settings.ACTION_BLUETOOTH_SETTINGS)},
            {"Display", R.drawable.ic_picture, SystemSettings.displayIntent(this)},
            {"Sound", R.drawable.ic_settings, SystemSettings.intent(this, Settings.ACTION_SOUND_SETTINGS)},
            {"Apps", R.drawable.ic_apps, SystemSettings.intent(this, Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)},
            {"Accessibility", R.drawable.ic_settings, SystemSettings.intent(this, Settings.ACTION_ACCESSIBILITY_SETTINGS)},
            {"My Fire TV", R.drawable.ic_settings, SystemSettings.intent(this, Settings.ACTION_DEVICE_INFO_SETTINGS)},
            {"Screen Mirroring", R.drawable.ic_picture, SystemSettings.intent(this, Settings.ACTION_CAST_SETTINGS)},
            {"All Settings", R.drawable.ic_settings, SystemSettings.intent(this, Settings.ACTION_SETTINGS)},
            {"Launcher", R.drawable.ic_home_settings,
                new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_LAUNCHER_SETTINGS)},
        };
        View first = null;
        for (int i = 0; i < tiles.length; i += 2) {
            LinearLayout line = new LinearLayout(this);
            line.setPadding(0, dp(10), 0, 0);
            for (int j = i; j < Math.min(i + 2, tiles.length); j++) {
                View t = tile((String) tiles[j][0], (Integer) tiles[j][1], (Intent) tiles[j][2]);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                lp.setMargins(dp(4), 0, dp(4), 0);
                line.addView(t, lp);
                if (first == null) first = t;
            }
            panel.addView(line);
        }
        setContentView(root);
        refreshPlayer();
        if (first != null) first.requestFocus();
    }

    private View tile(String label, int icon, Intent target) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(14), dp(8), dp(14));
        ImageView img = new ImageView(this);
        img.setImageResource(icon);
        t.addView(img, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView name = text(label, 13);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(6), 0, 0);
        t.addView(name);
        glass(t, 20);
        t.setOnClickListener(v -> {
            try {
                startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e) {
                Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
            }
            finish();
        });
        return t;
    }

    private void refreshPlayer() {
        player = null;
        try {
            MediaSessionManager m = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> l = m.getActiveSessions(new ComponentName(this, GuardRevival.class));
            if (!l.isEmpty()) player = l.get(0);
        } catch (Exception ignored) { }
        String title = null;
        if (player != null && player.getMetadata() != null) {
            title = player.getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE);
        }
        nowTitle.setText(title != null ? title : "Nothing playing");
    }

    private void toggle() {
        if (player == null) return;
        PlaybackState s = player.getPlaybackState();
        if (s != null && s.getState() == PlaybackState.STATE_PLAYING) player.getTransportControls().pause();
        else player.getTransportControls().play();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onPause() {
        super.onPause();
        finish();
        overridePendingTransition(0, 0);
    }
}
