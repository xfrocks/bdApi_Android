package com.xfrocks.api.androiddemo.discussion;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.xfrocks.api.androiddemo.Api;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.LoginActivity;
import com.xfrocks.api.androiddemo.QuickReplyFragment;
import com.xfrocks.api.androiddemo.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract public class DiscussionActivity extends AppCompatActivity implements QuickReplyFragment.Listener {

    public static final String EXTRA_ACCESS_TOKEN = "access_token";
    public static final String EXTRA_USER = "user";
    public static final String EXTRA_DISCUSSION_ID = "discussion_id";
    public static final String EXTRA_LOGIN_REDIRECTED_TO = "redirected_to";
    static final String STATE_ACCESS_TOKEN = "accessToken";
    static final String STATE_DISCUSSION_ID = "discussionId";

    ProgressBar mProgressBar;
    RecyclerView mMessageList;
    LinearLayoutManager mLayoutManager;

    QuickReplyFragment mQuickReply;

    Api.AccessToken mAccessToken;
    Api.User mUser;
    Api.Discussion mDiscussion;
    int mPages;
    int mPage;

    MessagesRequest mMessagesRequest;
    PatchRequest mPatchRequest;
    MessagesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mMessageList = (RecyclerView) findViewById(R.id.messageList);
        mMessageList.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mMessageList.setLayoutManager(mLayoutManager);

        mMessageList.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 0
                        && mLayoutManager.findLastCompletelyVisibleItemPosition() > mAdapter.getItemCount() - 5
                        && mPage < mPages
                        && mMessagesRequest == null) {
                    newMessagesRequest(mPage + 1).start();
                }
            }
        });

        mAdapter = new MessagesAdapter(this);
        mMessageList.setAdapter(mAdapter);

        mQuickReply = (QuickReplyFragment) getSupportFragmentManager().findFragmentById(R.id.quickReply);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_discussion, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent mainIntent = getIntent();
        if (mainIntent != null) {
            updateStatesFromIntent(mainIntent);
        }

        if (getDiscussionId() == 0) {
            finish();
            return;
        }

        if (mAccessToken != null) {
            if (mUser == null) {
                new UsersMeRequest(mAccessToken).start();
            }

            if (mAdapter.getItemCount() == 0) {
                newMessagesRequest(1).start();
            }
        } else {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.putExtra(LoginActivity.EXTRA_REDIRECT_TO, getLoginRedirectToPrefix() + getDiscussionId());

            startActivity(loginIntent);
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            int existingDiscussionId = getDiscussionId();
            updateStatesFromIntent(intent);

            if (getDiscussionId() != existingDiscussionId) {
                newMessagesRequest(1).start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mMessagesRequest != null) {
            mMessagesRequest.cancel();
        }

        if (mPatchRequest != null) {
            mPatchRequest.cancel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_ACCESS_TOKEN, mAccessToken);
        outState.putInt(STATE_DISCUSSION_ID, getDiscussionId());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_ACCESS_TOKEN)
                && savedInstanceState.containsKey(STATE_DISCUSSION_ID)) {
            mAccessToken = (Api.AccessToken) savedInstanceState.getSerializable(STATE_ACCESS_TOKEN);

            int discussionId = savedInstanceState.getInt(STATE_DISCUSSION_ID);
            mDiscussion = Api.makePlaceholderDiscussion(discussionId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                int discussionId = getDiscussionId();
                if (discussionId > 0 && mAccessToken != null) {
                    newMessagesRequest(1).start();
                }
                break;
            case R.id.permalink:
                if (mDiscussion != null) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mDiscussion.getPermalink()));
                    startActivity(browserIntent);
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onQuickReplySubmit() {
        newPostMessageRequest().start();
    }

    void setTheProgressBarVisibility(boolean visible) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    void setDiscussion(Api.Discussion discussion) {
        setTitle(discussion.getTitle());

        mDiscussion = discussion;
        mQuickReply.setup(mDiscussion.canPostMessage(), this);
        if (mDiscussion.canUploadAttachment()) {
            mQuickReply.setupAttach(makeAttachmentsUrl());
        } else {
            mQuickReply.setupAttach(null);
        }
    }

    int getDiscussionId() {
        if (mDiscussion == null) {
            return 0;
        }

        return mDiscussion.getId();
    }

    String getAttachmentHash() {
        return String.format(Locale.US, "%s-%d", getClass().getName(), getDiscussionId());
    }

    String getLoginRedirectToPrefix() {
        return getClass().getSimpleName() + "://";
    }

    void updateStatesFromIntent(Intent intent) {
        int discussionId = getDiscussionId();

        if (intent.hasExtra(EXTRA_ACCESS_TOKEN)) {
            mAccessToken = (Api.AccessToken) intent.getSerializableExtra(EXTRA_ACCESS_TOKEN);
        }

        if (intent.hasExtra(EXTRA_USER)) {
            mUser = (Api.User) intent.getSerializableExtra(EXTRA_USER);
        }

        if (intent.hasExtra(EXTRA_DISCUSSION_ID)) {
            discussionId = intent.getIntExtra(EXTRA_DISCUSSION_ID, discussionId);
        }

        if (intent.hasExtra(EXTRA_LOGIN_REDIRECTED_TO)) {
            String loginRedirectedTo = intent.getStringExtra(EXTRA_LOGIN_REDIRECTED_TO);
            String loginRedirectToPrefix = getLoginRedirectToPrefix();
            if (loginRedirectedTo.startsWith(loginRedirectToPrefix)) {
                try {
                    discussionId = Integer.parseInt(loginRedirectedTo.substring(loginRedirectToPrefix.length()));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }

        if (discussionId > 0 && discussionId != getDiscussionId()) {
            setDiscussion(Api.makePlaceholderDiscussion(discussionId));
        }
    }

    abstract MessagesRequest newMessagesRequest(int page);

    abstract PatchRequest newPatchRequest();

    abstract PostMessageRequest newPostMessageRequest();

    abstract void parseResponseForMessages(JSONObject response, ParsedMessageHandler handler);

    abstract void parseResponseForDiscussionThenSet(JSONObject response);

    abstract String makeAttachmentsUrl();

    interface ParsedMessageHandler {
        boolean onMessage(Api.DiscussionMessage message);
    }

    class UsersMeRequest extends Api.GetRequest {
        public UsersMeRequest(Api.AccessToken at) {
            super(Api.URL_USERS_ME, new Api.Params(at));
        }

        @Override
        protected void onSuccess(JSONObject response) {
            if (response.has("user")) {
                try {
                    JSONObject userJson = response.getJSONObject("user");
                    Api.User user = Api.makeUser(userJson);
                    if (user != null) {
                        mUser = user;
                    }
                } catch (JSONException e) {
                    // ignore
                }
            }
        }
    }

    abstract class MessagesRequest extends Api.GetRequest implements ParsedMessageHandler {

        final int mRequestPage;

        public MessagesRequest(String url, Api.Params params) {
            super(url, params);

            int paramPageInt = 1;
            try {
                String paramPage = params.get(Api.PARAM_PAGE);
                paramPageInt = Integer.valueOf(paramPage);
            } catch (NumberFormatException e) {
                // ignore
            }
            mRequestPage = paramPageInt;
        }

        @Override
        protected void onStart() {
            if (mMessagesRequest != null) {
                mMessagesRequest.cancel();
            }

            mMessagesRequest = this;

            if (mRequestPage == 1) {
                setTheProgressBarVisibility(true);
            }
        }

        @Override
        protected void onSuccess(JSONObject response) {
            if (mRequestPage == 1) {
                mAdapter.clear();
            }
            DiscussionActivity.this.mPage = mRequestPage;

            parseResponseForMessages(response, this);

            if (mPage == 1) {
                if (response.has("links")) {
                    try {
                        JSONObject links = response.getJSONObject("links");
                        mPages = links.getInt("pages");
                    } catch (JSONException e) {
                        // ignore
                    }
                }

                parseResponseForDiscussionThenSet(response);

                mMessageList.scrollToPosition(0);
            }
        }

        @Override
        protected void onComplete() {
            mMessagesRequest = null;
            setTheProgressBarVisibility(false);
        }

        @Override
        public boolean onMessage(Api.DiscussionMessage message) {
            mAdapter.addMessage(message);

            return true;
        }
    }

    abstract class PatchRequest extends Api.GetRequest implements ParsedMessageHandler {
        Api.DiscussionMessage mLatestMessage;
        final List<Api.DiscussionMessage> mNewMessages = new ArrayList<>();

        public PatchRequest(String url, Map<String, String> params) {
            super(url, params);
        }

        @Override
        protected void onStart() {
            if (mPatchRequest != null) {
                mPatchRequest.cancel();
            }

            mPatchRequest = this;
        }

        @Override
        protected void onSuccess(JSONObject response) {
            mLatestMessage = null;
            while (mAdapter.getItemCount() > 0) {
                Api.DiscussionMessage message = mAdapter.getMessage(0);

                if (message.getId() == null) {
                    mAdapter.removeMessage(0);
                } else {
                    mLatestMessage = message;
                    break;
                }
            }

            parseResponseForMessages(response, this);

            for (int i = mNewMessages.size() - 1; i >= 0; i--) {
                mAdapter.prependMessage(mNewMessages.get(i));
            }

            mMessageList.scrollToPosition(0);
        }

        @Override
        protected void onComplete() {
            mPatchRequest = null;
        }

        @Override
        public boolean onMessage(Api.DiscussionMessage message) {
            if (mLatestMessage == null
                    || mLatestMessage.getId() < message.getId()) {
                mNewMessages.add(message);
                return true;
            } else {
                return false;
            }
        }
    }

    abstract class PostMessageRequest extends Api.PostRequest {
        Api.DiscussionMessage mInTransitMessage;

        public PostMessageRequest(String url, Map<String, String> params) {
            super(url, params);
        }

        @Override
        protected void onStart() {
            if (mInTransitMessage != null) {
                mAdapter.prependMessage(mInTransitMessage);
                mAdapter.notifyItemInserted(0);
                mMessageList.scrollToPosition(0);
            }
        }

        @Override
        protected void onSuccess(JSONObject response) {
            newPatchRequest().start();
        }

        @Override
        protected void onError(VolleyError error) {
            showError(getErrorMessage(error));
        }

        void showError(String errorMessage) {
            if (TextUtils.isEmpty(errorMessage)) {
                return;
            }

            new AlertDialog.Builder(DiscussionActivity.this)
                    .setTitle(R.string.post_reply)
                    .setMessage(errorMessage)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    class MessagesAdapter extends RecyclerView.Adapter<ViewHolder> {
        static final int VIEW_TYPE_MINE = 0;
        static final int VIEW_TYPE_OTHER = 1;

        final ArrayList<Api.DiscussionMessage> mData = new ArrayList<>();
        final Format mTimeFormat;

        MessagesAdapter(Context context) {
            mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        }

        @Override
        public int getItemViewType(int position) {
            Api.DiscussionMessage message = mData.get(position);

            if (mUser != null
                    && mUser.getUserId().equals(message.getCreatorUserId())) {
                return VIEW_TYPE_MINE;
            } else {
                return VIEW_TYPE_OTHER;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resId = viewType == VIEW_TYPE_MINE
                    ? R.layout.list_item_my_message
                    : R.layout.list_item_message;

            View v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Api.DiscussionMessage message = mData.get(position);
            Api.DiscussionMessage messagePrev = null;
            Api.DiscussionMessage messageNext = null;
            if (position > 0) {
                messagePrev = mData.get(position - 1);
            }
            if (position < mData.size() - 1) {
                messageNext = mData.get(position + 1);
            }

            if (holder.avatar != null) {
                if (messageNext == null
                        || !message.getCreatorUserId().equals(messageNext.getCreatorUserId())) {
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.avatar.setContentDescription(message.getCreatorName());
                    App.getInstance().getNetworkImageLoader().get(
                            message.getCreatorAvatar(),
                            ImageLoader.getImageListener(holder.avatar, R.drawable.avatar_l, 0)
                    );
                } else {
                    holder.avatar.setVisibility(View.GONE);
                }
            }

            if (messagePrev == null
                    || !message.getCreatorUserId().equals(messagePrev.getCreatorUserId())
                    || messagePrev.getCreateDate() > message.getCreateDate() + 300) {
                holder.info.setVisibility(View.VISIBLE);

                String timeStr;
                if (message.getId() != null) {
                    timeStr = mTimeFormat.format(new Date(message.getCreateDate() * 1000L));
                } else {
                    timeStr = getString(R.string.now);
                }

                if (holder.avatar == null) {
                    holder.info.setText(timeStr);
                } else {
                    holder.info.setText(String.format("%1$s · %2$s",
                            message.getCreatorName(),
                            timeStr));
                }
            } else {
                holder.info.setVisibility(View.GONE);
            }

            holder.message.setText(message.getBodyPlainText());

            holder.attachments.removeAllViews();
            boolean hasAttachments = false;
            Iterator<Api.Attachment> attachmentsIterator = message.getAttachmentsIterator();
            while (attachmentsIterator.hasNext()) {
                final Api.Attachment attachment = attachmentsIterator.next();
                ImageView attachmentImageView = new AppCompatImageView(DiscussionActivity.this);

                int thumbnailSize = getResources().getDimensionPixelSize(R.dimen.attachment_thumbnail_size);
                int thumbnailMargin = getResources().getDimensionPixelSize(R.dimen.attachment_thumbnail_margin);
                LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(thumbnailSize, thumbnailSize);
                vp.setMargins(thumbnailMargin, thumbnailMargin, thumbnailMargin, thumbnailMargin);
                attachmentImageView.setLayoutParams(vp);
                attachmentImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                attachmentImageView.setContentDescription(getString(R.string.attachment_id_x_name_y,
                        attachment.getAttachmentId(), attachment.getFileName()));
                App.getInstance().getNetworkImageLoader().get(
                        attachment.getThumbnail(),
                        ImageLoader.getImageListener(attachmentImageView, R.drawable.avatar_l, 0)
                );

                attachmentImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(attachment.getPermalink()));
                        startActivity(browserIntent);
                    }
                });

                holder.attachments.addView(attachmentImageView);
                hasAttachments = true;
            }
            holder.attachments.setVisibility(hasAttachments ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        void clear() {
            mData.clear();
            notifyDataSetChanged();
        }

        Api.DiscussionMessage getMessage(int index) {
            return mData.get(index);
        }

        void addMessage(Api.DiscussionMessage message) {
            mData.add(message);
            notifyItemInserted(mData.size() - 1);
        }

        void prependMessage(Api.DiscussionMessage message) {
            mData.add(0, message);
            notifyItemInserted(0);
        }

        void removeMessage(int index) {
            mData.remove(index);
            notifyItemRemoved(index);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView message;
        final LinearLayout attachments;
        final TextView info;

        public ViewHolder(View v) {
            super(v);

            avatar = (ImageView) v.findViewById(R.id.avatar);
            message = (TextView) v.findViewById(R.id.message);
            attachments = (LinearLayout) v.findViewById(R.id.attachments);
            info = (TextView) v.findViewById(R.id.info);
        }
    }
}
