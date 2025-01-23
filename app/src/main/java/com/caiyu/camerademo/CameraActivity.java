package com.caiyu.camerademo;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.video.Recording;
import androidx.camera.view.PreviewView;

import com.google.android.material.button.MaterialButton;
import com.monke.mopermission.MoPermission;
import com.monke.mopermission.MoPermissionDialog;
import com.monke.mopermission.OnRequestNecessaryPermissionListener;
import com.caiyu.camerademo.camera.CameraManager;
import com.caiyu.camerademo.camera.RecordingCallback;
import com.caiyu.camerademo.utils.ToastUtil;

import java.util.List;

public class CameraActivity extends AppCompatActivity {
    private PreviewView previewView;
    private MaterialButton previewButton;
    private MaterialButton recordButton;
    private MaterialButton endButton;
    private MaterialButton switchButton;
    private Recording recording;
    private boolean isStartRecording = false;
    private boolean isRecording = false;
    private boolean isPreviewShowing = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);
        init();
    }

    private void init() {
        previewView = findViewById(R.id.previewView);
        previewButton = findViewById(R.id.previewButton);
        recordButton = findViewById(R.id.recordButton);
        endButton = findViewById(R.id.record_end_button);
        switchButton = findViewById(R.id.record_switch_button);

        MoPermission.Companion.requestNecessaryPermission(this, "权限申请", "获取摄像头与存储权限", "申请", "退出", new OnRequestNecessaryPermissionListener() {
            @Override
            public void success(@NonNull List<String> permissions) {
                ToastUtil.showToast(CameraActivity.this, "已获取摄像头权限");
                CameraManager.getInstance().init(CameraActivity.this, previewView);
                CameraManager.getInstance().addRecordingCallback(new RecordingCallback() {
                    @Override
                    public void onInit() {
                        bindClickMethod();
                    }

                    @Override
                    public void onStart() {
                        isStartRecording = true;
                        isRecording = true;
                        recordButton.setText("暂停");
                        switchButton.setVisibility(View.GONE);
                        endButton.setVisibility(View.VISIBLE);
                        ToastUtil.showToast(CameraActivity.this, "录制开始");
                    }

                    @Override
                    public void onPause() {
                        recordButton.setText("继续");
                        isStartRecording = true;
                        isRecording = false;
                        ToastUtil.showToast(CameraActivity.this, "录制暂停");
                    }

                    @Override
                    public void onResume() {
                        isStartRecording = true;
                        isRecording = true;
                        recordButton.setText("暂停");
                        ToastUtil.showToast(CameraActivity.this, "录制继续");
                    }

                    @Override
                    public void onStop() {
                        isRecording = false;
                        isStartRecording = false;
                        recordButton.setText("录制");
                        switchButton.setVisibility(View.VISIBLE);
                        endButton.setVisibility(View.GONE);
                        ToastUtil.showToast(CameraActivity.this, "录制结束");
                    }

                    @Override
                    public void onError() {
                        ToastUtil.showToast(CameraActivity.this, "录制失败");
                    }
                });

            }

            @Override
            public void fail(@NonNull List<String> permissions) {
                ToastUtil.showToast(CameraActivity.this, "未获取摄像头权限！可以关闭相关功能");
            }
        }, MoPermissionDialog.class, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO);
    }

    private void bindClickMethod() {
        switchButton.setOnClickListener(v -> CameraManager.getInstance().switchCamera());
        previewButton.setOnClickListener(v -> {
            if (isPreviewShowing) {
                CameraManager.getInstance().unbindPreview();
                previewView.setVisibility(View.INVISIBLE);
                isPreviewShowing = false;
            } else {
                CameraManager.getInstance().bindPreview();
                previewView.setVisibility(View.VISIBLE);
                isPreviewShowing = true;
            }
        });
        recordButton.setOnClickListener(v -> {
            if (!isStartRecording && !isRecording) {
                recording = CameraManager.getInstance().startRecord(null);
            } else if (recording != null && isStartRecording && isRecording) {
                recording.pause();
            } else if (recording != null && isStartRecording && !isRecording) {
                recording.resume();
            }
        });
        endButton.setOnClickListener(v -> {
            if (recording != null) {
                recording.stop();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraManager.getInstance().release();
    }
}