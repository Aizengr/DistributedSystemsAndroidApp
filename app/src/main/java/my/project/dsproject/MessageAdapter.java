package my.project.dsproject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

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

    private Context context;
    private List<Value> messagesList;
    private Profile profile;


    public MessageAdapter(Context cx, Profile profile,  List<Value> messagesList){
        this.context = cx;
        this.messagesList = messagesList;
        this.profile = profile;
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
            if (message.getFileType().equals("message")){
                return VIEW_TYPE_MESSAGE_SENT;
            }
            else if (message.getFileType().equals("image")){
                return VIEW_TYPE_IMAGE_SENT;
            }
            else if (message.getFileType().equals("video")){
                return VIEW_TYPE_VIDEO_SENT;
            }
            else if (message.getFileType().equals("attachment")){
                return VIEW_TYPE_ATTACHMENT_SENT;
            }
        } else {
            if (message.getFileType().equals("message")){
                return VIEW_TYPE_MESSAGE_RECEIVED;
            }
            else if (message.getFileType().equals("image")){
                return VIEW_TYPE_IMAGE_RECEIVED;
            }
            else if (message.getFileType().equals("video")){
                return VIEW_TYPE_VIDEO_RECEIVED;
            }
            else if (message.getFileType().equals("attachment")){
                return VIEW_TYPE_ATTACHMENT_RECEIVED;
            }
        }
        return 0;
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_messages, parent, false);
            return new MessageAdapter.SentMessageHolder(view);

        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_other_messages, parent, false);
            return new MessageAdapter.ReceivedMessageHolder(view);

        } else if (viewType == VIEW_TYPE_IMAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_images, parent, false);
            return new MessageAdapter.SentImageHolder(view);

//        } else if (viewType == VIEW_TYPE_IMAGE_RECEIVED) {
//            view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_other_images, parent, false);
//            return new MessageAdapter.ReceivedImageHolder(view);
//
//        } else if (viewType == VIEW_TYPE_VIDEO_SENT) {
//            view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_my_videos, parent, false);
//            return new MessageAdapter.SentVideoHolder(view);
//
//        } else if (viewType == VIEW_TYPE_VIDEO_RECEIVED) {
//            view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_other_videos, parent, false);
//            return new MessageAdapter.ReceivedVideoHolder(view);
//
        } else if (viewType == VIEW_TYPE_ATTACHMENT_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_files, parent, false);
            return new MessageAdapter.SentAttachmentHolder(view);
        }

//        } else if (viewType == VIEW_TYPE_ATTACHMENT_RECEIVED) {
//            view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.item_other_files, parent, false);
//            return new MessageAdapter.ReceivedAttachmentHolder(view);
//        }

        return null;
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
                break;
//            case VIEW_TYPE_IMAGE_RECEIVED:
//                ((MessageAdapter.ReceivedImageHolder) holder).bind(message);
//                break;
//            case VIEW_TYPE_VIDEO_SENT:
//                ((MessageAdapter.SentVideoHolder) holder).bind(message);
//                break;
//            case VIEW_TYPE_VIDEO_RECEIVED:
//                ((MessageAdapter.ReceivedVideoHolder) holder).bind(message);
//                break;
            case VIEW_TYPE_ATTACHMENT_SENT:
               ((MessageAdapter.SentAttachmentHolder) holder).bind(message);
               break;
//            case VIEW_TYPE_ATTACHMENT_RECEIVED:
//                ((MessageAdapter.ReceivedAttachmentHolder) holder).bind(message);
//                break;

        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        SentMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_message_me);
        }

        void bind(Value message) {
            messageText.setText(message.getMessage());

        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
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

    private class SentImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        SentImageHolder(View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.image_me);
        }

        void bind(Value message) {
            Bitmap bitmap = BitmapFactory.decodeFile(message.getMultimediaFile().getPath().toString());
            imageView.setImageBitmap(bitmap);

        }
    }

    private class SentAttachmentHolder extends RecyclerView.ViewHolder {
        TextView textView;

        SentAttachmentHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.text_attachment_me);
        }

        void bind(Value message) {
            System.out.println("FILENAME: " + message.getFilename());
            textView.setText(message.getFilename());

        }
    }
}
