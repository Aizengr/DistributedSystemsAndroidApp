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

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.SentHolder> {

    private Context context;
    private List<String> sentMessages;
    private Queue<Value> conversationHistory;


    public MessageAdapter(Context cx, List<String> sentMessages, Queue<Value> conversationHistory){
        this.context = cx;
        this.sentMessages = sentMessages;
        this.conversationHistory = conversationHistory;
    }

    @NonNull
    @Override
    public SentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater myInflater = LayoutInflater.from(context);
        View view = myInflater.inflate(R.layout.item_my_messages, parent, false);
        return new SentHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SentHolder holder, int position) {
        holder.message.setText(sentMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return sentMessages.size() + conversationHistory.size();
    }

    public class SentHolder extends RecyclerView.ViewHolder{

        TextView message;

        public SentHolder(@NonNull View itemView) {
            super(itemView);

            message = itemView.findViewById(R.id.text_message_me);

        }
    }
}
