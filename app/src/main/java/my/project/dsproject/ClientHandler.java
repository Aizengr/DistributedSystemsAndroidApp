package my.project.dsproject;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientHandler implements Runnable,Serializable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>(); //all connections
    public static ArrayList<ClientHandler> connectedPublishers = new ArrayList<>(); //connected publishers
    public static ArrayList<ClientHandler> connectedConsumers = new ArrayList<>(); //connected consumers

    public static Multimap<Profile,String> knownPublishers = ArrayListMultimap.create(); //known publishers for each topic
    public static Multimap<Profile,String> registeredConsumers = ArrayListMultimap.create(); //known consumers for each topic
    public static Multimap<String,Value> messagesMap = LinkedListMultimap.create(); //conversation history

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;


    public ClientHandler(Socket socket){
        try {
            this.socket = socket;
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            clientHandlers.add(this); //keeping all connections
            Value initMessage = (Value)in.readObject(); //reading the initial connection message on constructor checking component ID as well as username
            if (initMessage.getRequestType().equalsIgnoreCase("Publisher")) {
                connectedPublishers.add(this); //keeping only alive publishers
            }
            else if (initMessage.getRequestType().equalsIgnoreCase("Consumer")){
                connectedConsumers.add(this); //keeping only alive consumers
            }
            this.username = initMessage.getUsername();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            closeEverything(socket, out, in);
        }
    }
    public void run() {
        Object streamObject = readStream();
        Value currentMessage = (Value)streamObject;
        int correctPort = -1;
        String correctAddress = null;
        if(currentMessage!= null){
            if(currentMessage.getMessage().equalsIgnoreCase("portCheck")){
                correctPort = Broker.searchBrokerPort(currentMessage);
                correctAddress = Broker.getAddress(correctPort);
                System.out.println("FROM PORT CHECK: " + streamObject);
                System.out.println("CORRECT ADDRESS AND PORT FOUND - - - " + correctAddress + " - - - " + correctPort);
                sendCorrectBrokerPort(correctPort); //sending correct Broker port
                sendCorrectBrokerAddress(correctAddress); //sending correct Broker address
            }
        }
        if (correctPort == this.socket.getLocalPort() && Objects.equals(correctAddress, Broker.getAddress(this.socket.getLocalPort()))) { //if we are on the correct broker
            while (!socket.isClosed()) {
                Value value = (Value)readStream();
                System.out.println(value);
                if (value != null) {
                    if (value.getRequestType().equalsIgnoreCase("Publisher")) {
                        checkPublisher(value.getProfile(), value.getTopic());
                        if (!value.isFile()) {
                            addToHistory(value); //adding to history
                            broadcastMessage(value.getTopic(), value);//live message broadcasting to all connected consumers
                        } else {
                            List<Value> chunkList = new ArrayList<>();
                            while (value.getRemainingChunks() >= 0) { //while having remaining chunks for a file, read them
                                try {
                                    addToHistory(value); //adding all chunks to history
                                    chunkList.add(value);
                                    if (value.getRemainingChunks() == 0) {break;}
                                    value = (Value) in.readObject();
                                    System.out.println(value.getTopic());
                                    System.out.println(value);
                                } catch (IOException | ClassNotFoundException e) {
                                    System.out.println(e.getMessage());
                                }
                            }
                            broadcastFile(value.getTopic(), chunkList); // live file sharing to all connected consumers
                        }
                    } else if (value.getRequestType().equalsIgnoreCase("Consumer") && value.getMessage().equalsIgnoreCase("datareq")) { //initial case
                        checkConsumer(value.getProfile(), value.getTopic());
                        pull(value.getTopic()); //pulling history for the connected consumer with the datareq request
                    }
                }
            }
        } else {
            checkRemoveConsumer(correctPort); //check and remove consumer from alive connections
            checkRemovePublisher(correctPort); //in case of redirecting to another broker
        }
    }

    private void checkRemoveConsumer(int port){ //removes connected consumer in case of redirection to another broker
        if (connectedConsumers.contains(this)){
            System.out.println("SYSTEM: Redirecting consumer of: " + this.getUsername()
                    + " to Broker on port: " + port);
            connectedConsumers.remove(this);
        }
    }

    private void checkRemovePublisher(int port){ //removes connected publisher in case of redirection to another broker
        if (connectedPublishers.contains(this)){
            System.out.println("SYSTEM: Redirecting publisher of: " + this.getUsername()
                    + " to Broker on port: " + port);
            connectedConsumers.remove(this);
        }
    }


    private void sendCorrectBrokerPort(int port){
        try {
            out.writeObject(port); //sending correct broker port to UserNode
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendCorrectBrokerAddress(String address){
        try {
            out.writeObject(address); //sending correct broker port to UserNode
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastFile(String topic, List<Value> chunkList){ //broadcasting all chunks on the chunk list passed to all users connected
        for (ClientHandler consumer : connectedConsumers) { //except the one sending them
            if (!consumer.getUsername().equalsIgnoreCase(this.username)) {
                System.out.println("File sharing to topic: " + topic.toUpperCase() +
                        " from: " + this.username + " to: " + consumer.getUsername());
                try {
                    for (Value value : chunkList) {
                        value.setRequestType("liveFile");
                        consumer.out.writeObject(value);
                        consumer.out.flush();
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private void broadcastMessage(String topic, Value value){ //same as above for messages
        value.setRequestType("liveMessage");
        for (ClientHandler consumer : connectedConsumers){
            if (!consumer.getUsername().equalsIgnoreCase(this.username)){
                System.out.println("Broadcasting to topic: " + topic.toUpperCase() +
                        "for: " + consumer.username + " and value: " + value +
                        " from: " + value.getUsername());
                try {
                    consumer.out.writeObject(value);
                    consumer.out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void addToHistory(Value value) {
        messagesMap.put(value.getTopic(), value);
    }


    private synchronized void pull(String topic){ //main pull function
        int count = checkValueCount(topic); //retrieving messages and files from history data structure and sending them
        try {
            out.writeObject(count);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int messageCounter = 0;
        for (Map.Entry<String,Value> entry : messagesMap.entries()){
            if (entry.getKey().equalsIgnoreCase(topic)){
                try {
                    entry.getValue().setMessageNumber(messageCounter);
                    System.out.println("SYSTEM: Pulling: "  + entry.getValue());
                    out.writeObject(entry.getValue());
                    messageCounter++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int checkValueCount(String topic){ //checks how many messages we have for the specific topic
        int count = 0;
        for (Map.Entry<String,Value> entry : messagesMap.entries()){
            if (entry.getKey().equalsIgnoreCase(topic)){
                count++;
            }
        }
        return count;
    }

    public void checkConsumer(Profile profile, String topic){ //checks if consumer is known for the topic and adds them
        if (!(registeredConsumers.containsEntry(profile,topic))){
            System.out.println("SYSTEM: New consumer registered to topic: " + topic
                    + " with username: " + profile.getUsername());
            registeredConsumers.put(profile, topic);
        }
    }
    public void checkPublisher(Profile profile, String topic){ //checks if publisher is known for the topic and adds them
        if (!(knownPublishers.containsEntry(profile, topic))){
            System.out.println("SYSTEM: New publisher added to known Publishers for topic: " + topic
                    + " with username: " +profile.getUsername());
            knownPublishers.put(profile, topic);
        }
    }

    public Object readStream(){ //main reading object method
        try {
            return in.readObject();
        } catch (ClassNotFoundException | IOException e ){
            closeEverything(socket, out, in);
            //e.printStackTrace();
        }
        return null;
    }

    public String getUsername(){
        return this.username;
    }

    public void removeClientHandler(){ //disconnects client (both consumer and publisher)
        clientHandlers.remove(this);
        connectedPublishers.remove(this);
        connectedConsumers.remove(this);
        System.out.println("SYSTEM: A component has disconnected!");
    }

    public void closeEverything(Socket socket, ObjectOutputStream out, ObjectInputStream in){
        removeClientHandler(); //removes client and closes everything
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
