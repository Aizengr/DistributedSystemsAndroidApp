package my.project.dsproject;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

import static java.lang.Integer.parseInt;


import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony;

public class Consumer extends UserNode implements Runnable,Serializable {

    private String topic;
    private Queue<Value> conversationHistory;
    private Queue<Value> receivedMessageQueue;

    public Consumer(Profile profile, Handler handler, Queue<Value> conversationHistory, Queue<Value> receivedMessageQueue, String topic){
        super(profile, handler);
        this.conversationHistory = conversationHistory;
        this.receivedMessageQueue = receivedMessageQueue;
        this.topic = topic;
    }


    private void initializeConnection(){
        connect(currentPort, currentAddress, conRequest);
        aliveConsumerConnections.add(this);
    }

    @Override
    public void run() {
        if (running) {
            initializeConnection();
            final Message msg = new Message();
            if (this.socket != null) {
                topic = searchTopic(topic, conRequest);
                if (topic != null) {
                    List<Value> data = getConversationData(topic); //getting conversation data at first
                    Value[] sortedData = sortHistory(data); //sorting them based on message
                    for (int i = 0; i < sortedData.length; i++) {
                        List<Value> chunkList = new ArrayList<>();
                        Value currentValue = sortedData[i];
                        int totalChunks = 0;
                        if (currentValue.isFile()) { //if it is file we gather all chunks and sort them before adding them
                            totalChunks = currentValue.getRemainingChunks(); //to history
                            for (int j = i; j <= i + totalChunks; j++) { //nested for loop in order to keep the correct order
                                chunkList.add(data.get(j));
                            }
                            Value file = chunksToSingleValue(chunkList);
                            conversationHistory.add(file);

                        } else {
                            conversationHistory.add(currentValue);

                        }
                        i += totalChunks;
                    }
                    msg.what = 200;
                    this.handler.sendMessage(msg);
                    while (!socket.isClosed() && running) {
                        listenForMessage(); //listening for messages while we are connected
                    }
                }
            } else {
                System.out.println("SYSTEM: Consumer exiting...");
                msg.what = -100;
                this.handler.sendMessage(msg);
            }
        }
    }

    private Value[] sortHistory(List<Value> data){ //sorting history based on the number of the message
        Value[] sorted = new Value[data.size()];
        for (Value value : data){
            sorted[value.getMessageNumber()] = value;
        }
        return sorted;
    }

    private Value[] sortReceivedFile(List<Value> chunkList){
        Value[] sortedChunks = new Value[chunkList.size()];
        for (Value chunk : chunkList){ //and sorting them according to the number on the chunk name
            int index = parseInt(chunk.getFilename().substring(chunk.getFilename().lastIndexOf("_") + 1, chunk.getFilename().lastIndexOf(".")));
            sortedChunks[index] = chunk;
        }
        return sortedChunks;
    }

    private void listenForMessage(){ //main consumer functionality,listening for messages and files while connected as consumer to a specific topic
        try {
            Object message = objectInputStream.readObject();
            if (message instanceof Value && ((Value)message).getRequestType().equalsIgnoreCase("liveMessage")){ //live message case
                System.out.println("SYSTEM: Receiving live chat message:" + message);
                System.out.println(((Value) message).getProfile().getUsername() +":" + ((Value) message).getMessage());

                synchronized (this) {
                    receivedMessageQueue.add((Value)message);
                }

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
                Value[] sortedFileChunks = sortReceivedFile(chunkList);
                List<Value> sortedList = Arrays.asList(sortedFileChunks);
                Value newFile = chunksToSingleValue(sortedList);
                receivedMessageQueue.add(newFile);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            disconnect();
        }
    }

    private Value chunksToSingleValue(List<Value> chunkList){
        Value file;
        Value firstChunk = chunkList.get(0);
        String filename = firstChunk.getFilename().substring(0, firstChunk.getFilename().lastIndexOf("_"))
                + firstChunk.getFileExt();
        String fileType = firstChunk.getFileType();

        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outputFile);
            for (Value value: chunkList){
                System.out.println(value);
                fos.write(value.getChunk());
            }
            fos.flush();
            fos.close();
            outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(outputFile.getPath());
        MultimediaFile outputMultimediaFile = new MultimediaFile(outputFile, fileType);
        file = new Value(outputMultimediaFile, firstChunk.getProfile(), firstChunk.getTopic(), fileType);
        try {
            System.out.println(Files.size(file.getMultimediaFile().getPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
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
}