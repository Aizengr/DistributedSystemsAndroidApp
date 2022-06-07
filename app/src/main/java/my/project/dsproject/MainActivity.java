package my.project.dsproject;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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


    private static final int TOPIC_FOUND = 0;
    private static final int TOPIC_NOT_FOUND = -1;
    private static final int IN_PROGRESS = 3;
    private static final int NEW_MESSAGE_TEXT = 4;
    private static final int INC_HISTORY = 5;
    private static final int CONNECTION_FAILED = -100;

    private boolean history_ready = false;

    private Queue<Value> messageQueue;
    private Queue<Value> conversationHistory;
    private Queue<Value> receivedMessageQueue;

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
             String topic = topicEditText.getText().toString().trim();

            if (usernameCheck(username)){
                submitButton.setVisibility(v.INVISIBLE);
                Profile profile = new Profile(username);

                messageQueue = new LinkedBlockingQueue<>();
                conversationHistory = new LinkedBlockingQueue<>();
                receivedMessageQueue = new LinkedBlockingQueue<>();

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
                                Intent chatIntent = new Intent(MainActivity.this, ChatChannelActivity.class);
                                chatIntent.putExtra("connectionHandler", new Messenger(this));
                                chatIntent.putExtra("convoHistory", (Parcelable) conversationHistory);
                                chatIntent.putExtra("receivedMessages", (Parcelable) receivedMessageQueue);
                                startActivity(chatIntent);
                            }
                            else if (msg.what == IN_PROGRESS){
                                progressBar.setVisibility(View.VISIBLE);
                            }
                            else if (msg.what == NEW_MESSAGE_TEXT){
                                messageQueue.add(new Value(msg.getData().getString("NEW_MESSAGE_TEXT"), profile, topic, "Publisher"));
                            }
                            if (msg.what == 5) {
                                history_ready = true;
                            }
                        }
                    };

                    Consumer newCon = new Consumer(profile, connectionHandler, conversationHistory, receivedMessageQueue, topic);
                    Publisher newPub = new Publisher(profile, connectionHandler, messageQueue, topic);
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