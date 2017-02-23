package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

abstract public class ApiModel implements Serializable {

    @SerializedName("links")
    Map<String, String> mLinks;

    @SerializedName("permissions")
    Map<String, Boolean> mPermissions;

    abstract public Integer getId();

    public String getPermalink() {
        return getLink("permalink");
    }

    public boolean isComplete() {
        return true;
    }

    String getLink(String key) {
        if (mLinks == null) {
            return null;
        }

        return mLinks.get(key);
    }

    boolean checkPermission(String key) {
        if (mPermissions == null) {
            return false;
        }

        Boolean value = mPermissions.get(key);
        if (value == null) {
            return false;
        }

        return value;
    }
}
