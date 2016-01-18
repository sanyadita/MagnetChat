package com.magnet.imessage.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class InternetConnection {

    private static InternetConnection instance;

    private ConnectivityManager connectivityManager;

    private InternetConnection(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static InternetConnection getInstance(Context context) {
        if (instance == null) {
            instance = new InternetConnection(context);
        }
        return instance;
    }

    public static InternetConnection getInstance() {
        if (instance == null) {
            throw new Error("No context for instance");
        }
        return instance;
    }

    public boolean isAnyConnectionAvailable() {
        return (isWiFiAvailable() || isMobileInternetAvailable());
    }

    public boolean isMobileInternetAvailable() {
        return isSomeConnectionAvailable(ConnectivityManager.TYPE_MOBILE);
    }

    public boolean isWiFiAvailable() {
        return isSomeConnectionAvailable(ConnectivityManager.TYPE_WIFI);
    }

    private boolean isSomeConnectionAvailable(int type) {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getType() == type;
    }

}
