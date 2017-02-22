package com.xfrocks.api.androiddemo;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.xfrocks.api.androiddemo.persist.Row;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Api {

    public static final String PARAM_LOCALE = "locale";
    public static final String PARAM_OAUTH_TOKEN = "oauth_token";
    public static final String PARAM_ATTACHMENT_HASH = "attachment_hash";
    public static final String PARAM_FILE = "file";
    public static final String PARAM_PAGE = "page";
    public static final String PARAM_ORDER = "order";

    public static final String URL_OAUTH_TOKEN = "oauth/token";
    public static final String URL_OAUTH_TOKEN_FACEBOOK = "oauth/token/facebook";
    public static final String URL_OAUTH_TOKEN_TWITTER = "oauth/token/twitter";
    public static final String URL_OAUTH_TOKEN_GOOGLE = "oauth/token/google";
    public static final String URL_OAUTH_TOKEN_ASSOCIATE = "oauth/token/associate";
    public static final String URL_INDEX = "index";
    public static final String URL_USERS = "users";
    public static final String URL_USERS_ME = "users/me";
    public static final String URL_USERS_ME_AVATAR = "users/me/avatar";
    public static final String URL_TOOLS_LOGIN_SOCIAL = "tools/login/social";
    public static final String URL_CONVERSATION_MESSAGES = "conversation-messages";
    public static final String URL_CONVERSATIONS_ATTACHMENTS = "conversations/attachments";
    public static final String URL_POSTS = "posts";
    public static final String URL_POSTS_ATTACHMENTS = "posts/attachments";

    public static final String URL_OAUTH_TOKEN_PARAM_GRANT_TYPE = "grant_type";
    public static final String URL_OAUTH_TOKEN_PARAM_GRANT_TYPE_PASSWORD = "password";
    public static final String URL_OAUTH_TOKEN_PARAM_GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    public static final String URL_OAUTH_TOKEN_PARAM_GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String URL_OAUTH_TOKEN_PARAM_USERNAME = "username";
    public static final String URL_OAUTH_TOKEN_PARAM_PASSWORD = "password";
    public static final String URL_OAUTH_TOKEN_PARAM_TFA_PROVIDER_ID = "tfa_provider";
    public static final String URL_OAUTH_TOKEN_PARAM_TFA_TRIGGER = "tfa_trigger";
    public static final String URL_OAUTH_TOKEN_PARAM_TFA_PROVIDER_CODE = "code";
    public static final String URL_OAUTH_TOKEN_PARAM_REFRESH_TOKEN = "refresh_token";
    public static final String URL_OAUTH_TOKEN_PARAM_CODE = "code";
    public static final String URL_OAUTH_TOKEN_PARAM_REDIRECT_URI = "redirect_uri";
    public static final String URL_OAUTH_TOKEN_FACEBOOK_PARAM_TOKEN = "facebook_token";
    public static final String URL_OAUTH_TOKEN_TWITTER_PARAM_URI = "twitter_uri";
    public static final String URL_OAUTH_TOKEN_TWITTER_PARAM_AUTH = "twitter_auth";
    public static final String URL_OAUTH_TOKEN_GOOGLE_PARAM_TOKEN = "google_token";
    public static final String URL_OAUTH_TOKEN_RESPONSE_HEADER_TFA_PROVIDERS = "X-Api-Tfa-Providers";

    public static final String URL_USERS_PARAM_USER_ID = "user_id";
    public static final String URL_USERS_PARAM_USERNAME = "username";
    public static final String URL_USERS_PARAM_EMAIL = "user_email";
    public static final String URL_USERS_PARAM_PASSWORD = "password";
    public static final String URL_USERS_PARAM_DOB_YEAR = "user_dob_year";
    public static final String URL_USERS_PARAM_DOB_MONTH = "user_dob_month";
    public static final String URL_USERS_PARAM_DOB_DAY = "user_dob_day";
    public static final String URL_USERS_PARAM_EXTRA_DATA = "extra_data";
    public static final String URL_USERS_PARAM_EXTRA_TIMESTAMP = "extra_timestamp";

    public static final String URL_USERS_ME_AVATAR_PARAM_AVATAR = "avatar";

    public static final String URL_CONVERSATION_MESSAGES_PARAM_CONVERSATION_ID = "conversation_id";
    public static final String URL_CONVERSATION_MESSAGES_ORDER_REVERSE = "natural_reverse";
    public static final String URL_CONVERSATION_MESSAGES_PARAM_MESSAGE_BODY = "message_body";
    public static final String URL_CONVERSATION_MESSAGES_PARAM_ATTACHMENT_HASH = "attachment_hash";

    public static final String URL_POSTS_PARAM_THREAD_ID = "thread_id";
    public static final String URL_POSTS_ORDER_REVERSE = "natural_reverse";
    public static final String URL_POSTS_PARAM_POST_BODY = "post_body";
    public static final String URL_POSTS_PARAM_ATTACHMENT_HASH = "attachment_hash";

    public static AccessToken makeAccessToken(JSONObject response) {
        try {
            AccessToken at = new AccessToken();
            at.token = response.getString("access_token");
            at.userId = response.getLong("user_id");
            at.expireDate = new Date().getTime() + response.getLong("expires_in") * 1000;

            if (response.has("refresh_token")) {
                at.refreshToken = response.getString("refresh_token");
            }

            return at;
        } catch (JSONException e) {
            // ignore
        }

        return null;
    }

    public static User makeUser(JSONObject obj) {
        try {
            User u = new User();

            if (obj.has("user_id")) {
                u.userId = obj.getInt("user_id");

                if (obj.has("links")) {
                    u.avatar = obj.getJSONObject("links").getString("avatar_big");
                }
            } else {
                if (obj.has("associatable")) {
                    JSONObject assoc = obj.getJSONObject("associatable");
                    Iterator<String> assocUserIds = assoc.keys();
                    while (assocUserIds.hasNext()) {
                        String assocUserId = assocUserIds.next();
                        JSONObject assocUserJson = assoc.getJSONObject(assocUserId);
                        User assocUser = makeUser(assocUserJson);
                        u.associatable.add(assocUser);
                    }
                }

                if (obj.has("extra_data")
                        && obj.has("extra_timestamp")) {
                    u.extraData = obj.getString("extra_data");
                    u.extraTimestamp = obj.getLong("extra_timestamp");
                }
            }

            if (obj.has("username")) {
                u.username = obj.getString("username");
            }

            if (obj.has("user_email")) {
                u.userEmail = obj.getString("user_email");
            }

            if (obj.has("user_dob_year")
                    && obj.has("user_dob_month")
                    && obj.has("user_dob_day")) {
                u.userDobYear = obj.getInt("user_dob_year");
                u.userDobMonth = obj.getInt("user_dob_month");
                u.userDobDay = obj.getInt("user_dob_day");
            }

            return u;
        } catch (JSONException e) {
            // ignore
        }

        return null;
    }

    public static PlaceholderDiscussion makePlaceholderDiscussion(int discussionId) {
        PlaceholderDiscussion d = new PlaceholderDiscussion();
        d.id = discussionId;

        return d;
    }

    public static ConversationMessage makeConversationMessage(JSONObject obj) {
        try {
            ConversationMessage m = new ConversationMessage();

            m.creatorUserId = obj.getInt("creator_user_id");
            m.creatorName = obj.getString("creator_username");
            m.id = obj.getInt("message_id");
            m.createDate = obj.getInt("message_create_date");
            m.bodyPlainText = obj.getString("message_body_plain_text");

            if (obj.has("links")) {
                JSONObject links = obj.getJSONObject("links");
                if (links.has("creator_avatar")) {
                    m.creatorAvatar = links.getString("creator_avatar");
                }
            }

            if (obj.has("attachments")) {
                JSONArray attachments = obj.getJSONArray("attachments");
                for (int i = 0, l = attachments.length(); i < l; i++) {
                    JSONObject attachmentObj = attachments.getJSONObject(i);
                    Attachment attachment = makeAttachment(attachmentObj);

                    m.attachments.add(attachment);
                }
            }

            return m;
        } catch (JSONException e) {
            // ignore
        }

        return null;
    }

    public static ConversationMessage makeConversationMessage(Api.User user, String bodyPlainText) {
        ConversationMessage m = new ConversationMessage();

        m.creatorUserId = user.getUserId();
        m.creatorName = user.getUsername();
        m.creatorAvatar = user.getAvatar();
        m.bodyPlainText = bodyPlainText;
        m.createDate = (int) new Date().getTime() / 1000;

        return m;
    }

    public static Conversation makeConversation(JSONObject obj) {
        try {
            Conversation c = new Conversation();

            c.id = obj.getInt("conversation_id");
            c.title = obj.getString("conversation_title");

            if (obj.has("links")) {
                JSONObject links = obj.getJSONObject("links");
                c.permalink = links.getString("permalink");
            }

            if (obj.has("permissions")) {
                JSONObject permissions = obj.getJSONObject("permissions");
                c.permissionPostMessage = permissions.getBoolean("reply");
                c.permissionUploadAttachment = permissions.getBoolean("upload_attachment");
            }

            return c;
        } catch (JSONException e) {
            // ignore
        }

        return null;
    }

    public static Post makePost(JSONObject obj) {
        try {
            Post p = new Post();

            p.creatorUserId = obj.getInt("poster_user_id");
            p.creatorName = obj.getString("poster_username");
            p.id = obj.getInt("post_id");
            p.createDate = obj.getInt("post_create_date");
            p.bodyPlainText = obj.getString("post_body_plain_text");

            if (obj.has("links")) {
                JSONObject links = obj.getJSONObject("links");
                if (links.has("poster_avatar")) {
                    p.creatorAvatar = links.getString("poster_avatar");
                }
            }

            if (obj.has("attachments")) {
                JSONArray attachments = obj.getJSONArray("attachments");
                for (int i = 0, l = attachments.length(); i < l; i++) {
                    JSONObject attachmentObj = attachments.getJSONObject(i);
                    Attachment attachment = makeAttachment(attachmentObj);

                    p.attachments.add(attachment);
                }
            }

            return p;
        } catch (JSONException e) {
            // ignore
        }

        return null;
    }

    public static Post makePost(Api.User user, String bodyPlainText) {
        Post p = new Post();

        p.creatorUserId = user.getUserId();
        p.creatorName = user.getUsername();
        p.creatorAvatar = user.getAvatar();
        p.bodyPlainText = bodyPlainText;
        p.createDate = (int) new Date().getTime() / 1000;

        return p;
    }

    public static Thread makeThread(JSONObject obj) {
        try {
            Thread t = new Thread();

            t.id = obj.getInt("thread_id");
            t.title = obj.getString("thread_title");

            if (obj.has("links")) {
                JSONObject links = obj.getJSONObject("links");
                t.permalink = links.getString("permalink");
            }

            if (obj.has("permissions")) {
                JSONObject permissions = obj.getJSONObject("permissions");
                t.permissionPostMessage = permissions.getBoolean("post");
                t.permissionUploadAttachment = permissions.getBoolean("upload_attachment");
            }

            return t;
        } catch (JSONException e) {
            // ignore
        }

        return null;
    }

    public static Attachment makeAttachment(JSONObject obj) {
        try {
            Attachment a = new Attachment();

            a.attachmentId = obj.getInt("attachment_id");
            a.fileName = obj.getString("filename");

            if (obj.has("links")) {
                JSONObject links = obj.getJSONObject("links");
                a.permalink = links.getString("permalink");

                if (links.has("thumbnail")) {
                    a.thumbnail = links.getString("thumbnail");
                }
            }

            return a;
        } catch (JSONException e) {
            return null;
        }
    }

    public static String makeOneTimeToken(long userId, AccessToken at) {
        long timestamp = new Date().getTime() / 1000 + (long) 3600;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return e.getMessage();
        }

        md.update(String.format(Locale.US, "%d%d%s%s",
                userId,
                timestamp,
                at != null ? at.getToken() : "",
                BuildConfig.CLIENT_SECRET
        ).getBytes());
        byte[] digest = md.digest();

        StringBuilder sb = new StringBuilder();
        for (byte d : digest) {
            String h = Integer.toHexString(0xFF & d);
            while (h.length() < 2) {
                h = "0" + h;
            }
            sb.append(h);
        }

        return String.format(Locale.US, "%d,%d,%s,%s", userId, timestamp, sb, BuildConfig.CLIENT_ID);
    }

    public static String makeAuthorizeRedirectUri(String redirectTo) {
        if (TextUtils.isEmpty(BuildConfig.AUTHORIZE_REDIRECT_URI)) {
            return "";
        }

        if (TextUtils.isEmpty(redirectTo)) {
            return BuildConfig.AUTHORIZE_REDIRECT_URI;
        }

        try {
            return String.format(
                    "%s?redirect_to=%s",
                    BuildConfig.AUTHORIZE_REDIRECT_URI,
                    URLEncoder.encode(redirectTo, "UTF-8")
            );
        } catch (UnsupportedEncodingException e) {
            // ignore
        }

        return null;
    }

    public static String makeAuthorizeUri(String redirectTo) {
        try {
            String authorizeRedirectUri = makeAuthorizeRedirectUri(redirectTo);
            String encodedRedirectTo = "";
            if (authorizeRedirectUri != null) {
                encodedRedirectTo = URLEncoder.encode(authorizeRedirectUri, "UTF-8");
            }

            return String.format(
                    "%s/index.php?oauth/authorize/&client_id=%s&redirect_uri=%s&response_type=code&scope=%s",
                    BuildConfig.API_ROOT,
                    URLEncoder.encode(BuildConfig.CLIENT_ID, "UTF-8"),
                    encodedRedirectTo,
                    URLEncoder.encode("read", "UTF-8")
            );
        } catch (UnsupportedEncodingException e) {
            // ignore
        }

        return null;
    }

    public static String makeAttachmentsUrl(String path, String attachmentHash, AccessToken accessToken) {
        return makeUrl(com.android.volley.Request.Method.GET, path, new Params(accessToken)
                .and(PARAM_ATTACHMENT_HASH, attachmentHash));
    }

    private static String makeUrl(int method, String url, Map<String, String> params) {
        if (!url.contains("://")) {
            url = String.format("%s/index.php?%s", BuildConfig.API_ROOT, url.replace('?', '&'));
        }

        if (!url.contains("&locale=")
                && !params.containsKey(PARAM_LOCALE)) {
            Locale locale = Locale.getDefault();
            params.put(PARAM_LOCALE, String.format("%s-%s", locale.getLanguage(),
                    locale.getCountry()));
        }

        if (method != com.android.volley.Request.Method.POST) {
            // append params to url automatically, and clear the map
            for (String paramKey : params.keySet()) {
                String paramValue = params.get(paramKey);

                try {
                    url += String.format("%s%s=%s", url.contains("?") ? "&" : "?",
                            paramKey, URLEncoder.encode(paramValue, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            params.clear();
        }

        if (url.contains("&oauth_token=")
                && params.containsKey(PARAM_OAUTH_TOKEN)) {
            params.remove(PARAM_OAUTH_TOKEN);
        }

        return url;
    }

    private static class Request extends com.android.volley.Request<JSONObject> {

        final Map<String, String> mParams;
        Map<String, String> mResponseHeaders;

        public Request(int method, String url, Map<String, String> params) {
            super(method, makeUrl(method, url, params), null);

            mParams = params;

            // a tag must present at construction time so caller should know to cancel
            // the request when its life cycle is interrupted
            setTag(this.getClass().getSimpleName());

            if (BuildConfig.DEBUG) {
                // set a long time out for debugging
                setRetryPolicy(new DefaultRetryPolicy(60000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            }
        }

        public void start() {
            if (BuildConfig.DEBUG) {
                Log.v(getTag().toString(), "Request=" + getUrl() + " (" + getMethod() + ")");
                for (String key : mParams.keySet()) {
                    Log.v(getTag().toString(), "Request[" + key + "]=" + mParams.get(key));
                }
            }

            onStart();

            App.getInstance().getRequestQueue().add(this);
        }

        @Override
        protected Map<String, String> getParams() throws AuthFailureError {
            return mParams;
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            mResponseHeaders = response.headers;

            try {
                String jsonString =
                        new String(response.data, HttpHeaderParser.parseCharset(response.headers));

                if (BuildConfig.DEBUG) {
                    Log.v(getTag().toString(), "Response=" + jsonString);
                    for (Map.Entry<String, String> header : response.headers.entrySet()) {
                        Log.v(getTag().toString(),
                                "Response[" + header.getKey() + "]=" + header.getValue());
                    }
                }

                JSONObject jsonObject = new JSONObject(jsonString);

                return Response.success(jsonObject,
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new VolleyError(e));
            }
        }

        @Override
        protected void deliverResponse(JSONObject response) {
            onSuccess(response);
            onComplete();
        }

        @Override
        public void deliverError(VolleyError error) {
            if (BuildConfig.DEBUG) {
                Log.v(getTag().toString(), "Error=" + error);

                if (error.networkResponse != null) {
                    Log.v(getTag().toString(), "Error.NetworkResponse=" + new String(error.networkResponse.data));
                }
            }

            onError(error);
            onComplete();
        }

        protected void onStart() {
            // do something?
        }

        protected void onSuccess(JSONObject response) {
            // do something?
        }

        protected void onError(VolleyError error) {
            // do something?
        }

        protected void onComplete() {
            // do something?
        }

        protected String getErrorMessage(VolleyError error) {
            String message = null;

            if (error.getCause() != null) {
                message = error.getCause().getMessage();
            }

            if (message == null) {
                message = error.getMessage();
            }

            if (message == null && error.networkResponse != null) {
                try {
                    String jsonString = new String(error.networkResponse.data,
                            HttpHeaderParser.parseCharset(error.networkResponse.headers));

                    JSONObject jsonObject = new JSONObject(jsonString);
                    message = getErrorMessage(jsonObject);
                } catch (Exception e) {
                    // ignore
                }
            }

            return message;
        }

        protected String getErrorMessage(JSONObject response) {
            String message = null;

            try {
                if (response.has("error_description")) {
                    message = response.getString("error_description");
                } else if (response.has("errors")) {
                    try {
                        JSONArray errors = response.getJSONArray("errors");
                        if (errors.length() > 0) {
                            message = errors.getString(0);
                        }
                    } catch (JSONException je) {
                        JSONObject errors = response.getJSONObject("errors");
                        JSONArray names = errors.names();
                        String name = names.getString(0);
                        message = errors.getString(name);
                    }
                } else if (response.has("error")) {
                    message = response.getString("error");
                }
            } catch (Exception e) {
                // ignore
            }

            return message;
        }

        protected void parseRows(JSONObject obj, List<Row> rows) {
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                final Row row = new Row();
                row.key = keys.next();

                try {
                    parseRow(obj.get(row.key), row);
                    rows.add(row);
                } catch (JSONException e) {
                    // ignore
                }
            }
        }

        protected void parseRows(JSONArray array, List<Row> rows) {
            for (int i = 0; i < array.length(); i++) {
                final Row row = new Row();
                row.key = String.valueOf(i);

                try {
                    parseRow(array.get(i), row);
                    rows.add(row);
                } catch (JSONException e) {
                    // ignore
                }
            }
        }

        protected void parseRow(Object value, Row row) {
            if (value instanceof JSONObject) {
                row.value = "(object)";
                row.subRows = new ArrayList<>();
                parseRows((JSONObject) value, row.subRows);
            } else if (value instanceof JSONArray) {
                row.value = "(array)";
                row.subRows = new ArrayList<>();
                parseRows((JSONArray) value, row.subRows);
            } else {
                row.value = String.valueOf(value);
            }
        }
    }

    public static class GetRequest extends Api.Request {
        public GetRequest(String url, Map<String, String> params) {
            super(Method.GET, url, params);
        }
    }

    public static class OptionsRequest extends Api.Request {
        public OptionsRequest(String url, Map<String, String> params) {
            super(Method.OPTIONS, url, params);
        }
    }

    public static class PostRequest extends Api.Request {

        private final Map<String, InputStreamBody> mFiles = new HashMap<>();
        private MultipartEntityBuilder mBodyBuilder = null;
        private HttpEntity mBuiltBody = null;

        public PostRequest(String url, Map<String, String> params) {
            super(Method.POST, url, params);
        }

        @Override
        public void start() {
            super.start();

            if (BuildConfig.DEBUG) {
                for (String key : mFiles.keySet()) {
                    Log.v(getTag().toString(), "Request[" + key + "](file)=" + mFiles.get(key).getFilename());
                }
            }
        }

        @Override
        public String getBodyContentType() {
            if (mFiles.size() == 0) {
                return super.getBodyContentType();
            }

            return mBuiltBody.getContentType().getValue();
        }

        @Override
        public byte[] getBody() throws AuthFailureError {
            if (mFiles.size() == 0) {
                return super.getBody();
            }

            if (mBuiltBody == null) {
                mBodyBuilder = MultipartEntityBuilder.create();
                mBodyBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                for (Map.Entry<String, String> param : mParams.entrySet()) {
                    mBodyBuilder.addTextBody(param.getKey(), param.getValue());
                }

                for (Map.Entry<String, InputStreamBody> file : mFiles.entrySet()) {
                    mBodyBuilder.addPart(file.getKey(), file.getValue());
                }

                mBuiltBody = mBodyBuilder.build();
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                mBuiltBody.writeTo(bos);
            } catch (IOException e) {
                Log.e(getTag().toString(), "getBody: " + e.toString());
            }

            return bos.toByteArray();
        }

        void addFile(String key, String fileName, InputStream inputStream) throws IllegalAccessException {
            if (mBuiltBody != null) {
                throw new IllegalAccessException("Cannot addFile after body has been built.");
            }

            mFiles.put(key, new InputStreamBody(inputStream, fileName));
        }
    }

    public static class PushServerRequest extends Api.Request {
        public PushServerRequest(String deviceId, String topic, AccessToken at) {
            super(
                    Method.POST,
                    BuildConfig.PUSH_SERVER + ("/subscribe"),
                    new Params("device_type", "android")
                            .and("device_id", deviceId)
                            .and("hub_uri", BuildConfig.API_ROOT + "/index.php?subscriptions")
                            .and("hub_topic", topic)
                            .and("oauth_client_id", BuildConfig.CLIENT_ID)
                            .and("oauth_token", Api.makeOneTimeToken(at != null ? at.getUserId() : 0, at))
                            .and("extra_data[package]", App.getInstance().getApplicationContext().getPackageName())
            );
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            return Response.success(null, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

    public static class Params extends HashMap<String, String> {

        public Params(String key, Object value) {
            super(1);
            put(key, value);
        }

        public Params(AccessToken at) {
            super(1);

            if (at != null) {
                put(PARAM_OAUTH_TOKEN, at.getToken());
            }
        }

        public Params(String token) {
            super(1);
            put(PARAM_OAUTH_TOKEN, token);
        }

        public Params and(String key, Object value) {
            put(key, value);

            return this;
        }

        public Params andIf(boolean expression, String key, Object value) {
            if (expression) {
                put(key, value);
            }

            return this;
        }

        public Params and(List<Row> data) {
            for (Row row : data) {
                if (row.value != null
                        && !row.value.isEmpty()) {
                    if (!"file".equals(row.type)) {
                        put(row.key, row.value);
                    }
                }
            }

            return this;
        }

        public Params andClientCredentials() {
            put("client_id", BuildConfig.CLIENT_ID);
            put("client_secret", BuildConfig.CLIENT_SECRET);

            return this;
        }

        private void put(String key, Object value) {
            if (value != null) {
                put(key, String.valueOf(value));
            }
        }
    }

    public static class AccessToken implements Serializable {

        private String token;
        private String refreshToken;
        private long userId;
        private long expireDate;

        public String getToken() {
            return token;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public long getUserId() {
            return userId;
        }

        public boolean isValid() {
            return expireDate > new Date().getTime();
        }
    }

    public static class User implements Serializable {

        private Integer userId;
        private String username;
        private String userEmail;
        private Integer userDobYear;
        private Integer userDobMonth;
        private Integer userDobDay;

        private String avatar;

        private final List<User> associatable = new ArrayList<>();
        private String extraData;
        private long extraTimestamp;

        public Integer getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getEmail() {
            return userEmail;
        }

        public Integer getDobYear() {
            return userDobYear;
        }

        public Integer getDobMonth() {
            return userDobMonth;
        }

        public Integer getDobDay() {
            return userDobDay;
        }

        public User[] getAssocs() {
            return associatable.toArray(new User[associatable.size()]);
        }

        public String getExtraData() {
            return extraData;
        }

        public long getExtraTimestamp() {
            return extraTimestamp;
        }

        public String getAvatar() {
            return avatar;
        }

    }

    abstract public static class Discussion implements Serializable {

        Integer id;
        String title;

        String permalink;

        boolean permissionPostMessage;
        boolean permissionUploadAttachment;

        public Integer getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getPermalink() {
            return permalink;
        }

        public boolean canPostMessage() {
            return permissionPostMessage;
        }

        public boolean canUploadAttachment() {
            return permissionUploadAttachment;
        }

    }

    public static class DiscussionMessage implements Serializable {

        Integer creatorUserId;
        String creatorName;
        String creatorAvatar;

        Integer id;
        Integer createDate;
        String bodyPlainText;

        public String errorMessage;

        final List<Attachment> attachments = new ArrayList<>();

        public Integer getCreatorUserId() {
            return creatorUserId;
        }

        public String getCreatorName() {
            return creatorName;
        }

        public String getCreatorAvatar() {
            return creatorAvatar;
        }

        public Integer getId() {
            return id;
        }

        public Integer getCreateDate() {
            return createDate;
        }

        public String getBodyPlainText() {
            return bodyPlainText;
        }

        public Iterator<Attachment> getAttachmentsIterator() {
            return attachments.iterator();
        }
    }

    public static class PlaceholderDiscussion extends Discussion {

    }

    public static class Conversation extends Discussion {
    }

    public static class ConversationMessage extends DiscussionMessage {
    }

    public static class Thread extends Discussion {
    }

    public static class Post extends DiscussionMessage {
    }

    public static class Attachment implements Serializable {

        private Integer attachmentId;
        private String fileName;

        private String permalink;
        private String thumbnail;

        public Integer getAttachmentId() {
            return attachmentId;
        }

        public String getFileName() {
            return fileName;
        }

        public String getPermalink() {
            return permalink;
        }

        public String getThumbnail() {
            return thumbnail;
        }
    }

}
