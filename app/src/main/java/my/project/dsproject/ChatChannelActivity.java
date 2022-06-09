package my.project.dsproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatChannelActivity extends AppCompatActivity {

    private RecyclerView messageRecycler;
    private MessageAdapter messageAdapter;
    private List<Value> conversationHistory;
    private Queue<Value> receivedMessages;
    private List<Value> allMessagesList;


    Button sendButton;
    EditText editTextMessage;
    TextView textViewTopic;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_channel);

        Messenger mainMessenger = getIntent().getParcelableExtra("connectionHandler");
        conversationHistory = (List<Value>) getIntent().getSerializableExtra("convoHistory");
        receivedMessages = (Queue<Value>) getIntent().getSerializableExtra("receivedMessages");
        String topic = getIntent().getStringExtra("topic");
        Profile currentProfile = (Profile) getIntent().getSerializableExtra("profile");
        allMessagesList = new ArrayList<>();


        sendButton = findViewById(R.id.send_button);
        editTextMessage = findViewById(R.id.edit_message);
        textViewTopic = findViewById(R.id.topic_text);

        textViewTopic.setText(topic);

        messageRecycler = findViewById(R.id.recycler_chat); //finding elements and setting adapter
        messageAdapter = new MessageAdapter(this, currentProfile, allMessagesList);
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setAdapter(messageAdapter);

        if(conversationHistory!= null){
            for (Value value : conversationHistory){
                synchronized (this){
                    allMessagesList.add(value);
                    messageAdapter.notifyItemInserted(allMessagesList.size() - 1);
                }
            }
        }


        ExecutorService checkForNewMessage = Executors.newSingleThreadExecutor(); //Executor thread to listen for new messages
        checkForNewMessage.execute(() -> {
            while(true){
                if (!Objects.requireNonNull(MainActivity.allTopicReceivedMessages.get(topic)).isEmpty()){
                    synchronized (this){
                        allMessagesList.add(MainActivity.allTopicReceivedMessages.get(topic).poll());
                        //RUN ON UI THREAD NEEDED FOR UPDATING CONTENT ON THE MAIN UI THREAD FROM THE EXECUTOR
                        runOnUiThread(() -> messageAdapter.notifyItemInserted(allMessagesList.size() - 1));
                    }
                }
            }
        });


        sendButton.setOnClickListener(v -> { //on hitting send
            try { //minimizing keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                System.exit(1);
            }

            String messageToSend = editTextMessage.getText().toString();
            Value messageValue = new Value(messageToSend, currentProfile, topic, "Publisher");

            Message msg = new Message();
            msg.what = 400;
            Bundle msgBundle = new Bundle();
            msg.setData(msgBundle);
            msgBundle.putSerializable("NEW_MESSAGE_TEXT", messageValue);
            //getting message and sending it to the main handler
            try {
                mainMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            synchronized (this){
                allMessagesList.add(messageValue); //adding the message to the main message list
                messageAdapter.notifyItemInserted(allMessagesList.size() - 1);
            }
            editTextMessage.setText("");
        });

    }
}