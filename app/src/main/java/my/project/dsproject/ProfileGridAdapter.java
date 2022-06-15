package my.project.dsproject;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.util.ArrayList;

public class ProfileGridAdapter extends ArrayAdapter<MultimediaFile> {

    GridClickListener listener;

    public ProfileGridAdapter(@NonNull Context context, ArrayList<MultimediaFile> profileUploads, GridClickListener listener) {
        super(context, 0, profileUploads);
        this.listener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        View listItemView;
        MultimediaFile file = getItem(position);

        if (file.getFileType().equals("video")){
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.grid_item_video, parent, false);

            ImageView videoImage = listItemView.findViewById(R.id.grid_video_image);
            ImageButton playButton = listItemView.findViewById(R.id.grid_video_play_button);

            Bitmap thumbnail;
            thumbnail = ThumbnailUtils.createVideoThumbnail //retrieving
                    (file.getPath().toString(),
                            MediaStore.Video.Thumbnails.MINI_KIND);
            videoImage.setImageBitmap(thumbnail);

            playButton.setOnClickListener(v -> listener.onVideoClicked(file));
        }
        else {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.grid_item_image, parent, false);
            ImageView image = listItemView.findViewById(R.id.grid_image);

            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath().toString());
            image.setImageBitmap(bitmap);
            image.setClickable(true);

            image.setOnClickListener(v -> listener.onImageClicked(file));
        }

        return listItemView;
    }
}
