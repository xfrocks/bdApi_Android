package com.xfrocks.api.androiddemo.common.model;

import com.xfrocks.api.androiddemo.common.Api;

abstract public class ApiDiscussion extends ApiModel {
    abstract public String getTitle();

    abstract public String getCreatorName();

    abstract public String getCreatorAvatar();

    abstract public Integer getCreateDate();

    abstract public ApiDiscussionMessage getFirstMessage();

    abstract public boolean canPostMessage();

    abstract public boolean canUploadAttachment();

    abstract public String getGetMessagesUrl();

    abstract public Api.Params getGetMessagesParams(int page, ApiAccessToken accessToken);

    abstract public String getPostAttachmentsUrl(String attachmentHash, ApiAccessToken accessToken);

    abstract public String getPostMessagesUrl();

    abstract public Api.Params getPostMessagesParams(String bodyPlainText, String attachmentHash, ApiAccessToken accessToken);
}
