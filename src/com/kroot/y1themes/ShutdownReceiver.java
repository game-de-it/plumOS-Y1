package com.kroot.y1themes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class ShutdownReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent ignored) {
        Intent shutdown = new Intent("android.intent.action.ACTION_ALARM_REQUEST_SHUTDOWN");
        shutdown.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { context.startActivity(shutdown); }
        catch (SecurityException error) {
            android.util.Log.e("Y1Shutdown", "system permission required", error);
        }
    }
}
