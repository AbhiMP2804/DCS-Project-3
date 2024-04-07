import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Participant {
    public static boolean running;

    public static void main(String[] args) {
        if(args.length < 1 ){
            System.out.println("ERROR: Please give config file in command line");
            return;
        }
        String configFile = args[0];
        String configContents[] = new String[3];
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

        int paricipantId = Integer.parseInt(configContents[0]);
        String hostName = configContents[2].split(" ")[0];
        int serverPort = Integer.parseInt(configContents[2].split(" ")[1]);

        try{
            Socket socket = new Socket(hostName, serverPort);
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("\nConnected to the server.");
            System.out.println("Type 'exit' to close the connection.\n");

            while(true){
                System.out.print("Participant> ");
                String message = userInput.readLine();


                if ("exit".equalsIgnoreCase(message)) {
                    System.out.println("Closing the connection.");
                    socket.close();
                    break;
                }
                else if("register".equalsIgnoreCase(message.split(" ")[0])){
                    running = true;
                    MessageHandler listener = new MessageHandler(message.split(" ")[1], configContents[1]);
                    listener.start();

                    InetAddress localHost = InetAddress.getLocalHost();
                    message = message + " " + paricipantId + " " + localHost.getHostAddress();
                }
                else if("deregister".equalsIgnoreCase(message)){
                    running = false;
                    message = message + " " + paricipantId;
                }
                else if("disconnect".equalsIgnoreCase(message)){
                    running = false;
                    message = message + " " + paricipantId;
                }
                else if("reconnect".equalsIgnoreCase(message.split(" ")[0])){
                    if(message.split(" ").length < 2){
                        System.out.println("Error!! Port number not specified");
                        continue;
                    }
                    running = true;
                    MessageHandler listener = new MessageHandler(message.split(" ")[1], configContents[1]);
                    listener.start();

                    message = message + " " + paricipantId;
                }
                else if("msend".equalsIgnoreCase(message.split(" ")[0])){
                    message = message + " " + paricipantId;
                }
                else{
                    System.out.println("ERROR!! ENTER A VALID COMMAND");
                }
                //send command to coordinator
                out.println(message);

                //print message received from coordinator
                String response = in.readLine();
                System.out.println("Participant> " + response);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

class MessageHandler extends Thread{
   
    String port;
    String fileName;
    public MessageHandler(String port, String fileName) {
        this.port = port;
        this.fileName = fileName;
    }

    @Override
    public void run(){
        try {
            // Create a socket to listen for messages
            File file = new File(fileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    System.out.println("File created: " + file.getName());
                } catch (IOException e) {
                    System.out.println("An error occurred while creating the file.");
                    e.printStackTrace();
                }
            }
            ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port));
            
            while (Participant.running) {
                
                // Accept incoming connections
                Socket clientSocket = serverSocket.accept();

                // Create a BufferedReader to read messages from the client
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Read messages sent by the server
                String message;
                while ((message = reader.readLine()) != null) {

                     // Append message to file
                    try (FileWriter writer = new FileWriter(fileName, true)) {
                        writer.write(message + "\n");
                        // System.out.println("Message appended to file: " + message);
                    } catch (IOException e) {
                        System.out.println("An error occurred while appending the message to the file.");
                        e.printStackTrace();
                    }
                }

                // Close the reader and socket
                reader.close();
                clientSocket.close();
            }
            System.out.println("REACHED HERE: NOW CLOSING THREAD");
            
            // Close the server socket when the thread stops
            serverSocket.close();
            System.out.println("THREAD CLOSED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}