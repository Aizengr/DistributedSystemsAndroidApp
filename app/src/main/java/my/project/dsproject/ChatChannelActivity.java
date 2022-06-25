package my.project.dsproject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatChannelActivity<Public> extends AppCompatActivity implements ClickListener {

    private MessageAdapter messageAdapter;
    private List<Value> allMessagesList; //all message list that is used by Recycler view to update the UI with any new message

    public static final int PICK_IMAGE = 1000;
    public static final int CAPTURE_IMAGE = 1001;

    public static final int PICK_ATTACHMENT = 2000;
    public static final int PICK_VIDEO = 3000;
    public static final int CAPTURE_VIDEO = 3001;

    private static final int CAMERA_PERMISSION_CODE = 1;
    private ActivityResultLauncher<Intent> activityResultLauncher;

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
    ImageButton profileButton;
    Messenger mainMessenger;
    SearchView topicSearch;
    ProgressBar searchProgressBar;
    long lastSearchTime;
    RecyclerView messageRecycler;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_channel);
        Objects.requireNonNull(getSupportActionBar()).hide();
        mainMessenger = getIntent().getParcelableExtra("connectionHandler");
        topic = getIntent().getStringExtra("topic");
        profile = (Profile) getIntent().getSerializableExtra("profile");
        allMessagesList = new ArrayList<>();
        String [] subs = new String[profile.getUserSubscribedConversations().size()];
        profile.getUserSubscribedConversations().toArray(subs);

        LocalBroadcastManager.getInstance(this).registerReceiver(listener, //listener for changing topics
                new IntentFilter("TOPIC_NOT_FOUND"));

        //history list
        Queue<Value> conversationHistory = MainActivity.conversationHistory;

        lastSearchTime = 0;
        sendButton = findViewById(R.id.send_button);
        editTextMessage = findViewById(R.id.edit_message);
        textViewTopic = findViewById(R.id.topic_text);
        imageUploadButton = findViewById(R.id.image_upload_button);
        videoUploadButton = findViewById(R.id.video_upload_button);
        attachmentUploadButton = findViewById(R.id.attachment_upload_button);
        videoPlayButton = findViewById(R.id.video_play_button);
        cameraButton = findViewById(R.id.camera_button);
        topicSearch = findViewById(R.id.topic_search);
        textViewTopic.setText(topic);
        profileButton = findViewById(R.id.profile_button);

        searchProgressBar = findViewById(R.id.searchProgressBar);
        searchProgressBar.setVisibility(View.INVISIBLE);

        messageRecycler = findViewById(R.id.recycler_chat); //finding elements and setting adapter
        messageAdapter = new MessageAdapter(profile, allMessagesList, this);
        messageRecycler.setLayoutManager(new LinearLayoutManager(this));
        messageRecycler.setAdapter(messageAdapter);

        if (conversationHistory!=null){
            while(conversationHistory.peek()!=null){
                System.out.println(conversationHistory.peek());
                updateRecyclerMessages(conversationHistory.poll());
            }
        }

        ExecutorService checkForNewMessage = Executors.newSingleThreadExecutor(); //Executor thread to listen for new messages
        checkForNewMessage.execute(() -> {
            while(true){
                if (!MainActivity.receivedMessageQueue.isEmpty()){
                    synchronized (this){
                        allMessagesList.add(MainActivity.receivedMessageQueue.poll());
                    }
                    runOnUiThread(() -> {
                        messageAdapter.notifyItemInserted(allMessagesList.size() - 1);
                        messageRecycler.smoothScrollToPosition(allMessagesList.size() - 1);
                    });
                }
            }
        });


        sendButton.setOnClickListener(v -> { //on hitting send
            try { //minimizing keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editTextMessage.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
            String messageToSend = editTextMessage.getText().toString();

            if (!messageToSend.equals("")){
                Value messageValue = new Value(messageToSend, profile, topic, "Publisher", "message");

                MainActivity.sentMessageQueue.add(messageValue); //adding message to the q for publishing
                updateRecyclerMessages(messageValue); //adding it to the recycler view as well
                editTextMessage.setText("");
            }
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

        profileButton.setOnClickListener( v-> {
            Intent profileIntent = new Intent(ChatChannelActivity.this, ProfileActivity.class);
            profileIntent.putExtra("profile", profile);
            startActivity(profileIntent);
        });

        topicSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() { //search section listener
            @Override
            public boolean onQueryTextSubmit(String query) { //in case of submit we send the message to main handler
                if (!query.equals(topic)){
                    long actualSearchTime = (Calendar.getInstance()).getTimeInMillis();
                // Only one search every second to avoid key-down & key-up
                    if (actualSearchTime > lastSearchTime + 1000)
                    {
                        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, //disabling interaction until connecting to the next topic finishes
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        searchProgressBar.setVisibility(View.VISIBLE);
                        Message msg = new Message();
                        msg.what = 101;
                        Bundle bundle = new Bundle();
                        msg.setData(bundle);
                        bundle.putString("NEW_TOPIC", query);
                        try {
                            System.out.println("CHANGING TOPIC TO HANDLER");
                            mainMessenger.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        lastSearchTime=actualSearchTime;
                    }
               }
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            //RESULT ACTIVITY LAUNCHER TO MANAGE PERMISSION ISSUE WITH ANDROID 12 MANAGE STORAGE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager())
                    Toast.makeText(ChatChannelActivity.this,"Permission granted",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ChatChannelActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChatChannelActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
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
            MainActivity.sentMessageQueue.add(imageValue);
            updateRecyclerMessages(imageValue); //sending the file to the recycler

        } else if (requestCode == PICK_ATTACHMENT && resultCode == RESULT_OK){

            Value fileValue = createValueFromResult(data, "attachment");
            MainActivity.sentMessageQueue.add(fileValue);
            updateRecyclerMessages(fileValue); //sending the file to the recycler
        }
        else if (requestCode == PICK_VIDEO && resultCode == RESULT_OK){

            Value videoValue = createValueFromResult(data, "video");
            MainActivity.sentMessageQueue.add(videoValue);
            updateRecyclerMessages(videoValue); //sending the file to the recycler
        }
        else if (requestCode == CAPTURE_IMAGE && resultCode == RESULT_OK){

            Value imageValue = createValueFromCameraImageResult(data);
            MainActivity.sentMessageQueue.add(imageValue);
            updateRecyclerMessages(imageValue); //sending the file to the recycler
        }
        else if (requestCode == CAPTURE_VIDEO && resultCode == RESULT_OK) {

            Value videoValue = createValueFromCameraVideoResult(data);
            MainActivity.sentMessageQueue.add(videoValue);
            updateRecyclerMessages(videoValue); //sending the file to the recycler
        }
    }

    private Value createValueFromResult(Intent data, String fileType){ //creating value from intent data

        Uri uri = data.getData();
        String path = RealPathUtil.getRealPath(this, uri);
        System.out.println(path);
        MultimediaFile file = new MultimediaFile(path, fileType);
        return new Value(file, profile, topic, fileType);
    }

    private Value createValueFromCameraImageResult(Intent data){
        //camera result data need different approach

        Bitmap photo = (Bitmap) data.getExtras().get("data"); //retrieving bitmap

        File f = null; //creating a new file
        try {
            f = File.createTempFile("photo", ".png", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bitMapData = bos.toByteArray();

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(f);
            fos.write(bitMapData);
            fos.flush();
            fos.close();
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MultimediaFile file = new MultimediaFile(f.getPath(), "image");
        return new Value(file, profile, topic, "image");
    }

    private Value createValueFromCameraVideoResult(Intent data) {
        Uri uri = data.getData();
        String path = RealPathUtil.getRealPath(this, uri);
        System.out.println(path);
        MultimediaFile file = new MultimediaFile(path, "video");
        return new Value(file, profile, topic, "video");
    }

    private synchronized void updateRecyclerMessages(Value value){ //synced as the list data is also change on the executor thread
        synchronized (this){
            allMessagesList.add(value); //adding the message to the main message list for recycler
            messageAdapter.notifyItemInserted(allMessagesList.size() - 1);
            messageRecycler.smoothScrollToPosition(allMessagesList.size() - 1);
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
        System.out.println(Uri.parse(value.getMultimediaFile().getPath().toString()));

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(value.getMultimediaFile().getPath().toString()));
        intent.setDataAndType(Uri.parse(value.getMultimediaFile().getPath().toString()), "image/*");
        startActivity(intent);
    }

    private final BroadcastReceiver listener = new BroadcastReceiver() { //IN CASE SEARCHED TOPIC DOES NOT EXIST
        @Override
        public void onReceive( Context context, Intent intent ) {
            searchProgressBar.setVisibility(View.INVISIBLE);
            AlertDialog alertDialog = new AlertDialog.Builder(ChatChannelActivity.this).create();
            alertDialog.setTitle("Topic not found");
            alertDialog.setMessage("No topics found for: " + intent.getStringExtra("TOPIC_NOT_FOUND"));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    (dialog, which) -> dialog.dismiss());
            alertDialog.show();
        }
    };


    public static class RealPathUtil {

        public static String getRealPath(Context context, Uri fileUri) {
            String realPath;
            // SDK < API11
            if (Build.VERSION.SDK_INT < 11) {
                realPath = RealPathUtil.getRealPathFromURI_BelowAPI11(context, fileUri);
            }
            // SDK >= 11 && SDK < 19
            else if (Build.VERSION.SDK_INT < 19) {
                realPath = RealPathUtil.getRealPathFromURI_API11to18(context, fileUri);
            }
            // SDK > 19 (Android 4.4) and up
            else {
                realPath = RealPathUtil.getRealPathFromURI_API19(context, fileUri);
            }
            return realPath;
        }


        @SuppressLint("NewApi")
        public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
            String[] proj = {MediaStore.Images.Media.DATA};
            String result = null;

            CursorLoader cursorLoader = new CursorLoader(context, contentUri, proj, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();

            if (cursor != null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                result = cursor.getString(column_index);
                cursor.close();
            }
            return result;
        }

        public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri) {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = 0;
            String result = "";
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                result = cursor.getString(column_index);
                cursor.close();
                return result;
            }
            return result;
        }

        /**
         * Get a file path from a Uri. This will get the the path for Storage Access
         * Framework Documents, as well as the _data field for the MediaStore and
         * other file-based ContentProviders.
         *
         * @param context The context.
         * @param uri     The Uri to query.
         * @author paulburke
         */
        @SuppressLint("NewApi")
        public static String getRealPathFromURI_API19(final Context context, final Uri uri) {

            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

            // DocumentProvider
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {

                // Return the remote address
                if (isGooglePhotosUri(uri))
                    return uri.getLastPathSegment();

                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

            return null;
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        public static String getDataColumn(Context context, Uri uri, String selection,
                                           String[] selectionArgs) {

            Cursor cursor = null;
            final String column = "_data";
            final String[] projection = {
                    column
            };

            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int index = cursor.getColumnIndexOrThrow(column);
                    return cursor.getString(index);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            return null;
        }


        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        public static boolean isExternalStorageDocument(Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        public static boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        public static boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        public static boolean isGooglePhotosUri(Uri uri) {
            return "com.google.android.apps.photos.content".equals(uri.getAuthority());
        }

    }

}