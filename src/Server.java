/* 
									Server Class 
 * */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.joda.time.LocalDateTime;

public class Server {
	JFrame serverFrame = new JFrame("Server");
	static JTextArea serverMessageArea = new JTextArea(50, 50);

	private static final int PORT = 9000; // port number on which the server listens to the client

	private static ArrayList<String> names = new ArrayList<String>(); // HashSet to store the names of connected clients

	private static HashSet<PrintWriter> clients = new HashSet<PrintWriter>(); // HashSet to store the clients
																				// connections

	// Constructor to display server GUI
	public Server() {
		serverMessageArea.setEditable(true);
		serverFrame.getContentPane().add(new JScrollPane(serverMessageArea), "Center");
		serverFrame.pack();
		
		// Restore contents of the previous sessions
		try {
		File file = new File("ServerDB.txt"); 
		  BufferedReader br = new BufferedReader(new FileReader(file)); 
		  String st;
		  while ((st = br.readLine()) != null) {
		    System.out.println(st);
		    serverMessageArea.append(st+"\n");
		  }
		}
		catch(Exception exp) {
			System.out.println(exp);
		}
		serverMessageArea.append("\n"+LocalDateTime.now()+"\nChat Server is running\n");
		writeFile(LocalDateTime.now()+"\nChat Server is running\n");
	}
	
	// Method to write contents of the server to the file
	public static void writeFile(String msg){
		try { 
        File f1 = new File("/Users/snehithraj/eclipse-workspace/ThreePhaseCommit/src/ServerDB.txt");
        if(!f1.exists()) {
           f1.createNewFile();
        } 
        FileWriter fileWriter = new FileWriter(f1.getName(),true);
        BufferedWriter bw = new BufferedWriter(fileWriter);
        bw.write(msg);
        bw.close();
     } catch(IOException e){
        e.printStackTrace();
     }
  }

	// Main method to initialize the server
	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.serverFrame.setVisible(true);
		ServerSocket listener = new ServerSocket(PORT);
		try {
			while (true) {
				new Client(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	// Client class to handle connections
	private static class Client extends Thread {
		private String name;
		private Socket socket;
		private BufferedReader in;
		private PrintWriter out;

		// Function constructor to set the connection of the client trying to connect
		public Client(Socket socket) {
			this.socket = socket;
		}

		// Method to set the name of the client
		public void run() {
            try {
                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {
                        if (!names.contains(name)) {
                            names.add(name);
                            break;
                        }
                        else {
                        	out.println("duplicate");
                        }
                    }
                }
                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED,"+name);
                System.out.println(out);
                for(PrintWriter writer : clients) {
                	writer.println("\n"+name + " is connected\n");
                	System.out.println("broadcast");
                }
                clients.add(out);
                System.out.println("client "+name+" is connected");  
                serverMessageArea.append(name + " is connected\n");
                writeFile( name + " is connected\n");
               
                //String url = "http://localhost:9000";
                // Accept messages from this client and broadcast them.
                while (true) {
                    String s = in.readLine();
                    writeFile("Content:"+s+"\n");
                    serverMessageArea.append("Content:"+s+"\n");
                    System.out.println(s);
                    String input=s.split(",")[0];
                    System.out.println(input);
                    if (input == null) {
                        return;
                    }
                    for (PrintWriter writer : clients) {          		
                        writer.println("MESSAGE," + name + "," + input);
                    }
                    writeFile("From: "+name + ":" + input+"\n");
                    serverMessageArea.append( "From: "+name + ":" + input+"\n");
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    names.remove(name);    
                    System.out.println("nr");
                }
                if (out != null) {
                    clients.remove(out);  
                    for(PrintWriter writer : clients) {
                    	writer.println(name + " is disconnected\n");
                    	System.out.println("broadcast");
                    }
                	serverMessageArea.append(name + " is disconnected\n");
                	writeFile("\n"+name + " is disconnected\n");
                    System.out.println("cr");
                    
                }
                try {
                    socket.close();
                    writeFile( " Server is disconnected\n");
                } catch (IOException e) {
                System.out.println(e);
                }
            }
        }
    }
}