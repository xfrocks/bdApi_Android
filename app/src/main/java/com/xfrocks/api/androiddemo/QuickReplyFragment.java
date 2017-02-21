package com.xfrocks.api.androiddemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.xfrocks.api.androiddemo.helper.ChooserIntent;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class QuickReplyFragment extends Fragment {

    private static final int RC_PICK_FILE = 1;

    private LinearLayout mExtra;
    private ImageButton mAttach;
    private EditText mMessage;

    private String mUrlAttachments;
    private Listener mListener;

    private AttachmentsAdapter mPendingAttachments;
    private String mPendingMessage;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quick_reply, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mExtra = (LinearLayout) view.findViewById(R.id.extra);

        RecyclerView attachments = (RecyclerView) view.findViewById(R.id.attachments);
        LinearLayoutManager attachmentsLayoutManager = new LinearLayoutManager(getContext());
        attachmentsLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        attachments.setLayoutManager(attachmentsLayoutManager);

        mPendingAttachments = new AttachmentsAdapter();
        attachments.setAdapter(mPendingAttachments);

        mAttach = (ImageButton) view.findViewById(R.id.attach);
        mAttach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAttach();
            }
        });

        mMessage = (EditText) view.findViewById(R.id.message);
        mMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                attemptReply();
                return true;
            }
        });

        ImageButton reply = (ImageButton) view.findViewById(R.id.reply);
        reply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptReply();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_PICK_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = ChooserIntent.getUriFromChooser(getContext(), data);
                    if (uri != null && mListener != null) {
                        uploadAttach(uri);
                    }
                }
                break;
        }
    }

    public void setup(boolean visible, Listener listener) {
        mListener = listener;

        View view = getView();
        if (view == null) {
            return;
        }

        if (visible) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    public void setupAttach(String urlAttachments) {
        mUrlAttachments = urlAttachments;

        toggleExtraPanel();
    }

    public String getPendingMessage() {
        return mPendingMessage;
    }

    public void restorePending() {
        mMessage.setText(mPendingMessage);
        mMessage.selectAll();
        mPendingMessage = null;
    }

    private void toggleExtraPanel() {
        boolean canAttach = false;
        int visible = 0;

        if (mUrlAttachments != null && mUrlAttachments.length() > 0) {
            canAttach = true;
        }
        if (canAttach) {
            mAttach.setVisibility(View.VISIBLE);
            visible++;
        } else {
            mAttach.setVisibility(View.GONE);
        }

        mExtra.setVisibility(visible > 0 ? View.VISIBLE : View.GONE);
    }

    private void attemptAttach() {
        Intent chooserIntent = ChooserIntent.create(getContext(), R.string.pick_file_to_attach, "*/*");
        startActivityForResult(chooserIntent, RC_PICK_FILE);
    }

    private void uploadAttach(Uri uri) {
        try {
            String uploadId = new MultipartUploadRequest(getContext(), mUrlAttachments)
                    .addFileToUpload(uri.toString(), Api.PARAM_FILE)
                    .setUtf8Charset()
                    .setMaxRetries(2)
                    .setDelegate(new UploadStatusDelegate() {
                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {
                            if (isDetached()) {
                                return;
                            }

                            onAttachProgress(uploadInfo);
                        }

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, Exception exception) {
                            if (isDetached()) {
                                return;
                            }

                            onAttachFailed(uploadInfo);
                        }

                        @Override
                        public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                            if (isDetached()) {
                                return;
                            }

                            onAttachSuccess(uploadInfo);
                        }

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {
                            if (isDetached()) {
                                return;
                            }

                            onAttachFailed(uploadInfo);
                        }
                    })
                    .setNotificationConfig(new UploadNotificationConfig())
                    .startUpload();

            Attachment attachment = new Attachment();
            attachment.uri = uri;
            attachment.uploadId = uploadId;
            mPendingAttachments.addAttachmentAndNotify(attachment);
        } catch (MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void onAttachProgress(UploadInfo uploadInfo) {
        mPendingAttachments.updateAndNotify(uploadInfo.getUploadId(), uploadInfo.getProgressPercent());
    }

    private void onAttachFailed(UploadInfo uploadInfo) {
        mPendingAttachments.removeAndNotify(uploadInfo.getUploadId());
    }

    private void onAttachSuccess(UploadInfo uploadInfo) {
        mPendingAttachments.updateAndNotify(uploadInfo.getUploadId(), 100);
    }

    private void attemptReply() {
        mPendingMessage = mMessage.getText().toString().trim();
        if (mPendingMessage.isEmpty()) {
            return;
        }
        mMessage.setText("");

        mPendingAttachments.clearAndNotify();

        if (mListener == null) {
            return;
        }
        mListener.onQuickReplySubmit(this);
    }

    public interface Listener {
        void onQuickReplySubmit(QuickReplyFragment qr);
    }

    private class AttachmentsAdapter extends RecyclerView.Adapter<AttachmentViewHolder> {
        private ArrayList<Attachment> mAttachments = new ArrayList<>();

        @Override
        public AttachmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_attachment, parent, false);

            return new AttachmentViewHolder(v);
        }

        @Override
        public void onBindViewHolder(AttachmentViewHolder holder, int position) {
            Attachment attachment = mAttachments.get(position);
            holder.thumbnail.setImageURI(attachment.uri);

            holder.thumbnail.setAlpha(.5f + attachment.percent * .005f);
            holder.uploaded.setVisibility(attachment.percent > 99 ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return mAttachments.size();
        }

        public void addAttachmentAndNotify(Attachment attachment) {
            mAttachments.add(attachment);
            notifyItemInserted(mAttachments.size() - 1);
        }

        public void removeAndNotify(String uploadId) {
            int foundIndex = indexOfUploadId(uploadId);

            if (foundIndex > -1) {
                mAttachments.remove(foundIndex);
                notifyItemRemoved(foundIndex);
            }
        }

        public void updateAndNotify(String uploadId, int percent) {
            int foundIndex = indexOfUploadId(uploadId);

            if (foundIndex > -1) {
                mAttachments.get(foundIndex).percent = percent;
                notifyItemChanged(foundIndex);
            }
        }

        public void clearAndNotify() {
            mAttachments.clear();
            notifyDataSetChanged();
        }

        private int indexOfUploadId(String uploadId) {
            for (int i = 0; i < mAttachments.size(); i++) {
                if (uploadId.equals(mAttachments.get(i).uploadId)) {
                    return i;
                }
            }

            return -1;
        }
    }

    private class Attachment {
        Uri uri;
        String uploadId;
        int percent;
    }

    private class AttachmentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final ImageView uploaded;

        public AttachmentViewHolder(View v) {
            super(v);

            thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
            uploaded = (ImageView) v.findViewById(R.id.uploaded);
        }
    }
}
