package com.carl.killappassist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class PermissionActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Settings.canDrawOverlays(this)) {
            // Ask for overlay permission
            Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ);
        } else {
            onPermissionReady();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ) {
            if (Settings.canDrawOverlays(this)) {
                onPermissionReady();
            } else {
                Toast.makeText(this,
                    "Overlay permission needed for Kill button to show!",
                    Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void onPermissionReady() {
        Toast.makeText(this,
            "KillApp Overlay ready!\nEnable the Accessibility Service to activate.",
            Toast.LENGTH_LONG).show();

        // Open Accessibility settings for easy setup
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        finish();
    }
}
