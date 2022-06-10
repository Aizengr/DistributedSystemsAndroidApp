package my.project.dsproject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
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
import android.widget.VideoView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatChannelActivity extends AppCompatActivity  implements ClickListener {

    private RecyclerView messageRecycler;
    private MessageAdapter messageAdapter;
    private List<Value> conversationHistory; //history list
    private List<Value> allMessagesList; //all message list that is used by Recycler view to update the UI with any new message

    public static final int PICK_IMAGE = 1000;
    public static final int PICK_ATTACHMENT = 2000;
    public static final int PICK_VIDEO = 3000;

    public static final int PERMISSION_CODE_IMAGE = 100;
    public static final int PERMISSION_CODE_ATTACHMENT = 200;
    public static final int PERMISSION_CODE_VIDEO = 300;


    ImageButton sendButton;
    EditText editTextMessage;
    TextView textViewTopic;
    ImageButton imageUploadButton;
    ImageButton videoUploadButton;
    ImageButton videoPlayButton;
    ImageButton attachmentUploadButton;
    ImageButton cameraButton;
    VideoView videoView;
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
        videoPlayButton = findViewById(R.id.video_play_button);
        cameraButton = findViewById(R.id.camera_button);

        textViewTopic.setText(topic);

        messageRecycler = findViewById(R.id.recycler_chat); //finding elements and setting adapter
        messageAdapter = new MessageAdapter(this, profile, allMessagesList, this);
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

        videoUploadButton.setOnClickListener(v -> {

            if (checkPermission()) {
                pickVideoFromFiles();
            } else {
                requestPermission(); // Request Permission
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            //RESULT ACTIVITY LAUNCHER TO MANAGE PERMISSION ISSUE WITH ANDROID 12 MANAGE STORAGE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager())
                    Toast.makeText(ChatChannelActivity.this,"We Have Permission",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ChatChannelActivity.this, "You Denied the permission", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatChannelActivity.this, "You Denied the permission", Toast.LENGTH_SHORT).show();
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


    private void pickVideoFromFiles(){
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), PICK_VIDEO);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) { //handling ACTIVITY RESULTS
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {

            Value imageValue = createValueFromResult(data, "image");
            sendMessageToMainHandler(401, "NEW_MESSAGE_IMAGE_SENT", imageValue); //sending the file to the handler
            updateRecyclerMessages(imageValue); //sending the file to the recycler

        } else if (requestCode == PICK_ATTACHMENT && resultCode == RESULT_OK){

            Value fileValue = createValueFromResult(data, "attachment");
            sendMessageToMainHandler(402, "NEW_MESSAGE_ATTACHMENT_SENT", fileValue); //sending the file to the handler
            updateRecyclerMessages(fileValue); //sending the file to the recycler
        }
        else if (requestCode == PICK_VIDEO && resultCode == RESULT_OK){

            Value videoValue = createValueFromResult(data, "video");
            sendMessageToMainHandler(403, "NEW_MESSAGE_VIDEO_SENT", videoValue); //sending the file to the handler
            updateRecyclerMessages(videoValue); //sending the file to the recycler
        }

    }

    private Value createValueFromResult(Intent data, String fileType){
        String uriString = data.getData().getPath();
        String path = uriString.substring(uriString.indexOf(":")+1);
        MultimediaFile file = new MultimediaFile(path, fileType);
        Value value = new Value(file, profile, topic, fileType);
        return value;
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

    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private void requestPermission() { //PERMISSION RESULT ACTIVITY FOR MANAGE PERMISSIONS ACTIVATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            new AlertDialog.Builder(ChatChannelActivity.this)
                    .setTitle("Permission")
                    .setMessage("Please give Storage permissions.")
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

    @Override
    public void onVideoClicked(Value value) { //viewing video on click
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value.getMultimediaFile().getPath().toString()));
        intent.setDataAndType(Uri.parse(value.getMultimediaFile().getPath().toString()), "video/*");
        startActivity(intent);
    }

    @Override
    public void onDownloadClicked(Value value)  { //when clicking download button on video or attachment

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filename = value.getFilename().substring(0,value.getFilename().indexOf("."));
        String fileExt = value.getMultimediaFile().getFileExt();

        Path newPath = Paths.get(path.getPath() + "/" + filename + "." + fileExt);
        int counter = 1;
        String existString;
        while (Files.exists(newPath)){ //if file exists loop with a counter and change filename to filename%counter%.ext
            System.out.println(newPath);
            existString = String.format("(%s)", counter);
            newPath = Paths.get(path.getPath() + "/" + filename + existString + "." + fileExt);
            counter++;
        }
        File download = new File(String.valueOf(newPath)); //writing file
        try {
            download.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (download != null){
            Toast.makeText(ChatChannelActivity.this, "Downloading File", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onImageClicked(Value value) { //viewing image on click
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value.getMultimediaFile().getPath().toString()));
        intent.setDataAndType(Uri.parse(value.getMultimediaFile().getPath().toString()), "image/*");
        startActivity(intent);
    }


}