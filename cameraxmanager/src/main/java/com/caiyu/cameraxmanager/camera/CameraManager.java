package com.caiyu.cameraxmanager.camera;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static final class InstanceHolder {
        @SuppressLint("StaticFieldLeak")
        private static final CameraManager instance = new CameraManager();
    }
    public static CameraManager getInstance() {
        return InstanceHolder.instance;
    }
    @GuardedBy("this")
    private Context mContext;
    @GuardedBy("this")
    private PreviewView mPreviewView;
    @GuardedBy("this")
    private Preview mPreview;
    @GuardedBy("this")
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    @GuardedBy("this")
    private ProcessCameraProvider cameraProvider;
    @GuardedBy("this")
    private VideoCapture<Recorder> videoCapture;
    @GuardedBy("this")
    private CameraSelector cameraSelector;
    @GuardedBy("this")
    private Recording recording;
    @GuardedBy("this")
    private static final ArrayList<Quality> qualities = new ArrayList<>();

    @GuardedBy("this")
    private RecordingCallback callback;

    static {
        qualities.add(Quality.UHD);
        qualities.add(Quality.FHD);
        qualities.add(Quality.HD);
        qualities.add(Quality.SD);
    }

    public CameraManager() {
    }

    public synchronized void init(Context context) {
        init(context, null);
    }

    public synchronized void init(Context context, PreviewView previewView) {
        release();
        mContext = context;
        mPreviewView = previewView;
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        openDefaultCamera();
    }

    private void openDefaultCamera() {
        // 检查是否调用init()初始化
        openCheck();

        if (cameraProviderFuture != null) {
            cameraProviderFuture.cancel(true);
            cameraProviderFuture = null;
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(mContext);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (mPreviewView != null) {
                    bindDefaultPreview();
                }
                bindDefaultVideoCapture();
                callback.onInit();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "摄像组件异常： " + e);
            }
        }, ContextCompat.getMainExecutor(mContext));
    }

    private void openCheck() {
        if (mContext == null) {
            throw new NullPointerException("the param `context` is null, please call init() first");
        }
        if (cameraSelector == null) {
            throw new NullPointerException("the param `cameraSelector` is null, please call init() first");
        }
    }

    @SuppressLint("RestrictedApi")
    public void switchCamera() {
        openCheck();

        if (cameraProvider == null) {
            throw new NullPointerException("the param `cameraProvider` is null, please call openDefaultCamera() first");
        }

        cameraProvider.unbindAll();
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraSelector.getLensFacing() == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK)
                .build();

        // 重新绑定新的预览和视频录制实例
        if (mPreviewView != null) {
            unbindPreview();
            bindPreview();
        }
        unbindVideoCapture();
        bindVideoCapture();
    }

    private void bindDefaultPreview() {
        mPreview = new Preview.Builder().build();

        mPreview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        try {
            cameraProvider.bindToLifecycle((LifecycleOwner) mContext, cameraSelector, mPreview);
        } catch (Exception e) {
            Log.e(TAG, "bindPreview error:", e);
        }
    }

    public void bindPreview() {
        openCheck();

        if (mPreviewView == null | mPreview == null) {
            throw new NullPointerException("the param `mPreviewView` is null, please call init(context, previewView) first");
        }
        if (cameraProvider == null) {
            throw new NullPointerException("the param `cameraProvider` is null, please call init(context, previewView) first");
        }

        if (cameraProvider.isBound(mPreview)) {
            Log.w(TAG, "Preview has been bound, don't need to bind again");
            return;
        }
        cameraProvider.bindToLifecycle((LifecycleOwner) mContext, cameraSelector, mPreview);
    }

    public void unbindPreview() {
        if (cameraProvider != null && mPreview != null && cameraProvider.isBound(mPreview)) {
            cameraProvider.unbind(mPreview);
        }
    }

    private void bindDefaultVideoCapture() {
        QualitySelector qualitySelector = QualitySelector.fromOrderedList(qualities, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD));

        Recorder recorder = new Recorder.Builder()
                .setExecutor(ContextCompat.getMainExecutor(mContext))
                .setQualitySelector(qualitySelector)
                .build();
        videoCapture = VideoCapture.withOutput(recorder);
        try {
            cameraProvider.bindToLifecycle((LifecycleOwner) mContext, cameraSelector, videoCapture);
        } catch (Exception e) {
            Log.e(TAG, "bindVideoCapture error:", e);
        }
    }

    public void bindVideoCapture() {
        openCheck();

        if (videoCapture == null) {
            throw new NullPointerException("the param `videoCapture` is null, please call init(context) or init(context, previewView) first");
        }

        if (cameraProvider == null) {
            throw new NullPointerException("the param `cameraProvider` is null, please call init(context) or init(context, previewView) first");
        }

        if (cameraProvider.isBound(videoCapture)) {
            Log.w(TAG, "VideoCapture has been bound, don't need to bind again");
            return;
        }
        cameraProvider.bindToLifecycle((LifecycleOwner) mContext, cameraSelector, videoCapture);
    }

    public void unbindVideoCapture() {
        if (cameraProvider != null && videoCapture != null && cameraProvider.isBound(videoCapture)) {
            cameraProvider.unbind(videoCapture);
        }
    }

    public void addRecordingCallback(RecordingCallback callback) {
        this.callback = callback;
    }

    @SuppressLint("MissingPermission")
    public Recording startRecord(@Nullable String fileName) {
        openCheck();

        if (recording != null) {
            recording.stop();
            recording = null;
        }
        if (fileName == null) {
            fileName = "CameraX-recording-" + new SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(System.currentTimeMillis()) + ".mp4";
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Camera");

        MediaStoreOutputOptions mediaStoreOutput = new MediaStoreOutputOptions.Builder(mContext.getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build();

        recording = videoCapture.getOutput()
                .prepareRecording(mContext, mediaStoreOutput)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(mContext), new Consumer<VideoRecordEvent>() {
                    @Override
                    public void accept(VideoRecordEvent videoRecordEvent) {
                        if (callback != null) {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                Log.d(TAG, "recording start");
                                callback.onStart();
                            } else if (videoRecordEvent instanceof  VideoRecordEvent.Pause) {
                                Log.d(TAG, "recording pause");
                                callback.onPause();
                            } else if (videoRecordEvent instanceof VideoRecordEvent.Resume) {
                                Log.d(TAG, "recording resume");
                                callback.onResume();
                            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize){
                                Log.d(TAG, "recording stop");
                                callback.onStop();
                                VideoRecordEvent.Finalize finalizeEvent = (VideoRecordEvent.Finalize) videoRecordEvent;

                                int error = finalizeEvent.getError();
                                if (error != VideoRecordEvent.Finalize.ERROR_NONE) {
                                    Log.e(TAG, "recording error：code = " + error);
                                    callback.onError();
                                }
                            }
                        }
                    }
                });
        return recording;
    }

    public synchronized void release() {
        Log.d(TAG, "Camera release start");
        if (cameraProviderFuture != null) {
            cameraProviderFuture.cancel(true);
            cameraProviderFuture = null;
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
        cameraSelector = null;
        mPreview = null;
        mPreviewView = null;
        mContext = null;
        callback = null;
        Log.d(TAG, "Camera release end");
    }
}
