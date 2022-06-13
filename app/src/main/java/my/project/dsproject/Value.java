package my.project.dsproject;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;

public class Value implements Serializable{    //serializable object for all kinds of communication
                                                // with brokers as well as data passing

    private String message, topic, filename;
    private final Profile profile;
    private String requestType, fileID;
    private byte[] chunk;
    private int messageNumber = 0;
    private int remainingChunks;
    private boolean fileSharing = false;
    private MultimediaFile multimediaFile;
    String fileType;



    public Value(String message, Profile profile, String topic, String requestType) {
        this.message = message;
        this.profile = profile;
        this.topic = topic;
        this.requestType = requestType;
    }

    public Value(String message, Profile profile, String requestType) {
        this.message = message;
        this.profile = profile;
        this.requestType = requestType;
    }

    public Value(String message, Profile profile, String topic, String requestType, String fileType) {
        this.message = message;
        this.profile = profile;
        this.topic = topic;
        this.requestType = requestType;
        this.fileType = fileType;
    }


    public Value(MultimediaFile file, Profile profile, String topic, String fileType){
        this.multimediaFile = file;
        this.filename = this.multimediaFile.getFileName();
        this.profile = profile;
        this.topic = topic;
        this.fileType = fileType;
        this.fileSharing = true;
    }

    public Value(String message, String chunkName, Profile profile, String topic, String fileID, int remainingChunks, byte[] chunk, String requestType, String fileType){
        this.message = message;
        this.chunk = Arrays.copyOf(chunk,chunk.length);
        this.profile = profile;
        this.topic = topic;
        this.fileID = fileID;
        this.remainingChunks = remainingChunks;
        this.fileType = fileType;
        this.filename = chunkName;
        this.requestType = requestType;
        this.fileSharing = true;
    }


    @Override //toString override for printing value objects
    public String toString() { //prints all non-null fields
        String string = "Value: {";
        Class<? extends Value> c = this.getClass();
        Field[] fields = c.getDeclaredFields();
        for(Field field : fields){
            field.setAccessible(true);
            try {
                Object value = field.get(this);
                if (value != null) {
                    string = string.concat(field.getName() + "=" + value + ", ");
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        string = string.concat("}");
        return string;
    }

    public String getFileType() {
        return fileType;
    }

    public MultimediaFile getMultimediaFile() {
        return multimediaFile;
    }

    public void setMessageNumber(int number){
        this.messageNumber = number;
    }

    public int getMessageNumber(){
        return this.messageNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setRequestType(String requestType){
        this.requestType = requestType;
    }

    public boolean isFile(){
        return this.fileSharing;
    }

    public String getTopic(){return topic;}

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return this.profile.getUsername();
    }

    public String getRequestType(){
        return this.requestType;
    }

    public int getRemainingChunks() {
        return remainingChunks;
    }

    public String getFilename(){
        return this.filename;
    }

    public String getFileID(){return this.fileID;}

    public String getFileExt(){ return this.filename.substring(this.filename.lastIndexOf("."));}

    public byte[] getChunk() {
        return chunk;
    }

    public Profile getProfile() {
        return this.profile;
    }
}
