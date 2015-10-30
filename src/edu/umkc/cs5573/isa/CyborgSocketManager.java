package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.util.Map;

import com.almworks.sqlite4java.SQLiteException;

public class CyborgSocketManager {	
	private CyborgUdpService udpService;
	private CyborgTcpService tcpService;
	private SQLiteInstance sql;
	private int tcpPort;
	private int udpPort;

	public CyborgSocketManager(String userName, String ifName, String homeDirectory, int tcpPort, int udpPort, boolean softBroadcast) throws IOException, SQLiteException{
		this.udpService = new CyborgUdpService("UDPThread", udpPort, userName, ifName, homeDirectory);
		this.tcpService = new CyborgTcpService("TCPThread", tcpPort, userName, homeDirectory);
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		this.sql = SQLiteInstance.getInstance();
	}
	
	public void init(){
		udpService.start();
		tcpService.start();
		udpService.reqJoinUser();
		udpService.reqUserList();
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
	
	public Map<String, String> getUserList(){
		return udpService.getUserList();
	}
	public void reqFileProbe(String fileName) {
		udpService.reqFileProbe(fileName);
	}

	public void reqFile(String sso, String fileName) {
		// TODO Using TCPClient get file!!
		// Make TCP Client Socket and req file!!!
		Map<String, String> userList = udpService.getUserList();
		String ipAddress = userList.get(sso);
		if(ipAddress != null) tcpService.reqFile(ipAddress, tcpPort, sso, fileName);
		
	}
	
	public void reportViolation(String sso, String fileName, String violation){
		Map<String, String> userList = udpService.getUserList();
		//Make TCP Client Socket and report!
		String ipAddress = userList.get(sso);
		if(ipAddress != null) tcpService.reportViolation(ipAddress, tcpPort, sso, violation);
	}

	public void reqCert(String sso, UserInfo myInfo) {
		// Request x509 cert
		CertInfo info = sql.getCertInfo(sso);
		if(info == null){
			Map<String, String> userList = udpService.getUserList();
			String ipAddress = userList.get(sso);
			if(ipAddress != null) tcpService.reqCert(ipAddress, tcpPort, sso, myInfo);
		}else{
			Logger.getInstance().d(this, "The certificate is already exist. Skipping...");
		}
	}
}
