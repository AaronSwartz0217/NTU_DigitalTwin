package com.ntu.qidong.digitaltwin.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

/**
 * 网络工具类
 * 提供网络状态检查和 Toast 提示功能
 */
public class NetworkUtils {

    /**
     * 检查网络是否可用
     *
     * @param context 上下文
     * @return true if network is available
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }

        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) {
            return false;
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * 显示网络错误 Toast
     *
     * @param context 上下文
     */
    public static void showNetworkError(Context context) {
        if (context == null) {
            return;
        }
        Toast.makeText(context, "网络异常，请检查网络连接", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示一般错误 Toast
     *
     * @param context 上下文
     * @param message 错误信息
     */
    public static void showError(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示成功 Toast
     *
     * @param context 上下文
     * @param message 成功信息
     */
    public static void showSuccess(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
