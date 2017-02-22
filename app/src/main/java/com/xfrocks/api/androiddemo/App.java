package com.xfrocks.api.androiddemo;

import android.content.Context;
import android.net.Uri;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.xfrocks.api.androiddemo.helper.PubKeyManager;

import net.gotev.uploadservice.Logger;
import net.gotev.uploadservice.UploadService;

import java.io.File;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class App extends MultiDexApplication {

    private static App sInstance;

    private RequestQueue mRequestQueue;

    @Override
    public void onCreate() {
        super.onCreate();

        String publicKey = BuildConfig.PUBLIC_KEY;
        HttpStack httpStack = null;
        if (!TextUtils.isEmpty(publicKey)) {
            try {
                TrustManager tm[] = {new PubKeyManager(publicKey)};
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, tm, null);
                SSLSocketFactory pinnedSSLSocketFactory = context.getSocketFactory();
                httpStack = new HurlStack(null, pinnedSSLSocketFactory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mRequestQueue = Volley.newRequestQueue(getApplicationContext(), httpStack);

        UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
        if (BuildConfig.DEBUG) {
            Logger.setLogLevel(Logger.LogLevel.DEBUG);
        }

        sInstance = this;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    public synchronized static App getInstance() {
        return sInstance;
    }

    public static Uri getTempForCamera(Context context) {
        return Uri.fromFile(new File(context.getExternalCacheDir(), "camera.jpg"));
    }

    public static boolean getFeatureConfirmSignInWithRemember() {
        return BuildConfig.FEATURE_CONFIRM_SIGN_IN_WITH_REMEMBER > 0;
    }

    public static int getFeatureAttachmentResize() {
        return BuildConfig.FEATURE_ATTACHMENT_RESIZE;
    }
}
