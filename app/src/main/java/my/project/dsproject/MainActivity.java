package my.project.dsproject;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

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
import android.view.WindowManager;
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


    private static final int HISTORY_READY = 200;
    private static final int CONNECTION_FAILED = -1000;


    public static Queue<Value> sentMessageQueue;
    public static Queue<Value> conversationHistory;
    public static Queue<Value> receivedMessageQueue;


    Consumer currentConsumer;
    Publisher currentPublisher;
    String topic;
    Intent currentChat;
    Handler connectionHandler;


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
             progressBar.setVisibility(View.VISIBLE);
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

                sentMessageQueue = new LinkedBlockingQueue<>(); //thread safe lists
                conversationHistory = new LinkedBlockingQueue<>();
                receivedMessageQueue = new LinkedBlockingQueue<>();

                ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(); //executors instead of async task
                ExecutorService publisherExecutor = Executors.newSingleThreadExecutor(); //async tasks deprecated


                connectionHandler= new Handler(getMainLooper()){
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void handleMessage(Message msg){ //main handler for managing messages from threads
                        if (msg.what == TOPIC_NOT_FOUND){

                            showInvalidTopicDialog();
                            usernameTxtView.setVisibility(View.GONE);
                            usernameEditText.setVisibility(View.GONE);
                            topicTxtView.setText("Please re-submit conversation topic:");
                            submitButton.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);

                            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MainActivity.this);
                            Intent noTopicFoundIntent = new Intent("TOPIC_NOT_FOUND");
                            noTopicFoundIntent.putExtra("TOPIC_NOT_FOUND", topic);
                            localBroadcastManager.sendBroadcast(noTopicFoundIntent);

                        }
                        else if (msg.what == CONNECTION_FAILED){
                            connectionFailureAlert();
                        }
                        else if (msg.what == TOPIC_FOUND){
                            submitButton.setVisibility(View.VISIBLE);
                        }
                        else if (msg.what == HISTORY_READY) {

                            progressBar.setVisibility(View.INVISIBLE);
                            runOnUiThread(() -> getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)); //in case we come from changing topic
                            //PASSING ALL NECESSARY DATA TO THE CHAT ACTIVITY
                            currentChat = new Intent(MainActivity.this, ChatChannelActivity.class);
                            currentChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //disabling multiple intents so back button goes back to main
                            currentChat.putExtra("connectionHandler", new Messenger(this));
                            currentChat.putExtra("profile", profile);
                            currentChat.putExtra("topic", topic);
                            startActivity(currentChat);
                        }
                        else if (msg.what == TOPIC_CHANGED){
                            topic = msg.getData().getString("NEW_TOPIC");
                            System.out.println("TOPIC CHANGED TO : " + topic);

                            currentConsumer.disconnect(); //disconnecting current components
                            currentPublisher.disconnect();
                            consumerExecutor.shutdownNow(); //stopping previous threads
                            publisherExecutor.shutdownNow();

                            ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(); //creating new ones
                            consumerExecutor.execute( currentConsumer = new Consumer(profile, connectionHandler, conversationHistory, receivedMessageQueue, topic));//executor running consumer

                            ExecutorService publisherExecutor = Executors.newSingleThreadExecutor();
                            publisherExecutor.execute( currentPublisher = new Publisher(profile, connectionHandler, sentMessageQueue, topic));//executor running publisher

                        }
                    }
                };

                consumerExecutor.execute( currentConsumer = new Consumer(profile, connectionHandler, conversationHistory, receivedMessageQueue, topic));//executor thread to run publisher and consumer
                publisherExecutor.execute( currentPublisher = new Publisher(profile, connectionHandler, sentMessageQueue, topic));//executor thread to run publisher and consumer

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

    private void showInvalidTopicDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Invalid topic");
        alertDialog.setMessage("No topics found for: "+ topic + ". Please enter a new one.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private static boolean usernameCheck(String username) { //just a username check for length

        if (username.length() < 4 || username.length() > 16) {
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