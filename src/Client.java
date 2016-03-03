
import java.net.*;
import java.util.Scanner;
import java.io.*;

public class Client {
	
	public PrintWriter out;
	public BufferedReader in;
	public BufferedReader stdin;
	
	private static String[] getClientInfo(int id) {
		File file = new File("config_file.txt");
		try {
			Scanner scanner = new Scanner(file);
			String line = scanner.nextLine();
			int num = 0;
			while (scanner.hasNextLine() && num < id) {
				line = scanner.nextLine();
				num++;
			}
			scanner.close();
			return line.split(" ");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Send out a message to the client with ID destination
	 * @param destination
	 * @param message
	 */
	public static void unicast_send(int destination, String message) {
//		MetaData data = new MetaData()
		System.out.println("unicast_send(" + destination + ", " + message);
		
	}
	
	/**
	 * Print out the message 
	 * @param source
	 * @param message
	 */
	public static void unicast_receive(int source, String message) {
		
	}
	
	public static void main(String[] args) {
		int id = Integer.parseInt(args[0]);
		String[] info = getClientInfo(id);
		System.out.println(info[0] + ", " + info[1] + ", " + info[2] + ")");
		
		String serverName = info[1];
		int port = Integer.parseInt(info[2]);
		
		try {
			Socket client = new Socket(serverName, port);
			
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			
			String input;
			while ((input = stdIn.readLine()) != null) {
//				int num = Integer.parseInt(input.substring(0, 1));
//				unicast_send(num, input.substring(2));
//				out.println(input.substring(2));
				out.println(input);
				System.err.println("echo: " + in.readLine());
			}
			
			client.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}
