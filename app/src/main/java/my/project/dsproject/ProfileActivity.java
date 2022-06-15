package my.project.dsproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class ProfileActivity extends AppCompatActivity {

    GridView gridView;
    ImageButton backButton;
    ImageButton subsButton;
    FloatingActionButton uploadButton;
    TextView profileName;
    TextView subCount;
    ImageView profileImage;
    Profile profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        gridView = findViewById(R.id.simpleGridView);
        backButton = findViewById(R.id.back_button);
        subsButton = findViewById(R.id.chat_button);
        subCount = findViewById(R.id.sub_count);
        uploadButton =findViewById(R.id.upload_button);
        profileName = findViewById(R.id.profile_name);
        profileImage = findViewById(R.id.profile_image);

        Intent intent = getIntent();
        profile = (Profile) intent.getSerializableExtra("profile");

        ProfileGridAdapter profileGridAdapter = new ProfileGridAdapter(this, profile.getProfileFiles());


    }


}