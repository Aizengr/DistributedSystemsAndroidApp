package my.project.dsproject;

import java.io.*;
import java.net.Socket;
import java.util.*;
import static java.lang.Integer.parseInt;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class UserNode implements Serializable {

    protected Socket socket;
    protected Profile profile;
    protected Handler handler;
    protected int currentPort;
    protected String currentAddress;
    protected final String pubRequest = "Publisher";
    protected final String conRequest = "Consumer";

    protected ObjectOutputStream objectOutputStream;
    protected ObjectInputStream objectInputStream;
    protected Scanner inputScanner;

    protected static final int[] portNumbers = new int[]{3000}; //for testing 1 broker only please keep 1 port and run the broker on the same


    protected static HashMap<Integer, String> portsAndAddresses = new HashMap<>(); //ports and addresses
    protected static HashMap<Integer, Integer> availableBrokers = new HashMap<>(); //ids, ports
    protected static List<String> availableTopics = new ArrayList<>();

    protected static ArrayList<Publisher> alivePublisherConnections; //keeping alive publisher connections
    protected static ArrayList<Consumer> aliveConsumerConnections; //keeping alive consumer connections


    public UserNode(Profile profileName, Handler handler) {
        this(getRandomSocketPort(), profileName, handler);
    }

    public UserNode(int port, Profile profile, Handler handler) { //user node initialization
        this.currentPort = port;
        this.currentAddress = portsAndAddresses.get(port);
        this.profile = profile;
        this.handler = handler;
        alivePublisherConnections = new ArrayList<>();
        aliveConsumerConnections = new ArrayList<>();
    }

    private static int getRandomSocketPort() { //generates a random port for initial communication with a random Broker

        return portNumbers[new Random().nextInt(portNumbers.length)];

    }

    protected void connect(int port, String address, String type) { //initial connection method, initializes socket, streams and scanner as well as passes an
        try { //initial connection message to the broker when connecting (this is also used when switching connections between brokers)
            this.socket = new Socket(address, port);
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());
            this.inputScanner = new Scanner(System.in);
            try { //initial connection request for both publisher and consumer
                Value initMessage = new Value("Connection", this.profile, type);
                objectOutputStream.writeObject(initMessage);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Attempting connection: \n" + port + "\n" + address + "\n");
        } catch (IOException e) {
            if (e.getMessage().equalsIgnoreCase("Connection refused: connect")) {
                System.out.println(type + " connection failed. Broker at port: "
                        + port + " is currently unavailable.");
                disconnectAll();
            }
        }
    }

    protected String searchTopic(String topic, String requestType) { //initial search
        while(true) {
            final Message msg = new Message();
            final Message progress = new Message();
            progress.what = 201;
            this.handler.sendMessage(progress);
            int portResponse = checkBrokerPort(topic); //asking and receiving port number for correct Broker based on the topic
            String addressResponse = checkBrokerAddress();
            if (portResponse == 0 || addressResponse == null) {
                msg.what = -100;
                System.out.println("SYSTEM: There is no existing topic named: " + topic +". Here are available ones: " + availableTopics);
                this.handler.sendMessage(msg);
                break;
            } else if (portResponse != socket.getPort() || !addressResponse.equalsIgnoreCase(this.socket.getInetAddress().toString().substring(1))) { //if we are not connected to the right one, switch conn
                System.out.println("SYSTEM: Switching Publisher connection to another broker on port: " + portResponse + " and hostname: " + addressResponse);
                connect(portResponse, addressResponse, requestType);
            } else {
                msg.what = 100;
                this.handler.sendMessage(msg);
                break;
            }
        }
        return topic;
    }

    protected int checkBrokerPort(String topic){ //checking if we are on the correct broker
        int response = 0;
        try {
            Value portCheck = new Value("portCheck", this.profile, topic, pubRequest);
            objectOutputStream.writeObject(portCheck);
            objectOutputStream.flush();
            response = (int)objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            disconnect();
        }
        return response;
    }

    private String checkBrokerAddress(){ //checking if we are on the correct broker
        String response = null;
        try {
            response = (String)objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            disconnect();
        }
        return response;
    }

    protected void disconnect() { //this disconnects a single component (consumer/publisher) but not both
        try {
            if (this.objectInputStream != null) {
                this.objectInputStream.close();
            }
            if (this.objectOutputStream != null) {
                this.objectOutputStream.close();
            }
            if (this.socket != null) {
                this.socket.close();
            }
            if (this.inputScanner != null) {
                this.inputScanner.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    protected void disconnectComponents(int port) { //this disconnects both as consumer and as publisher from a broker
        for (Consumer consumer : aliveConsumerConnections) {
            if (consumer.currentPort == port) {
                try {
                    if (consumer.objectInputStream != null) {
                        consumer.objectInputStream.close();
                    }
                    if (consumer.objectOutputStream != null) {
                        consumer.objectOutputStream.close();
                    }
                    if (consumer.socket != null) {
                        consumer.socket.close();
                    }
                    if (consumer.inputScanner != null) {
                        consumer.inputScanner.close();
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        for (Publisher publisher : alivePublisherConnections) {
            if (publisher.currentPort == port) {
                try {
                    if (publisher.objectInputStream != null) {
                        publisher.objectInputStream.close();
                    }
                    if (publisher.objectOutputStream != null) {
                        publisher.objectOutputStream.close();
                    }
                    if (publisher.socket != null) {
                        publisher.socket.close();
                    }
                    if (publisher.inputScanner != null) {
                        publisher.inputScanner.close();
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    protected void disconnectPublishers() { //this disconnects userNode from all brokers as publisher
        for (Publisher pub : alivePublisherConnections) {
            try {
                if (pub.objectInputStream != null) {
                    pub.objectInputStream.close();
                }
                if (pub.objectOutputStream != null) {
                    pub.objectOutputStream.close();
                }
                if (pub.socket != null) {
                    pub.socket.close();
                }
                if (pub.inputScanner != null) {
                    pub.inputScanner.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    protected void disconnectConsumers() { //this disconnects userNode from all brokers as consumer
        for (Consumer con : aliveConsumerConnections) {
            try {
                if (con.objectInputStream != null) {
                    con.objectInputStream.close();
                }
                if (con.objectOutputStream != null) {
                    con.objectOutputStream.close();
                }
                if (con.socket != null) {
                    con.socket.close();
                }
                if (con.inputScanner != null) {
                    con.inputScanner.close();
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    protected void disconnectAll() { //this disconnects everything
        disconnectPublishers();
        disconnectConsumers();
    }

    static void setConfig() { //setting ports, hostnames and topics

        portsAndAddresses.put(3000, "192.168.100.3");
        portsAndAddresses.put(4000, "192.168.100.3");
        portsAndAddresses.put(5000, "192.168.100.3");

        availableBrokers.put(0,3000);
        availableBrokers.put(1,4000);
        availableBrokers.put(2,5000);

        availableTopics.add("DS1");
        availableTopics.add("DS2");
        availableTopics.add("DS3");
    }
}


