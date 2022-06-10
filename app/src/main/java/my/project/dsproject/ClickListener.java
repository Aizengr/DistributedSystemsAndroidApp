package my.project.dsproject;


import java.io.IOException;

public interface ClickListener { //interface for handling recycler view item clicks

    void onVideoClicked(Value value);

    void onDownloadClicked(Value value) throws IOException;

    void onImageClicked(Value value);
}
