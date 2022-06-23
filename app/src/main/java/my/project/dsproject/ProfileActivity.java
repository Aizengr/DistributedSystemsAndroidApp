package my.project.dsproject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.URISyntaxException;
import java.util.Objects;


public class ProfileActivity extends AppCompatActivity implements GridClickListener {

    public static final int PICK_IMAGE = 1000;
    public static final int PICK_VIDEO = 3000;

    GridView gridView;
    ImageButton backButton;
    ImageButton subsButton;
    FloatingActionButton uploadButton;
    TextView profileName;
    TextView subCount;
    ImageView profileImage;
    Profile profile;
    ProfileGridAdapter profileGridAdapter;
    private ActivityResultLauncher<Intent> activityResultLauncher;


    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Objects.requireNonNull(getSupportActionBar()).hide();
        gridView = findViewById(R.id.simpleGridView);
        backButton = findViewById(R.id.back_button);
        subsButton = findViewById(R.id.chat_button);
        subCount = findViewById(R.id.sub_count);
        uploadButton = findViewById(R.id.upload_button);
        profileName = findViewById(R.id.profile_name);
        profileImage = findViewById(R.id.profile_image);


        Intent intent = getIntent();
        profile = (Profile) intent.getSerializableExtra("profile");

        profileGridAdapter = new ProfileGridAdapter(this, profile.getProfileFiles(), this);
        gridView.setAdapter(profileGridAdapter);

        subCount.setText(String.format("Subbed to: %d conversations!", profile.subCount()));
        profileName.setText(profile.getUsername());


        uploadButton.setOnClickListener(v -> {

            String[] options = {"Image", "Video"}; //giving the option for both image and video capture with an alert
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Please pick action: ");
            builder.setItems(options, (dialog, which) -> {
                if ("Image".equals(options[which])) {
                    if (checkPermission()) {
                        Intent pickImage = new Intent();
                        pickImage.setType("image/*");
                        pickImage.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(pickImage, "Select Picture"), PICK_IMAGE);
                    } else {
                        requestPermission();
                    }
                } else {
                    if (checkPermission()) {
                        Intent pickVideo = new Intent();
                        pickVideo.setType("video/*");
                        pickVideo.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(Intent.createChooser(pickVideo, "Select a File to Upload"), PICK_VIDEO);
                    } else {
                        requestPermission();
                    }
                }
            }).show();

        });

        backButton.setOnClickListener(v -> this.finish());

        subsButton.setOnClickListener(v -> {


        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            //RESULT ACTIVITY LAUNCHER TO MANAGE PERMISSION ISSUE WITH ANDROID 12 MANAGE STORAGE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager())
                    Toast.makeText(ProfileActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(ProfileActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ProfileActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) { //handling ACTIVITY RESULTS
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            MultimediaFile imageValue = createMultimediaFromResult(data, "image");
            profile.addToProfile(imageValue);
            profileGridAdapter.notifyDataSetChanged();
            //FOR THE TIME BEING PROFILE FILES ARE ONLY VISIBLE LOCALLY
            //WE CAN IMPLEMENT A PROFILE SYSTEM ON BROKERS WHICH WILL KEEP THE PROFILE FILES EACH PROFILE
            // AND USE CONSUMER PUBLISHER FOR THE SAME

        } else if (requestCode == PICK_VIDEO && resultCode == RESULT_OK) {
            MultimediaFile videoValue = createMultimediaFromResult(data, "video");
            profile.addToProfile(videoValue);
            profileGridAdapter.notifyDataSetChanged();
        }
    }

    public MultimediaFile createMultimediaFromResult(Intent data, String filetype) {

        MultimediaFile file;
        Uri uri = data.getData();
        String path = getPathFromUri(this, uri);
        file = new MultimediaFile(path, filetype);
        return file;
    }


    @Override
    public void onVideoClicked(MultimediaFile file) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getPath().toString()));
        intent.setDataAndType(Uri.parse(file.getPath().toString()), "video/*");
        startActivity(intent);
        System.out.println(file.getPath().toString());

    }

    @Override
    public void onImageClicked(MultimediaFile file) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getPath().toString()));
        intent.setDataAndType(Uri.parse(file.getPath().toString()), "image/*");
        startActivity(intent);
        System.out.println(file.getPath().toString());

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


    private final String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private void requestPermission() { //PERMISSION RESULT ACTIVITY FOR MANAGE PERMISSIONS ACTIVATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            new AlertDialog.Builder(ProfileActivity.this)
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
            ActivityCompat.requestPermissions(ProfileActivity.this, permissions, 30);
        }
    }

    public static String getPathFromUri(final Context context, final Uri uri) { //source: https://stackoverflow.com/questions/17546101/get-real-path-for-uri-android

        final boolean isKitKat = true;

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
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
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

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