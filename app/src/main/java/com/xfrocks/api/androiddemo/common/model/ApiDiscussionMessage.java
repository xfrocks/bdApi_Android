package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;

abstract public class ApiDiscussionMessage extends ApiModel {

    @SerializedName("attachments")
    private ApiAttachment[] mAttachments;

    abstract public Integer getId();

    abstract public Integer getCreatorUserId();

    abstract public String getCreatorName();

    abstract public String getCreatorAvatar();

    abstract public Integer getCreateDate();

    abstract public String getBodyPlainText();

    public ApiAttachment[] getAttachments() {
        if (mAttachments == null) {
            return new ApiAttachment[0];
        }

        return mAttachments;
    }
}
