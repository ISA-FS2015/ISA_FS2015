package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;


import com.almworks.sqlite4java.SQLiteException;
/**
 * The main wrapper class of this tool. It starts from here!
 * @author Younghwan
 *
 */
public class ISAPMain {
	final static String WORK_DIR = Resources.WORK_DIR;
	/**
	 * Used for backend mode
	 */
	protected final static int PORT_NO = 55732;
	

	/**
	 * The main method
	 * @param args UserName, IFName, HomeDirectory, Backend(optional)
	 */
	public static void main(String[] args) {
		localPrint("Cyborg test program.");
		localPrint("Usage: ISAPMain <username> <interfacename> <HomeDirectory> <B-Backend Mode>");
		localPrint("E.g: ISAPMain user01 wlan0 ./cyborgman");
		CyborgController cyborg = null;
		try {
			if(args.length > 3){
				// Run in backend mode. Actual IF will be handled by ISA_Client
				// Get CyborgController instance
				cyborg = CyborgController.getInstance(args[0], args[1], args[2]);
				cyborg.init();
				ServerSocket serverSocket = new ServerSocket(PORT_NO);
				while(true)
				{
					try{
						Socket clientSocket = serverSocket.accept();
						PrintStream out =
						        new PrintStream(clientSocket.getOutputStream(), true);
					    InputStream in = clientSocket.getInputStream();
						cyborg.cli(out, in);
					}
					catch(IOException e){
						serverSocket.close();
					}
				}
			}else{
				if(args.length > 2){
					cyborg = CyborgController.getInstance(args[0], args[1], args[2]);
				}else{
					String userName = System.getProperty("user.name");
					cyborg = CyborgController.getInstance(userName, "wlan0", WORK_DIR);
				}
				cyborg.init();
				cyborg.cli(System.out, System.in);
			}
		} catch (IOException | SQLiteException e) {
			e.printStackTrace();
		}
		if(cyborg != null) cyborg.closeService();
		System.exit(0);
	}
	
	/**
	 * Prints message locally
	 * @param msg Message string to be printed
	 */
	public static void localPrint(String msg){
		System.out.println(msg);
	}
}
