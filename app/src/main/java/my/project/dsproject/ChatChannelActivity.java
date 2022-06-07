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
    private List<String> sentMessages;

    private Queue<Value> conversationHistory;
    private Queue<Value> receivedMessages;



    Button sendButton;
    EditText editTextMessage;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_channel);

        Messenger mainMessenger = getIntent().getParcelableExtra("connectionHandler");
        conversationHistory = getIntent().getParcelableExtra("convoHistory");
        receivedMessages = getIntent().getParcelableExtra("receivedMessages");

        sendButton = findViewById(R.id.send_button);
        editTextMessage = findViewById(R.id.edit_message);

        sentMessages = new ArrayList<>();

        messageRecycler = findViewById(R.id.recycler_chat);
        messageAdapter = new MessageAdapter(this, sentMessages, conversationHistory);
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setAdapter(messageAdapter);

        if (!conversationHistory.isEmpty()){

        }

        sendButton.setOnClickListener(v -> {
            try {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                System.exit(1);
            }
            String messageToSend = editTextMessage.getText().toString();

            Message msg = new Message();
            msg.what = 4;
            Bundle msgBundle = new Bundle();
            msg.setData(msgBundle);
            msgBundle.putString("NEW_MESSAGE_TEXT", messageToSend);
            try {
                mainMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            sentMessages.add(messageToSend);
            editTextMessage.setText("");
        });

    }

}