package ai.cerberus.demo;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements Choreographer.FrameCallback {
    private static final int BG = Color.rgb(5, 9, 13);
    private static final int SURFACE = Color.rgb(11, 18, 24);
    private static final int SURFACE_2 = Color.rgb(14, 25, 33);
    private static final int BORDER = Color.rgb(29, 53, 66);
    private static final int TEXT = Color.rgb(241, 247, 245);
    private static final int MUTED = Color.rgb(139, 160, 166);
    private static final int GREEN = Color.rgb(57, 245, 166);
    private static final int CYAN = Color.rgb(95, 219, 255);
    private static final int ORANGE = Color.rgb(255, 182, 87);
    private static final int RED = Color.rgb(255, 93, 108);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private TextView cpuValue;
    private TextView heapValue;
    private TextView rxValue;
    private TextView txValue;
    private TextView batteryValue;
    private TextView tempValue;
    private TextView thermalValue;
    private TextView fpsValue;
    private TextView foregroundValue;
    private TextView aiAppsValue;
    private TextView gpuValue;
    private TextView permissionValue;
    private TextView readinessValue;
    private TextView signatureValue;
    private TextView fingerprintValue;
    private TextView installerValue;
    private TextView heroStatus;
    private LinearLayout terminal;

    private long lastWallMs;
    private long lastCpuMs;
    private long lastRx;
    private long lastTx;
    private int frameCounter;
    private int lastFps;
    private long lastFpsTick;
    private int terminalSampleCounter;
    private boolean signaturePresent;
    private String fullFingerprint = "UNAVAILABLE";
    private String installerSource = "UNKNOWN";

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
        loadInstallIntegrity();
        lastWallMs = System.currentTimeMillis();
        lastCpuMs = Process.getElapsedCpuTime();
        lastRx = safeTraffic(TrafficStats.getUidRxBytes(Process.myUid()));
        lastTx = safeTraffic(TrafficStats.getUidTxBytes(Process.myUid()));
        lastFpsTick = System.currentTimeMillis();
        log("BOOT", "Cerberus Sentinel v0.3 product shell online", GREEN);
        log("INTEGRITY", "APK signature SHA-256: " + fullFingerprint, CYAN);
        log("SOURCE", "Installer: " + installerSource, CYAN);
        log("GPU", "System-wide GPU percentage unavailable on stock Android API", ORANGE);
    }

    @Override protected void onResume() {
        super.onResume();
        loadInstallIntegrity();
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
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(32));
        root.setBackgroundColor(BG);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);

        SentinelMark mark = new SentinelMark(this);
        brand.addView(mark, new LinearLayout.LayoutParams(dp(54), dp(54)));

        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams brandTextLp = new LinearLayout.LayoutParams(0, -2, 1f);
        brandTextLp.setMargins(dp(12), 0, 0, 0);
        brand.addView(brandText, brandTextLp);
        brandText.addView(label("CERBERUS", 22, TEXT, true));
        brandText.addView(label("AI ENDPOINT SENTINEL  ·  v0.3", 10, MUTED, true));

        TextView localChip = chip("LOCAL • PRIVATE", GREEN, Color.rgb(8, 39, 30));
        brand.addView(localChip);
        root.addView(brand);

        addSpace(root, 18);

        LinearLayout hero = cardGradient(Color.rgb(10, 29, 31), Color.rgb(12, 22, 32), GREEN);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.addView(label("DEVICE SECURITY POSTURE", 10, GREEN, true));

        LinearLayout heroRow = new LinearLayout(this);
        heroRow.setOrientation(LinearLayout.HORIZONTAL);
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        hero.addView(heroRow);

        LinearLayout heroCopy = new LinearLayout(this);
        heroCopy.setOrientation(LinearLayout.VERTICAL);
        heroRow.addView(heroCopy, new LinearLayout.LayoutParams(0, -2, 1f));

        heroStatus = label("MONITORING ACTIVE", 24, TEXT, true);
        heroCopy.addView(heroStatus);
        TextView heroSub = label("Real local telemetry. No cloud account. No fabricated signals.", 12, MUTED, false);
        heroSub.setPadding(0, dp(4), dp(10), 0);
        heroCopy.addView(heroSub);

        readinessValue = label("2/3\nREADY", 18, GREEN, true);
        readinessValue.setGravity(Gravity.CENTER);
        readinessValue.setBackground(roundRect(Color.rgb(9, 41, 31), GREEN, 18, 1));
        readinessValue.setPadding(dp(14), dp(10), dp(14), dp(10));
        heroRow.addView(readinessValue);

        LinearLayout heroChips = new LinearLayout(this);
        heroChips.setOrientation(LinearLayout.HORIZONTAL);
        heroChips.setPadding(0, dp(14), 0, 0);
        hero.addView(heroChips);
        heroChips.addView(chip("SIGNED APK", CYAN, Color.rgb(11, 34, 43)));
        TextView zeroRoot = chip("ZERO ROOT", TEXT, Color.rgb(22, 30, 38));
        LinearLayout.LayoutParams chipMargin = new LinearLayout.LayoutParams(-2, -2);
        chipMargin.setMargins(dp(8), 0, 0, 0);
        heroChips.addView(zeroRoot, chipMargin);
        TextView offline = chip("OFFLINE CORE", TEXT, Color.rgb(22, 30, 38));
        LinearLayout.LayoutParams chipMargin2 = new LinearLayout.LayoutParams(-2, -2);
        chipMargin2.setMargins(dp(8), 0, 0, 0);
        heroChips.addView(offline, chipMargin2);
        root.addView(hero);

        addSectionTitle(root, "LIVE TELEMETRY", "RAW DEVICE + APP SIGNALS");

        cpuValue = metricValue("--");
        heapValue = metricValue("--");
        addMetricPair(root,
                metricCard("APP CPU", cpuValue, "core-normalized", GREEN),
                metricCard("JAVA HEAP", heapValue, "live process memory", CYAN));

        rxValue = metricValue("--");
        txValue = metricValue("--");
        addMetricPair(root,
                metricCard("RX RATE", rxValue, "Cerberus UID", CYAN),
                metricCard("TX RATE", txValue, "Cerberus UID", CYAN));

        batteryValue = metricValue("--");
        tempValue = metricValue("--");
        addMetricPair(root,
                metricCard("BATTERY", batteryValue, "device state", GREEN),
                metricCard("BATTERY TEMP", tempValue, "raw Android sensor", ORANGE));

        fpsValue = metricValue("--");
        gpuValue = metricValue("N/A");
        addMetricPair(root,
                metricCard("RENDER FPS", fpsValue, "Cerberus UI", GREEN),
                metricCard("GPU LOAD", gpuValue, "API unavailable", ORANGE));

        addSectionTitle(root, "PROTECTION SIGNALS", "INSTALL + RUNTIME INTEGRITY");
        LinearLayout integrity = card(SURFACE, BORDER);
        integrity.addView(cardHeading("APP INTEGRITY"));

        signatureValue = statusRow(integrity, "APK SIGNATURE", "CHECKING", MUTED);
        fingerprintValue = statusRow(integrity, "CERT SHA-256", "--", MUTED);
        installerValue = statusRow(integrity, "INSTALL SOURCE", "--", MUTED);
        thermalValue = statusRow(integrity, "THERMAL STATE", "--", MUTED);
        permissionValue = statusRow(integrity, "USAGE ACCESS", "--", MUTED);

        Button usageButton = new Button(this);
        usageButton.setText("ENABLE ACTIVITY VISIBILITY");
        usageButton.setTextColor(Color.rgb(3, 18, 13));
        usageButton.setTextSize(12);
        usageButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        usageButton.setAllCaps(false);
        usageButton.setBackground(roundRect(GREEN, GREEN, 14, 0));
        usageButton.setPadding(dp(12), dp(6), dp(12), dp(6));
        usageButton.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(-1, dp(48));
        buttonLp.setMargins(0, dp(14), 0, 0);
        integrity.addView(usageButton, buttonLp);
        root.addView(integrity);

        addSectionTitle(root, "AI ACTIVITY", "VISIBLE FOREGROUND SIGNALS");
        LinearLayout activity = card(SURFACE, BORDER);
        activity.addView(cardHeading("RECENT APP ACTIVITY"));
        activity.addView(label("LAST FOREGROUND PACKAGE", 9, MUTED, true));
        foregroundValue = label("Permission required", 14, TEXT, true);
        foregroundValue.setPadding(0, dp(4), 0, dp(14));
        activity.addView(foregroundValue);
        activity.addView(divider());
        activity.addView(label("KNOWN AI APPS SEEN · LAST 10 MIN", 9, MUTED, true));
        aiAppsValue = label("Permission required", 14, CYAN, true);
        aiAppsValue.setPadding(0, dp(6), 0, 0);
        activity.addView(aiAppsValue);
        root.addView(activity);

        addSectionTitle(root, "FORENSICS", "RAW SIGNAL STREAM");
        LinearLayout terminalPanel = card(Color.rgb(4, 12, 15), Color.rgb(20, 74, 64));
        LinearLayout terminalHeader = new LinearLayout(this);
        terminalHeader.setOrientation(LinearLayout.HORIZONTAL);
        terminalHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView terminalTitle = label("CERBERUS // EVENT STREAM", 10, GREEN, true);
        terminalHeader.addView(terminalTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        terminalHeader.addView(chip("LIVE", GREEN, Color.rgb(7, 37, 28)));
        terminalPanel.addView(terminalHeader);
        terminal = new LinearLayout(this);
        terminal.setOrientation(LinearLayout.VERTICAL);
        terminal.setPadding(0, dp(8), 0, 0);
        terminalPanel.addView(terminal);
        root.addView(terminalPanel);

        TextView foot = label("Cerberus reports only signals Android exposes to an ordinary rootless app. Missing GPU telemetry remains explicitly unavailable.", 10, MUTED, false);
        foot.setPadding(dp(4), dp(18), dp(4), 0);
        root.addView(foot);
    }

    private void sampleSignals() {
        long now = System.currentTimeMillis();
        long cpuNow = Process.getElapsedCpuTime();
        long wallDelta = Math.max(1L, now - lastWallMs);
        long cpuDelta = Math.max(0L, cpuNow - lastCpuMs);
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        double cpuPct = (100.0 * cpuDelta / wallDelta) / cores;
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
        String rxRate = rate(rx, lastRx);
        String txRate = rate(tx, lastTx);
        rxValue.setText(rxRate);
        txValue.setText(txRate);
        lastRx = rx;
        lastTx = tx;

        Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batt != null) {
            int level = batt.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batt.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            int temp = batt.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            int pct = scale > 0 && level >= 0 ? Math.round(level * 100f / scale) : -1;
            batteryValue.setText(pct >= 0 ? pct + "%" : "N/A");
            tempValue.setText(String.format(Locale.US, "%.1f°C", temp / 10.0));
        }

        String thermal = thermalLabel();
        thermalValue.setText(thermal);
        thermalValue.setTextColor(thermalSeverityColor(thermal));

        boolean usageGranted = hasUsageAccess();
        permissionValue.setText(usageGranted ? "GRANTED" : "REQUIRED");
        permissionValue.setTextColor(usageGranted ? GREEN : ORANGE);

        updateReadiness(usageGranted);

        if (usageGranted) {
            UsageSnapshot snap = usageSnapshot();
            foregroundValue.setText(snap.latestPackage == null ? "No foreground event in window" : snap.latestPackage);
            aiAppsValue.setText(snap.aiPackages.isEmpty() ? "None detected" : join(snap.aiPackages));
        } else {
            foregroundValue.setText("Enable Usage Access to observe app activity");
            aiAppsValue.setText("Permission required");
        }

        terminalSampleCounter++;
        if (terminalSampleCounter % 5 == 0) {
            log("SAMPLE", String.format(Locale.US,
                    "cpu=%.1f%% · heap=%dMB · rx=%s · tx=%s · fps=%d · thermal=%s",
                    cpuPct, heapMb, rxRate, txRate, lastFps, thermal), GREEN);
        }
    }

    private void loadInstallIntegrity() {
        fullFingerprint = signatureFingerprint();
        signaturePresent = !"UNAVAILABLE".equals(fullFingerprint);
        installerSource = installSource();

        if (signatureValue != null) {
            signatureValue.setText(signaturePresent ? "PRESENT" : "UNKNOWN");
            signatureValue.setTextColor(signaturePresent ? GREEN : ORANGE);
        }
        if (fingerprintValue != null) {
            fingerprintValue.setText(signaturePresent ? shortFingerprint(fullFingerprint) : "UNAVAILABLE");
            fingerprintValue.setTextColor(CYAN);
        }
        if (installerValue != null) {
            installerValue.setText(installerSource);
            installerValue.setTextColor(CYAN);
        }
        if (readinessValue != null) updateReadiness(hasUsageAccess());
    }

    private void updateReadiness(boolean usageGranted) {
        int ready = 1;
        if (signaturePresent) ready++;
        if (usageGranted) ready++;
        readinessValue.setText(ready + "/3\nREADY");
        readinessValue.setTextColor(ready == 3 ? GREEN : ORANGE);
        heroStatus.setText(usageGranted ? "MONITORING ACTIVE" : "LIMITED VISIBILITY");
        heroStatus.setTextColor(usageGranted ? TEXT : ORANGE);
    }

    private String signatureFingerprint() {
        try {
            PackageManager pm = getPackageManager();
            Signature[] signatures;
            if (Build.VERSION.SDK_INT >= 28) {
                PackageInfo info = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                if (info.signingInfo == null) return "UNAVAILABLE";
                signatures = info.signingInfo.hasMultipleSigners()
                        ? info.signingInfo.getApkContentsSigners()
                        : info.signingInfo.getSigningCertificateHistory();
            } else {
                PackageInfo info = pm.getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = info.signatures;
            }
            if (signatures == null || signatures.length == 0) return "UNAVAILABLE";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signatures[0].toByteArray());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hash.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format(Locale.US, "%02X", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNAVAILABLE";
        }
    }

    private String installSource() {
        try {
            PackageManager pm = getPackageManager();
            String installer;
            if (Build.VERSION.SDK_INT >= 30) {
                installer = pm.getInstallSourceInfo(getPackageName()).getInstallingPackageName();
            } else {
                installer = pm.getInstallerPackageName(getPackageName());
            }
            return installer == null ? "SIDELOAD / UNKNOWN" : installer;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String shortFingerprint(String value) {
        if (value == null || value.length() <= 29) return value;
        return value.substring(0, 29) + "…";
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
                p.contains("grok") || p.contains("xai") || p.contains("poe") ||
                p.contains("character.ai");
    }

    private String thermalLabel() {
        if (Build.VERSION.SDK_INT < 29) return "API < 29";
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

    private int thermalSeverityColor(String thermal) {
        if ("CRITICAL".equals(thermal) || "EMERGENCY".equals(thermal) || "SHUTDOWN".equals(thermal)) return RED;
        if ("SEVERE".equals(thermal) || "MODERATE".equals(thermal)) return ORANGE;
        return GREEN;
    }

    private long safeTraffic(long value) {
        return value < 0 ? 0L : value;
    }

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
        TextView line = label(clock.format(new Date()) + "   " + type + "\n" + detail, 10, color, false);
        line.setTypeface(Typeface.MONOSPACE);
        line.setPadding(0, dp(5), 0, dp(5));
        terminal.addView(line, 0);
        while (terminal.getChildCount() > 14) terminal.removeViewAt(terminal.getChildCount() - 1);
    }

    private void addSectionTitle(LinearLayout root, String title, String subtitle) {
        addSpace(root, 22);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.BOTTOM);
        TextView titleView = label(title, 12, TEXT, true);
        line.addView(titleView, new LinearLayout.LayoutParams(0, -2, 1f));
        line.addView(label(subtitle, 9, MUTED, true));
        root.addView(line);
        addSpace(root, 8);
    }

    private LinearLayout card(int color, int stroke) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(16), dp(16), dp(16));
        box.setBackground(roundRect(color, stroke, 20, 1));
        return box;
    }

    private LinearLayout cardGradient(int start, int end, int stroke) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1), stroke);
        box.setBackground(bg);
        return box;
    }

    private LinearLayout metricCard(String title, TextView value, String note, int accent) {
        LinearLayout box = card(SURFACE_2, BORDER);
        box.addView(label(title, 9, MUTED, true));
        value.setTextColor(accent);
        value.setPadding(0, dp(7), 0, dp(3));
        box.addView(value);
        box.addView(label(note, 9, MUTED, false));
        return box;
    }

    private void addMetricPair(LinearLayout root, LinearLayout left, LinearLayout right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, -2, 1f);
        leftLp.setMargins(0, 0, dp(5), dp(10));
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, -2, 1f);
        rightLp.setMargins(dp(5), 0, 0, dp(10));
        row.addView(left, leftLp);
        row.addView(right, rightLp);
        root.addView(row);
    }

    private TextView metricValue(String value) {
        return label(value, 21, GREEN, true);
    }

    private TextView cardHeading(String value) {
        TextView heading = label(value, 13, TEXT, true);
        heading.setPadding(0, 0, 0, dp(12));
        return heading;
    }

    private TextView statusRow(LinearLayout parent, String name, String initial, int color) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView key = label(name, 10, MUTED, true);
        TextView val = label(initial, 11, color, true);
        val.setGravity(Gravity.RIGHT);
        row.addView(key, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(val, new LinearLayout.LayoutParams(0, -2, 1.25f));
        parent.addView(row);
        parent.addView(divider());
        return val;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(Color.rgb(24, 40, 49));
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        return v;
    }

    private TextView label(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setIncludeFontPadding(false);
        v.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return v;
    }

    private TextView chip(String value, int color, int fill) {
        TextView v = label(value, 9, color, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(9), dp(6), dp(9), dp(6));
        v.setBackground(roundRect(fill, color, 99, 1));
        return v;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radiusDp, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private void addSpace(LinearLayout root, int heightDp) {
        View space = new View(this);
        root.addView(space, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class UsageSnapshot {
        final String latestPackage;
        final List<String> aiPackages;
        UsageSnapshot(String latestPackage, List<String> aiPackages) {
            this.latestPackage = latestPackage;
            this.aiPackages = aiPackages;
        }
    }

    private static class SentinelMark extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path hex = new Path();

        SentinelMark(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setContentDescription("Cerberus Sentinel mark");
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) * 0.39f;

            hex.reset();
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(-90 + i * 60);
                float x = cx + (float) Math.cos(a) * r;
                float y = cy + (float) Math.sin(a) * r;
                if (i == 0) hex.moveTo(x, y); else hex.lineTo(x, y);
            }
            hex.close();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(8, 33, 29));
            paint.setShadowLayer(18f, 0f, 0f, Color.argb(110, 57, 245, 166));
            canvas.drawPath(hex, paint);

            paint.clearShadowLayer();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, w * 0.035f));
            paint.setColor(GREEN);
            canvas.drawPath(hex, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(TEXT);
            paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(w * 0.39f);
            Paint.FontMetrics fm = paint.getFontMetrics();
            float baseline = cy - (fm.ascent + fm.descent) / 2f;
            canvas.drawText("C", cx, baseline, paint);

            paint.setColor(CYAN);
            float eyeR = Math.max(1.8f, w * 0.035f);
            canvas.drawCircle(cx - w * 0.17f, cy - h * 0.18f, eyeR, paint);
            canvas.drawCircle(cx + w * 0.17f, cy - h * 0.18f, eyeR, paint);
            canvas.drawCircle(cx, cy + h * 0.23f, eyeR, paint);
        }
    }
}
