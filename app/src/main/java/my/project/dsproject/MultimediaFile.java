package my.project.dsproject;

import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultimediaFile implements Serializable {

    private String dateCreated;
    private String fileName;
    private String fileExt;
    private final String fileType;
    private final String fileID;
    private Path path;
    private File multimediaFile;
    private int numberOfChunks;
    private static final int CHUNK_KB_SIZE = 512 * 1024;


    public MultimediaFile(String loc, String fileType){
        this.path = Paths.get(loc);
        this.multimediaFile = new File(loc);
        this.fileName = multimediaFile.getName();
        this.fileType = fileType;
        this.setData();
        this.setFileExtension();
        this.fileID = UUID.randomUUID().toString(); //we implement file IDs to identify the chunks of the same file on consumer
    }

    public  MultimediaFile(File file, String fileType){
        this.path = Paths.get(file.getPath());
        this.multimediaFile = file;
        this.fileName = multimediaFile.getName();
        this.fileType = fileType;
        this.setData();
        this.setFileExtension();
        this.fileID = UUID.randomUUID().toString();
    }

    private void setData() { //method for file attributes to set date (or more if needed)
        try {
            BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
            this.dateCreated = String.valueOf(attr.creationTime());
        } catch (IOException e){
            System.out.println("Incorrect filepath.");
        }
    }

    private void setFileExtension(){ //method for managing filename string and getting file extension
        int index = this.fileName.lastIndexOf(".");
        if (index > 0 ){
            this.fileExt = fileName.substring(index + 1);
        }
    }

    public List<byte[]> splitInChunks(){ //method for splitting file in 512KB chunks with byte arrays
        try {
            System.out.println(this.path);
            byte[] multimediaFileByteArray = Files.readAllBytes(this.path);
            List<byte[]> chunks = new ArrayList<>();
            for (int i=0; i < multimediaFileByteArray.length;){
                byte[] chunk = new byte[Math.min(CHUNK_KB_SIZE, multimediaFileByteArray.length - i)];
                for (int j=0; j < chunk.length; j++,i++){
                    chunk[j] = multimediaFileByteArray[i];
                }
                chunks.add(chunk);
                this.increaseNumberOfChunks();
            }
            return chunks;
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getBytes(){
        return this.fileName.getBytes();
    }

    public String getFileExt() { return this.fileExt; }

    public String getFileName(){
        return this.fileName;
    }

    public String getFileType(){
        return this.fileType;
    }

    public String getDateCreated(){
        return this.dateCreated;
    }

    public File getFile() {return this.multimediaFile;}

    public Path getPath(){
        return this.path;
    }

    public void setFileName(String fileName){
        this.fileName = fileName;
    }

    public void setDateCreated(String date){
        this.dateCreated = date;
    }

    public void setMultimediaFile(File file){
        this.multimediaFile = file;
    }

    public void setPath(Path path){
        this.path = path;
    }

    public void setNumberOfChunks(int numberOfChunks){ this.numberOfChunks = numberOfChunks; }

    public void increaseNumberOfChunks(){this.numberOfChunks++;}

    public int getNumberOfChunks(){return this.numberOfChunks;}

    public String getFileID(){ return this.fileID;}
}
