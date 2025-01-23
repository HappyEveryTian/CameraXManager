package com.caiyu.camerademo.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    private static final String TAG = "ToastUtil";
    private static Toast toast = null;

    public static void showToast(Context context, String text) {
        cancelToast();
        toast = Toast.makeText(context.getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void cancelToast() {
        if (toast != null) {
            toast.cancel();
            toast = null;
        }
    }
}
