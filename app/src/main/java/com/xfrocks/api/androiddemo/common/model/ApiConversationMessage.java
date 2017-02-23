package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;

public class ApiConversationMessage extends ApiDiscussionMessage {

    @SerializedName("message_id")
    private Integer mId;

    @SerializedName("creator_user_id")
    private Integer mCreatorUserId;

    @SerializedName("creator_username")
    private String mCreatorName;

    @SerializedName("message_create_date")
    private Integer mCreateDate;

    @SerializedName("message_body_plain_text")
    private String mBodyPlainText;

    @Override
    public Integer getId() {
        return mId;
    }

    @Override
    public Integer getCreatorUserId() {
        return mCreatorUserId;
    }

    @Override
    public String getCreatorName() {
        return mCreatorName;
    }

    @Override
    public String getCreatorAvatar() {
        return getLink("creator_avatar");
    }

    @Override
    public Integer getCreateDate() {
        return mCreateDate;
    }

    @Override
    public String getBodyPlainText() {
        return mBodyPlainText;
    }
}
