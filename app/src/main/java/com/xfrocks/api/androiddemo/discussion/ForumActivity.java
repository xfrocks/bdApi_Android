package com.xfrocks.api.androiddemo.discussion;

import com.xfrocks.api.androiddemo.Api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForumActivity extends DiscussionListActivity {

    static final Pattern patternUrl = Pattern.compile("(index\\.php\\?|/)forums/(\\d+)/");

    public static int getForumIdFromUrl(String url) {
        Matcher m = patternUrl.matcher(url);
        if (m.find()) {
            String forumId = m.group(2);
            return Integer.parseInt(forumId);
        }

        return 0;
    }

    @Override
    String getGetDiscussionsUrl() {
        return Api.URL_THREADS;
    }

    @Override
    Api.Params getGetDiscussionsParams(int page, Api.AccessToken accessToken) {
        return new Api.Params(accessToken)
                .and(Api.PARAM_PAGE, page)
                .and(Api.URL_THREADS_PARAM_FORUM_ID, mDiscussionContainerId)
                .andIf(page > 1, "fields_exclude", "forum");
    }

    @Override
    void parseResponseForDiscussions(JSONObject response, ParsedDiscussionHandler handler) {
        if (!response.has("threads")) {
            return;
        }

        try {
            JSONArray threads = response.getJSONArray("threads");
            for (int i = 0, l = threads.length(); i < l; i++) {
                JSONObject threadJson = threads.getJSONObject(i);
                Api.Thread thread = Api.makeThread(threadJson);
                if (thread == null) {
                    return;
                }

                if (!handler.onDiscussion(thread)) {
                    return;
                }
            }
        } catch (JSONException e) {
            // ignore
        }
    }
}
