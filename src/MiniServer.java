

import java.net.*;
import java.io.*;

public class MiniServer extends Thread {
	
	private Socket clientSocket = null;
	private int id;

	public MiniServer(Socket socket, int id) {
		super("MiniServer");
		this.clientSocket = socket;
		this.id = id;
	}
	
	
	
	public void run() {
		// Read input and process
		System.out.println("Client " + id + " has connected.");
		
		try {
			PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			
			String input;
			while ((input = in.readLine()) != null) {
				System.out.println("Client " + id + ": " + input);
				out.println("Received your message.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Client " + id + " has disconnected.");
	}	
}