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

public class KillAppService extends AccessibilityService {
    private WindowManager windowManager;
    private View overlayView;
    private boolean isOverlayShown = false;
    private String lastForegroundPackage = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable;

    private static final String[] PROTECTED = {
        "com.android.systemui","com.android.launcher","com.itel.launcher",
        "com.transsion.XOSLauncher","com.transsion.hilauncher",
        "com.carl.killappassist","com.topjohnwu.magisk"
    };

    private static final String[] RECENTS_PKGS = {
        "com.transsion.XOSLauncher",
        "com.android.systemui",
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
        String pkg = event.getPackageName() != null ? event.getPackageName().toString() : "";
        int type = event.getEventType();

        if (!pkg.isEmpty() && !isProtected(pkg)) {
            lastForegroundPackage = pkg;
        }

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (isRecentsPkg(pkg)) {
                if (hideRunnable != null) handler.removeCallbacks(hideRunnable);
                showKillButton();
            } else {
                scheduleHide(1500);
            }
        }
    }

    private boolean isRecentsPkg(String pkg) {
        for (String r : RECENTS_PKGS) if (pkg.equals(r)) return true;
        return false;
    }

    private boolean isProtected(String pkg) {
        for (String p : PROTECTED) if (pkg.startsWith(p)) return true;
        return false;
    }

    private void scheduleHide(long delay) {
        if (hideRunnable != null) handler.removeCallbacks(hideRunnable);
        hideRunnable = this::hideKillButton;
        handler.postDelayed(hideRunnable, delay);
    }

    private void showKillButton() {
        if (isOverlayShown) return;
        handler.post(() -> {
            try {
                overlayView = buildButton();
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.y = 240;
                windowManager.addView(overlayView, params);
                isOverlayShown = true;
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private View buildButton() {
        TextView btn = new TextView(this);
        btn.setText("⛔  Kill App");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(15f);
        btn.setPadding(56, 28, 56, 28);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#CC2222"));
        bg.setCornerRadius(60f);
        bg.setStroke(2, Color.parseColor("#FF4444"));
        btn.setBackground(bg);
        btn.setOnClickListener(v -> {
            if (hideRunnable != null) handler.removeCallbacks(hideRunnable);
            doKill();
            hideKillButton();
            performGlobalAction(GLOBAL_ACTION_BACK);
        });
        btn.setOnLongClickListener(v -> {
            if (hideRunnable != null) handler.removeCallbacks(hideRunnable);
            hideKillButton();
            showToast("Cancelled");
            return true;
        });
        return btn;
    }

    private void doKill() {
        final String target = lastForegroundPackage;
        if (target == null || target.isEmpty()) { showToast("No target app found"); return; }
        if (isProtected(target)) { showToast("Cannot kill system app"); return; }
        try { ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
              if (am != null) am.killBackgroundProcesses(target); } catch (Exception e) {}
        try { Runtime.getRuntime().exec(new String[]{"sh","-c","am force-stop "+target}); } catch (Exception e) {}
        try { Runtime.getRuntime().exec(new String[]{"su","-c","am force-stop "+target}); } catch (Exception e) {}
        String name = target.contains(".") ? target.substring(target.lastIndexOf('.')+1) : target;
        showToast("Killed: " + name);
    }

    private void hideKillButton() {
        if (!isOverlayShown || overlayView == null) return;
        handler.post(() -> {
            try { windowManager.removeView(overlayView); } catch (Exception ignored) {}
            overlayView = null; isOverlayShown = false;
        });
    }

    private void showToast(String msg) {
        handler.post(() -> Toast.makeText(KillAppService.this, msg, Toast.LENGTH_SHORT).show());
    }

    @Override public void onInterrupt() { hideKillButton(); }
    @Override public void onDestroy() { hideKillButton(); super.onDestroy(); }
}
