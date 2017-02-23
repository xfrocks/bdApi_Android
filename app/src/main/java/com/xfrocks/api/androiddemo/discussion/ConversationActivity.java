package com.xfrocks.api.androiddemo.discussion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.google.gson.annotations.SerializedName;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.common.ApiBaseResponse;
import com.xfrocks.api.androiddemo.common.model.ApiConversation;
import com.xfrocks.api.androiddemo.common.model.ApiConversationMessage;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussion;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussionMessage;
import com.xfrocks.api.androiddemo.gcm.ChatOrNotifReceiver;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversationActivity extends DiscussionActivity {

    private static final Pattern patternUrl = Pattern.compile("(index\\.php\\?|/)conversations/(\\d+)/");

    public static int getConversationIdFromUrl(String url) {
        Matcher m = patternUrl.matcher(url);
        if (m.find()) {
            String conversationId = m.group(2);
            return Integer.parseInt(conversationId);
        }

        return 0;
    }

    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int conversationId = getDiscussionId();
                if (conversationId == 0
                        || !ChatOrNotifReceiver.ACTION.equals(intent.getAction())
                        || ChatOrNotifReceiver.getConversationId(intent) != conversationId) {
                    return;
                }

                int messageId = ChatOrNotifReceiver.getMessageId(intent);
                ApiDiscussionMessage latestMessage = mAdapter.getMessageAt0();
                if (latestMessage != null
                        && latestMessage.getId() > messageId) {
                    // this notification appeared to arrive a little too late
                    return;
                }

                // this broadcast is for new message in this conversation
                // process it now
                new PatchRequest().start();

                abortBroadcast();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mBroadcastReceiver, new IntentFilter(ChatOrNotifReceiver.ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }

    @Override
    void setDiscussion(int discussionId) {
        setDiscussion(ApiConversation.incompleteWithId(discussionId));
    }

    @Override
    ParsedMessages parseResponseForMessages(String response) {
        return App.getGsonInstance().fromJson(response, ConversationMessagesResponse.class);
    }

    static class ConversationMessagesResponse extends ApiBaseResponse implements ParsedMessages {
        @SerializedName("messages")
        List<ApiConversationMessage> messages;

        @SerializedName("conversation")
        ApiConversation conversation;

        @Override
        public List<? extends ApiDiscussionMessage> getMessages() {
            return messages;
        }

        @Override
        public ApiDiscussion getDiscussion() {
            return conversation;
        }

        @Override
        public Integer getPage() {
            Links links = getLinks();
            if (links == null) {
                return null;
            }

            return links.getPage();
        }

        @Override
        public Integer getPages() {
            Links links = getLinks();
            if (links == null) {
                return null;
            }

            return links.getPages();
        }
    }
}
