package com.urrecliner.blackrecords;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import static com.urrecliner.blackrecords.Vars.DELAY_RESTART;
import static com.urrecliner.blackrecords.Vars.isRecordingNow;
import static com.urrecliner.blackrecords.Vars.mContext;
import static com.urrecliner.blackrecords.Vars.utils;

class CountDownAsync extends AsyncTask<String, String, String> {

    private String jumpTo, title;
    private int downCount;
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... s) {
        jumpTo = s[0];
        title = s[1];
        downCount = Integer.parseInt(s[2]);
        while (downCount > 0) {
            SystemClock.sleep(1000);
            publishProgress("" + downCount);
            downCount--;
        }
        return "done";
    }
    protected void onProgressUpdate(String... s) {
        utils.displayCount(title+"\n"+downCount,Toast.LENGTH_SHORT, Color.DKGRAY);
    }

    @Override
    protected void onPostExecute(String m) {
        if (jumpTo.equals("v")) {
            if (!isRecordingNow) {
                VideoFragment.getInstance().autoStart();
            }
        }
        else if(jumpTo.equals("x")) {
            Handler mHandler = new Handler(Looper.getMainLooper());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    utils.beepOnce(5,0.5f);
                    utils.displayCount("I will be back in "+DELAY_RESTART+" secs.",Toast.LENGTH_LONG, Color.BLACK);
                    utils.beepOnce(5,0.5f);
                }
            }, 0);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent restartIntent = mContext.getPackageManager()     // exit and reload app
                            .getLaunchIntentForPackage(mContext.getPackageName() );
                    PendingIntent intent = PendingIntent.getActivity(
                            mContext, 0,
                            restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager manager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                    manager.set(AlarmManager.RTC, System.currentTimeMillis() + DELAY_RESTART*1000, intent);
                    System.exit(2);
                }
            }, 1000);

        }
        else
            Log.e("jumpTo","Code Error : "+jumpTo);
    }
}
