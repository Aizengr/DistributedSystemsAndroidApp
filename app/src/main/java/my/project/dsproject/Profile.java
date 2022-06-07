package my.project.dsproject;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.Serializable;
import java.util.*;


public class Profile implements Serializable{

    private String username;
    public static Multimap<String,MultimediaFile> userMultimediaFileMap; //profiles files
    private final Queue<MultimediaFile> pendingUpload; //q for upload
    private final List<String> userSubscribedConversations; //subbed user's topics

    public Profile(String username){
        this.username = username;
        this.userSubscribedConversations = new ArrayList<>();
        this.pendingUpload = new LinkedList<>();
        userMultimediaFileMap = ArrayListMultimap.create();
    }

    public void addFileToProfile(String fileName, MultimediaFile file){

        userMultimediaFileMap.put(fileName,file);
        addFileToUploadQueue(file);
    }

    public void addFileToUploadQueue(MultimediaFile file){
        pendingUpload.add(file);
    }

    public MultimediaFile getFileFromUploadQueue(){
        return pendingUpload.poll();
    }

    public boolean checkSub(String topic){
        return userSubscribedConversations.contains(topic);
    }

    public void sub(String topic){
        userSubscribedConversations.add(topic);
    }

    public void removeFile(String name, MultimediaFile file){
        userMultimediaFileMap.remove(name,file);
    }

    public void unSub(String conversationName){
        userSubscribedConversations.remove(conversationName.hashCode());
    }

    public String getUsername(){
        return this.username;
    }

    public void setUserName(String username){
        this.username = username;
    }

    public int checkUploadQueueCount(){
        return pendingUpload.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Profile profile = (Profile) o;
        return Objects.equal(username, profile.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }
}
