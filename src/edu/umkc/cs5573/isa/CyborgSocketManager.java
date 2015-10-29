package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.util.Map;

import com.almworks.sqlite4java.SQLiteException;

public class CyborgSocketManager {	
	private CyborgUdpService udpService;
	private CyborgTcpService tcpService;

	public CyborgSocketManager(String userName, String ifName, String homeDirectory, int tcpPort, int udpPort, boolean softBroadcast) throws IOException, SQLiteException{
		this.udpService = new CyborgUdpService("UDPThread", udpPort, userName, ifName, homeDirectory);
		this.tcpService = new CyborgTcpService("TCPThread", tcpPort, userName, homeDirectory);
	}
	
	public void init(){
		udpService.start();
		tcpService.start();
	}
	
	public void stopServices(){
		udpService.stopService();
		tcpService.stopService();		
	}
//	void serverSide(String ip, int portNumber){
//		ServerSocket serverSocket;
//		try {
//			serverSocket = new ServerSocket(portNumber);
//			Socket clientSocket = serverSocket.accept();
//			PrintWriter out =
//			    new PrintWriter(clientSocket.getOutputStream(), true);
//			BufferedReader in = new BufferedReader(
//			    new InputStreamReader(clientSocket.getInputStream()));
//			mSock = serverSocket.accept();
//			String inputLine, outputLine;
//            
//		    // Initiate conversation with client
//		    CTP kkp = new CTP("C:/Test");
//		    outputLine = kkp.processInput(null);
//		    out.println(outputLine);
//
//		    while ((inputLine = in.readLine()) != null) {
//		        outputLine = kkp.processInput(inputLine);
//		        out.println(outputLine);
//		        if (outputLine.equals("Bye."))
//		            break;
//		    }
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
	
	public void reqFileProbe(String fileName) {
		udpService.reqFileProbe(fileName);
	}

	public void reqFile(String ipAddress, String fileName) {
		// TODO Using TCPClient get file!!
		// Make TCP Client Socket and req file!!!
		
	}
	
	public void reportViolation(String userName, String fileName){
		Map<String, String> userList = udpService.getUserList();
		String ipAddress = userList.get(userName);
		// TODO Using TCPClient report the violation!! 
		//Make TCP Client Socket and report!
	}
}
