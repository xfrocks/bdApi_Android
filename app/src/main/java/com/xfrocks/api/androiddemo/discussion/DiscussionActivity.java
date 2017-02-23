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
import com.bumptech.glide.Glide;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.R;
import com.xfrocks.api.androiddemo.auth.LoginActivity;
import com.xfrocks.api.androiddemo.common.Api;
import com.xfrocks.api.androiddemo.common.ApiBaseResponse;
import com.xfrocks.api.androiddemo.common.ApiUsersMeRequest;
import com.xfrocks.api.androiddemo.common.model.ApiAccessToken;
import com.xfrocks.api.androiddemo.common.model.ApiAttachment;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussion;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussionMessage;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussionMessageInTransit;
import com.xfrocks.api.androiddemo.common.model.ApiUser;
import com.xfrocks.api.androiddemo.common.persist.ObjectAsFile;

import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

abstract public class DiscussionActivity extends AppCompatActivity implements QuickReplyFragment.Listener, ApiUsersMeRequest.Listener {

    public static final String EXTRA_ACCESS_TOKEN = "access_token";
    public static final String EXTRA_USER = "user";
    public static final String EXTRA_DISCUSSION_ID = "discussion_id";
    public static final String EXTRA_LOGIN_REDIRECTED_TO = "redirected_to";
    private static final String STATE_ACCESS_TOKEN = "accessToken";
    private static final String STATE_DISCUSSION_ID = "discussionId";

    private ProgressBar mProgressBar;
    private RecyclerView mMessageList;
    private LinearLayoutManager mLayoutManager;

    private QuickReplyFragment mQuickReply;

    private ApiAccessToken mAccessToken;
    private ApiUser mUser;
    private ApiDiscussion mDiscussion;
    private int mPages;
    private int mPage;

    private MessagesRequest mMessagesRequest;
    private PatchRequest mPatchRequest;
    MessagesAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mMessageList = (RecyclerView) findViewById(R.id.message_list);
        mMessageList.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mMessageList.setLayoutManager(mLayoutManager);

        mMessageList.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy >= 0
                        || mLayoutManager.findLastCompletelyVisibleItemPosition() <= mAdapter.getItemCount() - 5
                        || mPage >= mPages
                        || mMessagesRequest != null) {
                    return;
                }

                new MessagesRequest(mPage + 1).start();
            }
        });

        mAdapter = new MessagesAdapter(this);
        mMessageList.setAdapter(mAdapter);

        mQuickReply = (QuickReplyFragment) getSupportFragmentManager().findFragmentById(R.id.quick_reply);
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
                new ApiUsersMeRequest(this, mAccessToken).start();
            }

            if (mAdapter.getItemCount() == 0) {
                new MessagesRequest(1).start();
            }
        } else {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.putExtra(LoginActivity.EXTRA_REDIRECT_TO, getLoginRedirectToFull());

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
                new MessagesRequest(1).start();
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
            mAccessToken = (ApiAccessToken) savedInstanceState.getSerializable(STATE_ACCESS_TOKEN);

            int discussionId = savedInstanceState.getInt(STATE_DISCUSSION_ID);
            setDiscussion(discussionId);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (getDiscussionId() > 0 && mAccessToken != null) {
                    new MessagesRequest(1).start();
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
    public ApiAccessToken getEffectiveAccessToken() {
        return mAccessToken;
    }

    @Override
    public void onQuickReplySubmit() {
        mQuickReply.clearViews();
        new PostMessageRequest().start();
    }

    @Override
    public void onUsersMeRequestSuccess(ApiUser user) {
        mUser = user;
    }

    private void setTheProgressBarVisibility(boolean visible) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    void setDiscussion(ApiDiscussion discussion) {
        mDiscussion = discussion;

        setTitle(discussion.getTitle());
        mQuickReply.setup(discussion, this);

        if (discussion.isComplete()) {
            ObjectAsFile.save(this, ObjectAsFile.LATEST_DISCUSSION, discussion);
        }
    }

    int getDiscussionId() {
        if (mDiscussion == null) {
            return 0;
        }

        return mDiscussion.getId();
    }

    private String getLoginRedirectToPrefix() {
        return getClass().getSimpleName() + "://";
    }

    private String getLoginRedirectToFull() {
        return getLoginRedirectToPrefix() + getDiscussionId();
    }

    private void updateStatesFromIntent(Intent intent) {
        int discussionId = getDiscussionId();

        if (intent.hasExtra(EXTRA_ACCESS_TOKEN)) {
            mAccessToken = (ApiAccessToken) intent.getSerializableExtra(EXTRA_ACCESS_TOKEN);
        }

        if (intent.hasExtra(EXTRA_USER)) {
            mUser = (ApiUser) intent.getSerializableExtra(EXTRA_USER);
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
            setDiscussion(discussionId);
        }
    }

    abstract void setDiscussion(int discussionId);

    abstract ParsedMessages parseResponseForMessages(String response);

    interface ParsedMessages {
        List<? extends ApiDiscussionMessage> getMessages();

        ApiDiscussion getDiscussion();

        Integer getPage();

        Integer getPages();
    }

    class MessagesRequest extends Api.GetRequest {

        final int mRequestPage;

        MessagesRequest(int page) {
            super(mDiscussion.getGetMessagesUrl(), mDiscussion.getGetMessagesParams(page, mAccessToken));

            mRequestPage = page;
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
        protected void onSuccess(String response) {
            if (mRequestPage == 1) {
                mAdapter.clear();
            }
            mPage = mRequestPage;

            ParsedMessages data = parseResponseForMessages(response);
            List<? extends ApiDiscussionMessage> messages = data.getMessages();
            if (messages != null) {
                for (ApiDiscussionMessage message : messages) {
                    mAdapter.addMessage(message);
                }
            }

            ApiDiscussion discussion = data.getDiscussion();
            if (discussion != null) {
                setDiscussion(discussion);
            }

            Integer page = data.getPage();
            if (page != null) {
                mPage = page;
            }

            Integer pages = data.getPages();
            if (pages != null) {
                mPages = pages;
            }

            if (mPage == 1) {
                mMessageList.scrollToPosition(0);
            }
        }

        @Override
        protected void onComplete() {
            mMessagesRequest = null;
            setTheProgressBarVisibility(false);
        }
    }

    class PatchRequest extends Api.GetRequest {
        ApiDiscussionMessage mLatestMessage;
        final List<ApiDiscussionMessage> mNewMessages = new ArrayList<>();

        PatchRequest() {
            super(mDiscussion.getGetMessagesUrl(), mDiscussion.getGetMessagesParams(1, mAccessToken));
        }

        @Override
        protected void onStart() {
            if (mPatchRequest != null) {
                mPatchRequest.cancel();
            }

            mPatchRequest = this;
        }

        @Override
        protected void onSuccess(String response) {
            mLatestMessage = null;
            while (mAdapter.getItemCount() > 0) {
                ApiDiscussionMessage message = mAdapter.getMessageAt0();

                if (message.getId() == null) {
                    mAdapter.removeMessageAt0();
                } else {
                    mLatestMessage = message;
                    break;
                }
            }

            ParsedMessages data = parseResponseForMessages(response);
            List<? extends ApiDiscussionMessage> messages = data.getMessages();
            if (messages != null) {
                for (ApiDiscussionMessage message : messages) {
                    if (mLatestMessage == null
                            || mLatestMessage.getId() < message.getId()) {
                        mNewMessages.add(message);
                    } else {
                        break;
                    }
                }
            }

            for (int i = mNewMessages.size() - 1; i >= 0; i--) {
                mAdapter.prependMessage(mNewMessages.get(i));
            }

            mMessageList.scrollToPosition(0);
        }

        @Override
        protected void onComplete() {
            mPatchRequest = null;
        }
    }

    class PostMessageRequest extends Api.PostRequest {
        final ApiDiscussionMessageInTransit mInTransitMessage;

        PostMessageRequest() {
            super(mDiscussion.getPostMessagesUrl(),
                    mDiscussion.getPostMessagesParams(
                            mQuickReply.getPendingMessage(),
                            mQuickReply.getAttachmentHash(), mAccessToken
                    )
            );

            mInTransitMessage = new ApiDiscussionMessageInTransit(mUser, mQuickReply.getPendingMessage());
        }

        @Override
        protected void onStart() {
            if (mInTransitMessage != null) {
                mAdapter.prependMessage(mInTransitMessage);
                mMessageList.scrollToPosition(0);
            }
        }

        @Override
        protected void onSuccess(String response) {
            ApiBaseResponse data = App.getGsonInstance().fromJson(response, ApiBaseResponse.class);
            String error = data.getError();
            if (!TextUtils.isEmpty(error)) {
                showError(error);
                return;
            }

            new PatchRequest().start();
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

            mInTransitMessage.errorMessage = errorMessage;
            mAdapter.notifyMessageChanged(mInTransitMessage);
        }
    }

    class MessagesAdapter extends RecyclerView.Adapter<ViewHolder> {
        static final int VIEW_TYPE_MINE = 0;
        static final int VIEW_TYPE_OTHER = 1;

        final ArrayList<ApiDiscussionMessage> mData = new ArrayList<>();
        final Format mTimeFormat;

        MessagesAdapter(Context context) {
            mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        }

        @Override
        public int getItemViewType(int position) {
            ApiDiscussionMessage message = mData.get(position);

            if (mUser != null
                    && mUser.getId().equals(message.getCreatorUserId())) {
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
            ApiDiscussionMessage message = mData.get(position);
            ApiDiscussionMessage messagePrev = null;
            ApiDiscussionMessage messageNext = null;
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
                    Glide.with(DiscussionActivity.this)
                            .load(message.getCreatorAvatar())
                            .placeholder(R.drawable.avatar_l)
                            .into(holder.avatar);
                } else {
                    holder.avatar.setVisibility(View.GONE);
                }
            }

            if (messagePrev == null
                    || !message.getCreatorUserId().equals(messagePrev.getCreatorUserId())
                    || messagePrev.getCreateDate() > message.getCreateDate() + 300) {
                holder.info.setVisibility(View.VISIBLE);

                String timeStr;
                if (message instanceof ApiDiscussionMessageInTransit) {
                    ApiDiscussionMessageInTransit messageInTransit = (ApiDiscussionMessageInTransit) message;
                    if (!TextUtils.isEmpty(messageInTransit.errorMessage)) {
                        timeStr = getString(R.string.not_sent);
                    } else {
                        timeStr = getString(R.string.now);
                    }
                } else {
                    timeStr = mTimeFormat.format(new Date(message.getCreateDate() * 1000L));
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
            Iterator<ApiAttachment> attachmentsIterator = message.getAttachmentsIterator();
            while (attachmentsIterator != null && attachmentsIterator.hasNext()) {
                final ApiAttachment attachment = attachmentsIterator.next();
                ImageView attachmentImageView = new AppCompatImageView(DiscussionActivity.this);

                int thumbnailSize = getResources().getDimensionPixelSize(R.dimen.attachment_thumbnail_size);
                int thumbnailMargin = getResources().getDimensionPixelSize(R.dimen.attachment_thumbnail_margin);
                LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(thumbnailSize, thumbnailSize);
                vp.setMargins(thumbnailMargin, thumbnailMargin, thumbnailMargin, thumbnailMargin);
                attachmentImageView.setLayoutParams(vp);
                attachmentImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                attachmentImageView.setContentDescription(getString(R.string.attachment_id_x_name_y,
                        attachment.getId(), attachment.getFileName()));
                Glide.with(DiscussionActivity.this)
                        .load(attachment.getThumbnail())
                        .placeholder(R.drawable.avatar_l)
                        .into(attachmentImageView);

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

        void addMessage(ApiDiscussionMessage message) {
            mData.add(message);
            notifyItemInserted(mData.size() - 1);
        }

        void prependMessage(ApiDiscussionMessage message) {
            mData.add(0, message);
            notifyItemInserted(0);
        }

        ApiDiscussionMessage getMessageAt0() {
            return mData.get(0);
        }

        void removeMessageAt0() {
            mData.remove(0);
            notifyItemRemoved(0);
        }

        void notifyMessageChanged(ApiDiscussionMessage message) {
            int position = mData.indexOf(message);
            if (position > -1) {
                notifyItemChanged(position);
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView message;
        final LinearLayout attachments;
        final TextView info;

        ViewHolder(View v) {
            super(v);

            avatar = (ImageView) v.findViewById(R.id.avatar);
            message = (TextView) v.findViewById(R.id.message);
            attachments = (LinearLayout) v.findViewById(R.id.attachments);
            info = (TextView) v.findViewById(R.id.info);
        }
    }
}
