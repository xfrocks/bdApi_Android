package com.xfrocks.api.androiddemo.common;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

public class ApiBaseResponse {

    @SerializedName("error")
    private String mError;

    @SerializedName("error_description")
    private String mErrorDescription;

    @SerializedName("errors")
    private String[] mErrors;

    @SerializedName("links")
    private Links mLinks;

    public String getError() {
        if (mErrors != null && mErrors.length > 0) {
            return TextUtils.join(", ", mErrors);
        }

        if (!TextUtils.isEmpty(mError)) {
            return mError;
        }

        if (!TextUtils.isEmpty(mErrorDescription)) {
            return mErrorDescription;
        }

        return null;
    }

    public Links getLinks() {
        return mLinks;
    }

    public static class Links {
        @SerializedName("page")
        Integer mPage;

        @SerializedName("pages")
        Integer mPages;

        public Integer getPage() {
            return mPage;
        }

        public Integer getPages() {
            return mPages;
        }
    }
}
