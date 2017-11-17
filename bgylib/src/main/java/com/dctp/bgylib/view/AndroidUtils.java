package com.dctp.bgylib.view;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class AndroidUtils {
    public static int getPhoneDensityDpiChange(Context context){
        int change = 0;
        DisplayMetrics metric = new DisplayMetrics();
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metric);
        int densityDpi = metric.densityDpi;
        if (densityDpi >= 480) {
            change = 15;
        } else if (densityDpi >= 320) {
            change = 10;
        } else if (densityDpi >= 240) {
            change = 8;
        } else {
            change = 5;
        }
        return change;
    }
}