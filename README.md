# TwoPhaseCommit-Socket-Programming-Java

Description:
  The program consist of three components:
 - A passive server to relay commands between systems
 - Three clients acting as transaction participants
 - One client acting as the transaction coordinator.
 
 Server:
  The server facilitates relaying correspondence between your client processes. 
  It will have a GUI text box that displays message between clients as they transit the server.
  
 Participant:
  - Three clients will act as transaction participants. They will receive an arbitrary string from the 
    coordinator in the INIT phase. 
  - Each client will have two GUI buttons that allow the user to vote to either ABORT or COMMIT that 
    transaction (COMMIT consists of writing the arbitrary string to a file).
  - If the user votes to ABORT, the participant will immediately relay that command to the coordinator, 
    and the coordinator will initiate a GLOBAL ABORT accordingly. 
  - If the user votes to COMMIT, the participant will enter the READY state and prepare to COMMIT the 
    operation if instructed to do so by the coordinator.
  - Participants will set timers according to the 2PC protocol. If the coordinator times out, 
    participants will check with one another on how to proceed according to 2PC.
  - If the participants receive the GLOBAL COMMIT command from the coordinator, they will save the 
    arbitrary string to non-volatile storage. 
  - Anything saved into permanent storage will be loaded into the process and displayed to the user 
    upon the process starting.
    
  Coordinator:
  - The coordinator will accept an arbitrary string from a simple GUI that is then passed to the three participants.
  - The participants will then be given a finite amount of time to vote on whether to commit or abort writing that 
    string to a file.
  - The coordinator will handle votes according to 2PC. 
  - The coordinator will also implement timeouts according to 2PC (e.g., if a participant does not respond within a 
    finite amount of time, the coordinator will initiate a GLOBAL ABORT command).
    
 Steps of Execution:
   1.	Run the server code. Server UI will be displayed with pre-fetched information of previous session and server starts running.
   2.	Run the Coordinator code
   3.	Run the Participant code.
   4.	When prompted, enter the IP Address: 127.0.0.1(localhost)
   5.	Enter the preferred screen name.( Screen name should be unique )
   6.	Apply the same procedure for running multiple participants.
   7.	The coordinator and participants are now connected and are in INIT states and the participant 
      displays previous committed messages.
   8.	Timer has stated at the participant. Let the timer run out. The participant enters ABORT.
   9.	Now send message from the coordinator. The coordinator is in WAIT state.
   10.	The participant decide to vote either COMMIT or ABORT. The participant is in READY state.
   11.	Based on the votes the coordinator sends COMMIT or ABORT to the participants and enters into the respective state.
   12.	If the participant commits, it writes the message to the file.
   13.	The participant enters into either COMMIT or ABORT.
   14.	Send message from the coordinator. Timer has started. Let the timer run out at the coordinator. 
        The coordinator enters into ABORT.
   15.	Now again send the message from coordinator. One of the participants Votes.
        Now when the timer at the participant runs out. In this case ask the state of other participants. Act accordingly.
   16.	The Server UI will display the messages as http format.
   17.	If you want to quit close the participant window all the connected clients will be notified.
   18.	The participants and coordinator will also be notified when Server is offline.
   19.	Implemented multithread Server and Database for the server.
