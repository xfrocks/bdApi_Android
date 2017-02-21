package com.xfrocks.api.androiddemo.discussion;

import com.xfrocks.api.androiddemo.Api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
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
    MessagesRequest newMessagesRequest(int page) {
        return new PostsRequest(page);
    }

    @Override
    PatchRequest newPatchRequest() {
        return new ThreadPatchRequest();
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

    @Override
    String makeAttachmentsUrl() {
        String url = Api.makeAttachmentsUrl(Api.URL_POSTS_ATTACHMENTS, getAttachmentHash(), mAccessToken);
        url += String.format(Locale.US, "&thread_id=%d", getDiscussionId());

        return url;
    }

    class PostsRequest extends MessagesRequest {

        public PostsRequest(int page) {
            super(Api.URL_POSTS, new Api.Params(mAccessToken)
                    .and(Api.URL_POSTS_PARAM_THREAD_ID, getDiscussionId())
                    .and(Api.PARAM_PAGE, page)
                    .and(Api.PARAM_ORDER, Api.URL_POSTS_ORDER_REVERSE)
                    .andIf(page > 1, "fields_exclude", "thread"));
        }
    }

    class ThreadPatchRequest extends PatchRequest {
        public ThreadPatchRequest() {
            super(Api.URL_POSTS, new Api.Params(mAccessToken)
                    .and(Api.URL_POSTS_PARAM_THREAD_ID, getDiscussionId())
                    .and(Api.PARAM_ORDER, Api.URL_POSTS_ORDER_REVERSE)
                    .and("fields_exclude", "thread"));
        }
    }

    class PostPostRequest extends PostMessageRequest {
        public PostPostRequest() {
            super(Api.URL_POSTS, new Api.Params(mAccessToken)
                    .and(Api.URL_POSTS_PARAM_THREAD_ID, getDiscussionId())
                    .and(Api.URL_POSTS_PARAM_POST_BODY, mQuickReply.getPendingMessage())
                    .and(Api.URL_POSTS_PARAM_ATTACHMENT_HASH, getAttachmentHash())
                    .and("fields_include", "post_id"));

            mInTransitMessage = Api.makePost(mUser, mQuickReply.getPendingMessage());
        }
    }
}
