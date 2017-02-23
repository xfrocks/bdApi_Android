package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.InstanceCreator;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Date;

public class ApiAccessToken implements Serializable {

    @SerializedName("access_token")
    private String mToken;

    @SerializedName("refresh_token")
    private String mRefreshToken;

    @SerializedName("user_id")
    private long mUserId;

    @SerializedName("user_data")
    private ApiUser mUser;

    @SerializedName("expires_in")
    private long mExpiresIn;

    private final long mGeneratedTime;

    private ApiAccessToken(long generatedTime) {
        mGeneratedTime = generatedTime;
    }

    public String getToken() {
        return mToken;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public long getUserId() {
        return mUserId;
    }

    public ApiUser getUser() {
        return mUser;
    }

    public boolean isValid() {
        return (mGeneratedTime + mExpiresIn) > new Date().getTime();
    }

    public static class ApiAccessTokenCreator implements InstanceCreator<ApiAccessToken> {
        @Override
        public ApiAccessToken createInstance(Type type) {
            return new ApiAccessToken(new Date().getTime());
        }
    }
}
