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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ChatChannelActivity extends AppCompatActivity {

    private RecyclerView messageRecycler;
    private MessageAdapter messageAdapter;
    private List<Value> conversationHistory;
    private Queue<Value> receivedMessages;
    private List<Value> allMessagesList;



    Button sendButton;
    EditText editTextMessage;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_channel);

        Messenger mainMessenger = getIntent().getParcelableExtra("connectionHandler");
        conversationHistory = (List<Value>) getIntent().getSerializableExtra("convoHistory");
        receivedMessages = getIntent().getParcelableExtra("receivedMessages");
        String topic = getIntent().getStringExtra("topic");
        Profile currentProfile = (Profile) getIntent().getSerializableExtra("profile");

        allMessagesList = new ArrayList<>();

        if(conversationHistory!= null){
            for (Value value : conversationHistory){
                allMessagesList.add(value);
            }
        }


        sendButton = findViewById(R.id.send_button);
        editTextMessage = findViewById(R.id.edit_message);

        messageRecycler = findViewById(R.id.recycler_chat);
        messageAdapter = new MessageAdapter(this, currentProfile, allMessagesList);
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setAdapter(messageAdapter);


        sendButton.setOnClickListener(v -> {
            try {
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

            try {
                mainMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            allMessagesList.add(messageValue);
            editTextMessage.setText("");
        });

    }

}