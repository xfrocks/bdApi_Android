package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;

import java.util.Iterator;
import java.util.List;

abstract public class ApiDiscussionMessage extends ApiModel {

    @SerializedName("attachments")
    private List<ApiAttachment> mAttachments;

    abstract public Integer getCreatorUserId();

    abstract public String getCreatorName();

    abstract public String getCreatorAvatar();

    abstract public Integer getCreateDate();

    abstract public String getBodyPlainText();

    public Iterator<ApiAttachment> getAttachmentsIterator() {
        if (mAttachments == null) {
            return null;
        }

        return mAttachments.iterator();
    }
}
