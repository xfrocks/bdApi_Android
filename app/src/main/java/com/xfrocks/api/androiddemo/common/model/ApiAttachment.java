package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;

public class ApiAttachment extends ApiModel {

    @SerializedName("attachment_id")
    private Integer mId;

    @SerializedName("filename")
    private String mFileName;

    @Override
    public Integer getId() {
        return mId;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getThumbnail() {
        return getLink("thumbnail");
    }

}
