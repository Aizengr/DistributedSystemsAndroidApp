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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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


public class ProfileActivity extends AppCompatActivity implements GridClickListener{

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
        uploadButton =findViewById(R.id.upload_button);
        profileName = findViewById(R.id.profile_name);
        profileImage = findViewById(R.id.profile_image);



        Intent intent = getIntent();
        profile = (Profile) intent.getSerializableExtra("profile");

        profileGridAdapter = new ProfileGridAdapter(this, profile.getProfileFiles(), this);
        gridView.setAdapter(profileGridAdapter);

        subCount.setText(String.format("Subbed to: %d conversations!", profile.subCount()));
        profileName.setText(profile.getUsername());



        uploadButton.setOnClickListener(v -> {

            String[] options = {"Image","Video"}; //giving the option for both image and video capture with an alert
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Please pick action: ");
            builder.setItems(options, (dialog, which) -> {
                if ("Image".equals(options[which])){
                    if (checkPermission()){
                        Intent pickImage = new Intent();
                        pickImage.setType("image/*");
                        pickImage.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(pickImage, PICK_IMAGE);
                    }
                    else {
                        requestPermission();
                    }
                }
                else {
                    if (checkPermission()){
                        Intent pickVideo = new Intent();
                        pickVideo.setType("video/*");
                        pickVideo.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(pickVideo, PICK_VIDEO);
                    }
                    else {
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
                    Toast.makeText(ProfileActivity.this,"Permission granted",Toast.LENGTH_SHORT).show();
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

        }
        else if (requestCode == PICK_VIDEO && resultCode == RESULT_OK){
            MultimediaFile videoValue = createMultimediaFromResult(data, "video");
            profile.addToProfile(videoValue);
            profileGridAdapter.notifyDataSetChanged();
        }
    }

    public MultimediaFile createMultimediaFromResult(Intent data, String filetype){

        Uri uri = data.getData();
        String path = uri.getPath().substring(uri.getPath().indexOf(":") + 1);
        System.out.println(path);
        MultimediaFile file = new MultimediaFile(path, filetype);
        return file;
    }



    @Override
    public void onVideoClicked(MultimediaFile file) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getPath().toString()));
        intent.setDataAndType(Uri.parse(file.getPath().toString()), "video/*");
        startActivity(intent);

    }

    @Override
    public void onImageClicked(MultimediaFile file) {

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(file.getPath().toString()));
        intent.setDataAndType(Uri.parse(file.getPath().toString()), "image/*");
        startActivity(intent);

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

}