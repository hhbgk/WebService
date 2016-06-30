package com.hhbgk.webservice.discovery.util;

import android.util.Log;

/**
 * Author: bob
 * Date: 16-6-28 10:03
 * Version: V1
 * Description:
 */
public class Dbug {
    public static boolean ENABLE_DEBUG = true;

    /*public static void v(String tag, String msg) {
        if(ENABLE_DEBUG){
            Log.v(tag, msg);
        }
    }*/
    public static void d(String tag, String msg) {
        if(ENABLE_DEBUG){
            Log.d(tag, msg);
        }
    }
    public static void i(String tag, String msg) {
        if(ENABLE_DEBUG){
            Log.i(tag, msg);
        }
    }
    public static void w(String tag, String msg) {
        if(ENABLE_DEBUG){
            Log.w(tag, msg);
        }
    }
    public static void e(String tag, String msg) {
        if(ENABLE_DEBUG){
            Log.e(tag, msg);
        }
    }
}
