package my.project.dsproject;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {


    EditText usernameEditText;
    TextView usernameTxtView;
    EditText topicEditText;
    TextView topicTxtView;
    ProgressBar progressBar;


    private static final int TOPIC_FOUND = 100;
    private static final int TOPIC_NOT_FOUND = -100;

    private static final int CONNECTION_IN_PROGRESS = 300;

    private static final int NEW_MESSAGE_TEXT_SEND = 400;

    private static final int NEW_MESSAGE_IMAGE_SEND = 401;
    private static final int NEW_MESSAGE_FILE_RECEIVED = 501;

    private static final int NEW_MESSAGE_ATTACHMENT_SEND = 402;
    private static final int NEW_MESSAGE_ATTACHMENT_RECEIVED = 502;

    private static final int HISTORY_READY = 200;
    private static final int HISTORY_IN_PROGRESS = 201;

    private static final int CONNECTION_FAILED = -1000;

    private boolean history_ready = false;

    private Queue<Value> sentMessageQueue;
    private List<Value> conversationHistory;
    private Queue<Value> receivedMessageQueue;

    //HASHMAP TO KEEP ALL CONVERSATION RECEIVED MESSAGES FOR NOTIFICATIONS ON ANY CONVO NEW MESSAGE
    public static Map<String, Queue<Value>> allTopicReceivedMessages;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UserNode.setConfig();
        Button submitButton = findViewById(R.id.usernameSubmitBtrn);
        progressBar = findViewById(R.id.connectionProgressBar);
        progressBar.setVisibility(View.INVISIBLE);

        allTopicReceivedMessages = new HashMap<>();


        submitButton.setOnClickListener(v -> {


             usernameEditText = findViewById(R.id.usernameText);
             usernameTxtView = findViewById(R.id.usernameTextView);
             topicEditText = findViewById(R.id.topicText);
             topicTxtView = findViewById(R.id.topicTextView);

             String username = usernameEditText.getText().toString().trim();
             String topic = topicEditText.getText().toString().trim();

            if (usernameCheck(username)){
                submitButton.setVisibility(v.INVISIBLE);
                Profile profile = new Profile(username);

                sentMessageQueue = new LinkedBlockingQueue<>(); //thread safe
                conversationHistory = new ArrayList<>(); //NOT thread safe
                receivedMessageQueue = new LinkedBlockingQueue<>(); //thread safe


                ExecutorService serverConnection = Executors.newSingleThreadExecutor();
                serverConnection.execute(() -> {

                    @SuppressLint("HandlerLeak")
                    Handler connectionHandler= new Handler(getMainLooper()){
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void handleMessage(Message msg){
                            if (msg.what == TOPIC_NOT_FOUND){
                                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                                alertDialog.setTitle("Invalid topic");
                                alertDialog.setMessage("No topics found for: "+ topic + ". Please enter a new one.");
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        (dialog, which) -> dialog.dismiss());
                                alertDialog.show();
                                usernameTxtView.setVisibility(View.GONE);
                                usernameEditText.setVisibility(View.GONE);
                                topicTxtView.setText("Please re-submit conversation topic:");
                                submitButton.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.INVISIBLE);
                            }
                            else if (msg.what == CONNECTION_FAILED){
                                connectionFailureAlert();
                            }
                            else if (msg.what == TOPIC_FOUND){
                                progressBar.setVisibility(View.INVISIBLE);
                                submitButton.setVisibility(View.VISIBLE);
                            }
                            else if (msg.what == CONNECTION_IN_PROGRESS){
                                progressBar.setVisibility(View.VISIBLE);
                            }
                            else if (msg.what == HISTORY_IN_PROGRESS){
                                progressBar.setVisibility(View.VISIBLE);
                            }
                            else if (msg.what == NEW_MESSAGE_TEXT_SEND){
                                sentMessageQueue.add((Value)msg.getData().getSerializable("NEW_MESSAGE_TEXT"));
                            }
                            else if (msg.what == NEW_MESSAGE_IMAGE_SEND){
                                sentMessageQueue.add((Value)msg.getData().getSerializable("NEW_MESSAGE_IMAGE_SENT"));
                            }
                            else if (msg.what == NEW_MESSAGE_ATTACHMENT_SEND){
                                sentMessageQueue.add((Value)msg.getData().getSerializable("NEW_MESSAGE_ATTACHMENT_SENT"));
                            }
                            if (msg.what == HISTORY_READY) {
                                progressBar.setVisibility(View.INVISIBLE);

                                //adding to the queue
                                if (!allTopicReceivedMessages.containsKey(topic)){
                                    allTopicReceivedMessages.put(topic, receivedMessageQueue);
                                }
                                //PASSING ALL NECESSARY DATA TO THE CHAT ACTIVITY
                                Intent chatIntent = new Intent(MainActivity.this, ChatChannelActivity.class);
                                chatIntent.putExtra("connectionHandler", new Messenger(this));
                                chatIntent.putExtra("convoHistory", (Serializable) conversationHistory);
                                chatIntent.putExtra("profile", profile);
                                chatIntent.putExtra("topic", topic);
                                startActivity(chatIntent);
                            }
                        }
                    };

                    Consumer newCon = new Consumer(profile, connectionHandler, conversationHistory, receivedMessageQueue, topic);
                    Publisher newPub = new Publisher(profile, connectionHandler, sentMessageQueue, topic);
                    Thread pubThread = new Thread(newPub);
                    Thread conThread = new Thread(newCon);
                    conThread.start();
                    pubThread.start();

                });
            }
            else {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Invalid credentials");
                alertDialog.setMessage("Provided username is invalid. Please make sure that your username is between 5 to 10 characters and not blank.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            }
        });

    }

    private static boolean usernameCheck(String username) {

        if (username.length() < 5 || username.length() > 10) {
            return false;
        }
        return true;
    }

    private void connectionFailureAlert(){

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Server connection error");
        alertDialog.setMessage("Connection to server failed. Exiting the application...");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (alertDialog1, which) -> {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
        alertDialog.show();
    }

}