package com.example.appnote.database;

import android.content.Context;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubThread {
    public static final ExecutorService CreateSubThread =
            Executors.newFixedThreadPool(4);

    public static boolean checkNetworking (Context context, Runnable action){
        Boolean isnetworking = true;
        if(isnetworking){
            SubThread.CreateSubThread.execute(action);
            return true;
        }else {
            Toast.makeText(context,"Vui long ket noi mang",Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
