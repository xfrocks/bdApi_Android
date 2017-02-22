package com.xfrocks.api.androiddemo.discussion;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.android.volley.VolleyError;
import com.xfrocks.api.androiddemo.Api;
import com.xfrocks.api.androiddemo.LoginActivity;
import com.xfrocks.api.androiddemo.R;
import com.xfrocks.api.androiddemo.persist.ObjectAsFile;

import org.json.JSONObject;

public class ActionSendReceiver extends AppCompatActivity implements QuickReplyFragment.Listener {

    static final String QUERY_PARAM_ACTION = "action";
    static final String QUERY_PARAM_TYPE = "type";
    static final String QUERY_PARAM_STREAM = "stream";

    LinearLayout mInner;
    QuickReplyFragment mQuickReply;
    Button mButton;
    ProgressBar mProgressBar;

    Api.AccessToken mAccessToken;
    Api.Discussion mDiscussion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        String action = intent.getAction();
        String type = intent.getType();
        Uri stream = intent.getParcelableExtra(Intent.EXTRA_STREAM);

        if (intent.hasExtra(DiscussionActivity.EXTRA_LOGIN_REDIRECTED_TO)) {
            String loginRedirectedTo = intent.getStringExtra(DiscussionActivity.EXTRA_LOGIN_REDIRECTED_TO);
            Uri loginRedirectedToUri = Uri.parse(loginRedirectedTo);
            action = loginRedirectedToUri.getQueryParameter(QUERY_PARAM_ACTION);
            type = loginRedirectedToUri.getQueryParameter(QUERY_PARAM_TYPE);
            stream = Uri.parse(loginRedirectedToUri.getQueryParameter(QUERY_PARAM_STREAM));
        }

        if (!Intent.ACTION_SEND.equals(action)
                || TextUtils.isEmpty(type)) {
            finish();
            return;
        }

        if (intent.hasExtra(DiscussionActivity.EXTRA_ACCESS_TOKEN)) {
            mAccessToken = (Api.AccessToken) intent.getSerializableExtra(DiscussionActivity.EXTRA_ACCESS_TOKEN);
        }
        if (mAccessToken == null) {
            mAccessToken = (Api.AccessToken) ObjectAsFile.load(this, ObjectAsFile.ACCESS_TOKEN);
        }
        if (mAccessToken == null || !mAccessToken.isValid()) {
            String loginRedirectTo = new Uri.Builder()
                    .scheme(getClass().getSimpleName())
                    .authority(getClass().getName())
                    .appendQueryParameter(QUERY_PARAM_ACTION, action)
                    .appendQueryParameter(QUERY_PARAM_TYPE, type)
                    .appendQueryParameter(QUERY_PARAM_STREAM, stream.toString())
                    .build().toString();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.putExtra(LoginActivity.EXTRA_REDIRECT_TO, loginRedirectTo);

            startActivity(loginIntent);
            finish();
            return;
        }

        mDiscussion = (Api.Discussion) ObjectAsFile.load(this, ObjectAsFile.LATEST_DISCUSSION);
        if (mDiscussion == null || !mDiscussion.canUploadAttachment()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_discussion_send_receiver);
        setTitle(mDiscussion.getTitle());

        mInner = (LinearLayout) findViewById(R.id.inner);

        mQuickReply = (QuickReplyFragment) getSupportFragmentManager().findFragmentById(R.id.quick_reply);
        mQuickReply.setup(mDiscussion, this);

        mButton = (Button) findViewById(R.id.button);
        if (mButton != null) {
            mQuickReply.setButtonReplyVisibilityGone();
            mQuickReply.setEditTextMessageMultiLine();

            mButton.setVisibility(View.VISIBLE);
            mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mQuickReply.attemptReply();
                }
            });
        }

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        if (type.startsWith("image/") && stream != null) {
            mQuickReply.uploadAttach(stream);
        }
    }

    @Override
    public Api.AccessToken getEffectiveAccessToken() {
        return mAccessToken;
    }

    @Override
    public void onQuickReplySubmit() {
        new PostMessageRequest().start();
    }

    void setTheProgressBarVisibility(boolean visible) {
        if (mInner != null) {
            mInner.setAlpha(visible ? .5f : 1f);
        }

        if (mProgressBar != null) {
            mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    class PostMessageRequest extends Api.PostRequest {
        public PostMessageRequest() {
            super(mDiscussion.getPostMessagesUrl(),
                    mDiscussion.getPostMessagesParams(
                            mQuickReply.getPendingMessage(),
                            mQuickReply.getAttachmentHash(), mAccessToken
                    )
            );
        }

        @Override
        protected void onStart() {
            super.onStart();

            setTheProgressBarVisibility(true);
        }

        @Override
        protected void onSuccess(JSONObject response) {
            ActionSendReceiver.this.finish();
        }

        @Override
        protected void onError(VolleyError error) {
            showError(getErrorMessage(error));
        }

        @Override
        protected void onComplete() {
            super.onComplete();

            setTheProgressBarVisibility(false);
        }

        void showError(String errorMessage) {
            if (TextUtils.isEmpty(errorMessage)) {
                return;
            }

            new AlertDialog.Builder(ActionSendReceiver.this)
                    .setTitle(R.string.post_reply)
                    .setMessage(errorMessage)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

}