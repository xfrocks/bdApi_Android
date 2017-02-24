package com.xfrocks.api.androiddemo.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class AndroidPermissions {
    // https://gist.github.com/gotev/67c300c563bdf68a502c

    private final Context mContext;
    private final String[] mRequiredPermissions;
    private final ArrayList<String> mPermissionsToRequest = new ArrayList<>();

    public AndroidPermissions(Context context, String... requiredPermissions) {
        mContext = context;
        mRequiredPermissions = requiredPermissions;
    }

    public boolean needRequesting() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return false;
        }

        mPermissionsToRequest.clear();

        for (String permission : mRequiredPermissions) {
            if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToRequest.add(permission);
            }
        }

        return !mPermissionsToRequest.isEmpty();

    }

    public void requestPermissions(Fragment fragment, int requestCode) {
        String[] permissions = mPermissionsToRequest.toArray(new String[mPermissionsToRequest.size()]);
        fragment.requestPermissions(permissions, requestCode);
    }

    public boolean areAllRequiredPermissionsGranted(String[] permissions, int[] grantResults) {
        if (permissions == null || permissions.length == 0
                || grantResults == null || grantResults.length == 0) {
            return false;
        }

        LinkedHashMap<String, Integer> perms = new LinkedHashMap<>();

        for (int i = 0; i < permissions.length; i++) {
            if (!perms.containsKey(permissions[i])
                    || (perms.containsKey(permissions[i]) && perms.get(permissions[i]) == PackageManager.PERMISSION_DENIED))
                perms.put(permissions[i], grantResults[i]);
        }

        for (Map.Entry<String, Integer> entry : perms.entrySet()) {
            if (entry.getValue() != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }
}
