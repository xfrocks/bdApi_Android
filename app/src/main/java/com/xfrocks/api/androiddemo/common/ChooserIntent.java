package com.xfrocks.api.androiddemo.common;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import com.xfrocks.api.androiddemo.BuildConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChooserIntent {

    public static Intent create(Context context, int titleResId, String type) {
        Intent pickIntent = new Intent();
        pickIntent.setType(type);
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);

        Intent chooserIntent = Intent.createChooser(pickIntent, context.getString(titleResId));
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, buildCameraIntents(context));

        return chooserIntent;
    }

    public static Intent[] buildCameraIntents(Context context) {
        List<Intent> cameraIntents = new ArrayList<>();
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for (ResolveInfo res : listCam) {
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(res.activityInfo.packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, getFileProviderUri(context, getCameraFile(context)));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            cameraIntents.add(intent);
        }

        return cameraIntents.toArray(new Intent[cameraIntents.size()]);
    }

    public static Uri getUriFromChooser(Context context, Intent data) {
        Uri uri = null;
        if (data != null) {
            uri = data.getData();
        }
        if (uri == null) {
            uri = Uri.fromFile(getCameraFile(context));
        }

        return uri;
    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        if ("file".equals(uri.getScheme())) {
            List<String> pathSegments = uri.getPathSegments();
            return pathSegments.get(pathSegments.size() - 1);
        }

        ContentResolver cr = context.getContentResolver();
        String mimeType = cr.getType(uri);
        if (mimeType != null) {
            if (mimeType.startsWith("image/")) {
                // quick hack for images
                return mimeType.replace('/', '.');
            } else {
                String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
                Cursor metaCursor = cr.query(uri, projection, null, null, null);
                if (metaCursor != null) {
                    try {
                        if (metaCursor.moveToFirst()) {
                            return metaCursor.getString(0);
                        }
                    } finally {
                        metaCursor.close();
                    }
                }
            }
        }

        return "file.bin";
    }

    private static File getCameraFile(Context context) {
        return new File(context.getExternalCacheDir(), "camera.jpg");
    }

    private static Uri getFileProviderUri(Context context, File file) {
        // https://developer.android.com/training/camera/photobasics.html
        String authority = BuildConfig.APPLICATION_ID + ".file_provider";
        return FileProvider.getUriForFile(context, authority, file);
    }
}
