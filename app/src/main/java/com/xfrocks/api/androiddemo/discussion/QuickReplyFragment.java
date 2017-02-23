package com.xfrocks.api.androiddemo.discussion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.xfrocks.api.androiddemo.App;
import com.xfrocks.api.androiddemo.R;
import com.xfrocks.api.androiddemo.common.ApiConstants;
import com.xfrocks.api.androiddemo.common.ChooserIntent;
import com.xfrocks.api.androiddemo.common.model.ApiAccessToken;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussion;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

public class QuickReplyFragment extends Fragment {

    private static final int RC_PICK_FILE = 1;

    private LinearLayout mExtra;
    private ImageButton mAttach;
    private ImageButton mCamera;
    private EditText mMessage;
    private ImageButton mReply;

    private ApiDiscussion mDiscussion;
    private Listener mListener;

    private AttachmentsAdapter mPendingAttachments;
    private String mPendingMessage;

    @Override
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

        mCamera = (ImageButton) view.findViewById(R.id.camera);
        mCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptCamera();
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
        mMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                toggleExtraPanel();
            }
        });

        mReply = (ImageButton) view.findViewById(R.id.reply);
        mReply.setOnClickListener(new View.OnClickListener() {
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

    void setup(ApiDiscussion discussion, Listener listener) {
        mDiscussion = discussion;
        mListener = listener;

        View view = getView();
        if (view == null) {
            return;
        }

        if (discussion.canPostMessage()) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
            return;
        }

        toggleExtraPanel();
    }

    void setEditTextMessageMultiLine() {
        mMessage.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mMessage.setMinLines(3);

        ViewGroup.LayoutParams lp = mMessage.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        mMessage.setLayoutParams(lp);
    }

    void setButtonReplyVisibilityGone() {
        mReply.setVisibility(View.GONE);
    }

    String getAttachmentHash() {
        if (mDiscussion == null) {
            return "";
        }

        return mDiscussion.toString() + mDiscussion.getId();
    }

    String getPendingMessage() {
        return mPendingMessage;
    }

    private void toggleExtraPanel() {
        boolean canAttach = false;
        int visible = 0;

        if (mMessage.hasFocus()) {
            visible++;
        }

        if (mDiscussion != null && mDiscussion.canUploadAttachment()) {
            canAttach = true;
        }
        if (canAttach) {
            mAttach.setVisibility(View.VISIBLE);
            mCamera.setVisibility(View.VISIBLE);

            if (mPendingAttachments.getItemCount() > 0) {
                visible++;
            }
        } else {
            mAttach.setVisibility(View.GONE);
            mCamera.setVisibility(View.GONE);
        }

        mExtra.setVisibility(visible > 0 ? View.VISIBLE : View.GONE);
    }

    private void attemptAttach() {
        Intent chooserIntent = ChooserIntent.create(getContext(), R.string.pick_file_to_attach, "*/*");
        startActivityForResult(chooserIntent, RC_PICK_FILE);
    }

    private void attemptCamera() {
        Intent[] cameraIntents = ChooserIntent.buildCameraIntents(getContext());
        if (cameraIntents.length == 0) {
            // TODO: device without camera?
            return;
        }

        startActivityForResult(cameraIntents[0], RC_PICK_FILE);
    }

    private void attemptResize(final Uri uri, int size) {
        Glide.with(getActivity().getApplicationContext())
                .load(uri)
                .asBitmap()
                .override(size, size)
                .fitCenter()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        onImageResized(uri, resource);
                    }
                });
    }

    private void onImageResized(Uri uri, Bitmap resource) {
        String fileName = ChooserIntent.getFileNameFromUri(getContext(), uri);
        String prefix = fileName;
        String suffix = null;
        int indexOfDot = fileName.lastIndexOf(".");
        if (indexOfDot > -1) {
            prefix = fileName.substring(0, indexOfDot);
            suffix = fileName.substring(indexOfDot + 1).toLowerCase();
        }
        Bitmap.CompressFormat format = Bitmap.CompressFormat.JPEG;
        if ("png".equals(suffix)) {
            format = Bitmap.CompressFormat.PNG;
        } else if ("webp".equals(suffix)) {
            format = Bitmap.CompressFormat.WEBP;
        } else {
            suffix = "jpg";
        }

        File outputDir = getContext().getCacheDir();
        File outputFile;
        try {
            outputFile = File.createTempFile(prefix, "." + suffix, outputDir);
            FileOutputStream out = new FileOutputStream(outputFile);
            resource.compress(format, 70, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        uploadAttach(uri, outputFile.getPath(), fileName);
    }

    void uploadAttach(Uri uri) {
        int size = App.getFeatureAttachmentResize();
        if (size > 0) {
            attemptResize(uri, size);
        } else {
            uploadAttach(uri, uri.getPath(), null);
        }
    }

    private void uploadAttach(Uri uri, String path, String fileName) {
        ApiAccessToken accessToken = null;
        if (mListener != null) {
            accessToken = mListener.getEffectiveAccessToken();
        }
        if (accessToken == null) {
            return;
        }

        String serverUrl = mDiscussion.getPostAttachmentsUrl(getAttachmentHash(), accessToken);
        if (TextUtils.isEmpty(serverUrl)) {
            return;
        }

        if (path.startsWith("file:///")) {
            path = path.substring(7);
        }

        try {
            String uploadId = new MultipartUploadRequest(getContext(), serverUrl)
                    .addFileToUpload(path, ApiConstants.PARAM_FILE, fileName)
                    .setUtf8Charset()
                    .setAutoDeleteFilesAfterSuccessfulUpload(true)
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

    void attemptReply() {
        mPendingMessage = mMessage.getText().toString().trim();
        if (mPendingMessage.isEmpty()) {
            return;
        }

        if (mListener == null) {
            return;
        }
        mListener.onQuickReplySubmit();
    }

    void clearViews() {
        mMessage.setText("");
        mPendingAttachments.clearAndNotify();
    }

    interface Listener {

        ApiAccessToken getEffectiveAccessToken();

        void onQuickReplySubmit();
    }

    class AttachmentsAdapter extends RecyclerView.Adapter<AttachmentViewHolder> {
        final ArrayList<Attachment> mAttachments = new ArrayList<>();

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

        void addAttachmentAndNotify(Attachment attachment) {
            mAttachments.add(attachment);
            notifyItemInserted(mAttachments.size() - 1);
        }

        void removeAndNotify(String uploadId) {
            int foundIndex = indexOfUploadId(uploadId);

            if (foundIndex > -1) {
                mAttachments.remove(foundIndex);
                notifyItemRemoved(foundIndex);
            }
        }

        void updateAndNotify(String uploadId, int percent) {
            int foundIndex = indexOfUploadId(uploadId);

            if (foundIndex > -1) {
                mAttachments.get(foundIndex).percent = percent;
                notifyItemChanged(foundIndex);
            }
        }

        void clearAndNotify() {
            mAttachments.clear();
            notifyDataSetChanged();
        }

        int indexOfUploadId(String uploadId) {
            for (int i = 0; i < mAttachments.size(); i++) {
                if (uploadId.equals(mAttachments.get(i).uploadId)) {
                    return i;
                }
            }

            return -1;
        }
    }

    class Attachment {
        Uri uri;
        String uploadId;
        int percent;
    }

    class AttachmentViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final ImageView uploaded;

        AttachmentViewHolder(View v) {
            super(v);

            thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
            uploaded = (ImageView) v.findViewById(R.id.uploaded);
        }
    }
}
