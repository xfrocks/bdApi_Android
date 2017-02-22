package com.xfrocks.api.androiddemo.persist;

import android.content.Context;

import com.xfrocks.api.androiddemo.BuildConfig;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ObjectAsFile {

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String LATEST_DISCUSSION = "latestDiscussion";

    public static void save(Context context, String fileName, Serializable obj) {
        if (obj == null) {
            context.deleteFile(fileName);
            return;
        }

        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);

            os.writeObject(obj);
            os.close();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public static Serializable load(Context context, String fileName) {
        Serializable obj = null;

        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream is = new ObjectInputStream(fis);

            obj = (Serializable) is.readObject();
            is.close();
        } catch (FileNotFoundException e1) {
            // ignore
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }

        return obj;
    }

}
