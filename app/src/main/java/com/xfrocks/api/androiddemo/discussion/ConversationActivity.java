package com.xfrocks.api.androiddemo.discussion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.xfrocks.api.androiddemo.Api;
import com.xfrocks.api.androiddemo.gcm.ChatOrNotifReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversationActivity extends DiscussionActivity {

    static final Pattern patternUrl = Pattern.compile("(index\\.php\\?|/)conversations/(\\d+)/");

    public static int getConversationIdFromUrl(String url) {
        Matcher m = patternUrl.matcher(url);
        if (m.find()) {
            String conversationId = m.group(2);
            return Integer.parseInt(conversationId);
        }

        return 0;
    }

    BroadcastReceiver mBroadcastReceiver;

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
                Api.DiscussionMessage latestMessage = mAdapter.getMessageAt0();
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
        setDiscussion(Api.makeConversation(discussionId));
    }

    @Override
    PostMessageRequest newPostMessageRequest() {
        return new PostConversationMessageRequest();
    }

    @Override
    void parseResponseForMessages(JSONObject response, ParsedMessageHandler handler) {
        if (!response.has("messages")) {
            return;
        }

        try {
            JSONArray messages = response.getJSONArray("messages");
            for (int i = 0, l = messages.length(); i < l; i++) {
                JSONObject messageJson = messages.getJSONObject(i);
                Api.ConversationMessage message = Api.makeConversationMessage(messageJson);
                if (message == null) {
                    return;
                }

                if (!handler.onMessage(message)) {
                    return;
                }
            }
        } catch (JSONException e) {
            // ignore
        }
    }

    @Override
    void parseResponseForDiscussionThenSet(JSONObject response) {
        if (!response.has("conversation")) {
            return;
        }

        try {
            JSONObject conversationJson = response.getJSONObject("conversation");
            Api.Conversation conversation = Api.makeConversation(conversationJson);
            if (conversation == null) {
                return;
            }

            setDiscussion(conversation);
        } catch (JSONException e) {
            // ignore
        }
    }

    class PostConversationMessageRequest extends PostMessageRequest {
        @Override
        Api.DiscussionMessage makeInTransitMessage(String bodyPlainText) {
            return Api.makeConversationMessage(mUser, bodyPlainText);
        }
    }
}
