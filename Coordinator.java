import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;

public class Coordinator {

    static List<ParticipantList> participantList = new ArrayList<>();
    static List<Map.Entry<Long, String>> messageList = new ArrayList<>();

    
    public static void main(String[] args) {
        
        if(args.length < 1){
            System.out.println("ERROR!! Please enter config file.");
            return;
        }

        String configFile = args[0];
        String configContents[] = new String[2];
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                configContents[i] = line;
                i++;
            }
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
        int portNum = Integer.parseInt(configContents[0]);
        int threshold = Integer.parseInt(configContents[1]);

        try (ServerSocket serverSocket = new ServerSocket(portNum)){
            // ServerSocket TerminateSocket = new ServerSocket(terminatePort);
            System.out.println("Server is listening on port " + portNum);

            while (true) {
                Socket clientSoc = serverSocket.accept();
                System.out.println("Client connected: " + clientSoc.getInetAddress());

                // Create a new thread for each client
                ClientHandler clientHandler = new ClientHandler(clientSoc, participantList, messageList, threshold);
                clientHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;
    private List<ParticipantList> participantList = new ArrayList<>();
    private List<Map.Entry<Long, String>> messageList = new ArrayList<>(); 
    private int threshold;

    public ClientHandler(Socket clientSocket, List<ParticipantList> participantList, List<Map.Entry<Long, String>> messageList, int threshold) {
        this.clientSocket = clientSocket;
        this.participantList = participantList;
        this.messageList = messageList;
        this.threshold = threshold;
    }

    @Override
    public void run(){
        
        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                // PWD
                if ("register".equalsIgnoreCase(inputLine.split(" ")[0])) {
                    //message format = register <client port> <client id> <client ipaddress>
                    out.println(register(inputLine));                    
                }
                else if("deregister".equalsIgnoreCase(inputLine.split(" ")[0])){
                    out.println(deregister(inputLine));
                }
                else if("disconnect".equalsIgnoreCase(inputLine.split(" ")[0])){
                    out.println(disconnect(inputLine));
                }
                else if("reconnect".equalsIgnoreCase(inputLine.split(" ")[0])){
                    out.println(reconnect(inputLine));
                }
                else if("msend".equalsIgnoreCase(inputLine.split(" ")[0])){
                    out.println(msend(inputLine));
                }
                else{
                    out.println("enter a valid command");
                }    
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public String register(String inputLine){
        String partiPort = inputLine.split(" ")[1];
        String partiId = inputLine.split(" ")[2];
        String partiIPAddess = inputLine.split(" ")[3];
        
        Iterator<ParticipantList> itr = participantList.iterator();
        while (itr.hasNext()) {
            ParticipantList participant = itr.next();
            if (partiId.equals(participant.getpartiId())) {
                return "ERROR! Person with ID " + partiId + " already EXIST";
            }
        }
        
        participantList.add(new ParticipantList(partiId, partiIPAddess, partiPort, true, -1));

        System.out.println("Person with ID " + partiId + " REGISTERED.");
        return "SUCCESS!! Participant registered Successfully!";
    }

    public String deregister(String inputLine){

        String idToRemove = inputLine.split(" ")[1];
        Iterator<ParticipantList> itr = participantList.iterator();
        while (itr.hasNext()) {
            ParticipantList person = itr.next();
            if (idToRemove.equals(person.getpartiId())) {
                itr.remove();
                System.out.println("Person with ID " + idToRemove + " DEREGISTERED.");
                return "SUCCESS!! Deregister Successful";
            }
        }
        return "ERROR! You are not registered yet";
    }

    public String disconnect(String inputLine){

        String idToDisconnect = inputLine.split(" ")[1];
        Iterator<ParticipantList> itr = participantList.iterator();
        while (itr.hasNext()) {
            ParticipantList participant = itr.next();
            if (idToDisconnect.equals(participant.getpartiId())) {
                long currentTimeMillis = System.currentTimeMillis();
                participant.setOnlineStatus(false);
                participant.setDisconnectedNow(currentTimeMillis);
                System.out.println("Person with ID " + idToDisconnect + " DISCONNECTED.");
                return "SUCCESS!! Disconnect Successful";
            }
        }
        
        return "ERROR! You are not registered yet";
    }

    public String reconnect(String inputLine){
        
        String newPort = inputLine.split(" ")[1];
        String partiID = inputLine.split(" ")[2];
        
        Iterator<ParticipantList> itr = participantList.iterator();
        while (itr.hasNext()) {
            ParticipantList participant = itr.next();
            if (partiID.equals(participant.getpartiId())) {

                //update participant table
                participant.setOnlineStatus(true);
                participant.setPartiPort(newPort);
                participant.setDisconnectedNow(0);
                System.out.println("Person with ID " + partiID + " RECONNECTED.");

                int port = Integer.parseInt(participant.getPartiPort());
                String partiIP = participant.getIPAddress();
                
                try{
                    Thread.sleep(2000);
                    Socket socket = new Socket(partiIP, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    
                    long currentTimeMillis = System.currentTimeMillis();

                    long disconnectWindow = currentTimeMillis - participant.getDisconnectedNow();
                    
                    if(disconnectWindow > threshold){
                        for (Map.Entry<Long, String> item : messageList) {
                            long key = item.getKey();
                            String value = item.getValue();
                            if(key > currentTimeMillis-threshold){
                                out.println(value);
                            }
                        }
                    }
                    else{
                        for (Map.Entry<Long, String> item : messageList) {
                            String value = item.getValue();
                            out.println(value);
                        }
                    }
                    socket.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }

                return "SUCCESS!! Reconnect Successful";
            }
        }        
        return "ERROR! you are not registered yet";
    
    }
    public String msend(String inputLine){
        int len = inputLine.split(" ").length;
        String id = inputLine.split(" ")[len-1];

        Iterator<ParticipantList> itr = participantList.iterator();
        while (itr.hasNext()) {
            ParticipantList participant = itr.next();
            if (id.equals(participant.getpartiId()) && participant.getOnlineStatus()) {
                String message = "";
                String[] words = inputLine.split(" ");            
                if (words.length >= 2) {
                    String[] removedWords = new String[words.length - 2];
                    System.arraycopy(words, 1, removedWords, 0, words.length - 2);
                    message = String.join(" ", removedWords);
                }
                message = id + ": " + message;

                long currentTimeMillis = System.currentTimeMillis();
                messageList.add(new SimpleEntry<>(currentTimeMillis, message));

                MessageSender sender = new MessageSender(message, participantList);
                sender.start();
                
                return "SUCCESS!! Message sent successfully";
            }
        }   
        return "ERROR!! you are not registered yet";
    }
}

class ParticipantList {

    private String partiId;
    private String partiIPAddress;
    private String partiPort;
    private boolean onlineStatus;
    private long disconnectedNow;

    public ParticipantList(String id, String ip, String port, boolean status, long disconnectedNow){
        this.partiId = id;
        this.partiIPAddress = ip;
        this.partiPort = port;
        this.onlineStatus = status;
        this.disconnectedNow = disconnectedNow;
    }
    //getters
    public String getpartiId(){
        return partiId;
    }
    public String getIPAddress(){
        return partiIPAddress;
    }
    public String getPartiPort(){
        return partiPort;
    }
    public boolean getOnlineStatus(){
        return onlineStatus;
    }
    public long getDisconnectedNow(){
        return disconnectedNow;
    }
    //setters
    public void setOnlineStatus(boolean newStatus){
        this.onlineStatus = newStatus;
    }
    public void setPartiPort(String port){
        this.partiPort = port;
    }
    public void setDisconnectedNow(long disconnectedNow){
        this.disconnectedNow = disconnectedNow;
    }
}


class MessageSender extends Thread{
    String message;
    List<ParticipantList> participantList = new ArrayList<>();
    public MessageSender(String message, List<ParticipantList> participantList){
        this.message = message;
        this.participantList = participantList;
    }

    @Override
    public void run(){

        Iterator<ParticipantList> itr = participantList.iterator();
        while (itr.hasNext()) {
            ParticipantList participant = itr.next();
            System.out.println(participant.getpartiId());
            if (participant.getOnlineStatus()) {
                int port = Integer.parseInt(participant.getPartiPort());
                String partiIP = participant.getIPAddress();
                try{
                    Socket socket = new Socket(partiIP, port);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(message);
                    socket.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            }
        }   
        
    }
}