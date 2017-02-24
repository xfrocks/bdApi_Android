package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;
import com.xfrocks.api.androiddemo.common.Api;
import com.xfrocks.api.androiddemo.common.ApiConstants;

import java.util.Locale;

public class ApiThread extends ApiDiscussion {

    public static ApiThread incompleteWithId(final int id) {
        return new ApiThread() {
            @Override
            public Integer getId() {
                return id;
            }

            @Override
            public boolean isComplete() {
                return false;
            }
        };
    }

    @SerializedName("thread_id")
    Integer mId;

    @SerializedName("thread_title")
    String mTitle;

    @SerializedName("creator_username")
    String mCreatorName;

    @SerializedName("thread_create_date")
    Integer mCreateDate;

    @SerializedName("first_post")
    ApiPost mFirstMessage;

    @Override
    public Integer getId() {
        return mId;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getCreatorName() {
        return mCreatorName;
    }

    @Override
    public String getCreatorAvatar() {
        return getLink("first_poster_avatar");
    }

    @Override
    public Integer getCreateDate() {
        return mCreateDate;
    }

    @Override
    public ApiDiscussionMessage getFirstMessage() {
        return mFirstMessage;
    }

    @Override
    public boolean canPostMessage() {
        return checkPermission("post");
    }

    @Override
    public boolean canUploadAttachment() {
        return checkPermission("upload_attachment");
    }

    @Override
    public String getGetMessagesUrl() {
        return ApiConstants.URL_POSTS;
    }

    @Override
    public Api.Params getGetMessagesParams(int page, ApiAccessToken accessToken) {
        String fieldsInclude = null;
        if (page < 2) {
            fieldsInclude = "thread";
        }

        return new Api.Params(accessToken)
                .and(ApiConstants.URL_POSTS_PARAM_THREAD_ID, getId())
                .and(ApiConstants.PARAM_PAGE, page)
                .and(ApiConstants.PARAM_ORDER, ApiConstants.URL_POSTS_ORDER_REVERSE)
                .andFieldsInclude(ApiPost.class, fieldsInclude);
    }

    @Override
    public String getPostAttachmentsUrl(String attachmentHash, ApiAccessToken accessToken) {
        String url = Api.makeAttachmentsUrl(ApiConstants.URL_POSTS_ATTACHMENTS, attachmentHash, accessToken);
        url += String.format(Locale.US, "&thread_id=%d", getId());

        return url;
    }

    @Override
    public String getPostMessagesUrl() {
        return ApiConstants.URL_POSTS;
    }

    @Override
    public Api.Params getPostMessagesParams(String bodyPlainText, String attachmentHash, ApiAccessToken accessToken) {
        return new Api.Params(accessToken)
                .and(ApiConstants.URL_POSTS_PARAM_THREAD_ID, getId())
                .and(ApiConstants.URL_POSTS_PARAM_POST_BODY, bodyPlainText)
                .and(ApiConstants.URL_POSTS_PARAM_ATTACHMENT_HASH, attachmentHash)
                .and(ApiConstants.PARAM_FIELDS_INCLUDE, "post_id");
    }
}
