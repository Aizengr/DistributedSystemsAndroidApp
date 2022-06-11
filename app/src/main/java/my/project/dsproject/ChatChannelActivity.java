package my.project.dsproject;

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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    private MessageAdapter messageAdapter;
    private List<Value> allMessagesList; //all message list that is used by Recycler view to update the UI with any new message

    public static final int PICK_IMAGE = 1000;
    public static final int CAPTURE_IMAGE = 1001;

    public static final int PICK_ATTACHMENT = 2000;
    public static final int PICK_VIDEO = 3000;
    public static final int CAPTURE_VIDEO = 3001;

    private static final int CAMERA_PERMISSION_CODE = 1;


    ImageButton sendButton;
    EditText editTextMessage;
    TextView textViewTopic;
    ImageButton imageUploadButton;
    ImageButton videoUploadButton;
    ImageButton videoPlayButton;
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
        topic = getIntent().getStringExtra("topic");
        profile = (Profile) getIntent().getSerializableExtra("profile");
        allMessagesList = new ArrayList<>();

        //history list
        List<Value> conversationHistory = MainActivity.allTopicHistories.get(topic);

        sendButton = findViewById(R.id.send_button);
        editTextMessage = findViewById(R.id.edit_message);
        textViewTopic = findViewById(R.id.topic_text);
        imageUploadButton = findViewById(R.id.image_upload_button);
        videoUploadButton = findViewById(R.id.video_upload_button);
        attachmentUploadButton = findViewById(R.id.attachment_upload_button);
        videoPlayButton = findViewById(R.id.video_play_button);
        cameraButton = findViewById(R.id.camera_button);

        textViewTopic.setText(topic);

        RecyclerView messageRecycler = findViewById(R.id.recycler_chat); //finding elements and setting adapter
        messageAdapter = new MessageAdapter(this, profile, allMessagesList, this);
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setAdapter(messageAdapter);

        System.out.println("TEST" + conversationHistory);
        synchronized (this) {
            if (conversationHistory != null) {
                for (int i = 0; i < conversationHistory.size(); i++) {
                    System.out.println("FROM HISTORY -- "+ conversationHistory.get(i));
                    int totalChunks = 0;
                    if (conversationHistory.get(i).getChunk() == null) { //if chunk is null it is a message
                        updateRecyclerMessages(conversationHistory.get(i));
                    } else { //in case it is a chunk we need to gather all file chunks to a new value and pass it to the recycler
                        Value currentValue = conversationHistory.get(i);
                        System.out.println("FROM ACTIVITY - " + currentValue);
                        totalChunks = currentValue.getRemainingChunks();
                        String filename = currentValue.getFilename().substring(0, currentValue.getFilename().lastIndexOf("_"))
                                + currentValue.getFileExt();
                        String fileType = currentValue.getFileType();

                        File outputFile = new File(getCacheDir(), filename);
                        List <byte[]> fileByteList = new ArrayList<>();
                        for (int j = i, index = 0; j <= i + totalChunks; j++, index++) {
                            fileByteList.add(conversationHistory.get(j).getChunk());
                        }
                        try {
                            FileOutputStream fos = new FileOutputStream(outputFile);
                            for (byte[] b : fileByteList){
                                fos.write(b);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        MultimediaFile outputMultimediaFile = new MultimediaFile(outputFile, fileType);
                        Value newValue = new Value(outputMultimediaFile, currentValue.getProfile(), currentValue.getTopic(), fileType);
                        updateRecyclerMessages(newValue);
                    }
                    i += totalChunks; //need to skip already checked chunks//need to skip already checked chunks
                }
            }
        }

        ExecutorService checkForNewMessage = Executors.newSingleThreadExecutor(); //Executor thread to listen for new messages
        checkForNewMessage.execute(() -> {
            while(true){
                if (!Objects.requireNonNull(MainActivity.allTopicReceivedMessages.get(topic)).isEmpty()){
                    Value current = Objects.requireNonNull(MainActivity.allTopicReceivedMessages.get(topic)).peek();
                    assert current != null;
                    if (current.isFile()){
                        int totalChunks = current.getRemainingChunks();
                        List <byte[]> fileByteList = new ArrayList<>();
                        for (int i = 0; i <= totalChunks; i++){
                            fileByteList.add(Objects.requireNonNull(Objects.requireNonNull(MainActivity.allTopicReceivedMessages.get(topic)).poll()).getChunk());
                        }
                        String filename = current.getFilename().substring(0, current.getFilename().lastIndexOf("_"))
                                + current.getFileExt();
                        String fileType = current.getFileType();

                        File outputFile = new File(getCacheDir(), filename);
                        try {
                            FileOutputStream fos = new FileOutputStream(outputFile);
                            for (byte[] b : fileByteList){
                                fos.write(b);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        MultimediaFile outputMultimediaFile = new MultimediaFile(outputFile, fileType);
                        Value newValue = new Value(outputMultimediaFile, current.getProfile(), current.getTopic(), fileType);
                        synchronized (this){
                            allMessagesList.add(newValue);
                        }
                    } else {
                        synchronized (this){
                            allMessagesList.add(Objects.requireNonNull(MainActivity.allTopicReceivedMessages.get(topic)).poll());
                        }
                    }
                    runOnUiThread(() -> messageAdapter.notifyItemInserted(allMessagesList.size() - 1));
                }
            }
        });


        sendButton.setOnClickListener(v -> { //on hitting send
            try { //minimizing keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
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

        cameraButton.setOnClickListener( v -> {
            if (checkCameraPermission()) {
                useCamera();
            } else {
                String[] permissions = {Manifest.permission.CAMERA};
                ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_CODE); // Request Permission
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

    private boolean checkCameraPermission(){
        return ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
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


    private void useCamera(){

        String[] options = {"Image","Video"}; //giving the option for both image and video capture with an alert
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please pick action: ");
        builder.setItems(options, (dialog, which) -> {
            if ("Image".equals(options[which])){
                Intent imageCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(imageCaptureIntent, CAPTURE_IMAGE);
            }
            else {
                Intent imageCaptureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                startActivityForResult(imageCaptureIntent, CAPTURE_VIDEO);
            }
        }).show();
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
        else if (requestCode == CAPTURE_IMAGE && resultCode == RESULT_OK){

            Value imageValue = createValueFromCameraImageResult(data);
            sendMessageToMainHandler(401, "NEW_MESSAGE_IMAGE_SENT", imageValue); //sending the file to the handler
            updateRecyclerMessages(imageValue); //sending the file to the recycler
        }
        else if (requestCode == CAPTURE_VIDEO && resultCode == RESULT_OK) {

            Value videoValue = createValueFromCameraVideoResult(data);
            sendMessageToMainHandler(403, "NEW_MESSAGE_VIDEO_SENT", videoValue); //sending the file to the handler
            updateRecyclerMessages(videoValue); //sending the file to the recycler
        }
    }

    private Value createValueFromResult(Intent data, String fileType){ //creating value from intent data

        String uriString = data.getData().getPath();
        String path = uriString.substring(uriString.indexOf(":")+1);
        MultimediaFile file = new MultimediaFile(path, fileType);
        return new Value(file, profile, topic, fileType);
    }

    private Value createValueFromCameraImageResult(Intent data){
        //camera result data need different approach

        String newImageName = "photo.jpg";
        Bitmap photo = (Bitmap) data.getExtras().get("data"); //retrieving bitmap
        File f = new File(getApplicationContext().getCacheDir(), "photo.jpg"); //creating a new file

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG, 0, bos);
        byte[] bitMapData = bos.toByteArray();

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(f);
            fos.write(bitMapData);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MultimediaFile file = new MultimediaFile(getApplicationContext().getCacheDir().getPath() + "/" + newImageName, "image");
        return new Value(file, profile, topic, "image");
    }

    private Value createValueFromCameraVideoResult(Intent data) {
        Uri uri = data.getData();
        String path = getRealPathFromURI(ChatChannelActivity.this,uri);
        System.out.println(path);
        MultimediaFile file = new MultimediaFile(path, "video");
        return new Value(file, profile, topic, "video");
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

    private final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
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

    public String getRealPathFromURI(Context context, Uri contentUri) { //GETTING REAL PATH FROM URI
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                useCamera();
            }
        }
        else{
            Toast.makeText(ChatChannelActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVideoClicked(Value value) { //viewing video on click
        System.out.println(value.getMultimediaFile().getPath().toString());
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
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(download);
            fos.write(Files.readAllBytes(Paths.get(value.getMultimediaFile().getFile().getPath())));
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(ChatChannelActivity.this, "Downloading File", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageClicked(Value value) { //viewing image on click
        System.out.println(value.getMultimediaFile().getPath().toString());

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value.getMultimediaFile().getPath().toString()));
        intent.setDataAndType(Uri.parse(value.getMultimediaFile().getPath().toString()), "image/*");
        startActivity(intent);
    }


}