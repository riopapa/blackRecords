package com.urrecliner.blackrecords;

import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static com.urrecliner.blackrecords.Vars.FORMAT_DATE;
import static com.urrecliner.blackrecords.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackrecords.Vars.FORMAT_NOWTIME;
import static com.urrecliner.blackrecords.Vars.mActivity;
import static com.urrecliner.blackrecords.Vars.mContext;
import static com.urrecliner.blackrecords.Vars.mPackageLogPath;
import static com.urrecliner.blackrecords.Vars.mPackageNormalPath;
import static com.urrecliner.blackrecords.Vars.mPackagePath;
import static com.urrecliner.blackrecords.Vars.utils;
import static com.urrecliner.blackrecords.Vars.vTextLogInfo;

class Utils {
    private boolean ON_DEBUG = true;
    private String logDate = getMilliSec2String(System.currentTimeMillis(),FORMAT_DATE);

    String getMilliSec2String(long milliSec, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.KOREA);
        return dateFormat.format(new Date(milliSec));
    }

    boolean readyPackageFolder (File dir){
        try {
            if (!dir.exists()) return dir.mkdirs();
            else
                return true;
        } catch (Exception e) {
            Log.e("creating Folder error", dir + "_" + e.toString());
        }
        return false;
    }

    File[] getDirectoryList(File fullPath) {
        return fullPath.listFiles();
    }

    File[] getDirectoryFiltered(File fullPath, final String fileType) {
        File[] files = fullPath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file)
            {
                return (file.getPath().endsWith(fileType));
            }
        });
        return files;
    }

    /* delete files within directory if name is less than fileName */

    void deleteFiles(File directory, String fileName) {

        File[] files = directory.listFiles();
        if(null!=files){
            Collator myCollator = Collator.getInstance();
            for (File file : files) {
                String shortFileName = file.getName();
                if (myCollator.compare(shortFileName, fileName) < 0) {
                    file.delete();
                }
            }
        }
    }

    /* delete old directory / files if storage is less than xx */
    void checkDiskSpace() {
        long freeSize = mPackagePath.getFreeSpace() / 1000L;
        if (freeSize < 2500000) {  // 2.5Gb free storage
            File[] files = utils.getDirectoryList(mPackageNormalPath);
            if (files.length > 0) { // if any previous folder
                Arrays.sort(files);
                if (files.length > 1) { // more than 2 date directory
                    if (deleteRecursive(files[0]))
                        warnFreeSize(freeSize);
                }
                else {  // if this is only folder then remove some files within this directory
                    File[] subFiles = utils.getDirectoryList(files[0]);
                    if (subFiles.length > 5) {
                        Arrays.sort(subFiles);
                        int max = (subFiles.length > 10) ? 10 : subFiles.length - 1;
                        for (int i = 0; i < max; i++) {  // delete old file first
                            subFiles[i].delete();
                        }
                        warnFreeSize(freeSize);
                    } else {
                        beepOnce(1,1f);
                        String text = "Storage file structure error ! subFile count is " + subFiles.length +
                                " freeSize:" + mPackagePath.getFreeSpace() / 1000L;
                        vTextLogInfo.setText(text);
                        utils.logE("No FreeSize", "Something wrong");
                        beepOnce(2,1f);
                    }
                }
            }
            deleteOldLogs();
        }
    }

    private void warnFreeSize(long freeSizeOld) {
        long freeSizeNew = mPackagePath.getFreeSpace() / 1000L;
        customToast("Remove Old Videos ", Toast.LENGTH_SHORT, Color.GREEN);
    }

    /* delete directory and files under that directory */
    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        return fileOrDirectory.delete();
    }

    void deleteOldLogs() {
        final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        String oldDate = "log_" + sdfDate.format(System.currentTimeMillis() - 3*24*60*60*1000L);
        File[] files = getCurrentFileList(mPackageLogPath);
        Collator myCollator = Collator.getInstance();
        for (File file : files) {
            String shortFileName = file.getName();
            if (myCollator.compare(shortFileName, oldDate) < 0) {
                if (!file.delete())
                    Log.e("file","Delete Error "+file);
            }
        }
    }

    private File[] getCurrentFileList(File fullPath) {
        return fullPath.listFiles();
    }


    void log (String tag, String text) {
//        int pid = android.os.Process.myPid();
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "#" + traces[3].getLineNumber() + " {"+ tag + "} " + text;
        Log.w(tag , log);
        append2file(mPackageLogPath, "log_" + logDate + ".txt", getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  ": " + log);
        text = vTextLogInfo.getText().toString() + "\n" + getMilliSec2String(System.currentTimeMillis(), FORMAT_NOWTIME)+text;
        text = truncLine(text);
        vTextLogInfo.setText(text);
    }

    void logE(String tag, String text) {
        StackTraceElement[] traces;
        traces = Thread.currentThread().getStackTrace();
        String log = traceName(traces[5].getMethodName()) + traceName(traces[4].getMethodName()) + traceClassName(traces[3].getClassName())+"> "+traces[3].getMethodName() + "_" + traces[3].getLineNumber() + " [err:"+ tag + "] " + text;
        Log.e("<" + tag + ">" , log);
        append2file(mPackageLogPath, "log_" + logDate + ".txt", getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  "> " + log);
        append2file(mPackageLogPath, "log_" + logDate + "E.txt", getMilliSec2String(System.currentTimeMillis(), FORMAT_LOG_TIME) +  "> " + log);
        text = vTextLogInfo.getText().toString() + "\n" + text;
        text = truncLine(text);
        vTextLogInfo.setText(text);
    }

    static private String []omits = { "performResume", "performCreate", "dispatch", "callActivityOnResume", "access$",
            "handleReceiver", "handleMessage", "dispatchKeyEvent", "moveToState"};
    private String traceName (String s) {
        for (String o : omits) {
            if (s.contains(o)) return "";
        }
        return s + "> ";
    }

    private String traceClassName(String s) {
        return s.substring(s.lastIndexOf(".")+1);
    }

    void customToast  (String text, int short_Long, int backColor) {

        Toast toast = Toast.makeText(mContext, text, short_Long);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER, 0,0);
        View toastView = toast.getView(); // This'll return the default View of the Toast.

        /* And now you can get the TextView of the default View of the Toast. */
        TextView toastMessage = toastView.findViewById(android.R.id.message);
        toastMessage.setTextSize(28);
        if (backColor == Color.YELLOW)
            toastMessage.setTextColor(Color.BLUE);
        else
            toastMessage.setTextColor(Color.WHITE);
        toastMessage.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_launcher, 0, 0, 0);
//        toastMessage.setGravity(Gravity.CENTER_VERTICAL);
        toastMessage.setCompoundDrawablePadding(16);
        toastMessage.setPadding(4,4,24,4);
        toastView.setBackgroundColor(backColor);
        toast.show();
        log("customToast",text);
    }

    void displayCount(String text, int short_Long, int backColor) {

        Toast toast = Toast.makeText(mContext, text, short_Long);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER, 0,0);
        View toastView = toast.getView(); // This'll return the default View of the Toast.

        /* And now you can get the TextView of the default View of the Toast. */
        TextView tm = toastView.findViewById(android.R.id.message);
        tm.setTextSize(48);
        tm.setGravity(Gravity.CENTER);
        tm.setMaxWidth(2000);
        tm.setWidth(2000);

        if (backColor == Color.YELLOW)
            tm.setTextColor(Color.BLUE);
        else
            tm.setTextColor(Color.WHITE);
//        toastMessage.setCompoundDrawablesWithIntrinsicBounds(R.mipmap.ic_launcher, 0, 0, 0);
        tm.setCompoundDrawablePadding(16);
        tm.setPadding(4,4,4,4);
        toastView.setBackgroundColor(backColor);
        toast.show();
    }

    private String truncLine(String str) {
        String[] strArray = str.split("\n");
        if (strArray.length > 5) {
            String result = "";
            for (int i = strArray.length - 4; i < strArray.length; i++)
                result += strArray[i]+"\n";
            return result.substring(0,result.length()-1);
        }
        return str;
    }

    void append2file (File directory, String filename, String text) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            File file = new File(directory, filename);
            if (!file.exists()) {
                if(!file.createNewFile()) {
                    Log.e("createFile"," Error");
                }
            }
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write("\n" + text + "\n");
        } catch (IOException e) {
            String s = directory.toString() + filename + " Err:" + e.toString();
            utils.logE("append",s);
            Log.e("appendIOExcept1",  e.getMessage());
        } finally {
            try {
                if (bw != null) bw.close();
                if (fw != null) fw.close();
            } catch (IOException e) {
                String s = directory.toString() + filename + " close~" + e.toString();
                Log.e("appendIOExcept2",  e.getMessage());
            }
        }
    }

    void write2file (File directory, String filename, String text) {
        final File file = new File(directory, filename);
        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.write(text);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        }
        catch (IOException e) {
            String s = file.toString() + " Err:" + e.toString();
            utils.logE("write",s);
            e.printStackTrace();
        }
    }

//    public void singleBeep(Activity activity,int type) {
//        try {
//            Uri notification;
//            if (type == 0) {
//                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            }
//            else {
//                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//            }
//            Ringtone r = RingtoneManager.getRingtone(activity.getApplicationContext(), notification);
//            r.play();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private SoundPool soundPool = null;
    private int[] beepSound = {
            R.raw.beep0_animato,                    //  event button pressed
            R.raw.beep1_ddok,                       //  file limit reached
            R.raw.beep2_dungdong,                   //  close app, free storage
            R.raw.beep3_haze,                       //  event merge finished
            R.raw.beep4_recording,                  //  record button pressed
            R.raw.beep5_s_dew_drops,                //  normal merge finished
            R.raw.beep6_stoprecording               // stop recording
            };
    private int[] soundNbr = {0,0,0,0,0,0,0,0,0,0};

    void beepsInitiate() {

        SoundPool.Builder builder;
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        builder = new SoundPool.Builder();
        builder.setAudioAttributes(audioAttributes).setMaxStreams(5);
        soundPool = builder.build();
        for (int i = 0; i < beepSound.length; i++) {
            soundNbr[i] = soundPool.load(mContext, beepSound[i], 1);
        }
    }

    void beepOnce(int soundId,float volume) {

        if (soundPool == null) {
            beepsInitiate();
            final int id = soundId;
            final float vol = volume;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    beepSound(id, vol);
                }
            }, 2000);
        } else {
            beepSound(soundId, volume);
        }
    }
    private void beepSound(int soundId, float volume) {
        soundPool.play(soundNbr[soundId], volume, volume, 1, 0, 1f);
    }

    float calcDirection(double P1_latitude, double P1_longitude, double P2_latitude, double P2_longitude)
    {
        final double CONSTANT2RADIAN = (3.141592 / 180);
        final double CONSTANT2DEGREE = (180 / 3.141592);

        // 현재 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Cur_Lat_radian = P1_latitude * CONSTANT2RADIAN;
        double Cur_Lon_radian = P1_longitude * CONSTANT2RADIAN;

        // 목표 위치 : 위도나 경도는 지구 중심을 기반으로 하는 각도이기 때문에 라디안 각도로 변환한다.
        double Dest_Lat_radian = P2_latitude * CONSTANT2RADIAN;
        double Dest_Lon_radian = P2_longitude * CONSTANT2RADIAN;

        // radian distance
        double radian_distance =
                Math.acos(Math.sin(Cur_Lat_radian) * Math.sin(Dest_Lat_radian) + Math.cos(Cur_Lat_radian) * Math.cos(Dest_Lat_radian) * Math.cos(Cur_Lon_radian - Dest_Lon_radian));

        // 목적지 이동 방향을 구한다.(현재 좌표에서 다음 좌표로 이동하기 위해서는 방향을 설정해야 한다. 라디안 값임
        double radian_bearing = Math.acos((Math.sin(Dest_Lat_radian) - Math.sin(Cur_Lat_radian) * Math.cos(radian_distance)) / (Math.cos(Cur_Lat_radian) * Math.sin(radian_distance)));        // acos의 인수로 주어지는 x는 360분법의 각도가 아닌 radian(호도)값이다.

        double true_bearing;
        if (Math.sin(Dest_Lon_radian - Cur_Lon_radian) < 0)
            true_bearing = 360 - radian_bearing * CONSTANT2DEGREE;
        else
            true_bearing = radian_bearing * CONSTANT2DEGREE;
        return (float) true_bearing;
    }

    private ImageView vCompass;
    private float savedDegree;

    void drawCompass (float degree) {
        if (vCompass == null) {
            vCompass = mActivity.findViewById(R.id.imageViewCompass);
//            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        }

        RotateAnimation ra = new RotateAnimation(
                savedDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(200);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);
        vCompass.startAnimation(ra);
        savedDegree = -degree;
    }

}
