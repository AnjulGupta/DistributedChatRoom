
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;

/**
 * 
 */

/**
 * @author Samir Chaudhry
 *
 */
public class Server {
	
	private ServerSocket serverSocket;
	private static int minDelay;
	private static int maxDelay;
	
	public Server(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(100000);
	}
	
	public static void setUpServer() {
		File file = new File("config_file.txt");
		try {
			Scanner scanner = new Scanner(file);
			String[] line = scanner.nextLine().split(" ");
			String s = line[0].substring(line[0].indexOf("(") + 1);
			minDelay = Integer.parseInt(s.substring(0, s.indexOf(")")));
			s = line[1].substring(line[1].indexOf("(") + 1);
			maxDelay = Integer.parseInt(s.substring(0, s.indexOf(")")));
			scanner.close();
			System.out.println("minDelay: " + minDelay + " maxDelay: " + maxDelay);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		setUpServer();
		
//		int port = 1234;
		int port = Integer.parseInt(args[0]);
		
		ServerSocket serverSocket = null;
		boolean listeningSocket = true;
		int ids = 1;
		
		try {
			serverSocket = new ServerSocket(port);
			ArrayList<Socket> clients = new ArrayList<Socket>();
			while (listeningSocket) {
				Socket clientSocket = serverSocket.accept();
				MiniServer mini = new MiniServer(clientSocket, ids);
				clients.add(clientSocket);
				ids++;
				mini.start();
			}
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}