/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urrecliner.blackrecords;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.urrecliner.blackrecords.NewAppWidget.updateHomeButton;
import static com.urrecliner.blackrecords.Vars.DELAY_WAIT_EXIT;
import static com.urrecliner.blackrecords.Vars.isRecordingNow;
import static com.urrecliner.blackrecords.Vars.mActivity;
import static com.urrecliner.blackrecords.Vars.mContext;
import static com.urrecliner.blackrecords.Vars.mCurrentLocation;
import static com.urrecliner.blackrecords.Vars.mExitApplication;
import static com.urrecliner.blackrecords.Vars.mGoogleApiClient;
import static com.urrecliner.blackrecords.Vars.mIntervalEvent;
import static com.urrecliner.blackrecords.Vars.mLatitude;
import static com.urrecliner.blackrecords.Vars.mLongitude;
import static com.urrecliner.blackrecords.Vars.utils;
import static com.urrecliner.blackrecords.Vars.vImgBattery;
import static com.urrecliner.blackrecords.Vars.vTextBattery;
import static com.urrecliner.blackrecords.Vars.vTextDate;
import static com.urrecliner.blackrecords.Vars.vTextGPSValue;
import static com.urrecliner.blackrecords.Vars.vTextSpeed;
import static com.urrecliner.blackrecords.Vars.vTextTime;
import static com.urrecliner.blackrecords.Vars.videoFragment;
import static com.urrecliner.blackrecords.Vars.videoUtils;

//public class MainActivity extends Activity implements SensorEventListener {
public class MainActivity extends Activity {

    private long backPressedTime = 0;
    private String logId = "Main";
    private static ImageView vIconCompass = null;
    private static ImageView vIconArrow = null;
    private static long prevTime = 0;

//     * Code used in requesting runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
//     * Constant used in the location settings dialog.
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
//     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long GPS_UPDATE_INTERVAL = 5000;
    private static final long GPS_FASTEST_UPDATE_INTERVAL = GPS_UPDATE_INTERVAL / 2;

    // Keys for storing activity state in the Bundle.
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";

//     * Provides access to the Fused Location Provider API.
    private static FusedLocationProviderClient mFusedLocationClient;

//     * Provides access to the Location Settings API.
    private static SettingsClient mSettingsClient;
//     * Stores parameters for requests to the FusedLocationProviderApi.
    private static LocationRequest mLocationRequest;
    private static LocationSettingsRequest mLocationSettingsRequest;
    private static LocationCallback mLocationCallback;
    private static Boolean mRequestingLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mActivity = this;
        mContext = this;

        if (videoFragment == null) {
            videoUtils = new VideoUtils();
            videoFragment = new VideoFragment();
        }

        Log.w("onCreate","--");
        getFragmentManager().beginTransaction()
                .replace(R.id.container, VideoFragment.newInstance(), "Frag")
                .commit();

        if (PermissionProvider.isNotReady(getApplicationContext(), this,
                Manifest.permission.ACCESS_FINE_LOCATION) ||
                PermissionProvider.isNotReady(getApplicationContext(), this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ||
                PermissionProvider.isNotReady(getApplicationContext(), this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                PermissionProvider.isNotReady(getApplicationContext(), this,
                        Manifest.permission.CAMERA)) {
////            utils.customToast("Check android permission", Toast.LENGTH_LONG, Color.BLUE);
//            finish();
//            System.exit(0);
//            android.os.Process.killProcess(android.os.Process.myPid());
        }

        mRequestingLocationUpdates = false;

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        new Timer().schedule(new TimerTask() {
            public void run() {
                vIconArrow = findViewById(R.id.imageViewArrow);
                vIconArrow.setVisibility(View.INVISIBLE);
                vIconCompass = findViewById(R.id.imageViewCompass);
                vIconCompass.setVisibility(View.INVISIBLE);
                startLocationUpdates();
            }
        }, 2000);

        displayTime();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new MyConnectionCallBack())
                .addOnConnectionFailedListener(new MyOnConnectionFailedListener())
                .addApi(LocationServices.API)
                .build();
        utils.deleteOldLogs();

        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        iFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        iFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        try {
            unregisterReceiver(mBRBattery);
            registerReceiver(mBRBattery, iFilter);
        } catch (Exception e) {
//            utils.logE("register", e.toString());
        }
    }

    BroadcastReceiver mBRBattery = new BroadcastReceiver(){
        int count = 0;
        public void onReceive (Context context, Intent intent){
            String action = intent.getAction();
            utils.log("CC","mBRBattery "+action);
            count++;
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)){
                onBatteryChanged(intent);
            }
            if(action.equals(Intent.ACTION_BATTERY_LOW)){
                // To do
            }
            if (action.equals(Intent.ACTION_BATTERY_OKAY)){
                // To do
            }
            if (action.equals(Intent.ACTION_POWER_CONNECTED)){
                // To do
            }
            if (action.equals(Intent.ACTION_POWER_DISCONNECTED)){
                // To do
            }
        }
    };

    public void onBatteryChanged(Intent intent){
        int plug, status, scale, level, ratio;
        String sPlug = "";
        String sStatus = "";

        utils.log("BB","Battery Changed");

        plug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        ratio = level * 100 / scale;

        utils.log("x","plug:"+plug+" status: "+status+" scale:"+scale+" level:"+level+" ratio:"+ratio);
        switch (plug){
            case BatteryManager.BATTERY_PLUGGED_AC:
                sPlug = "AC";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                sPlug = "USB";
                break;
            default:
                sPlug = "BATTERY";
                break;
        }

        switch (status){
            case BatteryManager.BATTERY_STATUS_CHARGING:
                sStatus = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                sStatus = "not charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                sStatus = "discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                sStatus = "fully charged";
                break;
            default:
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                sStatus = "Unknwon status";
                break;
        }
    }

    long keyDownTime = 0;
    @Override
    public boolean onKeyDown(final int keyCode, KeyEvent event) {

//        utils.log("KEYCODE",""+keyCode);
        long nowTime;
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                nowTime = System.currentTimeMillis();
                if (keyDownTime == 0)
                    keyDownTime = nowTime;

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (System.currentTimeMillis() < keyDownTime + 700)
                            VideoFragment.mergeEvent(mIntervalEvent);
                    }
                }, 500);

//                Log.w("time","down="+keyDownTime+" now="+nowTime+" "+(nowTime-keyDownTime));
                if (nowTime > keyDownTime && nowTime < keyDownTime + 400) {  // double clicked
                    keyDownTime = 0;
                    if (isRecordingNow) {
                        try {
                            String text = "Forced STOP";
                            utils.customToast(text, Toast.LENGTH_LONG, Color.CYAN);
                            videoFragment.stopRecording();
                        } catch (Exception e) {
                            utils.logE(logId, "STOP~" + e.getMessage());
                        }
                        try {
                            new Timer().schedule(new TimerTask() {
                                public void run() {
                                    new CountDownAsync().execute("x", "Exit & Reload", DELAY_WAIT_EXIT + "");
                                }
                            }, 500);
                        } catch (Exception e) {
                            utils.logE(logId, "ReStart~" + e.getMessage());
                        }
                    } else {
                        String text = "Forced QUIT";
                        utils.customToast(text, Toast.LENGTH_LONG, Color.YELLOW);
                        videoFragment.exitBlackRecords();
                    }
                } else {
//                    if (!isRecordingNow)
//                        videoFragment.startRecording();
                    keyDownTime = nowTime;
                }
                break;
            default:
                utils.logE("key", keyCode + " Pressed");
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        long nowTime = System.currentTimeMillis();
        long intervalTime = nowTime - backPressedTime;

        if (100 < intervalTime && 1000 > intervalTime) {
            mExitApplication = true;
            updateHomeButton(mContext);
            ImageButton iB = findViewById(R.id.btnRecord);
            iB.setImageResource(R.mipmap.icon_record_inactive);
            new Timer().schedule(new TimerTask() {
                public void run() {
                    mActivity.finishAffinity();
                    System.exit(0);
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 100);
        }
        backPressedTime = nowTime;
//        utils.customToast("DOUBLE BACK BUTTON to quit this app", Toast.LENGTH_SHORT, Color.GREEN);
    }

    public class MyConnectionCallBack implements GoogleApiClient.ConnectionCallbacks {
        public void onConnected(Bundle bundle) {
        }
        public void onConnectionSuspended(int i) {
        }
    }

    public class MyOnConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        }
    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(GPS_UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(GPS_FASTEST_UPDATE_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                showSpeedDirection();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    private static void startLocationUpdates() {
        Log.w("loc","startLocationUpdates");
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(mActivity, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
//                        utils.log("1", "All location settings are satisfied.");
                        if (ActivityCompat.checkSelfPermission(mContext,
                                Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            utils.logE("Location", "NOT Permitted");
                            return;
                        }
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        showSpeedDirection();
                    }
                })
                .addOnFailureListener(mActivity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                utils.log("2", "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(mActivity, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    utils.logE("3", "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                utils.logE("4", errorMessage);
                                utils.customToast(errorMessage, Toast.LENGTH_LONG, Color.RED);
                                mRequestingLocationUpdates = false;
                        }
                        showSpeedDirection();
                    }
                });
    }


    private static double mXLast, mYLast, mXOld, mYOld;

    private static void showSpeedDirection() {

//        int[] colors = {
//                ContextCompat.getColor(mContext, R.color.White), ContextCompat.getColor(mContext, R.color.JadeGreen),
//                ContextCompat.getColor(mContext, R.color.Yellow), ContextCompat.getColor(mContext, R.color.Gold),
//                ContextCompat.getColor(mContext, R.color.HotPink), ContextCompat.getColor(mContext, R.color.OrangeRed),
//                ContextCompat.getColor(mContext, R.color.Red)};

        prevTime = System.currentTimeMillis();
        if (mCurrentLocation != null) {
            mLatitude = mCurrentLocation.getLatitude();
            mLongitude = mCurrentLocation.getLongitude();
            if (mLatitude != 0 && mCurrentLocation.getSpeed() > 1) {
                vIconCompass.setVisibility(View.VISIBLE);
                vIconArrow.setVisibility(View.VISIBLE);
                Float locationSpeed = mCurrentLocation.getSpeed() * 3.6f;
                vTextSpeed.setText(String.format(Locale.US, "%.0f", locationSpeed));

                if (locationSpeed > 10) {
                    float degree = utils.calcDirection(mXOld, mYOld, mLatitude, mLongitude);
                    utils.drawCompass(degree);
                    mXOld = mXLast;
                    mYOld = mYLast;
                    mXLast = mLatitude;
                    mYLast = mLongitude;
                }
                String txt = mLatitude + ", " + mLongitude;
                vTextGPSValue.setText(txt);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
        showSpeedDirection();
    }

    final static Handler HHMMHandler = new Handler() {
        public void handleMessage(Message msg) { showNowTime(); }
    };

    final static Timer minutesTimer = new Timer();

    static void displayTime() {
        final TimerTask HHMMTask = new TimerTask() {
            @Override
            public void run() {
                if (!mExitApplication)
                    HHMMHandler.sendEmptyMessage(0);
            }
        };
        minutesTimer.schedule(HHMMTask, 100, 59500);
    }

    private static void showNowTime() {

        long now = System.currentTimeMillis();
        vTextDate.setText(utils.getMilliSec2String(now, "MM-dd(EEE)"));
        vTextTime.setText(utils.getMilliSec2String(now, "HH:mm"));
        showBattery();
        if (mLatitude == 0 || (now - 120 * 1000 > prevTime)) {
            startLocationUpdates();
            if (isRecordingNow) {
                vTextGPSValue.setText("Not Activated or Staying..");
                vTextSpeed.setText("-_-");
                vIconCompass.setVisibility(View.INVISIBLE);
            }
        }
    }

    private static boolean isCharging;
    private static void showBattery() {
        IntentFilter intFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, intFilter);
        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float) scale;
        vTextBattery.setText(String.format(Locale.US, "%.0f", batteryPct));
        vTextBattery.setTextColor((batteryPct < 30) ? Color.RED : Color.BLACK);
        if (isCharging) {
            if (batteryPct > 0.7)
                vImgBattery.setImageResource(R.drawable.icon_power_high);
            else if (batteryPct > 0.3)
                vImgBattery.setImageResource(R.drawable.icon_power_good);
            else
                vImgBattery.setImageResource(R.drawable.icon_power_low);
        } else
            vImgBattery.setImageResource(R.drawable.icon_power_off);
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation);
//        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            utils.log("1", "Displaying permission rationale to provide additional context.");
        } else {
            utils.log("2", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        utils.log("1", "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                utils.log("2", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    utils.log("3", "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                }
            }
        }
    }
}
