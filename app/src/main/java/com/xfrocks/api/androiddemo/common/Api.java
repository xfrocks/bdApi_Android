package com.xfrocks.api.androiddemo.common;

import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.gson.annotations.SerializedName;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.BuildConfig;
import com.xfrocks.api.androiddemo.common.model.ApiAccessToken;
import com.xfrocks.api.androiddemo.common.model.ApiModel;
import com.xfrocks.api.androiddemo.common.persist.Row;

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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
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

import static com.xfrocks.api.androiddemo.common.ApiConstants.PARAM_ATTACHMENT_HASH;

public class Api {

    public static String makeOneTimeToken(long userId, ApiAccessToken at) {
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

    public static String makeAttachmentsUrl(String path, String attachmentHash, ApiAccessToken accessToken) {
        return makeUrl(com.android.volley.Request.Method.GET, path, new Params(accessToken)
                .and(PARAM_ATTACHMENT_HASH, attachmentHash));
    }

    private static String makeUrl(int method, String url, Map<String, String> params) {
        if (url == null) {
            url = "";
        }

        if (!url.contains("://")) {
            url = String.format("%s/index.php?%s", BuildConfig.API_ROOT, url.replace('?', '&'));
        }

        if (!url.contains("&locale=")
                && !params.containsKey(ApiConstants.PARAM_LOCALE)) {
            Locale locale = Locale.getDefault();
            params.put(ApiConstants.PARAM_LOCALE, String.format("%s-%s", locale.getLanguage(),
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
                && params.containsKey(ApiConstants.PARAM_OAUTH_TOKEN)) {
            params.remove(ApiConstants.PARAM_OAUTH_TOKEN);
        }

        return url;
    }

    private static class Request extends com.android.volley.Request<String> {

        final Map<String, String> mParams;
        protected Map<String, String> mResponseHeaders;

        Request(int method, String url, Map<String, String> params) {
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
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
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

                return Response.success(jsonString,
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (Exception e) {
                return Response.error(new VolleyError(e));
            }
        }

        @Override
        protected void deliverResponse(String response) {
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

        protected void onSuccess(String response) {
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

                    ApiBaseResponse data = App.getGsonInstance().fromJson(jsonString, ApiBaseResponse.class);
                    message = data.getError();
                } catch (Exception e) {
                    // ignore
                }
            }

            return message;
        }

        protected void parseRows(String response, List<Row> rows) {
            try {
                JSONObject obj = new JSONObject(response);
                parseRows(obj, rows);
            } catch (JSONException e) {
                // ignore
            }
        }

        void parseRows(JSONObject obj, List<Row> rows) {
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

        void parseRows(JSONArray array, List<Row> rows) {
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

        void parseRow(Object value, Row row) {
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

        protected void addFile(String key, String fileName, InputStream inputStream) throws IllegalAccessException {
            if (mBuiltBody != null) {
                throw new IllegalAccessException("Cannot addFile after body has been built.");
            }

            mFiles.put(key, new InputStreamBody(inputStream, fileName));
        }
    }

    public static class PushServerRequest extends Api.Request {
        public PushServerRequest(String deviceId, String topic, ApiAccessToken at) {
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
    }

    public static class Params extends HashMap<String, String> {

        public Params(String key, Object value) {
            super(1);
            put(key, value);
        }

        public Params(ApiAccessToken at) {
            super(1);

            if (at != null) {
                put(ApiConstants.PARAM_OAUTH_TOKEN, at.getToken());
            }
        }

        public Params(String token) {
            super(1);
            put(ApiConstants.PARAM_OAUTH_TOKEN, token);
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

        public Params andFieldsInclude(Class<? extends ApiModel> clazz, String initialValue) {
            StringBuilder valueBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(initialValue)) {
                valueBuilder.append(initialValue);
            }

            return andFieldsInclude(clazz, valueBuilder, null);
        }

        Params andFieldsInclude(Class<? extends ApiModel> clazz, StringBuilder valueBuilder, String prefix) {
            Field[] fields = ReflectionUtils.getFieldsUpTo(clazz, null);
            for (Field field : fields) {
                SerializedName sn = field.getAnnotation(SerializedName.class);
                if (sn != null) {
                    String serializedName = sn.value();

                    Class fieldType = field.getType();
                    if (fieldType.isArray()) {
                        fieldType = fieldType.getComponentType();
                    }

                    if (ApiModel.class.isAssignableFrom(fieldType)) {
                        String fieldPrefix = serializedName;
                        if (!TextUtils.isEmpty(prefix)) {
                            fieldPrefix = String.format("%s.%s", prefix, fieldPrefix);
                        }

                        //noinspection unchecked
                        andFieldsInclude((Class<? extends ApiModel>) fieldType, valueBuilder, fieldPrefix);
                        continue;
                    }

                    if (valueBuilder.length() > 0) {
                        valueBuilder.append(',');
                    }

                    if (!TextUtils.isEmpty(prefix)) {
                        valueBuilder.append(prefix);
                        valueBuilder.append('.');
                    }

                    valueBuilder.append(serializedName);
                }
            }

            return and(ApiConstants.PARAM_FIELDS_INCLUDE, valueBuilder.toString());
        }

        private void put(String key, Object value) {
            if (value != null) {
                put(key, String.valueOf(value));
            }
        }
    }
}
