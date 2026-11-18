package com.kroot.y1themes;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/** Reapply the standalone frontend policy after boot and app replacement. */
public final class DeviceSetupReceiver extends BroadcastReceiver {
    static void apply(Context context) {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 0) return;
        PackageManager pm = context.getPackageManager();
        try {
            if (pm.getApplicationEnabledSetting("com.innioasis.y1") != PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
                pm.setApplicationEnabledSetting("com.innioasis.y1",
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            ComponentName usb = new ComponentName("com.android.systemui",
                    "com.android.systemui.usb.UsbStorageActivity");
            if (pm.getComponentEnabledSetting(usb) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
                pm.setComponentEnabledSetting(usb, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            if (!android.os.Environment.MEDIA_SHARED.equals(android.os.Environment.getExternalStorageState()))
                setUsbFunction(context, "adb", true);
            android.util.Log.i("Y1Setup", "stock FE disabled; USB storage activity enabled; idle USB is adb");
        } catch (Exception error) {
            android.util.Log.e("Y1Setup", "cannot apply standalone policy", error);
        }
    }
    static void setUsbFunction(Context context, String function, boolean makeDefault) throws Exception {
        Object usb = context.getSystemService("usb");
        usb.getClass().getMethod("setCurrentFunction", String.class, boolean.class)
                .invoke(usb, function, makeDefault);
    }
    @Override public void onReceive(Context context, Intent intent) {
        apply(context);
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) &&
                context.getSharedPreferences("MainActivity", 0).getBoolean("apkUpdatePending", false)) {
            context.startActivity(new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        }
    }
}
