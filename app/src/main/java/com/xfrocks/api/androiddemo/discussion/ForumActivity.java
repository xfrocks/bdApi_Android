package com.xfrocks.api.androiddemo.discussion;

import com.google.gson.annotations.SerializedName;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.common.Api;
import com.xfrocks.api.androiddemo.common.ApiBaseResponse;
import com.xfrocks.api.androiddemo.common.ApiConstants;
import com.xfrocks.api.androiddemo.common.model.ApiAccessToken;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussion;
import com.xfrocks.api.androiddemo.common.model.ApiThread;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForumActivity extends DiscussionListActivity {

    private static final Pattern patternUrl = Pattern.compile("(index\\.php\\?|/)forums/(\\d+)/");

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
        return ApiConstants.URL_THREADS;
    }

    @Override
    Api.Params getGetDiscussionsParams(int page, ApiAccessToken accessToken) {
        return new Api.Params(accessToken)
                .and(ApiConstants.PARAM_PAGE, page)
                .and(ApiConstants.URL_THREADS_PARAM_FORUM_ID, mDiscussionContainerId)
                .andIf(page > 1, "fields_exclude", "forum");
    }

    @Override
    ParsedDiscussions parseResponseForDiscussions(String response) {
        return App.getGsonInstance().fromJson(response, ThreadsResponse.class);
    }

    static class ThreadsResponse extends ApiBaseResponse implements ParsedDiscussions {
        @SerializedName("threads")
        List<ApiThread> threads;

        @Override
        public List<? extends ApiDiscussion> getDiscussions() {
            return threads;
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
