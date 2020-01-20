package com.urrecliner.blackrecords;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackrecords.Vars.isRecordingNow;
import static com.urrecliner.blackrecords.Vars.utils;

public class PowerConnectionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        utils.log("C","Power changed "+action);
        IntentFilter intFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intFilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
//            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
//            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
//            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        if (!isRecordingNow && !isCharging) {
            utils.customToast("UnPlugged! App Exit", Toast.LENGTH_LONG, Color.CYAN);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    System.exit(0);
                }
            }, 3000);
        }
    }
}
