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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
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
    private static final int TOPIC_CHANGED = 101;
    private static final int TOPIC_NOT_FOUND = -100;

    private static final int CONNECTION_IN_PROGRESS = 300;

    private static final int NEW_MESSAGE_TEXT_SEND = 400;

    private static final int NEW_MESSAGE_IMAGE_SEND = 401;

    private static final int NEW_MESSAGE_ATTACHMENT_SEND = 402;

    private static final int NEW_MESSAGE_VIDEO_SEND = 403;


    private static final int HISTORY_READY = 200;
    private static final int HISTORY_IN_PROGRESS = 201;

    private static final int CONNECTION_FAILED = -1000;


    private Queue<Value> sentMessageQueue;
    public static Queue<Value> conversationHistory;
    public static Queue<Value> receivedMessageQueue;


    Consumer currentConsumer;
    Publisher currentPublisher;
    String topic;
    Intent currentChat;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        UserNode.setConfig();
        Button submitButton = findViewById(R.id.usernameSubmitBtrn);
        progressBar = findViewById(R.id.connectionProgressBar);
        progressBar.setVisibility(View.INVISIBLE);



        submitButton.setOnClickListener(v -> {

             usernameEditText = findViewById(R.id.usernameText);
             usernameTxtView = findViewById(R.id.usernameTextView);
             topicEditText = findViewById(R.id.topicText);
             topicTxtView = findViewById(R.id.topicTextView);

             String username = usernameEditText.getText().toString().trim();
             topic = topicEditText.getText().toString().trim();

            if (usernameCheck(username)){
                try { //minimizing keyboard
                    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(usernameEditText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }
                submitButton.setVisibility(View.INVISIBLE);
                Profile profile = new Profile(username);

                sentMessageQueue = new LinkedBlockingQueue<>(); //thread safe
                conversationHistory = new LinkedBlockingQueue<>();
                receivedMessageQueue = new LinkedBlockingQueue<>();


                ExecutorService serverConnection = Executors.newSingleThreadExecutor();
                serverConnection.execute(() -> { //executor thread to run publisher and consumer

                    @SuppressLint("HandlerLeak")
                    Handler connectionHandler= new Handler(getMainLooper()){
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void handleMessage(Message msg){ //main handler for managing messages from threads
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
                            else if (msg.what == TOPIC_FOUND){;
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
                            else if (msg.what == NEW_MESSAGE_VIDEO_SEND){
                                sentMessageQueue.add((Value)msg.getData().getSerializable("NEW_MESSAGE_VIDEO_SENT"));
                            }
                            if (msg.what == HISTORY_READY) {
                                progressBar.setVisibility(View.INVISIBLE);

                                System.out.println("TOPIC HISTORY- FOR "+   topic);
                                System.out.println("TOPIC HISTORY- FOR "+   conversationHistory);
                                //PASSING ALL NECESSARY DATA TO THE CHAT ACTIVITY
                                currentChat = new Intent(MainActivity.this, ChatChannelActivity.class);
                                currentChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                currentChat.putExtra("connectionHandler", new Messenger(this));
                                currentChat.putExtra("profile", profile);
                                currentChat.putExtra("topic", topic);
                                startActivity(currentChat);
                            }
                            if (msg.what == TOPIC_CHANGED){
                                progressBar.setVisibility(View.VISIBLE);
                                topic = msg.getData().getString("NEW_TOPIC");
                                System.out.println("TOPIC CHANGED TO : " + topic);

                                serverConnection.execute(() -> {

                                    currentPublisher.disconnect();
                                    currentConsumer.disconnect();

                                    currentPublisher = new Publisher(profile, this, sentMessageQueue, topic);
                                    currentConsumer = new Consumer(profile, this, conversationHistory, receivedMessageQueue, topic, MainActivity.this);

                                    Thread currentPublisherThread= new Thread(currentPublisher);
                                    Thread currentConsumerThread= new Thread(currentConsumer);
                                    currentConsumerThread.start();//starting our threads
                                    currentPublisherThread.start();
                                });
                            }
                        }
                    };

                    currentConsumer = new Consumer(profile, connectionHandler, conversationHistory, receivedMessageQueue, topic, this);
                    currentPublisher = new Publisher(profile, connectionHandler, sentMessageQueue, topic);
                    Thread currentPublisherThread= new Thread(currentPublisher);
                    Thread currentConsumerThread= new Thread(currentConsumer);
                    currentConsumerThread.start(); //starting our threads
                    currentPublisherThread.start();

                });
            }
            else {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Invalid credentials"); //username check
                alertDialog.setMessage("Provided username is invalid. Please make sure that your username is between 5 to 16 characters and not blank.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        (dialog, which) -> dialog.dismiss());
                alertDialog.show();
            }
        });

    }

    private static boolean usernameCheck(String username) { //just a username check for length

        if (username.length() < 5 || username.length() > 16) {
            return false;
        }
        return true;
    }

    private void connectionFailureAlert(){ //in case broker connection is not happening

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