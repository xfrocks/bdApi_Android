package com.xfrocks.api.androiddemo.discussion;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.xfrocks.api.androiddemo.R;
import com.xfrocks.api.androiddemo.auth.LoginActivity;
import com.xfrocks.api.androiddemo.common.Api;
import com.xfrocks.api.androiddemo.common.model.ApiAccessToken;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussion;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussionMessage;

import java.text.Format;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

abstract public class DiscussionListActivity extends AppCompatActivity {

    public static final String EXTRA_DISCUSSION_CONTAINER_ID = "container_id";
    private static final String STATE_ACCESS_TOKEN = "accessToken";
    private static final String STATE_DISCUSSION_CONTAINER_ID = "containerId";

    private ProgressBar mProgressBar;
    private LinearLayoutManager mLayoutManager;

    private ApiAccessToken mAccessToken;
    int mDiscussionContainerId;
    private int mPages;
    private int mPage;

    private DiscussionsRequest mDiscussionsRequest;
    private DiscussionsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discussion_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        RecyclerView mDiscussionList = (RecyclerView) findViewById(R.id.discussion_list);
        mDiscussionList.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(this);
        mDiscussionList.setLayoutManager(mLayoutManager);

        mDiscussionList.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy < 0
                        && mLayoutManager.findLastCompletelyVisibleItemPosition() > mAdapter.getItemCount() - 5
                        && mPage < mPages
                        && mDiscussionsRequest == null) {
                    new DiscussionsRequest(mPage + 1).start();
                }
            }
        });

        mAdapter = new DiscussionsAdapter(this);
        mDiscussionList.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_discussion_list, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent mainIntent = getIntent();
        if (mainIntent != null) {
            updateStatesFromIntent(mainIntent);
        }

        if (mDiscussionContainerId == 0) {
            finish();
            return;
        }

        if (mAccessToken != null) {
            if (mAdapter.getItemCount() == 0) {
                new DiscussionsRequest(1).start();
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
            int existingId = mDiscussionContainerId;
            updateStatesFromIntent(intent);

            if (mDiscussionContainerId != existingId) {
                new DiscussionsRequest(1).start();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mDiscussionsRequest != null) {
            mDiscussionsRequest.cancel();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_ACCESS_TOKEN, mAccessToken);
        outState.putInt(STATE_DISCUSSION_CONTAINER_ID, mDiscussionContainerId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState.containsKey(STATE_ACCESS_TOKEN)
                && savedInstanceState.containsKey(STATE_DISCUSSION_CONTAINER_ID)) {
            mAccessToken = (ApiAccessToken) savedInstanceState.getSerializable(STATE_ACCESS_TOKEN);

            mDiscussionContainerId = savedInstanceState.getInt(STATE_DISCUSSION_CONTAINER_ID);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (mDiscussionContainerId > 0 && mAccessToken != null) {
                    new DiscussionsRequest(1).start();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void setTheProgressBarVisibility(boolean visible) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String getLoginRedirectToPrefix() {
        return getClass().getSimpleName() + "://";
    }

    private String getLoginRedirectToFull() {
        return getLoginRedirectToPrefix() + mDiscussionContainerId;
    }

    private void updateStatesFromIntent(Intent intent) {
        if (intent.hasExtra(DiscussionActivity.EXTRA_ACCESS_TOKEN)) {
            mAccessToken = (ApiAccessToken) intent.getSerializableExtra(DiscussionActivity.EXTRA_ACCESS_TOKEN);
        }

        if (intent.hasExtra(EXTRA_DISCUSSION_CONTAINER_ID)) {
            mDiscussionContainerId = intent.getIntExtra(EXTRA_DISCUSSION_CONTAINER_ID, mDiscussionContainerId);
        }

        if (intent.hasExtra(DiscussionActivity.EXTRA_LOGIN_REDIRECTED_TO)) {
            String loginRedirectedTo = intent.getStringExtra(DiscussionActivity.EXTRA_LOGIN_REDIRECTED_TO);
            String loginRedirectToPrefix = getLoginRedirectToPrefix();
            if (loginRedirectedTo.startsWith(loginRedirectToPrefix)) {
                try {
                    mDiscussionContainerId = Integer.parseInt(loginRedirectedTo.substring(loginRedirectToPrefix.length()));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
    }

    abstract String getGetDiscussionsUrl();

    abstract Api.Params getGetDiscussionsParams(int page, ApiAccessToken accessToken);

    abstract ParsedDiscussions parseResponseForDiscussions(String response);

    interface ParsedDiscussions {
        List<? extends ApiDiscussion> getDiscussions();

        Integer getPage();

        Integer getPages();
    }

    class DiscussionsRequest extends Api.GetRequest {

        final int mRequestPage;

        DiscussionsRequest(int page) {
            super(getGetDiscussionsUrl(), getGetDiscussionsParams(page, mAccessToken));

            mRequestPage = page;
        }

        @Override
        protected void onStart() {
            if (mDiscussionsRequest != null) {
                mDiscussionsRequest.cancel();
            }

            mDiscussionsRequest = this;

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

            ParsedDiscussions data = parseResponseForDiscussions(response);

            List<? extends ApiDiscussion> discussions = data.getDiscussions();
            if (discussions != null) {
                for (ApiDiscussion discussion : discussions) {
                    mAdapter.addDiscussion(discussion);
                }
            }

            Integer page = data.getPage();
            if (page != null) {
                mPage = page;
            }

            Integer pages = data.getPages();
            if (pages != null) {
                mPages = pages;
            }
        }

        @Override
        protected void onComplete() {
            mDiscussionsRequest = null;
            setTheProgressBarVisibility(false);
        }
    }

    class DiscussionsAdapter extends RecyclerView.Adapter<ViewHolder> {
        final ArrayList<ApiDiscussion> mData = new ArrayList<>();
        final Format mTimeFormat;

        DiscussionsAdapter(Context context) {
            mTimeFormat = android.text.format.DateFormat.getTimeFormat(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            int resId = R.layout.list_item_discussion;

            View v = LayoutInflater.from(parent.getContext()).inflate(resId, parent, false);

            return new ViewHolder(v, this);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ApiDiscussion discussion = mData.get(position);

            holder.avatar.setContentDescription(discussion.getCreatorName());
            Glide.with(DiscussionListActivity.this)
                    .load(discussion.getCreatorAvatar())
                    .placeholder(R.drawable.avatar_l)
                    .into(holder.avatar);

            holder.title.setText(discussion.getTitle());

            holder.info.setText(String.format("%1$s · %2$s",
                    discussion.getCreatorName(),
                    mTimeFormat.format(new Date(discussion.getCreateDate() * 1000L))));

            String firstMessageBodyPlainText = null;
            ApiDiscussionMessage firstMessage = discussion.getFirstMessage();
            if (firstMessage != null) {
                firstMessageBodyPlainText = firstMessage.getBodyPlainText();
            }
            if (!TextUtils.isEmpty(firstMessageBodyPlainText)) {
                holder.message.setVisibility(View.VISIBLE);
                holder.message.setText(firstMessageBodyPlainText);
            } else {
                holder.message.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        void onViewClick(ViewHolder vh) {
            int position = vh.getAdapterPosition();
            ApiDiscussion discussion = mData.get(position);
            if (discussion == null) {
                return;
            }

            Intent threadIntent = new Intent(DiscussionListActivity.this, ThreadActivity.class);
            threadIntent.putExtra(ThreadActivity.EXTRA_ACCESS_TOKEN, mAccessToken);
            threadIntent.putExtra(ThreadActivity.EXTRA_DISCUSSION_ID, discussion.getId());

            startActivity(threadIntent);
        }

        void clear() {
            mData.clear();
            notifyDataSetChanged();
        }

        void addDiscussion(ApiDiscussion discussion) {
            mData.add(discussion);
            notifyItemInserted(mData.size() - 1);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView title;
        final TextView info;
        final TextView message;

        ViewHolder(View v, final DiscussionsAdapter adapter) {
            super(v);

            avatar = (ImageView) v.findViewById(R.id.avatar);
            title = (TextView) v.findViewById(R.id.title);
            info = (TextView) v.findViewById(R.id.info);
            message = (TextView) v.findViewById(R.id.message);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    adapter.onViewClick(ViewHolder.this);
                }
            });
        }
    }
}
