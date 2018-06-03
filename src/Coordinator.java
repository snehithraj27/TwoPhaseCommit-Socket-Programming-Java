
/* 								
									Coordinator Class
 * */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;


// Client class to initialize the GUI
public class Coordinator {
	BufferedReader in;
	PrintWriter out;
	JFrame frame = new JFrame("Coordinator");
	JTextField textField = new JTextField(40);
	JTextArea messageArea = new JTextArea(15, 50);

	ArrayList<String> participant = new ArrayList<String>(); // ArrayList to store the participants

	public Coordinator() { // Constructor to initialize the Client UI
		textField.setEditable(false);
		messageArea.setEditable(false);
		frame.getContentPane().add(textField, "North");
		frame.getContentPane().add(new JScrollPane(messageArea), "Center");
		frame.pack();
	}

	// Method for dialog box to enter the IP address of the Server. Enter
	// "127.0.0.1" as the IP address
	private String getServerAddress() {
		return JOptionPane.showInputDialog(frame, "Enter IP Address of the Server:", "Welcome to the ChatRoomSystem",
				JOptionPane.QUESTION_MESSAGE);
	}

	// Method for dialog box to register the name of the Coordinator
	private String getName() {
		return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
				JOptionPane.PLAIN_MESSAGE);
	}

	int t=0; //Variable t is declared to keep track of the message
	int m;// Variable m is declared to keep track of the message
	Timer timer = null;
	// Method to connect to server and start processing messages between
	// participant, coordinator and
	// server
	private void run() throws IOException, ParseException {
		String serverAddress = getServerAddress(); // getting the IP address from getServerAddress method
		Socket socket = new Socket(serverAddress, 9000); // Initialize a new socket connection
		in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // instance of Buffer Reader to accept
																					// messeges from the server
		out = new PrintWriter(socket.getOutputStream(), true); // instance of PrintWriter to send messages to the server
		DateTimeFormatter dtfd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String tDate = dtfd.format(LocalDateTime.now());
		System.out.println(tDate);
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { // get text from the text field to send to the participants
				out.println(textField.getText() + ", POST/localhost/server http/1.1,Host:" + serverAddress+ ",User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:"
						+ Integer.toString(textField.getText().length()) + ",Date:" + tDate);
				textField.setText("");
				t = 0;
				m=1;
			}
		});
		String sName = null;
		int commit = 0;
		int timeInterval = 100;
		String state = null;
		// Process all messages from server, according to the protocol.
		try {
			while (true) {
				String line = in.readLine();
				System.out.println(line);
				if (line.startsWith("SUBMITNAME")) {
					out.println(getName()); // Send the desired screen name to the server for acceptance
				}
				// block of code is executed if name is accepted by the server
				else if (line.startsWith("NAMEACCEPTED")) {
					textField.setEditable(true);
					sName = line.split(",")[1];
					messageArea.append("You are " + sName + "\nStart sending messages\n");
					state = "INIT";
					messageArea.append("State: " + state + ".\n");
				} else if (line.startsWith("duplicate")) {
					JOptionPane.showMessageDialog(null, "Name already exist.Please choose another name");
				} else if (line.contains("disconnected")) {
					messageArea.append(line + "\n"); // notifying if any of the clients is disconnected
				} else if (line.contains("connected")) {
					messageArea.append(line + "\n"); // notifying if any of the clients is disconnected
					String[] msg = line.split(" ");
					participant.add(msg[0]);
				}
				// block of code is executed if message is received from participant
				else if (line.startsWith("MESSAGE")) {
					System.out.println(line);
					String[] msg = line.split(",");
					if (msg[1].contains("Coordinator") && t == 0) {
						messageArea.append("\nMessage :" + msg[2]);
					}
					if (line.contains(sName) && t == 0 && m==1) { // Start the timer
						System.out.println("Inside Timer");
						timer = new Timer();
						messageArea.append("\nTimer has started for : " + timeInterval + " seconds.\n");
						state = "WAIT";	// WAIT state when the coordinator sends the message and waits for the votes from the participants. 
						messageArea.append("State: " + state + ".\n");
						timer.schedule(new RemindTask(), timeInterval * 1000);
					}
					// block of code is executed if participant votes either commit or abort
					else if (line.contains("Vote") && t == 0) {
						// block of code is executed if participant votes abort
						if (line.contains("Abort")) {
							messageArea.append("\n" + msg[1] + " wants to abort\n");
							messageArea.append("\nGlobal Abort Initiated\n");
							state = "ABORT";
							messageArea.append("State: " + state + ".\n");
							messageArea.append("\nState :INIT\n");
							t=1;
							//Initiating global Abort
							out.println("gAbort, POST/localhost/server http/1.1,Host:" + serverAddress+ ",User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:10,Date:" + tDate); 
							if(m==1) {
							timer.cancel();
							}
							m=0;
							t=1;
							// block of code is executed if participant votes commit
						} else if (line.contains("Commit")) {
							messageArea.append("\n" + msg[1] + " wants to commit\n");						
								commit++;
								messageArea.append("No of Commits: " + Integer.toString(commit) + "\n");
							if (commit == participant.size()||commit==3) {
								// global commit initiated if all the participants voted commit																	
								out.println("gCommit, POST/localhost/server http/1.1,Host:" + serverAddress+",User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:10,Date:" + tDate); 						
								messageArea.append("\nGlobal Commit initiated\n");
								state = "COMMIT";
								messageArea.append("\nState: " + state + ".\n");
								commit = 0;
								t = 1;
								timer.cancel();
								messageArea.append("\nState :INIT\n");
							}
						}
					}
				}
			}
		} catch (Exception e1) {
			socket.close();
			System.out.println(e1);
		}
	}

	DateTimeFormatter dtfd1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	String tDate1 = dtfd1.format(LocalDateTime.now());

	//class is called when the timer runs out and all the participants have not responded.
	// Initiate Global abort and terminate the timer.
	class RemindTask extends TimerTask {
		public void run() {
			t = 1;
			System.out.println("Time's up!");
			messageArea.append("\nTime Out.\n");
			messageArea.append("State: ABORT.\n");
			messageArea.append("Initiating Global Abort\n");
			out.println("gAbort, POST/localhost/server http/1.1,Host:127.0.0.1,User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:10,Date:"+ tDate1);
			timer.cancel(); 
		}
	}

	public static void main(String[] args) throws Exception {
		Coordinator client = new Coordinator();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}