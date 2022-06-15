package my.project.dsproject;

import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.Serializable;
import java.util.*;


public class Profile implements Serializable{

    private String username;
    private final ArrayList<MultimediaFile> profileMultimediaFileList; //profiles files
    private final List<String> userSubscribedConversations; //subbed user's topics

    public Profile(String username){
        this.username = username;
        this.userSubscribedConversations = new ArrayList<>();
        profileMultimediaFileList = new ArrayList<>();
    }

    public ArrayList <MultimediaFile> getProfileFiles(){
        return profileMultimediaFileList;
    }

    public void addToProfile(MultimediaFile file) {
        if (file != null) {
            profileMultimediaFileList.add(file);
        }
    }

    public boolean checkSub(String topic){
        return userSubscribedConversations.contains(topic);
    }

    public void sub(String topic){
        userSubscribedConversations.add(topic);
    }

    public int subCount(){
        return userSubscribedConversations.size();
    }

    public List<String> getUserSubscribedConversations(){
        return userSubscribedConversations;
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
