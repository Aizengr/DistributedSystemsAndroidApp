package my.project.dsproject;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Instrumentation;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatChannelActivity extends AppCompatActivity {

    private RecyclerView messageRecycler;
    private MessageAdapter messageAdapter;
    private List<Value> conversationHistory; //history list
    private List<Value> allMessagesList; //all message list that is used by Recycler view to update the UI with any new message

    public static final int PICK_IMAGE = 1000;
    public static final int PICK_ATTACHMENT = 2000;
    public static final int PERMISSION_CODE_IMAGE = 100;
    public static final int PERMISSION_CODE_ATTACHMENT = 200;


    ImageButton sendButton;
    EditText editTextMessage;
    TextView textViewTopic;
    ImageButton imageUploadButton;
    ImageButton videoUploadButton;
    ImageButton attachmentUploadButton;
    ImageButton cameraButton;
    Profile profile;
    String topic;
    Messenger mainMessenger;
    private ActivityResultLauncher<Intent> activityResultLauncher;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_channel);

        mainMessenger = getIntent().getParcelableExtra("connectionHandler");
        conversationHistory = (List<Value>) getIntent().getSerializableExtra("convoHistory");
        topic = getIntent().getStringExtra("topic");
        profile = (Profile) getIntent().getSerializableExtra("profile");
        allMessagesList = new ArrayList<>();


        sendButton = findViewById(R.id.send_button);
        editTextMessage = findViewById(R.id.edit_message);
        textViewTopic = findViewById(R.id.topic_text);
        imageUploadButton = findViewById(R.id.image_upload_button);
        videoUploadButton = findViewById(R.id.video_upload_button);
        attachmentUploadButton = findViewById(R.id.attachment_upload_button);
        cameraButton = findViewById(R.id.camera_button);

        textViewTopic.setText(topic);

        messageRecycler = findViewById(R.id.recycler_chat); //finding elements and setting adapter
        messageAdapter = new MessageAdapter(this, profile, allMessagesList);
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setAdapter(messageAdapter);

        if(conversationHistory!= null){
            for (Value value : conversationHistory){
                updateRecyclerMessages(value);
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
            Value messageValue = new Value(messageToSend, profile, topic, "Publisher", "message");

            sendMessageToMainHandler(400, "NEW_MESSAGE_TEXT", messageValue);

            updateRecyclerMessages(messageValue);

            editTextMessage.setText("");
        });

        imageUploadButton.setOnClickListener(v -> { //image upload button on click
            if (checkPermission()) {
                pickImageFromGallery();
            } else {
                requestPermission(); // Request Permission
            }
        });

        attachmentUploadButton.setOnClickListener(v -> { //attachment upload button on click
            if (checkPermission()) {
                pickAttachmentFromFiles();
            } else {
                requestPermission(); // Request Permission
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult( ActivityResult result ) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager())
                        Toast.makeText(ChatChannelActivity.this,"We Have Permission",Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(ChatChannelActivity.this, "You Denied the permission", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChatChannelActivity.this, "You Denied the permission", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int readCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            int writeCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return readCheck == PackageManager.PERMISSION_GRANTED && writeCheck == PackageManager.PERMISSION_GRANTED;
        }
    }

    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            new AlertDialog.Builder(ChatChannelActivity.this)
                    .setTitle("Permission")
                    .setMessage("Please give Storage permission.")
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            intent.addCategory("android.intent.category.DEFAULT");
                            intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                            activityResultLauncher.launch(intent);
                        } catch (Exception e) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            activityResultLauncher.launch(intent);
                        }
                    })
                    .setCancelable(false)
                    .show();
        } else {
            ActivityCompat.requestPermissions(ChatChannelActivity.this, permissions, 30);
        }
    }


    private void pickAttachmentFromFiles(){
        Intent intent = new Intent();
        intent.setType("text/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), PICK_ATTACHMENT);
    }

    private void pickImageFromGallery(){
        Intent imageUploadIntent = new Intent(); //creating a new intent for a result activity
        imageUploadIntent.setType("image/*");
        imageUploadIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(imageUploadIntent, "Select Picture"), PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { //handling result activities
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            String uriString = data.getData().getPath();
            String path = uriString.substring(uriString.indexOf(":")+1);

            MultimediaFile image = new MultimediaFile(path);

            Value imageValue = new Value(image, profile, topic, "image");

            sendMessageToMainHandler(401, "NEW_MESSAGE_IMAGE_SENT", imageValue); //sending the file to the handler

            updateRecyclerMessages(imageValue);
        } else if (requestCode == PICK_ATTACHMENT && resultCode == RESULT_OK){
            String uriString = data.getData().getPath();
            String path = uriString.substring(uriString.indexOf(":")+1);



            MultimediaFile file = new MultimediaFile(path);

            Value fileValue = new Value(file, profile, topic, "attachment");

            sendMessageToMainHandler(402, "NEW_MESSAGE_ATTACHMENT_SENT", fileValue); //sending the file to the handler

            updateRecyclerMessages(fileValue);
        }
    }


    @Override //handling runtime permission request codes
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case PERMISSION_CODE_IMAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    pickImageFromGallery();
                }
                else {
                    Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_CODE_ATTACHMENT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    pickAttachmentFromFiles();
                }
                else {
                    Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void sendMessageToMainHandler(int code, String name, Value value){ //method to send to main handler

        Message msg = new Message();
        msg.what = code;
        Bundle msgBundle = new Bundle();
        msg.setData(msgBundle);
        msgBundle.putSerializable(name, value);
        try {
            mainMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateRecyclerMessages(Value value){ //synced as the list data is also change on the executor thread
        synchronized (this){
            allMessagesList.add(value); //adding the message to the main message list for recycler
            messageAdapter.notifyItemInserted(allMessagesList.size() - 1);
        }
    }
}