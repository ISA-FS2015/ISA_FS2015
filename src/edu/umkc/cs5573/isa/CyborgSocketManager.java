package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.util.Map;

import com.almworks.sqlite4java.SQLiteException;
/**
 * <p>Wrapper class for handling TCP and UDP communication.</p>
 * @author Younghwan
 * @see CyborgUdpService
 * @see CyborgTcpService
 */
public class CyborgSocketManager {
	private CyborgUdpService udpService;
	private CyborgTcpService tcpService;
	private SQLiteInstance sql;
	private int tcpPort;
//	private int udpPort;

	/**
	 * Constructor
	 * @param userName
	 * @param ifName
	 * @param homeDirectory
	 * @param tcpPort
	 * @param udpPort
	 * @param softBroadcast Sometimes broadcast not allowed in some network. We broadcast by sending every packet from x.x.x.1 to x.x.x.254
	 * @throws IOException
	 * @throws SQLiteException
	 */
	public CyborgSocketManager(String userName, String ifName, String homeDirectory, int tcpPort, int udpPort, boolean softBroadcast) throws IOException, SQLiteException{
		this.udpService = new CyborgUdpService("UDPThread", udpPort, userName, ifName, homeDirectory);
		this.tcpService = new CyborgTcpService("TCPThread", tcpPort, userName, homeDirectory);
		this.tcpPort = tcpPort;
//		this.udpPort = udpPort;
		this.sql = SQLiteInstance.getInstance();
	}
	
	/**
	 * Initialization
	 */
	public void init(){
		udpService.start();
		tcpService.start();
		udpService.reqUserList();
		udpService.reqJoinUser();
	}
	
	/**
	 * Stops service including TCP and UDP service
	 */
	public void stopServices()
	{
		udpService.stopService();
		tcpService.stopService();
	}
	
	/**
	 * Retrieves user list from UDP communication
	 * @return
	 */
	public Map<String, String> getUserList()
	{
		return udpService.getUserList();
	}
	
	/**
	 * Request file probe through UDP
	 * @param fileName
	 */
	public void reqFileProbe(String fileName)
	{
		udpService.reqFileProbe(fileName);
	}

	/**
	 * Request file to the specific user(SSO)
	 * @param sso The sso name who has the file
	 * @param fileName
	 */
	public void reqFile(String sso, String fileName)
	{
		// Make TCP Client Socket and req file!!!
		Map<String, String> userList = getUserList();
		String ipAddress = userList.get(sso);
		if(ipAddress != null) tcpService.reqFile(ipAddress, tcpPort, sso, fileName);
		
	}

	/**
	 * Reports the violation against the owner
	 * @param sso The owner(SSO)
	 * @param fileName
	 * @param violation
	 */
	public void reportViolation(String sso, String fileName, String violation)
	{
		Map<String, String> userList = getUserList();
		//Make TCP Client Socket and report!
		String ipAddress = userList.get(sso);
		if(ipAddress != null) tcpService.reportViolation(ipAddress, tcpPort, sso, fileName, violation);
	}

	/**
	 * Requests the trust to get X509 certificate
	 * @param sso
	 * @param myInfo
	 */
	public void reqCert(String sso, UserInfo myInfo)
	{
		// Request x509 cert
		CertInfo info = sql.getCertInfo(sso);
		if(info == null){
			Map<String, String> userList = getUserList();
			String ipAddress = userList.get(sso);
			if(ipAddress != null) tcpService.reqCert(ipAddress, tcpPort, sso, myInfo);
		}else{
			Logger.getInstance().d(this, "The certificate is already exist. Skipping...");
		}
	}
}
