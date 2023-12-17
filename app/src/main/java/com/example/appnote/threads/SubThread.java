package com.example.appnote.threads;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubThread {
    public static final ExecutorService CreateSubThread =
            Executors.newFixedThreadPool(4);

    public static boolean runSubThread(Context context, Runnable action){
        boolean isnetworking = isNetworkAvailable(context);
        if(isnetworking){
            SubThread.CreateSubThread.execute(action);
            return true;
        }else {
            Toast.makeText(context,"Vui long ket noi mang",Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        }
        return false;
    }
}
