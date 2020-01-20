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
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.urrecliner.blackrecords.Vars.CountEvent;
import static com.urrecliner.blackrecords.Vars.DELAY_AUTO_RECORD;
import static com.urrecliner.blackrecords.Vars.FORMAT_LOG_TIME;
import static com.urrecliner.blackrecords.Vars.PATH_EVENT;
import static com.urrecliner.blackrecords.Vars.PATH_NORMAL;
import static com.urrecliner.blackrecords.Vars.activeEvent;
import static com.urrecliner.blackrecords.Vars.eventMerge;
import static com.urrecliner.blackrecords.Vars.evtLatitude;
import static com.urrecliner.blackrecords.Vars.evtLongitude;
import static com.urrecliner.blackrecords.Vars.isRecordingNow;
import static com.urrecliner.blackrecords.Vars.mActivity;
import static com.urrecliner.blackrecords.Vars.mBtnEvent;
import static com.urrecliner.blackrecords.Vars.mContext;
import static com.urrecliner.blackrecords.Vars.mExitApplication;
import static com.urrecliner.blackrecords.Vars.mIntervalEvent;
import static com.urrecliner.blackrecords.Vars.mIntervalNormal;
import static com.urrecliner.blackrecords.Vars.mLatitude;
import static com.urrecliner.blackrecords.Vars.mLongitude;
import static com.urrecliner.blackrecords.Vars.mPackageEventPath;
import static com.urrecliner.blackrecords.Vars.mPackageLogPath;
import static com.urrecliner.blackrecords.Vars.mPackageNormalDatePath;
import static com.urrecliner.blackrecords.Vars.mPackageNormalPath;
import static com.urrecliner.blackrecords.Vars.mPackagePath;
import static com.urrecliner.blackrecords.Vars.mPackageWorkingPath;
import static com.urrecliner.blackrecords.Vars.mediaRecorder;
import static com.urrecliner.blackrecords.Vars.normalMerge;
import static com.urrecliner.blackrecords.Vars.utils;
import static com.urrecliner.blackrecords.Vars.vImgBattery;
import static com.urrecliner.blackrecords.Vars.vKm;
import static com.urrecliner.blackrecords.Vars.vTextActiveCount;
import static com.urrecliner.blackrecords.Vars.vTextBattery;
import static com.urrecliner.blackrecords.Vars.vTextCountEvent;
import static com.urrecliner.blackrecords.Vars.vTextDate;
import static com.urrecliner.blackrecords.Vars.vTextGPSValue;
import static com.urrecliner.blackrecords.Vars.vTextLogInfo;
import static com.urrecliner.blackrecords.Vars.vTextRecord;
import static com.urrecliner.blackrecords.Vars.vTextSpeed;
import static com.urrecliner.blackrecords.Vars.vTextTime;
import static com.urrecliner.blackrecords.Vars.vTextureView;
import static com.urrecliner.blackrecords.Vars.videoUtils;
import static com.urrecliner.blackrecords.VideoUtils.chooseOptimalSize;

public class VideoFragment extends Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {

    private String logID = "VF";

    public VideoFragment() {
    }
    private static VideoFragment instance;

    public static VideoFragment getInstance() {
        return instance;
    }

    private ImageButton mBtnRecord = null;
    View viewCamera = null;

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "f_d";

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

//     * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
    private CameraDevice mCameraDevice;

//     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession}
    private CameraCaptureSession mPreviewSession;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
//            utils.log("1", width + "," + height);
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
//            utils.log("1", width + "," + height);
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    private Size mPreviewSize;
    private Size mVideoSize;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

//     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
//            utils.log("1", ".StateCallback onOpened _y_");
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != vTextureView) {
                configureTransform(vTextureView.getWidth(), vTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
//            utils.log(" 1", "> .StateCallback onDisconnected _y_");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
//            utils.log(" 1", "> .StateCallback onError _y_");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Integer mSensorOrientation;
    private CaptureRequest.Builder mPreviewBuilder;

    public static VideoFragment newInstance() {
//        Log.w("1", "newInstance");
        return new VideoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        utils.log("1", "_y");
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        instance = this;
        return inflater.inflate(R.layout.blackbox_landscape, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        viewCamera = view;
        if (!mExitApplication) {
            initiate(getActivity(), getContext(), view);
            mBtnEvent.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mergeEvent(mIntervalEvent); // mIntervalEvent
                }
            });
            RecordClicked();
        }
        new Timer().schedule(new TimerTask() {  // autoStart
            public void run() {
                autoHandler.sendEmptyMessage(0);
            }
        }, 2000);
    }

    final static Handler autoHandler = new Handler() {
        public void handleMessage(Message msg) {
            new CountDownAsync().execute("v", "AutoStart", DELAY_AUTO_RECORD+"");
        }
    };

    void autoStart() {
        if (!isRecordingNow) {
            utils.log("auto", "Auto Start");
            startPreview();
            mCameraOpenCloseLock.release();
            startRecording();
        }
    }

    private void RecordClicked() {
        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (isRecordingNow) {
                        utils.customToast("Stop Record", Toast.LENGTH_LONG, Color.GREEN);
                        stopRecording();
                    } else {
                        utils.customToast("Start Record", Toast.LENGTH_LONG, Color.GREEN);
                        startRecording();
                    }
                } catch (Exception e) {
                    utils.logE(logID, "\n"+e.getMessage());
                }
            }
        });
        mBtnRecord.setEnabled(true);
    }

//    private void EventClicked() {
//
//        mBtnEvent.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                mergeEvent(mIntervalEvent); // mIntervalEvent
//            }
//        });
//    }

    public static void mergeEvent(long interval) {

        if (!isRecordingNow) return;
        activeEvent++;
        String text = "<"+activeEvent+">";
        vTextActiveCount.setText(text);
        utils.customToast("EVENT button Pressed", Toast.LENGTH_SHORT, Color.RED);

        mBtnEvent.setImageResource(R.mipmap.icon_event_recording);
        final long startTime = System.currentTimeMillis() - interval - interval / 2;
        GpsTracker gpsTracker = new GpsTracker(mContext);
        Location location = gpsTracker.getLocation();
        if (location != null) {
            evtLatitude = gpsTracker.getLatitude();
            evtLongitude = gpsTracker.getLongitude();
        } else {
            evtLatitude = mLatitude;
            evtLongitude = mLongitude;
        }
        new Timer().schedule(new TimerTask() {
            public void run() {
                eventMerge.merge(startTime);
            }
        }, interval + interval / 2);
    }

    private void initiate(Activity activity, Context context, View view) {
        /* variables in Vars */
        mActivity = activity;
        mContext = context;
        vTextureView = view.findViewById(R.id.texture);
        vTextDate = view.findViewById(R.id.textDate);
        vTextTime = view.findViewById(R.id.textTime);
        vTextSpeed = view.findViewById(R.id.textSpeed);
        vKm = view.findViewById(R.id.textKm);
        vTextGPSValue = view.findViewById(R.id.textGPSValue);
        vTextLogInfo = view.findViewById(R.id.textLogInfo);
        vTextCountEvent = view.findViewById(R.id.textCountEvent);
        vTextActiveCount = view.findViewById(R.id.activeCount);
        vTextRecord = view.findViewById(R.id.textCountRecords);
        vTextBattery = view.findViewById(R.id.textBattery);
        vImgBattery = view.findViewById(R.id.imgBattery);

        mBtnRecord = view.findViewById(R.id.btnRecord);
        mBtnRecord.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mBtnEvent = view.findViewById(R.id.btnEvent);
        mBtnEvent.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mBtnEvent.setImageResource(R.mipmap.icon_event_not_allowed);

        isRecordingNow = false;
        eventMerge.initialize(PATH_EVENT);
        normalMerge.initialize(PATH_NORMAL);

        utils.readyPackageFolder(mPackagePath);
        utils.readyPackageFolder(mPackageLogPath);
        utils.readyPackageFolder(mPackageWorkingPath);
        utils.readyPackageFolder(mPackageEventPath);
        utils.readyPackageFolder(mPackageNormalPath);
        utils.readyPackageFolder(mPackageNormalDatePath);
        utils.beepsInitiate();

        CountEvent = utils.getDirectoryFiltered(mPackageEventPath, "mp4").length;
        String txt = "" + CountEvent;
        vTextCountEvent.setText(txt);

        vTextActiveCount.setText("");
        ImageButton eb = view.findViewById(R.id.btnExit);
        eb.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
//                utils.log("1", "Exit Application");
                exitBlackRecords();
            }
        });
    }

    @Override
    public void onResume() {
//        utils.log("1", "-");
        if (!mExitApplication) {
            startBackgroundThread();
            if (vTextureView.isAvailable()) {
                openCamera(vTextureView.getWidth(), vTextureView.getHeight());
            } else {
                vTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }
        super.onResume();
    }

    public void exitBlackRecords() {
        mExitApplication = true;
        try {
            stopRecording();
        } catch (Exception e) {
            utils.logE(logID, "\n"+e.getMessage());
        }
        new Timer().schedule(new TimerTask() {
            public void run() {
                mActivity.finishAffinity();
                System.exit(0);
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }, 1000);
    }

    @Override
    public void onClick(View view) {
    }

    private void startBackgroundThread() {

        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }
    }

    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    private void requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new VideoUtils.ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
//        utils.log("1", "_y");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        VideoUtils.ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                VideoUtils.ErrorDialog.newInstance(getString(R.string.permission_request))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    @SuppressWarnings("MissingPermission")
    String cameraId;

    private void openCamera(int width, int height) {
//        utils.log("1",  "w:" + width + ", h:" + height + " _y_");
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }

            mVideoSize = videoUtils.chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                vTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                vTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mediaRecorder = new MediaRecorder();
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            utils.logE(logID,"\n"+e.toString());
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_LONG).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            VideoUtils.ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void startPreview() {
        try {
            closePreviewSession();
            SurfaceTexture texture = vTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                utils.logE(logID, "onConfigureFailed~ ");
                                Toast.makeText(activity, "onConfigureFailed Failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            utils.logE(logID, "\n"+e.toString());
        }
    }

//     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            if (mPreviewSession != null)
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException | IllegalArgumentException e) {
            utils.logE(logID, e.toString());
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    static final long MAX_FILE_SIZE = 24 * 1000000;
    static final int ENCODING_RATE = 18000000;
    static final int FrameRate = 24;

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

    private void setUpMediaRecorder() throws IOException {
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(getOutputFileName(0, "mp4").toString());
            mediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
            mediaRecorder.setVideoEncodingBitRate(ENCODING_RATE);
        } catch (Exception e) {
            utils.logE(logID,e.toString());
        }
        try {
            mediaRecorder.setVideoFrameRate(FrameRate);
            mediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);    //  was AAC
            int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            switch (mSensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mediaRecorder.setOrientationHint(VideoUtils.DEFAULT_ORIENTATIONS.get(rotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mediaRecorder.setOrientationHint(VideoUtils.INVERSE_ORIENTATIONS.get(rotation));
                    break;
            }
            mediaRecorder.prepare();
        } catch (Exception e) {
            utils.logE(logID,e.toString());
        }
        setUpNextFile();
        utils.log("2", "PREPARED");
    }

    private File getOutputFileName(long delta, String fileType) {

        String time = utils.getMilliSec2String(System.currentTimeMillis() + delta, FORMAT_LOG_TIME);
        return new File(mPackageWorkingPath, time + "." + fileType);
    }

    private boolean normalLater = false;
    final Handler normalHandler = new Handler() {
        public void handleMessage(Message msg) {
            normalSaving();
            normalLater = false;
            prepareNormalSave();
        }
    };

    private void prepareNormalSave() {
        if (isRecordingNow && !normalLater && !mExitApplication) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    if (!mExitApplication) {
                        normalHandler.sendEmptyMessage(0);
                    }
                }
            }, mIntervalNormal);
            normalLater = true;
        }
    }

    private void normalSaving() {
        final long startTime = System.currentTimeMillis() - mIntervalNormal - mIntervalEvent;
        if (!mExitApplication) {
            normalMerge.merge(startTime);
        }
    }

    private boolean blinkingLater;

    public void startRecording() {

        List<Surface> surfaces = new ArrayList<>();
        try {
            closePreviewSession();
            setUpMediaRecorder();
            SurfaceTexture texture = vTextureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);
        } catch (Exception e) {
            utils.logE(logID,e.toString());
        }

        // Set up Surface for the MediaRecorder
        try {
            Surface recorderSurface = mediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);
        } catch (Exception e) {
            utils.logE(logID,"getSurface\n" + e.toString());
        }

        try {
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mediaRecorder.start();
                        }
                    });
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_LONG).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (Exception e) {
            utils.logE(logID,"mCameraDevice\n" + e.toString());
        }
        startBlinking();
        prepareNextFile();
    }

    final Handler nextFileHandler = new Handler() {
        public void handleMessage(Message msg) { assignNextFile();
        }
    };

    private void prepareNextFile() {
        if (isRecordingNow && !mExitApplication) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    nextFileHandler.sendEmptyMessage(0);
                }
            }, 500);
        }
    }

    int nextCount = 0;
    private void assignNextFile() {
        try {
            File nextFileName = getOutputFileName(3000, "mp4");
            mediaRecorder.setNextOutputFile(nextFileName);
            nextCount++;
            String s = nextCount+"";
            vTextRecord.setText(s);
        } catch (IOException e) {
            utils.logE(logID,"nxtFile\n" + e.toString());
        }
    }

    public void startBlinking() {
        mBtnRecord.setEnabled(false);
        utils.beepOnce(4, 0.2f);
        mBtnRecord.setImageResource(R.mipmap.icon_record_running);
        new Timer().schedule(new TimerTask() {
            public void run() {
                mBtnEvent.setEnabled(true);
                mBtnEvent.setImageResource(R.mipmap.icon_event_available);
            }
        }, mIntervalEvent);
        isRecordingNow = true;
        ready2Blinking();
        prepareNormalSave();
        new Timer().schedule(new TimerTask() {
            public void run() {
                recordEnableHandler.sendEmptyMessage(0);
            }
        }, 2000);
    }

    final Handler recordEnableHandler = new Handler() {
        public void handleMessage(Message msg) {
            mBtnRecord.setEnabled(true);
        }
    };

    final Handler blinkHandler = new Handler() {
        public void handleMessage(Message msg) {
            blinkingLater = false;
            blinkOnRecord();
            ready2Blinking();
        }
    };

    private int blinkInterval = 2000;

    private void ready2Blinking() {
        if (isRecordingNow && !blinkingLater && !mExitApplication) {
            new Timer().schedule(new TimerTask() {
                public void run() {
                    blinkHandler.sendEmptyMessage(0);
                }
            }, blinkInterval);
            blinkingLater = true;
        }
    }

    private boolean isBlinking;

    private void blinkOnRecord() {

        if (isRecordingNow) {
//            isBlinking ^= true;
//            if (isBlinking) {
//                blinkInterval = 1000;
//            } else {
//                blinkInterval = 800;
//            }
        } else {
            mBtnRecord.setImageResource(R.mipmap.icon_record_inactive);
            mBtnRecord.setVisibility(View.VISIBLE);
        }
    }

    private void setUpNextFile() {

        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                if (mExitApplication) exitBlackRecords();
                switch (what) {
                    case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                        mediaRecorder.stop();
                        mediaRecorder.reset();
                        utils.beepOnce(1, 0.5f);
                        utils.logE(logID, "MAX_FILE SIZE_REACHED ");
                        new Timer().schedule(new TimerTask() {
                            public void run() {
                                startRecording();
                            }
                        }, 2000);
                        break;
                    case MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED:
                        prepareNextFile();
                        break;
                    default:
//                        utils.log("d","default " + what);
                }
            }
        });
    }

    private void closePreviewSession() {
        if (mPreviewSession != null) {
            try {
                mPreviewSession.close();
                mPreviewSession = null;
            } catch (Exception e) {
                utils.logE(logID, "\n" +e.toString());
            }
        }
    }

    public void stopRecording() {
//        utils.log( "1", " finishRecordingVideo _y_");

//        PersistableBundle persistableBundle;
//        persistableBundle = mediaRecorder.getMetrics ();
//        dumpBundle(persistableBundle);

        if (isRecordingNow) {
            try {
                mediaRecorder.stop();
            } catch (IllegalStateException e) {
                utils.logE(logID, "stop\n" + e.toString());
            }
            try {
                mediaRecorder.reset();
            } catch (IllegalStateException e) {
                utils.logE(logID, "reset\n" + e.toString());
            }
        }
        stopBlinking();

//        startPreview(); haha
    }

    public void stopBlinking() {
//        utils.log("1","");
        isRecordingNow = false;
        try {
            if (mBtnRecord == null)
                mBtnRecord = mActivity.findViewById(R.id.btnRecord);
            mBtnRecord.setImageResource(R.mipmap.icon_record_inactive);

            if (mBtnEvent == null)
                mBtnEvent = mActivity.findViewById(R.id.btnEvent);
            mBtnEvent.setImageResource(R.mipmap.icon_event_not_allowed);
            vTextRecord.setText("");
            mBtnEvent.setEnabled(false);
            utils.beepOnce(6, 0.1f);
        } catch (Exception e) {
            utils.logE(logID,"\n"+e.toString());
        }
    }

        private void configureTransform(int viewWidth, int viewHeight) {

        Activity activity = getActivity();
        if (null == vTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        vTextureView.setTransform(matrix);
    }

//    private void dumpBundle(PersistableBundle bundle) {
//        for (String key : bundle.keySet()) {
//            Object value = bundle.get(key);
//            utils.log("dump", String.format("%s = %s (%s)", key,
//                    value.toString(), value.getClass().getName()));
//        }
//    }

}
