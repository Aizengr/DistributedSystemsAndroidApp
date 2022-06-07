package my.project.dsproject;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static java.lang.Integer.parseInt;

import android.os.Handler;
import android.os.Message;

public class Consumer extends UserNode implements Runnable,Serializable {

    private String topic;
    private Queue<Value> conversationHistory;
    private Queue<Value> receivedMessageQueue;

    public Consumer(Profile profile, Handler handler, Queue<Value> conversationHistory, Queue<Value> receivedMessageQueue, String topic){
        super(profile, handler);
        this.conversationHistory = conversationHistory;
        this.receivedMessageQueue = receivedMessageQueue;
        this.topic = topic;
        aliveConsumerConnections.add(this);
    }

    private void initializeConnection(){
        connect(currentPort, currentAddress, conRequest);
        aliveConsumerConnections.add(this);
    }

    @Override
    public void run() {
        initializeConnection();
        final Message msg = new Message();
        if (this.socket != null) {
            topic = searchTopic(topic);
            if (topic != null) {
                List<Value> data = getConversationData(topic); //getting conversation data at first
                List<Value> chunkList = new ArrayList<>(); //separating chunks from live messages
                for (Value message : data) {
                    if (message.isFile()) {
                        //NEED TO DO WORK HERE IN CASE IT IS A FILE IN ORDER TO SEPARATE OR NOT FROM MESSAGES
                        chunkList.add(message); //if its a file add it to the chunklist
                    } else {
                        System.out.println(message);
                        conversationHistory.add(message);
                    }
                }
                msg.what = 5;
                this.handler.sendMessage(msg);
                writeFilesByID(chunkList); //sorting chunk list and writing files
                while (!socket.isClosed()) {
                    listenForMessage(); //listening for messages while we are connected
                }
            }
        } else {
            System.out.println("SYSTEM: Consumer exiting...");
            msg.what = -100;
            this.handler.sendMessage(msg);
        }
    }

    private void listenForMessage(){ //main consumer functionality,listening for messages and files while connected as consumer to a specific topic
        try {
            Object message = objectInputStream.readObject();
            if (message instanceof Value && ((Value)message).getRequestType().equalsIgnoreCase("liveMessage")){ //live message case
                System.out.println("SYSTEM: Receiving live chat message:" + message);
                System.out.println(((Value) message).getProfile().getUsername() +":" + ((Value) message).getMessage());
            }
            else if (message instanceof Value && ((Value)message).getRequestType().equalsIgnoreCase("liveFile")){ //live file case
                System.out.println("SYSTEM: " + ((Value) message).getUsername() + " has started file sharing. Filename: " + ((Value) message).getFilename());
                List<Value> chunkList = new ArrayList<>();
                int incomingChunks = ((Value) message).getRemainingChunks();
                for (;incomingChunks >= 0; incomingChunks--){
                    chunkList.add((Value)message);
                    if (incomingChunks == 0){break;}
                    message = objectInputStream.readObject();
                }
                System.out.println(chunkList);
                writeFilesByID(chunkList);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            disconnect();
        }
    }


    private List<Value> getConversationData(String topic){ //getting conversation history once we connect to the topic
        List<Value> data = new ArrayList<>();
        Value value = new Value("datareq", this.profile, topic, conRequest);
        try {
            objectOutputStream.writeObject(value);
            objectOutputStream.flush();
            int incomingTopicMessages = (Integer)objectInputStream.readObject(); //asking how many messages (chunks + livechat) to read
            System.out.println("SYSTEM: Number of conversation history messages and files: " + incomingTopicMessages);
            for(int i= 0; i < incomingTopicMessages; i++){
                data.add((Value)objectInputStream.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            disconnect();
        }
        return data;
    }

    private synchronized void writeFilesByID(List<Value> chunkList){ //withdrawal and writing of all files received
        String temp ="";
        List<String> fileIDs = new ArrayList<>();
        for (Value chunk : chunkList) { //separating chunks by file id
            System.out.println("Chunklist");
            System.out.println(chunk);
            if (!chunk.getFileID().equalsIgnoreCase(temp)) {
                fileIDs.add(chunk.getFileID());
                temp = chunk.getFileID();
            }
        }
        for (String id : fileIDs){ //for each id we keep the chunks in a list
            List <Value> fileList = new ArrayList<>();
            for (Value chunk : chunkList){
                System.out.println("fileList");
                System.out.println(chunk);
                if (id.equalsIgnoreCase(chunk.getFileID())){
                    fileList.add(chunk);
                }
            }
            System.out.println(fileList);
            Value[] sortedChunks = new Value[fileList.size()];
            for (Value chunk : fileList){ //and sorting them according to the number on the chunk name
                int index = parseInt(chunk.getFilename().substring(chunk.getFilename().indexOf("_") + 1, chunk.getFilename().indexOf(".")));
                sortedChunks[index] = chunk;
            }
            String filename = sortedChunks[0].getFilename().substring(0, sortedChunks[0].getFilename().indexOf("_"));
            String fileExt = sortedChunks[0].getFilename().substring(sortedChunks[0].getFilename().indexOf("."));
            Path path = Paths.get(downloadPath + filename + fileExt);
            int counter = 1;
            String existString;
            while (Files.exists(path)){ //if file exists loop with a counter and change filename to filename%counter%.ext
                System.out.println(path);
                existString = String.format("(%s)", counter);
                path = Paths.get(downloadPath + filename + existString + fileExt);
                counter++;
            }
            File download = new File(String.valueOf(path)); //writing file
            System.out.println("SYSTEM: Downloading file at: " + path);
            try {
                FileOutputStream os = new FileOutputStream(download);
                for (Value chunk : sortedChunks) {
                    System.out.println("sortedList");
                    System.out.println(chunk);
                    os.write(chunk.getChunk());
                }
                os.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
                disconnect();
            }
        }
    }
}