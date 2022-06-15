package my.project.dsproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter{

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_IMAGE_SENT = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
    private static final int VIEW_TYPE_VIDEO_SENT = 5;
    private static final int VIEW_TYPE_VIDEO_RECEIVED = 6;
    private static final int VIEW_TYPE_ATTACHMENT_SENT = 7;
    private static final int VIEW_TYPE_ATTACHMENT_RECEIVED = 8;

    private final List<Value> messagesList;
    private final Profile profile;
    private final ClickListener listener;


    public MessageAdapter(Profile profile, List<Value> messagesList, ClickListener listener){
        this.messagesList = messagesList;
        this.profile = profile;
        this.listener = listener;
    }

    @Override
    public int getItemCount() {

        if (messagesList != null){
            return messagesList.size();
        }
        return 0;
    }

    // Determines the appropriate ViewType according to the sender and type of the message
    @Override
    public int getItemViewType(int position) {
        Value message = messagesList.get(position);

        if (message.getProfile().getUsername().equalsIgnoreCase(this.profile.getUsername())){
            switch (message.getFileType()) {
                case "message":
                    return VIEW_TYPE_MESSAGE_SENT;
                case "image":
                    return VIEW_TYPE_IMAGE_SENT;
                case "video":
                    return VIEW_TYPE_VIDEO_SENT;
                case "attachment":
                    return VIEW_TYPE_ATTACHMENT_SENT;
            }
        } else {
            switch (message.getFileType()) {
                case "message":
                    return VIEW_TYPE_MESSAGE_RECEIVED;
                case "image":
                    return VIEW_TYPE_IMAGE_RECEIVED;
                case "video":
                    return VIEW_TYPE_VIDEO_RECEIVED;
                case "attachment":
                    return VIEW_TYPE_ATTACHMENT_RECEIVED;
            }
        }
        return 0;
    }

    // Inflates the appropriate layout according to the ViewType.
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_messages, parent, false);
            return new SentMessageHolder(view);

        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_other_messages, parent, false);
            return new ReceivedMessageHolder(view);

        } else if (viewType == VIEW_TYPE_IMAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_images, parent, false);
            return new SentImageHolder(view);

        } else if (viewType == VIEW_TYPE_IMAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_other_images, parent, false);
            return new ReceivedImageHolder(view);

        } else if (viewType == VIEW_TYPE_VIDEO_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_videos, parent, false);
            return new SentVideoHolder(view);

        } else if (viewType == VIEW_TYPE_VIDEO_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_other_videos, parent, false);
            return new ReceivedVideoHolder(view);

        } else if (viewType == VIEW_TYPE_ATTACHMENT_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_files, parent, false);
            return new SentAttachmentHolder(view);

        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_other_files, parent, false);
            return new ReceivedAttachmentHolder(view);
        }
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Value message =  messagesList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((MessageAdapter.SentMessageHolder) holder).bind(message);
                break;

            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((MessageAdapter.ReceivedMessageHolder) holder).bind(message);
                break;

            case VIEW_TYPE_IMAGE_SENT:
                ((MessageAdapter.SentImageHolder) holder).bind(message);

                ((SentImageHolder) holder).imageView.setOnClickListener(v ->
                        listener.onImageClicked(messagesList.get(position)));

                ((SentImageHolder) holder).imageDownloadButton.setOnClickListener(v ->
                        listener.onDownloadClicked(messagesList.get(position)));
                break;

            case VIEW_TYPE_IMAGE_RECEIVED:
                ((MessageAdapter.ReceivedImageHolder) holder).bind(message);

                ((ReceivedImageHolder) holder).imageView.setOnClickListener(v ->
                        listener.onImageClicked(messagesList.get(position)));

                ((ReceivedImageHolder) holder).imageDownloadButton.setOnClickListener(v ->
                        listener.onDownloadClicked(messagesList.get(position)));
                break;

            case VIEW_TYPE_VIDEO_SENT:
                ((MessageAdapter.SentVideoHolder) holder).bind(message);

                ((SentVideoHolder) holder).videoPlayButton.setOnClickListener(v ->
                        listener.onVideoClicked(messagesList.get(position)));

                ((SentVideoHolder) holder).downloadButton.setOnClickListener(v ->
                        listener.onDownloadClicked(messagesList.get(position)));
                break;

            case VIEW_TYPE_VIDEO_RECEIVED:
                ((MessageAdapter.ReceivedVideoHolder) holder).bind(message);

                ((ReceivedVideoHolder) holder).videoPlayButton.setOnClickListener(v ->
                        listener.onVideoClicked(messagesList.get(position)));

                ((ReceivedVideoHolder) holder).downloadButton.setOnClickListener(v ->
                        listener.onDownloadClicked(messagesList.get(position)));
                break;

            case VIEW_TYPE_ATTACHMENT_SENT:
                ((MessageAdapter.SentAttachmentHolder) holder).bind(message);
                ((SentAttachmentHolder) holder).attachmentDownloadButton.setOnClickListener(v ->
                        listener.onDownloadClicked(messagesList.get(position)));
                break;

            case VIEW_TYPE_ATTACHMENT_RECEIVED:
                ((MessageAdapter.ReceivedAttachmentHolder) holder).bind(message);
                ((ReceivedAttachmentHolder) holder).attachmentDownloadButton.setOnClickListener(v ->
                        listener.onDownloadClicked(messagesList.get(position)));
                break;

        }
    }

    private static class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_message_me);
        }

        void bind(Value message) {
            messageText.setText(message.getMessage());

        }
    }

    private static class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, nameText;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.text_message_other);
            nameText = itemView.findViewById(R.id.text_user_other);
        }

        void bind(Value message) {
            messageText.setText(message.getMessage());
            nameText.setText(message.getProfile().getUsername());

        }
    }

    private static class SentImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton imageDownloadButton;

        SentImageHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_me);
            imageDownloadButton = itemView.findViewById(R.id.download_image_button_me);
        }

        void bind(Value message) {
            Bitmap bitmap = BitmapFactory.decodeFile(message.getMultimediaFile().getPath().toString());
            imageView.setImageBitmap(bitmap);
            imageView.setClickable(true);

        }
    }
    private static class ReceivedImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameText;
        ImageButton imageDownloadButton;

        ReceivedImageHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_other);
            nameText = itemView.findViewById(R.id.text_user_other);
            imageDownloadButton = itemView.findViewById(R.id.download_image_button_other);
        }

        void bind(Value message) {
            Bitmap bitmap = BitmapFactory.decodeFile(message.getMultimediaFile().getPath().toString());
            imageView.setImageBitmap(bitmap);
            imageView.setClickable(true);
            nameText.setText(message.getProfile().getUsername());

        }
    }


    private static class SentAttachmentHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageButton attachmentDownloadButton;

        SentAttachmentHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_attachment_me);
            attachmentDownloadButton =itemView.findViewById(R.id.download_attachment_button_me);
        }

        void bind(Value message) {
            textView.setText(message.getFilename());
        }
    }

    private static class ReceivedAttachmentHolder extends RecyclerView.ViewHolder {
        TextView textView, nameText;
        ImageButton attachmentDownloadButton;

        ReceivedAttachmentHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.text_attachment_other);
            attachmentDownloadButton =itemView.findViewById(R.id.download_attachment_button_other);
            nameText = itemView.findViewById(R.id.text_user_other);

        }

        void bind(Value message) {

            textView.setText(message.getFilename());
            nameText.setText(message.getProfile().getUsername());
        }
    }

    private static class SentVideoHolder extends RecyclerView.ViewHolder {

        ImageView videoImageView;
        ImageButton videoPlayButton;
        ImageButton downloadButton;

        SentVideoHolder(View itemView) {
            super(itemView);
            videoPlayButton = itemView.findViewById(R.id.video_play_button);
            videoImageView = itemView.findViewById(R.id.video_image_me);
            downloadButton = itemView.findViewById(R.id.download_video_button_me);
        }

        void bind(Value message) {
            try {
                Bitmap thumbnail;
                thumbnail = ThumbnailUtils.createVideoThumbnail //retrieving and scaling the bitmap
                        (message.getMultimediaFile().getPath().toString(),
                                MediaStore.Video.Thumbnails.MINI_KIND);
                thumbnail = Bitmap.createScaledBitmap(thumbnail, 180, 130, true);
                videoImageView.setImageBitmap(thumbnail);
                videoPlayButton.setVisibility(View.VISIBLE);
                videoPlayButton.bringToFront();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }
    private static class ReceivedVideoHolder extends RecyclerView.ViewHolder {

        ImageView videoImageView;
        ImageButton videoPlayButton;
        ImageButton downloadButton;
        TextView nameText;

        ReceivedVideoHolder(View itemView) {
            super(itemView);
            videoPlayButton = itemView.findViewById(R.id.video_play_button);
            videoImageView = itemView.findViewById(R.id.video_image_other);
            downloadButton = itemView.findViewById(R.id.download_video_button_other);
            nameText = itemView.findViewById(R.id.text_user_other);
        }

        void bind(Value message) {
            try {
                nameText.setText(message.getProfile().getUsername());
                Bitmap thumbnail;
                thumbnail = ThumbnailUtils.createVideoThumbnail //retrieving
                        (message.getMultimediaFile().getPath().toString(),
                                MediaStore.Video.Thumbnails.MINI_KIND);
                videoImageView.setImageBitmap(thumbnail);
                videoPlayButton.setVisibility(View.VISIBLE);
                videoPlayButton.bringToFront();
            } catch (NullPointerException e){
                e.printStackTrace();
            }
        }
    }

}