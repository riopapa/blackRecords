package com.urrecliner.blackrecords;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Timer;
import java.util.TimerTask;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context c = context;
//        if (utils == null)
//            utils = new Utils();
        String model = Build.MODEL;
//        utils.log("Booted", "Model:"+ model);
//            ;           // SM-G965N             Nexus 6P
//            phoneMake = Build.MANUFACTURER;     // samsung              Huawei
        if (model.equals("Nexus 6P")) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    Intent i = new Intent(c, MainActivity.class);
                    c.startActivity(i);
                }
            }, 5000);
        }
    }
}
