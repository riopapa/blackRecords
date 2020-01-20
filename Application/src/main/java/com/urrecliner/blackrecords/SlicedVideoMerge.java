package com.urrecliner.blackrecords;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.urrecliner.blackrecords.Vars.CountEvent;
import static com.urrecliner.blackrecords.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackrecords.Vars.PATH_EVENT;
import static com.urrecliner.blackrecords.Vars.activeEvent;
import static com.urrecliner.blackrecords.Vars.eventFileName;
import static com.urrecliner.blackrecords.Vars.evtLatitude;
import static com.urrecliner.blackrecords.Vars.evtLongitude;
import static com.urrecliner.blackrecords.Vars.mActivity;
import static com.urrecliner.blackrecords.Vars.mExitApplication;
import static com.urrecliner.blackrecords.Vars.mLatitude;
import static com.urrecliner.blackrecords.Vars.mLongitude;
import static com.urrecliner.blackrecords.Vars.mPackageEventPath;
import static com.urrecliner.blackrecords.Vars.mPackageNormalDatePath;
import static com.urrecliner.blackrecords.Vars.mPackageWorkingPath;
import static com.urrecliner.blackrecords.Vars.utils;
import static com.urrecliner.blackrecords.Vars.vTextActiveCount;
import static com.urrecliner.blackrecords.Vars.vTextCountEvent;

public class SlicedVideoMerge {

    private static String logID = "sliceMerge";
    private TextView mLogInfo;
    private ImageButton mEventButton;

    private boolean isEventMerge;
    private Collator myCollator = Collator.getInstance();
    private long normalStartTime = 0;
    private SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_LOG_TIME, Locale.US);

    public void initialize(String subPath) {

        mLogInfo = mActivity.findViewById(R.id.textLogInfo);
        mEventButton = mActivity.findViewById(R.id.btnEvent);
//        utils.log("1", "sliceMerge " + subPath);
        if (subPath.equals(PATH_EVENT)) {
            isEventMerge = true;
        }
        else {
            isEventMerge = false;
            normalStartTime = 0;
        }
        utils.beepsInitiate();
    }

    public void merge(long startTime) {
        if (mExitApplication)
            return;
        if (isEventMerge) {
            mEventButton.setImageResource(R.mipmap.icon_event_available);
            mEventButton.setEnabled(true);
        }
        try {
            new MergeFileTask().execute("" + startTime);
        } catch (Exception e) {
            utils.logE(logID,"Exception: "+e.toString());
        }
    }
    private String outputFile;
    private long beginTime;
    private class MergeFileTask extends AsyncTask< String, String, String> {

        @Override
        protected void onPreExecute() {
        }
        @Override
        protected String doInBackground(String... inputParams) {
            long startTime = Long.parseLong(inputParams[0]);
            long endTime = 0;
            String outputText;
            String beginTimeS = null;
            String endTimeS;
            File files2Merge[];

            if (isEventMerge) {
                beginTime = startTime;
            }
            else {
                if (normalStartTime == 0) {
                    normalStartTime = startTime;
                }
                beginTime = normalStartTime;
            }

            files2Merge = utils.getDirectoryList(mPackageWorkingPath);
            if (files2Merge.length < 3) {
                publishProgress("<<file[] too short", "" +files2Merge.length);
            }
            else {
                Arrays.sort(files2Merge);
                endTimeS = files2Merge[files2Merge.length - 2].getName();
                try {
                    Date date = sdf.parse(endTimeS);
                    endTime = date.getTime();
                } catch (ParseException e) {
                    utils.logE("parse", endTimeS + ", " + e.toString()+e.toString());
                }
                beginTimeS = utils.getMilliSec2String(beginTime, FORMAT_LOG_TIME);
                if (!isEventMerge) {    // normal
                    outputFile = new File(mPackageNormalDatePath, beginTimeS + ".mp4").toString();
                    outputText = beginTimeS + ". " + mLatitude + ", " + mLongitude + "z" + ".txt";
                    utils.write2file(mPackageNormalDatePath, outputText, " ");
                    normalStartTime = endTime;
                } else {
                    outputFile = new File(mPackageEventPath, beginTimeS + ".mp4").toString();
                }

                String mergedString = merge2OneVideo(beginTimeS, endTimeS, files2Merge);
                if (mergedString != "")
                    publishProgress(mergedString);
                else if (isEventMerge) {
                    eventFileName = outputFile;
                    int len = eventFileName.length();
                    MediaPlayer mp = new MediaPlayer();
                    try {
                        mp.setDataSource(eventFileName);
                        mp.prepare();
                    }catch (IOException e) {
                        e.printStackTrace();
                        utils.logE(logID,"IOException: "+e.toString());
                    }
                    int duration = mp.getDuration() / 1000;
                    mp.release();
                    String outputInfoFile = eventFileName.substring(0,len-4) + "x" + duration + "z" + evtLatitude + "," + evtLongitude + ".mp4";
                    File file = new File (eventFileName);
                    file.renameTo(new File(outputInfoFile));
//                     if (FFmpeg.getInstance(mContext).isSupported()) {
//                        String srtFile = makeSRTFile(duration);
//                        ffmPegMergeSRT(srtFile);
//                    } else {
//                        // ffmpeg is not supported
//                        Log.e("ffmpec", " not supported!");
//                    }
                }
            }
            return beginTimeS;
       }

        private String merge2OneVideo(String beginTimeS, String endTimeS, File[] files2Merge) {
            List<Movie> listMovies = new ArrayList<>();
            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();
//            Log.w("#of files","files2Merge "+files2Merge.length);
//            Log.w("time","begin " +beginTimeS+" end "+endTimeS);
            for (File file : files2Merge) {
                String shortFileName = file.getName();
                if (myCollator.compare(shortFileName, beginTimeS) >= 0 &&
                        myCollator.compare(shortFileName, endTimeS) < 0) {
                    String shortFileType = shortFileName.substring(shortFileName.length() -3);
//                    Log.w("add to movie " + isEventMerge,shortFileName);
                    if (shortFileType.equals("mp4")) {
                        try {
                            listMovies.add(MovieCreator.build(file.toString()));
                        } catch (Exception e) {
                            utils.logE(logID,"mergeOne~ "+e.toString());
                            return "<<nullptr>> " + isEventMerge + shortFileName + " will be ignored";
                        }
                    }
                }
            }
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
//                            utils.log("movi Tag " + count, track.getHandler());
                    if (track.getHandler().equals("vide")) {
                        videoTracks.add(track);
                    }
                    else if (!isEventMerge) { // track.getHandler().equals("soun") &&
                        audioTracks.add(track);
                    }
                }
            }

            if (!videoTracks.isEmpty()) {
                Movie outputMovie = new Movie();
                try {
                        outputMovie.addTrack(new AppendTrack(videoTracks.toArray(new Track[0])));
                        if (!isEventMerge)
                            outputMovie.addTrack(new AppendTrack(audioTracks.toArray(new Track[0])));
                        Container container = new DefaultMp4Builder().build(outputMovie);
                        FileChannel fileChannel = new RandomAccessFile(outputFile, "rw").getChannel();
                        container.writeContainer(fileChannel);
                        fileChannel.close();
                } catch (IOException e) {
                    utils.logE(logID,"IOException~ "+e.toString());
                    return "<<Error in DoIn>>" + e.toString();
                }
            } else {
                utils.beepOnce(3, 1f);
                return "<< NO TRACKS in container >> pass";
            }
            return "";
        }

        @Override
        protected void onProgressUpdate(String... values) {
            String debugText = values[0];
            mLogInfo.setText(debugText);
            if (values[0].substring(0,1).equals("<")) {
                utils.customToast(debugText, Toast.LENGTH_SHORT, Color.RED);
//                utils.logE("1", debugText);
            }
            else {
//                utils.log("2", debugText);
            }
        }
        @Override
        protected void onCancelled(String result) {
        }

        @Override
        protected void onPostExecute(String doI ) {

            if (isEventMerge) {
                utils.beepOnce(3, .5f);
                String countStr = "" + ++CountEvent;
                vTextCountEvent.setText(countStr);
                utils.customToast("Event Recording completed", Toast.LENGTH_SHORT, Color.DKGRAY);
                activeEvent--;
                String text = (activeEvent==0) ? "":"<"+activeEvent+">";
                vTextActiveCount.setText(text);
                mEventButton.setImageResource(R.mipmap.icon_event_available);
            }
            else {
//                utils.beepOnce(5, .3f);
                String [] parms = doI.split(",");
                utils.deleteFiles(mPackageWorkingPath, parms[0]);
                utils.checkDiskSpace();
            }
        }
    }
}
