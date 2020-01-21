package com.urrecliner.blackrecords;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Environment;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;

public class Vars {
    static Utils utils = new Utils();
    static TextView vTextDate = null;
    static TextView vTextTime = null;
    static TextView vTextGPSValue = null;
    static TextView vTextCountEvent = null;
    static TextView vTextSpeed = null;
    static TextView vKm = null;
    static TextView vTextLogInfo = null;
    static TextView vTextActiveCount = null;
    static TextView vTextRecord = null;
    static TextView vTextBattery = null;
    static ImageView vImgBattery = null;
    static VideoFragment videoFragment;
    static VideoUtils videoUtils;

    static final String FORMAT_LOG_TIME = "yyyy-MM-dd HH.mm.ss.SSS";
    static final String FORMAT_DATE = "yyyy-MM-dd";
    static final String FORMAT_NOWTIME = "HH.mm.ss ";

    private static final String PATH_PACKAGE = "blackRecords";
    static final String PATH_EVENT = "event";
    static final String PATH_NORMAL = "normal";
    private static final String PATH_WORKING = "working";
    private static final String PATH_LOG = "log";

    static File mPackagePath = new File(Environment.getExternalStorageDirectory(), PATH_PACKAGE);
    static File mPackageEventPath = new File(mPackagePath, PATH_EVENT);
    static File mPackageNormalPath = new File(mPackagePath, PATH_NORMAL);
    static File mPackageNormalDatePath = new File(mPackageNormalPath, utils.getMilliSec2String(System.currentTimeMillis(), FORMAT_DATE));
    static File mPackageWorkingPath = new File(mPackagePath, PATH_WORKING);
    static File mPackageLogPath = new File(mPackagePath, PATH_LOG);

    static MediaRecorder mediaRecorder;

    static Activity mActivity = null;
    static Context mContext = null;
    static boolean mExitApplication = false;
    static boolean isRecordingNow = false;

    static Location mCurrentLocation;
    static GoogleApiClient mGoogleApiClient;

    static double mLatitude = 0;
    static double mLongitude = 0;
    static double evtLatitude = 0;
    static double evtLongitude = 0;

    static ImageButton mBtnEvent = null;

    static SlicedVideoMerge eventMerge = new SlicedVideoMerge();
    static SlicedVideoMerge normalMerge = new SlicedVideoMerge();

    static int CountEvent;

    static long mIntervalNormal = 90 * 1000;
    static long mIntervalEvent = 20 * 1000;
    static String eventFileName;
    static int activeEvent = 0;

    // Recording will be auto start after DELAY_AUTO_RECORD when app is loaded
    // when KeyDown press, it will wait one more KeyDown for exit app till DELAY_WAIT_EXIT
    // or app will be purged and restarted after DELAY_RESTART
    final static int DELAY_AUTO_RECORD = 4;
    final static int DELAY_WAIT_EXIT = 10;
    final static int DELAY_RESTART = 60;

    static AutoFitTextureView vTextureView;
}
