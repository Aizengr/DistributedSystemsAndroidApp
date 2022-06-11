package my.project.dsproject;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static java.lang.Integer.parseInt;


import android.os.Handler;
import android.os.Message;

public class Consumer extends UserNode implements Runnable,Serializable {

    private String topic;
    private final List<Value> conversationHistory;
    private Queue<Value> receivedMessageQueue;

    public Consumer(Profile profile, Handler handler, List<Value> conversationHistory, Queue<Value> receivedMessageQueue, String topic){
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
        final Message inProgressMessage = new Message();
        if (this.socket != null) {
            topic = searchTopic(topic, conRequest);
            if (topic != null) {
                inProgressMessage.what = 201;
                this.handler.sendMessage(inProgressMessage);
                List<Value> data = getConversationData(topic); //getting conversation data at first

                for (int i = 0; i < data.size(); i++){ // for correct order
                    List<Value> chunkList = new ArrayList<>();
                    Value currentValue = data.get(i);
                    int totalChunks = 0;
                    if (currentValue.isFile()) { //if it is file we gather all chunks and sort them before adding them
                        totalChunks = currentValue.getRemainingChunks(); //to history
                        for (int j = i; j <= i + totalChunks; j++){ //nested for loop in order to keep the correct order
                            chunkList.add(data.get(j));
                        }
                        Value[] sortedChunks = sortChunks(chunkList);
                        for (Value value: sortedChunks){
                            synchronized (this) {
                                conversationHistory.add(value);
                            }
                        }
                    } else {
                        synchronized (this) { //if it is a message we add it to history
                            conversationHistory.add(currentValue);
                        }
                    }
                    i += totalChunks;
                }
                msg.what = 200;
                this.handler.sendMessage(msg);
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
                Value[] sortedFileChunks = sortChunks(chunkList);
                receivedMessageQueue.addAll(Arrays.asList(sortedFileChunks));
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


    private Value[] sortChunks(List<Value> chunkList){
        Value[] sortedChunks = new Value[chunkList.size()];
        for (Value chunk : chunkList){ //and sorting them according to the number on the chunk name
            int index = parseInt(chunk.getFilename().substring(chunk.getFilename().lastIndexOf("_") + 1, chunk.getFilename().indexOf(".")));
            sortedChunks[index] = chunk;
        }
       return sortedChunks;
    }

}