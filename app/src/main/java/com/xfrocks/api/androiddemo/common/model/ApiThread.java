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

    @SerializedName("links")
    Links mLinks;

    @SerializedName("permissions")
    Permissions mPermissions;

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
        if (mLinks == null) {
            return null;
        }

        return mLinks.mCreatorAvatar;
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
    public String getPermalink() {
        if (mLinks == null) {
            return null;
        }

        return mLinks.mPermalink;
    }

    @Override
    public boolean canPostMessage() {
        if (mPermissions == null) {
            return false;
        }

        Boolean permission = mPermissions.mPostMessage;
        if (permission == null) {
            return false;
        }

        return permission;
    }

    @Override
    public boolean canUploadAttachment() {
        if (mPermissions == null) {
            return false;
        }

        Boolean permission = mPermissions.mUploadAttachment;
        if (permission == null) {
            return false;
        }

        return permission;
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

    @SuppressWarnings("unused")
    static class Links extends ApiModel {
        @SerializedName("first_poster_avatar")
        String mCreatorAvatar;

        @SerializedName("permalink")
        String mPermalink;
    }

    @SuppressWarnings("unused")
    static class Permissions extends ApiModel {
        @SerializedName("post")
        Boolean mPostMessage;

        @SerializedName("upload_attachment")
        Boolean mUploadAttachment;
    }
}
