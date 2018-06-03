
/* 									
									Client Class
 * */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;


// Client class to initialize the GUI
public class Participant {
	BufferedReader in;
	PrintWriter out;
	JFrame frame = new JFrame("Participant");
	JTextField textField = new JTextField(40);
	JTextArea messageArea = new JTextArea(40, 30);
	JButton abortButton = new JButton("Abort");
	JButton commitButton = new JButton("Commit");

	ArrayList<String> participant = new ArrayList<String>(); //ArrayList to store the participants

	public Participant() { // Constructor to initialize the Participant
		messageArea.setEditable(false);
		abortButton.setEnabled(false);
		commitButton.setEnabled(false);
		//textField.setEditable(false);
		frame.getContentPane().add(textField, "North");
		frame.getContentPane().add(abortButton, "West");
		frame.getContentPane().add(commitButton, "East");
		frame.getContentPane().add(new JScrollPane(messageArea), "Center");
		frame.pack();
	}

	// Method for dialog box to enter the IP address of the Server. Enter
	// "127.0.0.1" as the IP address
	private String getServerAddress() {
		return JOptionPane.showInputDialog(frame, "Enter IP Address of the Server:", "Welcome to the ChatRoomSystem",
				JOptionPane.QUESTION_MESSAGE);
	}

	// Method for dialog box to register the name of the Client
	private String getName() {
		return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
				JOptionPane.PLAIN_MESSAGE);
	}
	int t; // t is used to keep track of the timer
	int m; // m is used to handle message
	Timer timer = null; // initialize timer variable
	
	// Method to connect to server and start processing messages between client and server
	private void run() throws IOException, ParseException {
		String serverAddress = getServerAddress(); // getting the IP address from getServerAddress method
		Socket socket = new Socket(serverAddress, 9000); // Initialize a new socket connection
		in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // instance of Buffer Reader to accept
																					// messeges from the server
		out = new PrintWriter(socket.getOutputStream(), true); // instance of PrintWriter to send messages to the server
		DateTimeFormatter dtfd = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		String tDate = dtfd.format(LocalDateTime.now());
		System.out.println(tDate);
		// String text=null;
		
		// send message to other participants using text field.
		textField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { // get text from the text area
				out.println(textField.getText() + ",POST /localhost/server http/1.1,Host:" + serverAddress+ ",User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:"
						+ Integer.toString(textField.getText().length()) + ",Date:" + tDate);
				textField.setText("");
				m=1;
				t=1;
			}
		});
		
		// send vote abort 
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e1) {
				out.println("VoteAbort,POST /localhost/server http/1.1,Host:" + serverAddress+ ",User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:9,Date:" + tDate);
				messageArea.append("\nYou voted as : Abort\n");
				abortButton.setEnabled(false);
				commitButton.setEnabled(false);
				t = 0;
				m=1;
			}
		});
		
		//send vote commit
		commitButton.addActionListener(new ActionListener() {
			// Timer timer;
			public void actionPerformed(ActionEvent e2) {
				out.println("VoteCommit,POST /localhost/server http/1.1,Host:" + serverAddress+ ",User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:10,Date:" + tDate);
				messageArea.append("\nYou voted as : Commit\n");		
				abortButton.setEnabled(false);
				commitButton.setEnabled(false);
				t = 0;
				m=1;			
			}
			});
		
		int timeInterval = 60; //setting time interval
		String sName = null;
		String text = null;
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
					// textField.setEditable(true);
					sName = line.split(",")[1];
					messageArea.append("You are " + sName + "\n");
					
					// Load previous committed messages
					try {
						File file = new File(sName);
						BufferedReader br = new BufferedReader(new FileReader(file));
						String st;
						messageArea.append("\nPrevious commited messages\n");
						while ((st = br.readLine()) != null) {
							System.out.println(st);
							messageArea.append(st + "\n");
						}
					} catch (Exception exp) {
						System.out.println(exp);
					}
					
					timer = new Timer();
					state = "INIT";
					messageArea.append("\nState: " + state + ".\n"); // Init state till the vote request is request from the coordinator.													
					messageArea.append("\nTimer has started for: " + timeInterval+ " seconds.\nWaiting for Vote request from the coordinator.\n");
					timer.schedule(new RemindTask1(), timeInterval * 1000); // schedule ReminderTask1 for the specified time interval before message is received from the coordinator
					
				} else if (line.startsWith("duplicate")) {
					JOptionPane.showMessageDialog(null, "Name already exist.Please choose another name");
				} else if (line.contains("disconnected")) {
					messageArea.append(line + "\n"); // notifying if any of the participants is disconnected
				} else if (line.contains("connected")) {
					messageArea.append(line + "\n");// notifying if any of the participants is disconnected
					
				}
				// block of code is executed if message is received from Coordinator or the Participant
				else if (line.startsWith("MESSAGE")) {
					String[] msg = line.split(",");
					if (sName.contains(msg[1]) && (t == 0)) {
						System.out.println("Inside Timer");
						timer = new Timer();
						messageArea.append("\nTimer has started for " + timeInterval+ " seconds.\nWaiting for the response from the Coordinator.\n");
						timer.schedule(new RemindTask(), timeInterval * 1000); // schedule ReminderTask for the specified time interval after message is received from the coordinator
					} else if (line.contains("gAbort")) { // Aborting the message if Global abort is received from the Coordinator
						
						messageArea.append("\nGlobal abort Initiated. Abort the message\n");
						state = "ABORT"; 
						messageArea.append("State: " + state + ".\n");
						messageArea.append("\nState :INIT\n");//ABORT state when coordinator sends Global Abort.
						timer.cancel();
						abortButton.setEnabled(false);
						commitButton.setEnabled(false);
						if(m==1) {
							timer.cancel();
						}
						m=0;
					} else if (line.contains("gCommit")) { // Committing the message if Global commit is received from the Coordinator
						messageArea.append("\nGlobal commit Initiated. Commit the message\n");
						state = "COMMIT";
						messageArea.append("State: " + state + ".\n");
						messageArea.append("\nState :INIT\n");
						timer.cancel();
						writeFile(text, sName); // Calling the write File method to write the string to the file.
					} else {
						if (msg[1].contains("Coordinator")) { //block of code is executed if coordinator sends a message
							timer.cancel();
							messageArea.append("\nMessage from" + msg[1] + ": " + msg[2] + "\nVote either COMMIT or ABORT.\n");
							messageArea.append("State: READY");
							abortButton.setEnabled(true);
							commitButton.setEnabled(true);
							text = "\n"+msg[2] + "\n";
						} else if (msg[2].startsWith("COMMIT")) { //block of code is executed when other participant sends its State as COMMIT
							messageArea.append("\nMessage from" + msg[1] + ": " + msg[2] + "\n");
							state = "COMMIT";
							messageArea.append("State: " + state + ".\n");
							messageArea.append("\nCommit the message\n");
							messageArea.append("\nState :INIT\n");
						} else if (msg[2].startsWith("ABORT")) { //block of code is executed when participant votes abort
							state = "ABORT";
							messageArea.append("State: " + state + ".\n");
							messageArea.append("\nState :INIT\n");
							messageArea.append("\nAbort the message\n");
						}
						else if((!msg[1].contains(sName))&&(!msg[2].contains("Vote"))){
							messageArea.append("\nMessage from" + msg[1] + ": " + msg[2] + "\n");
							
						}
					}
				}

			}
		} catch (Exception e1) {
			socket.close();
			messageArea.append("Server is offline");
		}
	}

	// method to write the string received from the coordinator to the file
	public static void writeFile(String msg, String name) {
		try {
			File f1 = new File("/Users/snehithraj/eclipse-workspace/TwoPhaseCommit/src/" + name);
			if (!f1.exists()) {
				f1.createNewFile();
			}
			FileWriter fileWriter = new FileWriter(f1.getName(), true);
			BufferedWriter bw = new BufferedWriter(fileWriter);
			bw.write(msg);
			bw.close();
			System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// block of code is executed once the timer runs out after coordinator sends message
	class RemindTask extends TimerTask {
		public void run() {
			System.out.println("Time's up!");
			messageArea.append("\nTime Out.\n");
			messageArea.append("\nNo response from the Coordinator.\nSend message to other participants\n");
			t = 1;
			textField.setEditable(true);
			timer.cancel();
		}
	}

	DateTimeFormatter dtfd1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	String tDate1 = dtfd1.format(LocalDateTime.now());

	// block of code is executed if Timer has run out before coordinator sends message
	class RemindTask1 extends TimerTask {
		public void run() {
			System.out.println("Time's up!");
			messageArea.append("\nTime Out.\nNo response from the Coordinator.");
			messageArea.append("\nState: ABORT.\n");
			messageArea.append("\nYou voted as: ABORT.\n");
			//messageArea.append("\nState: INIT.\n");
			out.println("VoteAbort,POST/localhost/server http/1.1,Host:127.0.0.1,User-Agent:Mozilla/5.0,Content-Type:text,Content-Length:10,Date:"+ tDate1);
			t = 1;
			timer.cancel();
		}
	}

	public static void main(String[] args) throws Exception {
		Participant client = new Participant();
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setVisible(true);
		client.run();
	}
}