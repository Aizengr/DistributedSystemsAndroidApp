package my.project.dsproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Queue;

public class MessageAdapter extends RecyclerView.Adapter{

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_FILE_SENT = 3;
    private static final int VIEW_TYPE_FILE_RECEIVED = 4;

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
        return messagesList.size();
    }

    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        Value message = (Value) messagesList.get(position);

        if (message.getProfile().getUsername().equalsIgnoreCase(this.profile.getUsername())) {
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_my_messages, parent, false);
            return new MessageAdapter.SentHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_other_messages, parent, false);
            return new MessageAdapter.ReceivedHolder(view);
        }

        return null;
    }

    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Value message =  messagesList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((MessageAdapter.SentHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((MessageAdapter.ReceivedHolder) holder).bind(message);
        }
    }

    private class SentHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        SentHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.text_message_me);
        }

        void bind(Value message) {
            messageText.setText(message.getMessage());

        }
    }

    private class ReceivedHolder extends RecyclerView.ViewHolder {
        TextView messageText, nameText;

        ReceivedHolder(View itemView) {
            super(itemView);

            messageText = (TextView) itemView.findViewById(R.id.text_message_other);
            nameText = (TextView) itemView.findViewById(R.id.text_user_other);
        }

        void bind(Value message) {

            messageText.setText(message.getMessage());

            nameText.setText(message.getProfile().getUsername());

        }
    }
}
