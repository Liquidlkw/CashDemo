package com.example.cashdemo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static final boolean DEBUG = true;

    private static final String PATH = Environment.getExternalStorageDirectory().getPath() + File.separator + "log" + File.separator;
    private static final String FILE_NAME = "crash";
    private static final String FILE_NAME_SUFFIX = ".txt";
    private static CrashHandler sInstance = new CrashHandler();
    private UncaughtExceptionHandler mDefaultCrashHandler;
    private Context mContext;


    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return sInstance;
    }

    public void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        mContext = context.getApplicationContext();
    }


    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        dumpExceptionToSDCard(e);
        uploadExceptionToServer();
        e.printStackTrace();

        //如果系统提供了默认的异常处理器则交给系统去结束进程 否则就自己结束进程
        if (mDefaultCrashHandler != null) {
            mDefaultCrashHandler.uncaughtException(t, e);
        } else {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    private void dumpExceptionToSDCard(Throwable e) {
        //如果SD卡不存在或无法使用，则不写入 直接return
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (DEBUG) {
                Log.w(TAG, "sdCard unmounted,skip dump exception");
            }
        }

        File dir = new File(PATH);
        Log.d(TAG, "dumpExceptionToSDCard: " + PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        long current = System.currentTimeMillis();
        String time = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(current);
        File file = new File(PATH + FILE_NAME + FILE_NAME_SUFFIX);

        try {
            //此处会创建文件并可以写入文件
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            pw.println(time);
            dumpPhoneInfo(pw);
            pw.println();
            e.printStackTrace();
            pw.close();
        } catch (IOException ioException) {
            ioException.printStackTrace();
            Log.e(TAG, "dump crash info failed");
        }

    }

    //转储发生异常的机型
    private void dumpPhoneInfo(PrintWriter pw) {
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            //app版本信息
            pw.print("App Version：");
            pw.print(packageInfo.versionName);
            pw.print('-');
            pw.println(packageInfo.versionCode);
            //Android 版本号
            pw.print("OS Version：");
            pw.print(Build.VERSION.RELEASE);
            pw.print('-');
            pw.println(Build.VERSION.SDK_INT);
            //手机制造商
            pw.print("Vendor: ");
            pw.println(Build.MANUFACTURER);
            //手机型号
            pw.print("Model: ");
            pw.println(Build.MODEL);
            //cpu架构
            pw.print("CPU ABI: ");
            pw.println(Build.SUPPORTED_ABIS);


        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    //上传异常到您的服务器
    private void uploadExceptionToServer() {
        //TODO Upload Exception Message to Server
    }


}
