package com.xfrocks.api.androiddemo.common.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class ApiUser extends ApiModel {

    @SerializedName("user_id")
    private Integer mId;

    @SerializedName("username")
    private String mUsername;

    @SerializedName("user_email")
    private String mEmail;

    @SerializedName("user_dob_year")
    private Integer mDobYear;

    @SerializedName("user_dob_month")
    private Integer mDobMonth;

    @SerializedName("user_dob_day")
    private Integer mDobDay;

    @SerializedName("associatable")
    private Map<Integer, ApiUser> mAssociatable;

    @SerializedName("extra_data")
    private String mExtraData;

    @SerializedName("extra_timestamp")
    private long mExtraTimestamp;

    @Override
    public Integer getId() {
        return mId;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getEmail() {
        return mEmail;
    }

    public Integer getDobYear() {
        return mDobYear;
    }

    public Integer getDobMonth() {
        return mDobMonth;
    }

    public Integer getDobDay() {
        return mDobDay;
    }

    public ApiUser[] getAssocs() {
        int count = mAssociatable == null ? 0 : mAssociatable.size();
        ApiUser[] array = new ApiUser[count];
        if (count == 0) {
            return array;
        }

        Integer[] userIds = mAssociatable.keySet().toArray(new Integer[count]);
        for (int i = 0; i < userIds.length; i++) {
            Integer userId = userIds[i];
            array[i] = mAssociatable.get(userId);
        }

        return array;
    }

    public String getExtraData() {
        return mExtraData;
    }

    public long getExtraTimestamp() {
        return mExtraTimestamp;
    }

    public String getAvatar() {
        return getLink("avatar_big");
    }

}
