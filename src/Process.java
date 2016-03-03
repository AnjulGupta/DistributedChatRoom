import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author Samir Chaudhry
 *
 */
public class Process {

	// Min and max delays for the delay
	private static int minDelay;
	private static int maxDelay;
	// A mapping of processId's to their corresponding metadata information
	private static HashMap<Integer, MetaData> list = new HashMap<Integer, MetaData>();
	// Whether 
	private static boolean closed = false;
	private static ArrayList<Integer> v_timestamps = new ArrayList<Integer>();
	private static boolean fifoOrdering = false;
	private static boolean causalOrdering = false;
	private static ArrayList<Message> holdBackQueue = new ArrayList<Message>();
	
	// Do I even need a constructor...?
	public Process(int port) {
		
	}
	
	/**
	 * Print the list of processes/sockets in our list
	 */
	public static void printProcesses() {
		System.out.println("minDelay: " + minDelay + " maxDelay: " + maxDelay);
		for (int i = 0; i < list.size(); i++) {
			MetaData data = list.get(i+1);
			if (data != null) {
				String[] info = data.getProcessInfo();
				System.out.println("========================");
				System.out.println(info[0] + " " + info[1] + " " + info[2]);
				if (data.isOpen())
					System.out.println("Socket is open");
				System.out.println("========================");
			}
			else
				System.out.println(i + " is null!");
		}
	}
	
	/**
	 * Returns the time as a string formatted as hour:minutes:seconds
	 * @return
	 */
	public static String getTime() {
		return new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
	}
	
	/**
	 * Gets/sets the min and max delay given first line of the config file
	 * @param line
	 */
	public static void getMinMaxDelay(String[] line) {
		String s = line[0].substring(line[0].indexOf("(") + 1);
		minDelay = Integer.parseInt(s.substring(0, s.indexOf(")")));
		s = line[1].substring(line[1].indexOf("(") + 1);
		maxDelay = Integer.parseInt(s.substring(0, s.indexOf(")")));
	}
	
	/**
	 * Given an input string, splits it into a string array and adds it to the global process list
	 * @param input
	 */
	public static void addProcessToList(String input, int id) {
//		System.out.println("Adding process " + id + " to list: " + input);
		String[] info = input.split(" ");
		MetaData data = new MetaData(info, null, null, false);
		list.put(id, data);
		v_timestamps.add(id-1, 0);
	}
	
	/**
	 * Reads in from the config file and gets all the processes' info
	 * @param id
	 * @return
	 */
	public static void scanConfigFile(int id) {
		File file = new File("../config_file.txt");
		try {
			Scanner scanner = new Scanner(file);
			
			// Get the min and max delay
			String[] line = scanner.nextLine().split(" ");
			getMinMaxDelay(line);

			// Scan through config file adding MetaData to list for every process
			String input = "";
			boolean found = false;
			int num = 1;
			while (scanner.hasNext()) {
				if (num == id) {
					input = scanner.nextLine();
					addProcessToList(input, id);
					found = true;
				}
				else {
					input = scanner.nextLine();
					addProcessToList(input, Integer.parseInt(input.substring(0, 1)));
				}
				num++;
			}
			scanner.close();
			if (!found)
				System.err.println("Invalid process ID!");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Starts up the client
	 * @param id
	 * @param serverName
	 * @param port
	 */
	public static void startClient(final int id, final String serverName, final int port) {
//		System.out.println("Starting client " + id + " at " + serverName + " on port " + port);
        (new Thread() {
            @Override
            public synchronized void run() {
            	readAndSendMessages(id);
//    			System.err.println("Closing client");
            }
        }).start();
	}
	
	/**
	 * Reads messages in from stdIn and sends them to the correct process
	 * @param id
	 */
	public static void readAndSendMessages(int id) {
    	try {
    		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        	String input;
        	boolean exit = false;
			while (!exit && (input = stdIn.readLine()) != null) {
				/*
				final String message = input;
				final int clientId = id;
		        (new Thread() {
		            @Override
		            public synchronized void run() {
						if (checkUnicastInput(message)) {
							int destination = Integer.parseInt(message.substring(5, 6));
							message = message.substring(7);
							v_timestamps.set(id-1, v_timestamps.get(clientId-1)+1);
							sendMessage(message, destination, clientId);
						}
						else if (checkMulticastInput(message)) {
							v_timestamps.set(clientId-1, v_timestamps.get(clientId-1)+1);
							message = message.substring(6);
							System.out.println("Incremented timestamp of " + clientId + " to " + v_timestamps.get(clientId-1));
							multicast(message, clientId);
						}
						else if (isExitCommand(message)) {
							v_timestamps.set(clientId-1, v_timestamps.get(clientId-1)+1);
							multicast(message, clientId);
							exit = true;
						}
						else if (!message.isEmpty()) {
							System.err.println("send <#> <message>");
						}
		            }
		        }).start();
				*/
				
				if (checkUnicastInput(input)) {
					int destination = Integer.parseInt(input.substring(5, 6));
					input = input.substring(7);
					int time = v_timestamps.get(id-1)+1;
					v_timestamps.set(id-1, time);
					sendMessage(input, destination, id, time);
				}
				else if (checkMulticastInput(input)) {
					input = input.substring(6);
					multicast(input, id);
				}
				else if (isExitCommand(input)) {
					v_timestamps.set(id-1, v_timestamps.get(id-1)+1);
					multicast(input, id);
					exit = true;
				}
				else if (!input.isEmpty()) {
					System.err.println("send <#> <message>");
				}
				
			}
			System.err.println("Closing client " + id);
			stdIn.close();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Given a valid string and process, sends the message to the processes' socket
	 * @param input
	 */
	public static void sendMessage(String input, int destination, int source, int time) {
		MetaData data = list.get(destination);
		if (data != null) {
			String[] destinationInfo = data.getProcessInfo();
			
			try {
				if (!data.isOpen()) {
//					System.out.println("Creating socket to " + destinationInfo[1] + ", " + destinationInfo[2]);
					Socket s = new Socket(destinationInfo[1], Integer.parseInt(destinationInfo[2]));
					data.setSocket(s);
					data.setWriter(new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true));
					data.setOpen(true);
				}
				String message = input;
				input = formatMessage(message, destinationInfo, source, time);
				data.getWriter().println(input);
				data.getWriter().flush();
				System.out.println("Sent \"" + message + "\" to process " + destination + ", system time is " + getTime());
			} catch (IOException e) {
				System.err.println("ERROR: Failed to create a socket to process " + destination);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Multicasts a message to every process
	 * @param message
	 */
	public static void multicast(String message, int source) {
		int time = v_timestamps.get(source-1) + 1;
		v_timestamps.set(source-1, time);
		System.out.println("Incremented timestamp of " + source + " to " + v_timestamps.get(source-1));
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i+1) != null && source != (i+1))
				sendMessage(message, i+1, source, time);
		}
	}

	/**
	 * Checks whether the message input is a valid unicast input
	 * A valid input is of the form: send <#> <message>
	 * @param input
	 * @return
	 */
	public static boolean checkUnicastInput(String input) {
		if (input.length() > 6 && input.substring(0, 4).equals("send")) {
			input = input.substring(5);
			return Character.isDigit(input.charAt(0)) && Character.isWhitespace(input.charAt(1));
		}
		else 
			return false;
	}
	
	/**
	 * Checks whether the message input is a valid multicast input
	 * A valid input is of the form: msend <message>
	 * @param input
	 * @return
	 */
	public static boolean checkMulticastInput(String input) {
		if (input.length() >= 6 && input.substring(0, 5).equals("msend")) {
			input = input.substring(5);
			return Character.isWhitespace(input.charAt(0));
		}
		else 
			return false;
	}
	
	/**
	 * Updates the vector time and formats the message to include metadata
	 * @param input "<message>"
	 * @param data
	 * @param processNumber
	 * @return "<source> <destination> <IP> <port> <time>: <message>"
	 */
	public static String formatMessage(String message, String[] destinationInfo, int source, int time) {
		return source + " " + destinationInfo[0] + " " + destinationInfo[1] + " " + destinationInfo[2] + " " + time + ": " + message;
	}

	/**
	 * Starts the server in a new thread
	 * Loops until every process has been connected to
	 * @param serverName
	 * @param port
	 */
	public static void startServer(String serverName, final int port) {
        (new Thread() {
            @Override
            public void run() {
                ServerSocket ss;
                try {
                    ss = new ServerSocket(port);
                    
                    // Keep looping until every MetaData is open
                    while (!closed) {
	                    final Socket s = ss.accept();
	                    
	                    // Create a new thread for each connection
	                    (new Thread() {
	                    	@Override
	                    	public void run() {
	                    		receiveMessages(s);
	                    	}
	                    }).start();

                    }
                    System.err.println("Server is closing.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
	}
	
	/**
	 * Once a client connects to the server, keep reading messages from the client
	 * If first time connecting, gets the socket's info into the MetaData
	 * @param s
	 */
	public static void receiveMessages(final Socket s) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
	        String input;
			while (!closed && (input = in.readLine()) != null) {
				final String message = input;
                // Create a new thread for each message
                (new Thread() {
                	@Override
                	public void run() {
                		unicastReceive(message, s);
                	}
                }).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("Server is closing.");
	}

	/**
	 * Prints out the message, implementing a delay if necessary
	 * @param source
	 * @param message
	 */
	public static void unicastReceive(String input, Socket s) {
		int source = Integer.parseInt(input.substring(0, 1));
		int id = Integer.parseInt(input.substring(2, 3));
//		System.out.println("input is : " + input);
		int time = readTimeFromInput(input);
		String message = input.substring(input.indexOf(":") + 2);
		
		boolean deliver = delayMessage(id, time, input);
		
		if (deliver)
			deliverMessage(input, message, s, id, source);
	}
	
	public static void deliverMessage(String input, String message, Socket s, int id, int source) {
		System.out.println("Received \"" + message + "\" from process " + source + ", system time is " + getTime());
		
		if (list.get(id).isOpen()) {
			if (isExitCommand(message)) {
				try {
					s.close();
					list.get(source).setSocket(null);
					closed = socketsOpen();
					System.out.println("Process " + source + " is now closed (closed = " + closed + ").");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			getSocketInfo(input.substring(2), s);
		}
		
		// Check if this updates any messages on the queue
		checkHoldBackQueue(id);
	}
	
	public static synchronized void checkHoldBackQueue(int id) {
//		System.out.println("Checking hold back queue!");
		for (int i = 0; i < holdBackQueue.size(); i++) {
			
			Message msg = holdBackQueue.get(i);
			int msgSource = msg.getSource();
			int v_time = v_timestamps.get(msgSource-1);
			
			if (msg.getTimestamp() == v_time + 1) {
				System.out.println("Msg should be delivered now.");
				holdBackQueue.remove(i);
				v_timestamps.set(msgSource-1, v_time+1);
				MetaData data = list.get(msgSource-1);
				String input = msg.getMessage();
				String message = input.substring(input.indexOf(":") + 2);
				deliverMessage(input, message, data.getSocket(), id, msgSource-1);
			}
		}
	}

	/**
	 * Returns the time from a formatted string input
	 * @param input: "<source> <destination> <IP> <port> <time>: <message>"
	 * @return <time>, -1 if not found
	 */
	public static synchronized int readTimeFromInput(String input) {
		int timeEnd = input.indexOf(':') - 1;
		int timeStart = timeEnd;
		while (timeStart > 0 && !Character.isWhitespace(input.charAt(timeStart-1))) {
			timeStart--;
		}
		if (timeEnd >= 0) {
//			System.out.println("TimeEnd: " + timeEnd + "; timeStart = " + timeStart);
			String time = input.substring(timeStart, timeEnd+1);
			return Integer.parseInt(time);
		}
		return -1;
	}
	
	/**
	 * Delays a message: adds in network delay and places elements in holdback queue 
	 * @param time
	 */
	public static synchronized boolean delayMessage(int source, int time, String input) {
		// Sleep for a random time to simulate network delay
		sleepRandomTime();
		
		// If implementing FIFO ordering, check the sequence vector 
		if (fifoOrdering) {
			int v_time = v_timestamps.get(source - 1);
			
			System.out.println("Current time is " + v_time + ", msg time is " + time);
			
			// If the time has already passed, ignore the message
			if (time < v_time + 1) {
				System.err.println("Message arriving too late!");
				return false;
			}
			
			// If the vector_time has come, deliver the message
			if (time == v_time + 1) {
				v_timestamps.set(source - 1, time);
				return true;
			}
			
			Message msg = new Message(time, input, source);
			System.err.println("Adding \"" + input + "\" to holdBackQueue");
			holdBackQueue.add(msg);
		}
		else if (causalOrdering) {
			
		}
		return false;
	

	}

	/**
	 * Sleeps the current thread for a random amount of time bounded my min and max delay
	 */
	public static void sleepRandomTime() {
		int random = minDelay + (int)(Math.random() * (maxDelay - minDelay + 1));
		
		try {
			Thread.sleep(random);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}

	/**
	 * Checks whether the message input is equal to exit
	 * @param input 
	 * @return True or False
	 */
	public static boolean isExitCommand(String input) {
		return input.equals("exit");
	}

	public static synchronized boolean socketsOpen() {
		for (MetaData data : list.values()) {
			if (data.getSocket() != null)
				return true;
		}
		return false;
	}

	/**
	 * Given a new socket and its first message sent, determine the socket info from the metadata
	 * and populate your data with its info
	 * @param input
	 * @param s
	 * @return
	 */
	public static void getSocketInfo(String input, Socket s) {
		int colon = input.indexOf(':');
		input = input.substring(0, colon);
		String[] info = input.split(" ");
		if (info.length == 1)
			return;
		int id = Integer.parseInt(info[0]);
		MetaData data = list.get(id);
		data.setProcessInfo(info);
		
		try {
			data.setSocket(s);
			data.setWriter(new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true));
			data.setOpen(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Determines whether the received message is a valid input
	 * Invalid input = a string with a colon in it
	 * @param input
	 * @return
	 */
	public static boolean invalidInput(String input) {
		int index = input.indexOf(':');
		return (index != -1 && index >= 0);
	}
	
	private static void determineOrdering(String order) {
		if (order != null) {
			if (order.equals("fifo")) {
				fifoOrdering = true;
				return;
			}
			else if (order.equals("causal")) {
				causalOrdering = true;
				return;
			}
		}
		System.err.println("No valid ordering entered! Using FIFO ordering.");
		fifoOrdering = true;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String order = null;
		if (args.length < 2) {
			System.err.println("./process <id> <ordering>");
			System.err.println("<ordering> = (fifo, causal)");
		}
		else
			order = args[1];
		
		// Get the process ID number
		int id = Integer.parseInt(args[0]);
		
		// Determine the multicast ordering to use
		determineOrdering(order);
		
		// Read in the config file
		scanConfigFile(id);
		
		// Get the current process information from id; if not found, return
		String[] info = list.get(id).getProcessInfo();
		if (info == null)
			return;
		
		// Get the port of this process
		int port = Integer.parseInt(info[2]);
		
		printProcesses();
		
		// Start up the server; start up the clients as they connect
		startServer(info[1], port);
		startClient(id, info[1], Integer.parseInt(info[2]));
	}
}

class MetaData {
	private String[] process;
	private Socket socket;
	private PrintWriter out;
	private boolean open;
	
	public MetaData(String[] process, Socket socket, PrintWriter out, boolean open) {
		this.process = process;
		this.socket = socket;
		this.out = out;
		this.open = open;
	}
	
	public String[] getProcessInfo() {
		return this.process;
	}
	
	public void setProcessInfo(String[] info) {
		this.process = info;
	}
	
	public Socket getSocket() {
		return this.socket;
	}
	
	public void setSocket(Socket sock) {
		this.socket = sock;
	}
	
	public PrintWriter getWriter() {
		return this.out;
	}
	
	public void setWriter(PrintWriter writer) {
		this.out = writer;
	}
	
	public boolean isOpen() {
		return this.open;
	}
	
	public void setOpen(boolean bool) {
		this.open = bool;
	}
}

class Message {
	private int timestamp;
	private String message;
	private int source;
	
	public Message(int time, String msg, int id) {
		this.timestamp = time;
		message = msg;
		this.source = id;
	}
	
	public int getTimestamp() {
		return this.timestamp;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public int getSource() {
		return this.source;
	}
}