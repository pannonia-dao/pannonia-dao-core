package ai.cerberus.demo;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.view.Choreographer;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements Choreographer.FrameCallback {
    private static final int BG = Color.rgb(3, 8, 12);
    private static final int PANEL = Color.rgb(7, 16, 22);
    private static final int GRID = Color.rgb(13, 36, 42);
    private static final int TEXT = Color.rgb(214, 255, 229);
    private static final int MUTED = Color.rgb(91, 130, 119);
    private static final int GREEN = Color.rgb(50, 255, 139);
    private static final int CYAN = Color.rgb(65, 220, 255);
    private static final int ORANGE = Color.rgb(255, 181, 60);
    private static final int RED = Color.rgb(255, 75, 93);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private TextView cpuValue, heapValue, rxValue, txValue, batteryValue, tempValue;
    private TextView thermalValue, fpsValue, foregroundValue, aiAppsValue, gpuValue, permissionValue;
    private LinearLayout terminal;
    private Button usageButton;

    private long lastWallMs;
    private long lastCpuMs;
    private long lastRx;
    private long lastTx;
    private int frameCounter = 0;
    private int lastFps = 0;
    private long lastFpsTick = 0;
    private int terminalSampleCounter = 0;

    private final Runnable sampler = new Runnable() {
        @Override public void run() {
            sampleSignals();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        buildUi();
        lastWallMs = System.currentTimeMillis();
        lastCpuMs = Process.getElapsedCpuTime();
        lastRx = safeTraffic(TrafficStats.getUidRxBytes(Process.myUid()));
        lastTx = safeTraffic(TrafficStats.getUidTxBytes(Process.myUid()));
        lastFpsTick = System.currentTimeMillis();
        log("BOOT", "Cerberus Signal Lab v0.2 online", GREEN);
        log("SENSOR", "Real local telemetry pipeline armed", CYAN);
        log("GPU", "System-wide GPU percentage unavailable on stock Android API", ORANGE);
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(sampler);
        handler.post(sampler);
        Choreographer.getInstance().removeFrameCallback(this);
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(sampler);
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override public void doFrame(long frameTimeNanos) {
        frameCounter++;
        long now = System.currentTimeMillis();
        if (now - lastFpsTick >= 1000L) {
            lastFps = frameCounter;
            frameCounter = 0;
            lastFpsTick = now;
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 18, 18, 28);
        root.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);

        root.addView(text("CERBERUS // SIGNAL LAB", 28, TEXT, true, true));
        root.addView(text("LOCAL ANDROID TELEMETRY · BASELINE v0.2", 11, GREEN, true, true));
        root.addView(text("[ REAL SIGNALS ]  [ ZERO ROOT ]  [ OFFLINE ]", 10, CYAN, true, true));

        LinearLayout banner = panel();
        banner.addView(text("LIVE DEFENSE TELEMETRY", 12, MUTED, true, true));
        banner.addView(text("Measure first. Infer later. Never fabricate unavailable signals.", 13, TEXT, false, true));
        root.addView(banner);

        LinearLayout row1 = row();
        cpuValue = value("--"); heapValue = value("--"); fpsValue = value("--"); gpuValue = value("UNAVAILABLE");
        row1.addView(metric("APP CPU", cpuValue, GREEN));
        row1.addView(metric("APP HEAP", heapValue, CYAN));
        row1.addView(metric("RENDER FPS", fpsValue, GREEN));
        row1.addView(metric("GPU %", gpuValue, ORANGE));
        root.addView(row1);

        LinearLayout row2 = row();
        rxValue = value("--"); txValue = value("--"); batteryValue = value("--"); tempValue = value("--");
        row2.addView(metric("RX RATE", rxValue, CYAN));
        row2.addView(metric("TX RATE", txValue, CYAN));
        row2.addView(metric("BATTERY", batteryValue, GREEN));
        row2.addView(metric("TEMP", tempValue, ORANGE));
        root.addView(row2);

        LinearLayout device = panel();
        device.addView(text("DEVICE STATE", 11, MUTED, true, true));
        thermalValue = text("THERMAL        --", 13, TEXT, true, true);
        permissionValue = text("USAGE ACCESS   --", 13, TEXT, true, true);
        device.addView(thermalValue);
        device.addView(permissionValue);
        root.addView(device);

        LinearLayout activity = panel();
        activity.addView(text("FOREGROUND / AI APP ACTIVITY", 11, MUTED, true, true));
        foregroundValue = text("LAST FOREGROUND PACKAGE\n--", 13, GREEN, true, true);
        aiAppsValue = text("AI APPS SEEN · LAST 10 MIN\n--", 13, CYAN, true, true);
        activity.addView(foregroundValue);
        activity.addView(aiAppsValue);
        usageButton = new Button(this);
        usageButton.setText("GRANT / CHECK USAGE ACCESS");
        usageButton.setTextColor(Color.WHITE);
        usageButton.setAllCaps(false);
        usageButton.setTextSize(13);
        usageButton.setBackgroundColor(Color.rgb(0, 92, 74));
        usageButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        activity.addView(usageButton);
        root.addView(activity);

        LinearLayout terminalPanel = panel();
        terminalPanel.addView(text("TERMINAL // RAW SIGNAL STREAM", 11, MUTED, true, true));
        terminal = new LinearLayout(this);
        terminal.setOrientation(LinearLayout.VERTICAL);
        terminalPanel.addView(terminal);
        root.addView(terminalPanel);

        root.addView(text("> GPU NOTE: Android exposes no portable system-wide per-app GPU utilization percentage to ordinary apps. Cerberus reports this as UNAVAILABLE instead of inventing a value.\n> Usage activity requires explicit Usage Access permission from Android Settings.", 10, MUTED, false, true));
    }

    private void sampleSignals() {
        long now = System.currentTimeMillis();
        long cpuNow = Process.getElapsedCpuTime();
        long wallDelta = Math.max(1L, now - lastWallMs);
        long cpuDelta = Math.max(0L, cpuNow - lastCpuMs);
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        double cpuPct = Math.min(100.0, (100.0 * cpuDelta / wallDelta) / cores);
        cpuValue.setText(String.format(Locale.US, "%.1f%%", cpuPct));
        lastWallMs = now;
        lastCpuMs = cpuNow;

        Runtime rt = Runtime.getRuntime();
        long heapMb = (rt.totalMemory() - rt.freeMemory()) / (1024L * 1024L);
        heapValue.setText(heapMb + " MB");
        fpsValue.setText(lastFps + " fps");
        gpuValue.setText("N/A");

        long rx = safeTraffic(TrafficStats.getUidRxBytes(Process.myUid()));
        long tx = safeTraffic(TrafficStats.getUidTxBytes(Process.myUid()));
        rxValue.setText(rate(rx, lastRx));
        txValue.setText(rate(tx, lastTx));
        lastRx = rx;
        lastTx = tx;

        Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batt != null) {
            int level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batt.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int temp = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int pct = scale > 0 ? Math.round(level * 100f / scale) : -1;
            batteryValue.setText(pct >= 0 ? pct + "%" : "N/A");
            tempValue.setText(String.format(Locale.US, "%.1f°C", temp / 10.0));
        }

        thermalValue.setText("THERMAL        " + thermalLabel());
        boolean usageGranted = hasUsageAccess();
        permissionValue.setText("USAGE ACCESS   " + (usageGranted ? "GRANTED" : "REQUIRED"));
        permissionValue.setTextColor(usageGranted ? GREEN : ORANGE);

        if (usageGranted) {
            UsageSnapshot snap = usageSnapshot();
            foregroundValue.setText("LAST FOREGROUND PACKAGE\n" + (snap.latestPackage == null ? "--" : snap.latestPackage));
            if (snap.aiPackages.isEmpty()) {
                aiAppsValue.setText("AI APPS SEEN · LAST 10 MIN\nnone detected");
            } else {
                aiAppsValue.setText("AI APPS SEEN · LAST 10 MIN\n" + join(snap.aiPackages));
            }
        } else {
            foregroundValue.setText("LAST FOREGROUND PACKAGE\npermission required");
            aiAppsValue.setText("AI APPS SEEN · LAST 10 MIN\npermission required");
        }

        terminalSampleCounter++;
        if (terminalSampleCounter % 5 == 0) {
            log("SAMPLE", String.format(Locale.US,
                    "cpu=%.1f%% heap=%dMB rx=%s tx=%s fps=%d thermal=%s",
                    cpuPct, heapMb, rate(rx, lastRx), rate(tx, lastTx), lastFps, thermalLabel()), GREEN);
        }
    }

    private boolean hasUsageAccess() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private UsageSnapshot usageSnapshot() {
        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        long end = System.currentTimeMillis();
        long begin = end - 10L * 60L * 1000L;
        UsageEvents events = usm.queryEvents(begin, end);
        UsageEvents.Event e = new UsageEvents.Event();
        String latest = null;
        long latestTs = -1L;
        Set<String> ai = new LinkedHashSet<>();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            int type = e.getEventType();
            boolean foreground = type == UsageEvents.Event.MOVE_TO_FOREGROUND;
            if (Build.VERSION.SDK_INT >= 29) foreground = foreground || type == UsageEvents.Event.ACTIVITY_RESUMED;
            if (foreground) {
                String pkg = e.getPackageName();
                if (e.getTimeStamp() >= latestTs) {
                    latestTs = e.getTimeStamp();
                    latest = pkg;
                }
                if (isAiPackage(pkg)) ai.add(pkg);
            }
        }
        return new UsageSnapshot(latest, new ArrayList<>(ai));
    }

    private boolean isAiPackage(String pkg) {
        if (pkg == null) return false;
        String p = pkg.toLowerCase(Locale.US);
        return p.contains("openai") || p.contains("chatgpt") || p.contains("anthropic") ||
                p.contains("claude") || p.contains("bard") || p.contains("gemini") ||
                p.contains("copilot") || p.contains("perplexity") || p.contains("deepseek") ||
                p.contains("grok") || p.contains("poe") || p.contains("character.ai");
    }

    private String thermalLabel() {
        if (Build.VERSION.SDK_INT < 29) return "API<29";
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        switch (pm.getCurrentThermalStatus()) {
            case PowerManager.THERMAL_STATUS_NONE: return "NONE";
            case PowerManager.THERMAL_STATUS_LIGHT: return "LIGHT";
            case PowerManager.THERMAL_STATUS_MODERATE: return "MODERATE";
            case PowerManager.THERMAL_STATUS_SEVERE: return "SEVERE";
            case PowerManager.THERMAL_STATUS_CRITICAL: return "CRITICAL";
            case PowerManager.THERMAL_STATUS_EMERGENCY: return "EMERGENCY";
            case PowerManager.THERMAL_STATUS_SHUTDOWN: return "SHUTDOWN";
            default: return "UNKNOWN";
        }
    }

    private long safeTraffic(long v) { return v < 0 ? 0L : v; }

    private String rate(long current, long previous) {
        long delta = Math.max(0L, current - previous);
        if (delta >= 1024L * 1024L) return String.format(Locale.US, "%.2f MB/s", delta / 1048576.0);
        if (delta >= 1024L) return String.format(Locale.US, "%.1f KB/s", delta / 1024.0);
        return delta + " B/s";
    }

    private String join(List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append("• ").append(values.get(i));
        }
        return sb.toString();
    }

    private void log(String type, String detail, int color) {
        if (terminal == null) return;
        TextView line = text(clock.format(new Date()) + "  > " + type + "\n" + detail, 10, color, false, true);
        terminal.addView(line, 0);
        while (terminal.getChildCount() > 18) terminal.removeViewAt(terminal.getChildCount() - 1);
    }

    private TextView text(String s, int sp, int color, boolean bold, boolean mono) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(10, 7, 10, 7);
        if (mono) v.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        else if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private TextView value(String s) {
        TextView v = text(s, 17, GREEN, true, true);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private LinearLayout panel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(12, 12, 12, 12);
        p.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 8, 0, 8);
        p.setLayoutParams(lp);
        return p;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private LinearLayout metric(String label, TextView value, int color) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(5, 9, 5, 9);
        box.setBackgroundColor(GRID);
        TextView l = text(label, 9, MUTED, true, true);
        l.setGravity(Gravity.CENTER);
        value.setTextColor(color);
        box.addView(l);
        box.addView(value);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.setMargins(2, 3, 2, 3);
        box.setLayoutParams(lp);
        return box;
    }

    private static class UsageSnapshot {
        final String latestPackage;
        final List<String> aiPackages;
        UsageSnapshot(String latestPackage, List<String> aiPackages) {
            this.latestPackage = latestPackage;
            this.aiPackages = aiPackages;
        }
    }
}
