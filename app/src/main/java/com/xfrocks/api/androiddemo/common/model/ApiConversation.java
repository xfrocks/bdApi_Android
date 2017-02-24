package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;
import com.xfrocks.api.androiddemo.common.Api;
import com.xfrocks.api.androiddemo.common.ApiConstants;

public class ApiConversation extends ApiDiscussion {

    public static ApiConversation incompleteWithId(final int id) {
        return new ApiConversation() {
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

    @SerializedName("conversation_id")
    Integer mId;

    @SerializedName("conversation_title")
    String mTitle;

    @SerializedName("creator_username")
    String mCreatorName;

    @SerializedName("conversation_create_date")
    Integer mCreateDate;

    @SerializedName("first_message")
    ApiConversationMessage mFirstMessage;

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
        return null;
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
        return checkPermission("reply");
    }

    @Override
    public boolean canUploadAttachment() {
        return checkPermission("upload_attachment");
    }

    @Override
    public String getGetMessagesUrl() {
        return ApiConstants.URL_CONVERSATION_MESSAGES;
    }

    @Override
    public Api.Params getGetMessagesParams(int page, ApiAccessToken accessToken) {
        String fieldsInclude = null;
        if (page < 2) {
            fieldsInclude = "conversation";
        }

        return new Api.Params(accessToken)
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_CONVERSATION_ID, getId())
                .and(ApiConstants.PARAM_PAGE, page)
                .and(ApiConstants.PARAM_ORDER, ApiConstants.URL_CONVERSATION_MESSAGES_ORDER_REVERSE)
                .andFieldsInclude(ApiConversationMessage.class, fieldsInclude);
    }

    @Override
    public String getPostAttachmentsUrl(String attachmentHash, ApiAccessToken accessToken) {
        return Api.makeAttachmentsUrl(ApiConstants.URL_CONVERSATIONS_ATTACHMENTS, attachmentHash, accessToken);
    }

    @Override
    public String getPostMessagesUrl() {
        return ApiConstants.URL_CONVERSATION_MESSAGES;
    }

    @Override
    public Api.Params getPostMessagesParams(String bodyPlainText, String attachmentHash, ApiAccessToken accessToken) {
        return new Api.Params(accessToken)
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_CONVERSATION_ID, getId())
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_MESSAGE_BODY, bodyPlainText)
                .and(ApiConstants.URL_CONVERSATION_MESSAGES_PARAM_ATTACHMENT_HASH, attachmentHash)
                .and(ApiConstants.PARAM_FIELDS_INCLUDE, "message_id");
    }
}
