package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;

public class ApiAttachment extends ApiModel {

    @SerializedName("attachment_id")
    private Integer mId;

    @SerializedName("filename")
    private String mFileName;

    @SerializedName("links")
    private Links mLinks;

    public Integer getId() {
        return mId;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getPermalink() {
        if (mLinks == null) {
            return null;
        }

        return mLinks.mPermalink;
    }

    public String getThumbnail() {
        if (mLinks == null) {
            return null;
        }

        return mLinks.mThumbnail;
    }

    @SuppressWarnings("unused")
    static class Links extends ApiModel {
        @SerializedName("permalink")
        String mPermalink;

        @SerializedName("thumbnail")
        String mThumbnail;
    }
}
