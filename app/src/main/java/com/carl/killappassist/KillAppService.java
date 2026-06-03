package com.carl.killappassist;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class KillAppService extends AccessibilityService {

    private WindowManager windowManager;
    private View overlayView;
    private boolean isOverlayShown = false;
    private String lastForegroundPackage = "";

    // Packages that must never be killed
    private static final String[] PROTECTED = {
        "com.android.systemui",
        "com.android.launcher",
        "com.itel.launcher",
        "com.transsion.hilauncher",
        "com.carl.killappassist",
        "com.topjohnwu.magisk"
    };

    // Known recents/overview packages on XOS / AOSP
    private static final String[] RECENTS_PACKAGES = {
        "com.android.systemui",
        "com.itel.systemui",
        "com.android.launcher3",
        "com.transsion.hilauncher"
    };

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        String pkg = event.getPackageName() != null
                ? event.getPackageName().toString() : "";

        int type = event.getEventType();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (isRecentsPackage(pkg)) {
                // Capture foreground app just before recents opened
                String fg = getForegroundApp();
                if (fg != null && !fg.isEmpty() && !isProtected(fg)) {
                    lastForegroundPackage = fg;
                }
                showKillButton();
            } else {
                hideKillButton();
                // Track last real foreground app
                if (!pkg.isEmpty() && !isRecentsPackage(pkg) && !isProtected(pkg)) {
                    lastForegroundPackage = pkg;
                }
            }
        }
    }

    private boolean isRecentsPackage(String pkg) {
        for (String r : RECENTS_PACKAGES) {
            if (pkg.equals(r)) return true;
        }
        return false;
    }

    private boolean isProtected(String pkg) {
        for (String p : PROTECTED) {
            if (pkg.startsWith(p)) return true;
        }
        return false;
    }

    private String getForegroundApp() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return null;

            List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
            if (procs == null) return null;

            for (ActivityManager.RunningAppProcessInfo p : procs) {
                if (p.importance ==
                        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    if (!isProtected(p.processName)) {
                        return p.processName;
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    // ─── Overlay UI ────────────────────────────────────────────────────────────

    private void showKillButton() {
        if (isOverlayShown) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                overlayView = buildButton();

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
                );

                // Bottom-center, sits above nav bar
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.y = 240;

                windowManager.addView(overlayView, params);
                isOverlayShown = true;

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private View buildButton() {
        TextView btn = new TextView(this);
        btn.setText("⛔  Kill App");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(15f);
        btn.setPadding(56, 28, 56, 28);
        btn.setGravity(Gravity.CENTER);

        // Red pill shape
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor("#CC2222"));
        bg.setCornerRadius(60f);
        bg.setStroke(2, Color.parseColor("#FF4444"));
        btn.setBackground(bg);

        // Tap → kill
        btn.setOnClickListener(v -> {
            doKill();
            hideKillButton();
            performGlobalAction(GLOBAL_ACTION_BACK);
        });

        // Long press → cancel / dismiss
        btn.setOnLongClickListener(v -> {
            hideKillButton();
            showToast("Cancelled");
            return true;
        });

        return btn;
    }

    private void doKill() {
        final String target = lastForegroundPackage;

        if (target == null || target.isEmpty()) {
            showToast("No target app found");
            return;
        }

        if (isProtected(target)) {
            showToast("Cannot kill system app");
            return;
        }

        boolean killed = false;

        // Method 1: killBackgroundProcesses (no root needed)
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(target);
                killed = true;
            }
        } catch (Exception e) {
            // fallthrough
        }

        // Method 2: am force-stop (works best with root/system privileges)
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", "am force-stop " + target});
            killed = true;
        } catch (Exception e) {
            // fallthrough
        }

        // Method 3: su force-stop (if Magisk root available)
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + target});
            killed = true;
        } catch (Exception e) {
            // ignore
        }

        if (killed) {
            // Friendly short name
            String appName = target.contains(".")
                ? target.substring(target.lastIndexOf('.') + 1) : target;
            showToast("Killed: " + appName);
        } else {
            showToast("Kill failed — try enabling root");
        }
    }

    private void hideKillButton() {
        if (!isOverlayShown || overlayView == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {}
            overlayView = null;
            isOverlayShown = false;
        });
    }

    private void showToast(String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(KillAppService.this, msg, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onInterrupt() {
        hideKillButton();
    }

    @Override
    public void onDestroy() {
        hideKillButton();
        super.onDestroy();
    }
}
