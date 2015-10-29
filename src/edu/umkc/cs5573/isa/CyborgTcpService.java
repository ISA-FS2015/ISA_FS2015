package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import com.almworks.sqlite4java.SQLiteException;

public class CyborgTcpService extends Thread {
	private final static int BASE_SCORE = 100;
	private boolean isRunning = false;
	private ServerSocket mServerSocket;
	private String mUserName;
	private String mHomeDirectory;
	private SQLiteInstance sql;
	private Logger logger;
	private MessageQueue mQueue;
	public CyborgTcpService(String threadName, int portNum, String userName, String homeDirectory)
			throws IOException, SQLiteException{
		super(threadName);
		this.mServerSocket = new ServerSocket(portNum);
		this.mUserName = userName;
		this.mHomeDirectory = homeDirectory;
		this.isRunning = true;
		this.sql = SQLiteInstance.getInstance();
		this.logger = Logger.getInstance();
		this.mQueue = MessageQueue.getInstance();
	}
	public void run(){
		while(isRunning && !mServerSocket.isClosed()){
			try {
				Socket clientSocket = mServerSocket.accept();
				new TcpThread(clientSocket).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void stopService(){
		this.isRunning = false;
	}
	public class TcpThread extends Thread{
		private static final int PREFIX = 8;
		private static final String DELIMITER = "#";
		private boolean isRunning = false;
		private Socket mSocket;
		public TcpThread(Socket sock){
			this.mSocket = sock;
			this.isRunning = true;
		}
		public void run(){
			try {
				BufferedReader ibr =
			               new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
	            DataOutputStream ods = new DataOutputStream(mSocket.getOutputStream());
				while(isRunning && mSocket.isConnected()){
					// Process the request
					processReq(ibr, ods);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void processReq(BufferedReader is, DataOutputStream os){
			try {
	            String recvd = is.readLine();
	            if(recvd.length() >= PREFIX){
	            	String[] reqs = recvd.split(DELIMITER);
	            	if("REQ_FILE".equals(reqs[0])){
	            		os.writeBytes(doFileTransferProcess(reqs));
	            	}else if("REQ_TRST".equals(reqs[0])){
	            		os.writeBytes(doIssueCertificates(reqs));
	            	}else if("REPORT_VIOLATION".equals(reqs[0])){
	            		os.writeBytes(doReaction(reqs));
	            	}
	            }
			} catch (IOException | GeneralSecurityException e) {
				e.printStackTrace();
				try {
					os.writeBytes("Error:" + e.getMessage());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		private String doReaction(String[] reqs) {
			String sso = reqs[1];
			String name = reqs[2];
			String fileName = reqs[3];
			String access = reqs[4];
			String msg = "The prohibited access detected from the following user:"
					+ "\nSSO: " + sso
					+ "\nName: " + name
					+ "\nAccess attempted: " + access
					+ "\nPlease type 'delete " + fileName + " from " + sso + "' to delete remotely,"
					+ "\nor 'restore " + fileName + " from " + sso + "' to restore remotely,"
					+ "\nor 'allow " + fileName + " from " + sso + "' to do nothing.";
			logger.d(this, msg);
			msg = null;
			String response = "";
			while(msg == null){
				try {
					msg = mQueue.getFirstMessage();
					if(msg.startsWith("delete ")){
						String[] cmds = msg.split(" ");
						if(cmds.length > 3){
							if(cmds[1].equals(fileName) &&
								cmds[2].equals("from") &&
								cmds[3].equals(sso)){
								// Delete the file remotely
								response = "REACTION" + DELIMITER + "DELETE";
							}
						}
					}else if(msg.startsWith("restore ")){
						String[] cmds = msg.split(" ");
						if(cmds.length > 3){
							if(cmds[1].equals(fileName) &&
								cmds[2].equals("from") &&
								cmds[3].equals(sso)){
								// Restore the file remotely
								String fileBase64 = "";
								response = "REACTION" + DELIMITER + "RESTORE" + DELIMITER + fileBase64;
							}
						}
					}else if(msg.startsWith("allow ")){
						String[] cmds = msg.split(" ");
						if(cmds.length > 3){
							if(cmds[1].equals(fileName) &&
								cmds[2].equals("from") &&
								cmds[3].equals(sso)){
								// Do nothing
								response = "REACTION" + DELIMITER + "ALLOW";
							}
						}
					}
					msg = null;
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return response;
		}
		public String doFileTransferProcess(String[] reqs) throws CertificateException, IOException{
    		// Do some trust process
    		String trust = reqs[1];
    		X509Util x509Util = new X509Util(StaticUtil.base64ToBytes(trust));
    		// Checking if the user is trusted by me
    		String sso = X509Util.parseDN(x509Util.getCertificate().getSubjectDN().getName())[0];
    		UserInfo info = sql.getUserInfo(sso);
    		if(info == null){
                return "Error"+DELIMITER+"InvalidTrustee";
    		}
    		// Checking if the cert is valid
			if(!x509Util.isCertValid(x509Util.getCertificate().getPublicKey(), mUserName, info.getSso())){
                return "Error"+DELIMITER+"InvalidCert";
			}
    		// Get filename
    		String fileName = reqs[2];
    		// Send the file to the requestor
    		File file = new File(fileName);
    		if(file.exists()){
	            return "Size"+ DELIMITER + Long.toString(file.length()) + DELIMITER + StaticUtil.encodeFileToBase64Binary(fileName);
    		}else{
	            return "Error"+DELIMITER+"NoFile";
    		}
		}
		
		public String doIssueCertificates(String[] reqs) throws IOException, GeneralSecurityException{
    		boolean isTrustworthy = false;
    		// Ask the required information from the trustee
    		isTrustworthy = validateUser(reqs);
    		if(isTrustworthy){
        		// If qualified, issue a X509 Cert.
    			KeyPairGenerator keyGen;
				keyGen = KeyPairGenerator.getInstance(CyborgSecurity.ALGORITHM);
    			keyGen.initialize(1024);
    			final KeyPair pair = keyGen.generateKeyPair();
    			String issuerDN = X509Util.makeDN(mUserName, "CyborgCorp", "Kansas City", "US");
    			String sso = reqs[1];
    			String subjectDN = X509Util.makeDN(sso, "UMKC", "Kansas City", "US");
    			X509Certificate issuedX509 = X509Util.issueCertificate(issuerDN, subjectDN,
    					pair, 31, "SHA1withRSA");
    			PrivateKey prk = pair.getPrivate();
    			PublicKey pbk = pair.getPublic();
    			// Insert userinfo into DB
    			insertUserInfoIntoDB(reqs, prk, pbk);
    			//os.write( issuedX509.getEncoded() );
    			return "X509"+ DELIMITER + StaticUtil.byteToBase64(issuedX509.getEncoded());
    		}else{
    			return "Error" + DELIMITER + "Revoked";
    		}
		}
		
		private void insertUserInfoIntoDB(String[] reqs, PrivateKey prk, PublicKey pbk) {
			String sso = reqs[1];
			int type = Integer.parseInt(reqs[2]);
			String name = reqs[3];
			String organization = reqs[4];
			String email = reqs[5];
			String phoneNumber = reqs[6];
			String privateKey = StaticUtil.byteToBase64(prk.getEncoded());
			String publicKey = StaticUtil.byteToBase64(pbk.getEncoded());
			UserInfo info = new UserInfo(sso, type, name, organization, email, phoneNumber,BASE_SCORE, privateKey, publicKey);
			sql.pushUserInfo(info);
		}
		
		private boolean validateUser(String[] reqs) {
			// the index start from 1 because 0 is used by prefix
			if(reqs.length == 6){
				String sso = reqs[1];
				int type = Integer.parseInt(reqs[2]);
				String name = reqs[3];
				String organization = reqs[4];
				String email = reqs[5];
				String phoneNumber = reqs[6];
				String msg = "The following user is requesting a trust."
						+ "\nSSO: " + sso
						+ "\nType: " + UserInfo.getTypeString(type)
						+ "\nName: " + name
						+ "\nOrganization: " + organization
						+ "\ne-mail: " + email
						+ "\nphone number: " + phoneNumber
						+ "\nPlease type 'allow " + sso + "' to allow,"
						+ "\notherwise 'revoke " + sso + "' to revoke.";
				logger.d(this, msg);
				msg = null;
				while(msg == null){
					try {
						msg = mQueue.getFirstMessage();
						if(msg.startsWith("allow ")){
							String[] cmds = msg.split(" ");
							if(cmds.length > 1){
								String requestor = cmds[1];
								if(sso.equals(requestor)){
									mQueue.deque();
									return true;
								}
							}
						}else if(msg.startsWith("revoke ")){
							String[] cmds = msg.split(" ");
							if(cmds.length > 1){
								String requestor = cmds[1];
								if(sso.equals(requestor)){
									mQueue.deque();
									return false;
								}
							}
						}
						msg = null;
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return false;
		}
	}
}
