package my.project.dsproject;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import static java.lang.Integer.parseInt;

public class Broker implements Serializable {

    private final int id;
    private final InetAddress address;

    private static final HashMap<Integer,String> portsAndAddresses = new HashMap<>(); //ports and addresses
    private static final HashMap<Integer,Integer> availableBrokers =  new HashMap<>(); //ids, ports
    private static final HashMap<BigInteger,String> hashedTopics = new HashMap<>();//hash and topics
    private static final HashMap<String,Integer> topicsToBrokers = new HashMap<>(); //topic and broker ids
    private static final List<String> availableTopics = new ArrayList<>();

    private final ServerSocket serverSocket;

    public Broker(ServerSocket serverSocket, InetAddress address, int id) throws SocketException {
        this.serverSocket = serverSocket;
        this.serverSocket.setReuseAddress(true);
        this.address = address;
        this.id = id;
        readConfig(System.getProperty("user.dir").concat("\\src\\main\\java\\my\\project\\dsproject\\config.txt")); //reading ports ids and ips from config file
        hashTopics();
        assignTopicsToBrokers();
    }


    public void startBroker(){
        try {
            while (!serverSocket.isClosed()){
                Socket socket = serverSocket.accept();
                System.out.println("SYSTEM: A new component connected!");
                ClientHandler clientHandler = new ClientHandler(socket); //client handler as a different thread for both consumer and publisher to handle each connection
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e){
            closeServerSocket();
        }
    }


    public void closeServerSocket(){
        try {
            if (serverSocket != null){
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public String getBrokerAddress(){
        return this.address.toString();
    }

    public String getBrokerPort(){
        return Integer.toString(serverSocket.getLocalPort());
    }


    public int getBrokerID(){
        return this.id;
    }

    private void readConfig(String path){ //reading ports, hostnames and topics from config file
        File file = new File(path); //same method on both brokers and user node
        try {
            Scanner reader = new Scanner(file);
            reader.useDelimiter(","); // comma as delimiter
            String id, hostname, port;
            id = reader.next();
            while(reader.hasNext() && !id.equalsIgnoreCase("#")){ //when # is read we know that we reached topics section
                hostname = reader.next();
                port = reader.next();
                portsAndAddresses.put(parseInt(port),hostname);
                availableBrokers.put(parseInt(id),parseInt(port));
                id = reader.next();
            }
            while(reader.hasNext()){
                String topic = reader.next();
                availableTopics.add(topic);
            }
        } catch (FileNotFoundException e){
            System.out.println(e.getMessage());
        }
    }

    private static void hashTopics(){ //hashing topics with SHA-1 and getting their decimal value
        for (String topic : availableTopics){
            BigInteger hash = new BigInteger(encryptThisString(topic),16);
            hashedTopics.put(hash, topic);
        }
    }

    private static void assignTopicsToBrokers(){ //hash(topic) mod N to assign topics to brokers
        for (Map.Entry<BigInteger, String> entry : hashedTopics.entrySet()){
            topicsToBrokers.put(entry.getValue(), entry.getKey().mod(BigInteger.valueOf(3)).intValue());
        }
        System.out.println("Topics to Brokers: " + topicsToBrokers);
    }

    public static String encryptThisString(String input){ //SHA-1 encryption method
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            return hashText.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAddress(int correctPort) {
        System.out.println(portsAndAddresses.get(correctPort));
        return portsAndAddresses.get(correctPort);
    }

    public static int searchBrokerPort(Value value){ //searching for the correct broker given the topic
        int port = 0;
        int id = -1;
        for (Map.Entry<String, Integer> entry : topicsToBrokers.entrySet()){
            if (entry.getKey().equalsIgnoreCase(value.getTopic())){
                id = entry.getValue(); //getting the id
            }
        }
        for (Map.Entry<Integer, Integer> entry : availableBrokers.entrySet()){
            if (entry.getKey().equals(id)){
                port = entry.getValue(); //finding the correct broker based on the id retrieved above
            }
        }
        return port;
    }

    public static void main(String[] args) throws IOException {

        int socket, id;
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        System.out.println("Give socket port: ");
        socket = Integer.parseInt(reader.readLine());
        System.out.println("Give broker ID: ");
        id = Integer.parseInt(reader.readLine());

        ServerSocket serverSocket = new ServerSocket(socket); //port numbers 3000/4000/5000
        Broker broker = new Broker(serverSocket, InetAddress.getByName("192.168.100.3"), id); //with IDs 0/1/2 respectively
        System.out.println("SYSTEM: Broker_" + broker.getBrokerID()+" initialized at: "
                + serverSocket + "with address: " +  broker.getBrokerAddress());
        broker.startBroker();
    }
}
