package com.xfrocks.api.androiddemo.discussion;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import com.xfrocks.api.androiddemo.common.AndroidPermissions;
import com.xfrocks.api.androiddemo.common.ApiConstants;
import com.xfrocks.api.androiddemo.common.ChooserIntent;
import com.xfrocks.api.androiddemo.common.model.ApiAccessToken;
import com.xfrocks.api.androiddemo.common.model.ApiDiscussion;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadService;
import net.gotev.uploadservice.UploadServiceBroadcastReceiver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.UUID;

public class QuickReplyFragment extends Fragment {

    private static final int RC_PICK_FILE = 1;
    private static final int RC_ATTEMPT_ATTACH = 2;
    private static final int RC_ATTEMPT_CAMERA = 3;
    private static final String STATE_PENDING_ATTACHMENTS = "pendingAttachments";
    private static final String STATE_ATTACHMENT_HASH = "attachmentHash";

    private LinearLayout mExtra;
    private ImageButton mAttach;
    private ImageButton mCamera;
    private EditText mMessage;
    private ImageButton mReply;

    private ApiDiscussion mDiscussion;
    private Listener mListener;

    private Receiver mReceiver;
    private AndroidPermissions mUploadPermissions;
    private AttachmentsAdapter mPendingAttachmentsAdapter;
    private String mPendingMessage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReceiver = new Receiver();
        mUploadPermissions = new AndroidPermissions(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        mPendingAttachmentsAdapter = new AttachmentsAdapter();
    }

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
        attachments.setAdapter(mPendingAttachmentsAdapter);

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
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mReceiver, new IntentFilter(UploadService.NAMESPACE + ".uploadservice.broadcast.status"));
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case RC_ATTEMPT_ATTACH:
                if (mUploadPermissions.areAllRequiredPermissionsGranted(permissions, grantResults)) {
                    attemptAttachStep2();
                }
                break;
            case RC_ATTEMPT_CAMERA:
                if (mUploadPermissions.areAllRequiredPermissionsGranted(permissions, grantResults)) {
                    attemptCameraStep2();
                }
                break;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_PENDING_ATTACHMENTS, mPendingAttachmentsAdapter.mData);
        outState.putString(STATE_ATTACHMENT_HASH, mPendingAttachmentsAdapter.mAttachmentHash);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_PENDING_ATTACHMENTS)) {
                //noinspection unchecked
                mPendingAttachmentsAdapter.mData = (ArrayList<Attachment>) savedInstanceState.getSerializable(STATE_PENDING_ATTACHMENTS);
                mPendingAttachmentsAdapter.notifyDataSetChanged();
            }

            if (savedInstanceState.containsKey(STATE_ATTACHMENT_HASH)) {
                mPendingAttachmentsAdapter.mAttachmentHash = savedInstanceState.getString(STATE_ATTACHMENT_HASH);
            }
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
        return mPendingAttachmentsAdapter.mAttachmentHash;
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

            if (mPendingAttachmentsAdapter.getItemCount() > 0) {
                visible++;
            }
        } else {
            mAttach.setVisibility(View.GONE);
            mCamera.setVisibility(View.GONE);
        }

        mExtra.setVisibility(visible > 0 ? View.VISIBLE : View.GONE);
    }

    private void attemptAttach() {
        if (mUploadPermissions.needRequesting()) {
            mUploadPermissions.requestPermissions(this, RC_ATTEMPT_ATTACH);
            return;
        }

        attemptAttachStep2();
    }

    private void attemptAttachStep2() {
        Intent chooserIntent = ChooserIntent.create(getContext(), R.string.pick_file_to_attach, "*/*");
        startActivityForResult(chooserIntent, RC_PICK_FILE);
    }

    private void attemptCamera() {
        if (mUploadPermissions.needRequesting()) {
            mUploadPermissions.requestPermissions(this, RC_ATTEMPT_CAMERA);
            return;
        }

        attemptCameraStep2();
    }

    private void attemptCameraStep2() {
        Intent[] cameraIntents = ChooserIntent.buildCameraIntents(getContext());
        if (cameraIntents.length == 0) {
            // TODO: device without camera?
            return;
        }

        startActivityForResult(cameraIntents[0], RC_PICK_FILE);
    }

    private void attemptResize(final String attachmentUuid, final Uri uri, int size) {
        Glide.with(getActivity().getApplicationContext())
                .load(uri)
                .asBitmap()
                .override(size, size)
                .fitCenter()
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        onImageResized(attachmentUuid, uri, resource);
                    }
                });
    }

    private void onImageResized(String attachmentUuid, Uri uri, Bitmap resource) {
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

        File outputDir = getContext().getExternalCacheDir();
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

        uploadAttach(attachmentUuid, outputFile.getPath(), fileName);
    }

    void uploadAttach(Uri uri) {
        int size = App.getFeatureAttachmentResize();

        Attachment attachment = new Attachment();
        mPendingAttachmentsAdapter.addAndNotify(attachment);

        if (size > 0) {
            attemptResize(attachment.uuid, uri, size);
        } else {
            uploadAttach(attachment.uuid, uri.getPath(), null);
        }
    }

    private void uploadAttach(String attachmentUuid, String path, String fileName) {
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
                    .setMaxRetries(2)
                    .setNotificationConfig(new UploadNotificationConfig())
                    .startUpload();

            mPendingAttachmentsAdapter.updateAndNotify(attachmentUuid, uploadId, path);
        } catch (MalformedURLException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void onAttachProgress(UploadInfo uploadInfo) {
        mPendingAttachmentsAdapter.updateAndNotify(uploadInfo.getUploadId(), uploadInfo.getProgressPercent());
    }

    private void onAttachFailed(UploadInfo uploadInfo) {
        mPendingAttachmentsAdapter.removeAndNotify(uploadInfo.getUploadId());
    }

    private void onAttachSuccess(UploadInfo uploadInfo) {
        mPendingAttachmentsAdapter.updateAndNotify(uploadInfo.getUploadId(), 100);
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
        mPendingAttachmentsAdapter.clearAndNotify();
    }

    interface Listener {

        ApiAccessToken getEffectiveAccessToken();

        void onQuickReplySubmit();
    }

    class AttachmentsAdapter extends RecyclerView.Adapter<AttachmentViewHolder> {
        ArrayList<Attachment> mData = new ArrayList<>();
        String mAttachmentHash;

        AttachmentsAdapter() {
            generateAttachmentHash();
        }

        @Override
        public AttachmentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_attachment, parent, false);

            return new AttachmentViewHolder(v);
        }

        @Override
        public void onBindViewHolder(AttachmentViewHolder holder, int position) {
            Attachment attachment = mData.get(position);

            if (attachment.path != null) {
                Glide.with(getContext())
                        .load(attachment.path)
                        .into(holder.thumbnail);
            } else {
                holder.thumbnail.setImageResource(R.drawable.avatar_l);
            }

            holder.thumbnail.setAlpha(.5f + attachment.percent * .005f);
            holder.uploaded.setVisibility(attachment.percent > 99 ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        void addAndNotify(Attachment attachment) {
            mData.add(attachment);
            notifyItemInserted(mData.size() - 1);
        }

        void updateAndNotify(String uuid, String uploadId, String path) {
            int foundIndex = indexOfUuid(uuid);

            if (foundIndex > -1) {
                Attachment attachment = mData.get(foundIndex);
                attachment.uploadId = uploadId;
                attachment.path = path;
                notifyItemChanged(foundIndex);
            }
        }

        void updateAndNotify(String uploadId, int percent) {
            int foundIndex = indexOfUploadId(uploadId);

            if (foundIndex > -1) {
                mData.get(foundIndex).percent = percent;
                notifyItemChanged(foundIndex);
            }
        }

        void removeAndNotify(String uploadId) {
            int foundIndex = indexOfUploadId(uploadId);

            if (foundIndex > -1) {
                mData.remove(foundIndex);
                notifyItemRemoved(foundIndex);
            }
        }

        void clearAndNotify() {
            mData.clear();
            notifyDataSetChanged();
        }

        int indexOfUuid(String uuid) {
            for (int i = 0; i < mData.size(); i++) {
                if (uuid.equals(mData.get(i).uuid)) {
                    return i;
                }
            }

            return -1;
        }

        int indexOfUploadId(String uploadId) {
            for (int i = 0; i < mData.size(); i++) {
                if (uploadId.equals(mData.get(i).uploadId)) {
                    return i;
                }
            }

            return -1;
        }

        void generateAttachmentHash() {
            mAttachmentHash = UUID.randomUUID().toString();
        }
    }

    class Receiver extends UploadServiceBroadcastReceiver {
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
    }

    static class Attachment implements Serializable {
        final String uuid;
        String uploadId;
        String path;
        int percent;

        Attachment() {
            uuid = UUID.randomUUID().toString();
        }
    }

    static class AttachmentViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumbnail;
        final ImageView uploaded;

        AttachmentViewHolder(View v) {
            super(v);

            thumbnail = (ImageView) v.findViewById(R.id.thumbnail);
            uploaded = (ImageView) v.findViewById(R.id.uploaded);
        }
    }
}
