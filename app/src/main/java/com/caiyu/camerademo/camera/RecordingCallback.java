package com.caiyu.camerademo.camera;

public interface RecordingCallback {
    public void onInit();
    public void onStart();
    public void onPause();
    public void onResume();
    public void onStop();

    public void onError();
}