package com.xfrocks.api.androiddemo.common.model;

import android.support.annotation.NonNull;

public class ApiDiscussionMessageInTransit extends ApiDiscussionMessage {

    public String errorMessage;
    private final ApiUser mUser;
    private final String mBodyPlainText;

    public ApiDiscussionMessageInTransit(@NonNull ApiUser user, String bodyPlainText) {
        this.mUser = user;
        this.mBodyPlainText = bodyPlainText;
    }

    @Override
    public Integer getId() {
        return null;
    }

    @Override
    public Integer getCreatorUserId() {
        return mUser.getId();
    }

    @Override
    public String getCreatorName() {
        return mUser.getUsername();
    }

    @Override
    public String getCreatorAvatar() {
        return mUser.getAvatar();
    }

    @Override
    public Integer getCreateDate() {
        return null;
    }

    @Override
    public String getBodyPlainText() {
        return mBodyPlainText;
    }

    @Override
    public boolean isComplete() {
        return false;
    }
}
