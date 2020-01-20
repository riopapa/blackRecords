package com.urrecliner.blackrecords;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

public class PermissionProvider {

    public static boolean isNotReady(Context context, Activity activity, String Permission) {
        if (ContextCompat.checkSelfPermission(context, Permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Permission}, 123);
            if (ContextCompat.checkSelfPermission(context, Permission)
                    == PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {
                Toast.makeText(context,Permission + " is not granted..", Toast.LENGTH_LONG).show();
                return true;
            }
        }
        else return false;
    }
}
