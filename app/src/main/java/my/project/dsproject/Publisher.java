package my.project.dsproject;

import android.os.Handler;
import android.os.Message;


import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Queue;


public class Publisher extends UserNode implements Runnable, Serializable{

    private String topic;
    private Queue<Value> messageQueue;

    public Publisher(Profile profile, Handler handler, Queue messageQueue, String topic){
        super(profile, handler);
        this.messageQueue = messageQueue;
        this.topic = topic;
    }

    private void initializeConnection(){
        connect(currentPort, currentAddress, pubRequest);
        alivePublisherConnections.add(this);
    }

    @Override
    public void run() {
        if (running) {
            System.out.println("CONSUMER THREAD ID " + Thread.currentThread().getId() +
                    " AND NAME " + Thread.currentThread().getName() +  " Running: " + running);
            initializeConnection();
            final Message msg = new Message();
            if (this.socket != null) {
                topic = searchTopic(topic, pubRequest);
                if (topic == null){
                    msg.what = -100; //topic not found
                    this.handler.sendMessage(msg);
                }
                while (!socket.isClosed() && running) {
                    Value newMessage = checkForNewMessage();
                    if (newMessage != null) {
                        if (!newMessage.isFile()) {
                            push(newMessage);
                        } else {
                            System.out.println(newMessage.getMultimediaFile());
                            pushChunks(topic, newMessage.getMultimediaFile());
                        }
                    }
                }
            } else {
                System.out.println("SYSTEM: Publisher exiting...");
                msg.what = -1000;
                this.handler.sendMessage(msg);
            }
        }
    }


    public Value checkForNewMessage() {

        Value newMessage = null;
        if (!this.messageQueue.isEmpty()){
            newMessage = messageQueue.poll();
        }
        return newMessage;
    }

    public void pushChunks(String topic, MultimediaFile file){ //splitting in chunks and pushing each one
        List<byte[]> chunkList = file.splitInChunks();
        String fileID = file.getFileID();
        Value chunk;
        for (int i = 0; i < chunkList.size(); i++) { //get all byte arrays, create chunk name and value obj
            StringBuilder strB = new StringBuilder(file.getFileName());
            String chunkName = strB.insert(file.getFileName().lastIndexOf("."), String.format("_%s", i)).toString();
            chunk = new Value("SYSTEM: Sending file chunk", chunkName, this.profile, topic, fileID,
                    file.getNumberOfChunks() - i - 1, chunkList.get(i), pubRequest, file.getFileType());
            push(chunk);
        }
    }

    public void push(Value value){ //initial push

        try {
            System.out.printf("SYSTEM: Trying to push to topic: %s with value: %s%n\n", value.getTopic() , value);
            if (value.getMessage() != null){
                objectOutputStream.writeObject(value); // if value is not null write to stream
                objectOutputStream.flush();
            }
            else throw new RuntimeException("SYSTEM: Could not write to stream. Message corrupted.\n"); //else throw exc
        } catch (IOException e){
            System.out.println(e.getMessage());
            disconnect();
        }
    }
}
