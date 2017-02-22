package com.xfrocks.api.androiddemo.discussion;

import com.xfrocks.api.androiddemo.Api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadActivity extends DiscussionActivity {

    static final Pattern patternUrl = Pattern.compile("(index\\.php\\?|/)threads/(\\d+)/");

    public static int getThreadIdFromUrl(String url) {
        Matcher m = patternUrl.matcher(url);
        if (m.find()) {
            String threadId = m.group(2);
            return Integer.parseInt(threadId);
        }

        return 0;
    }

    @Override
    void setDiscussion(int discussionId) {
        setDiscussion(Api.makeThread(discussionId));
    }

    @Override
    PostMessageRequest newPostMessageRequest() {
        return new PostPostRequest();
    }

    @Override
    void parseResponseForMessages(JSONObject response, ParsedMessageHandler handler) {
        if (!response.has("posts")) {
            return;
        }

        try {
            JSONArray posts = response.getJSONArray("posts");
            for (int i = 0, l = posts.length(); i < l; i++) {
                JSONObject postJson = posts.getJSONObject(i);
                Api.Post post = Api.makePost(postJson);
                if (post == null) {
                    return;
                }

                if (!handler.onMessage(post)) {
                    return;
                }
            }
        } catch (JSONException e) {
            // ignore
        }
    }

    @Override
    void parseResponseForDiscussionThenSet(JSONObject response) {
        if (!response.has("thread")) {
            return;
        }

        try {
            JSONObject threadJson = response.getJSONObject("thread");
            Api.Thread thread = Api.makeThread(threadJson);
            if (thread == null) {
                return;
            }

            setDiscussion(thread);
        } catch (JSONException e) {
            // ignore
        }
    }

    class PostPostRequest extends PostMessageRequest {
        @Override
        Api.DiscussionMessage makeInTransitMessage(String bodyPlainText) {
            return Api.makePost(mUser, bodyPlainText);
        }
    }
}
