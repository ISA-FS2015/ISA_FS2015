package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import com.almworks.sqlite4java.SQLiteException;

public class CyborgTcpService extends Thread {
	private final static int BASE_SCORE = 100;
	private final static String REQTYPE_FILE = "REQ_FILE";
	private final static String REQTYPE_TRST = "REQ_TRST";
	private final static String REQTYPE_REPORT_VIOLATION = "REQ_REPORT_VIOLATION";
	private static final String DELIMITER = "#";
	private static final String RESPONSE_ERROR = "Error";
	private static final String RESPONSE_X509_CERT = "X509";
	private static final String RESPONSE_FILE_SIZE = "Size";
	private static final String RESPONSE_REACTION = "Reaction";
	private static final String REACTION_DELETE = "Delete";
	private static final String REACTION_RESTORE = "Restore";
	private static final String REACTION_ALLOW = "Allow";
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
				new ServerThread(clientSocket).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void stopService()
	{
		this.isRunning = false;
	}
	
	public class ServerThread extends Thread{
		private static final int PREFIX = 8;
		private boolean isRunning = false;
		private Socket mSocket;
		public ServerThread(Socket sock)
		{
			this.mSocket = sock;
			this.isRunning = true;
		}
		public void run()
		{
			try {
				BufferedReader ibr =
			               new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
	            DataOutputStream ods = new DataOutputStream(mSocket.getOutputStream());
				while(isRunning && mSocket.isConnected()){
					// Process the request
					processReq(ibr, ods);
				}
				ibr.close();
				ods.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void processReq(BufferedReader is, DataOutputStream os){
			try {
	            String recvd = is.readLine();
	            if(recvd.length() >= PREFIX){
	            	String[] reqs = recvd.split(DELIMITER);
	            	if(REQTYPE_FILE.equals(reqs[0])){
	            		os.writeBytes(doFileTransferProcess(reqs));
	            	}else if(REQTYPE_TRST.equals(reqs[0])){
	            		os.writeBytes(doIssueCertificates(reqs));
	            	}else if(REQTYPE_REPORT_VIOLATION.equals(reqs[0])){
	            		os.writeBytes(doReaction(reqs));
	            	}
	            }
			} catch (IOException | GeneralSecurityException e) {
				e.printStackTrace();
				try {
					os.writeBytes(RESPONSE_ERROR + DELIMITER + e.getMessage());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		private String doReaction(String[] reqs)
		{
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
								response = RESPONSE_REACTION + DELIMITER + REACTION_DELETE;
							}
						}
					}else if(msg.startsWith("restore ")){
						String[] cmds = msg.split(" ");
						if(cmds.length > 3){
							if(cmds[1].equals(fileName) &&
								cmds[2].equals("from") &&
								cmds[3].equals(sso)){
								// Restore the file remotely
								String fileBase64;
								try {
									fileBase64 = StaticUtil.encodeFileToBase64Binary(fileName);
									response = RESPONSE_REACTION + DELIMITER + REACTION_RESTORE + DELIMITER + fileBase64;
								} catch (IOException e) {
									e.printStackTrace();
									logger.d(this, e.getMessage());
								}
							}
						}
					}else if(msg.startsWith("allow ")){
						String[] cmds = msg.split(" ");
						if(cmds.length > 3){
							if(cmds[1].equals(fileName) &&
								cmds[2].equals("from") &&
								cmds[3].equals(sso)){
								// Do nothing
								response = RESPONSE_REACTION + DELIMITER + REACTION_ALLOW;
							}
						}
					}
					msg = null;
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return response;
		}
		
		public String doFileTransferProcess(String[] reqs)
				throws CertificateException, IOException{
    		// Do some trust process
    		String trust = reqs[1];
    		X509Util x509Util = new X509Util(StaticUtil.base64ToBytes(trust));
    		// Checking if the user is trusted by me
    		String sso = X509Util.parseDN(x509Util.getCertificate().getSubjectDN().getName())[0];
    		UserInfo info = sql.getUserInfo(sso);
    		if(info == null){
                return RESPONSE_ERROR+DELIMITER+"InvalidTrustee";
    		}
    		// Checking if the cert is valid
			if(!x509Util.isCertValid(x509Util.getCertificate().getPublicKey(), mUserName, info.getSso())){
                return RESPONSE_ERROR+DELIMITER+"InvalidCert";
			}
    		// Get filename
    		String fileName = reqs[2];
    		// Send the file to the requestor
    		File file = new File(fileName);
    		if(file.exists()){
    			FileInfo fileInfo = sql.getFileInfo(file.toPath());
	            return RESPONSE_FILE_SIZE + DELIMITER
	            		+ Long.toString(file.length()) + DELIMITER
	            		+ StaticUtil.encodeFileToBase64Binary(fileName) +DELIMITER
	            		+ fileInfo.getType();
    		}else{
	            return RESPONSE_ERROR+DELIMITER+"NoFile";
    		}
		}
		
		public String doIssueCertificates(String[] reqs)
				throws IOException, GeneralSecurityException{
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
    			return RESPONSE_X509_CERT+ DELIMITER + StaticUtil.byteToBase64(issuedX509.getEncoded());
    		}else{
    			return RESPONSE_ERROR + DELIMITER + "Revoked";
    		}
		}
		
		private void insertUserInfoIntoDB(String[] reqs, PrivateKey prk, PublicKey pbk)
		{
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
		
		private boolean validateUser(String[] reqs)
		{
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
	
	// Client Requesting method part - start
	
	public void reqCert(String ipAddress, int portNum, String sso, UserInfo myInfo){
		StringBuilder payload = new StringBuilder();
		payload.append(myInfo.getSso())
				.append(DELIMITER)
				.append(myInfo.getType())
				.append(DELIMITER)
				.append(myInfo.getName())
				.append(DELIMITER)
				.append(myInfo.getOrganization())
				.append(DELIMITER)
				.append(myInfo.getEmail())
				.append(DELIMITER)
				.append(myInfo.getPhoneNumber());
		Requestor req = new Requestor(ipAddress, portNum, sso);
		req.setRequest(REQTYPE_TRST, payload.toString().split(DELIMITER));
		req.start();
	}
	
	public void reqFile(String ipAddress, int portNum, String sso, String fileName) {
		CertInfo cert = sql.getCertInfo(sso);
		if (cert == null){
			logger.d(this, "No certification corresponding to the owner. Please get a trust first.");
		}else{
			String certStr = cert.getCert();
			Requestor req = new Requestor(ipAddress, portNum, sso);
			StringBuilder payload = new StringBuilder();
			req.setRequest(REQTYPE_TRST, payload.toString().split(DELIMITER));
			req.start();
		}
	}
	
	public void reportViolation(String ipAddress, int portNum, String sso, String violation){
		StringBuilder payload = new StringBuilder();
		Requestor req = new Requestor(ipAddress, portNum, sso);
		req.setRequest(REQTYPE_REPORT_VIOLATION, payload.toString().split(DELIMITER));
		req.start();
		
	}
	
	// Client Requesting method part - end

	/**
	 * The requesting thread class through TCP client socket
	 * @author Younghwan
	 *
	 */
	public class Requestor extends Thread{
		private Socket mSocket;
		private String mIpAddress;
		private String mSso;
		private int mPortNum;
		private String mReqType;
		private String[] mPayLoad;
		private Logger logger;
		/**
		 * Constructor
		 * @param ipAddress the destination IP address
		 * @param portNum the destination TCP port number
		 * @param sso The SSO ID of the destination(Not yours)
		 */
		public Requestor(String ipAddress, int portNum, String sso){
			this.mIpAddress = ipAddress;
			this.mPortNum = portNum;
			this.mSso = sso;
			this.logger = Logger.getInstance();
		}
		
		/**
		 * Initialize the information to be sent
		 * @param reqtype Set the request type(PREFIX of the packet)
		 * @param payload The data to be sent
		 */
		public void setRequest(String reqtype, String[] payload){
			this.mReqType = reqtype;
			this.mPayLoad = payload;
		}
		
		@Override
		public void run(){
			try {
				this.mSocket = new Socket(mIpAddress, mPortNum);
				processReq();
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
				logger.d(this, e.getMessage());
			}
		}

		private void processReq() throws IOException {
		    if(mReqType.equals(REQTYPE_TRST)){
		    	doCertRequest();
		    }else if(mReqType.equals(REQTYPE_FILE)){
		    	doFileRequest();
		    }else if(mReqType.equals(REQTYPE_REPORT_VIOLATION)){
		    	doReportViolation();
		    }
		}
		

		private void doCertRequest() throws IOException
		{
			PrintWriter out =
			        new PrintWriter(mSocket.getOutputStream(), true);
		    BufferedReader in =
		        new BufferedReader(
		            new InputStreamReader(mSocket.getInputStream()));
	    	StringBuilder payload = new StringBuilder(mReqType);
	    	payload.append(joinStrs(mPayLoad, DELIMITER));
		    out.write(payload.toString());
		    String result = in.readLine();
		    if(result.startsWith(RESPONSE_X509_CERT)){
		    	String[] payloads = result.split(DELIMITER);
		    	if(payloads.length > 1){
		    		String cert = payloads[1];
		    		try {
						X509Util x509Util = new X509Util(cert);
						String dn = x509Util.getCertificate().getIssuerDN().getName();
						String sso = X509Util.parseDN(dn)[0];
						CertInfo info = sql.getCertInfo(sso);
						if(info == null){
							info = new CertInfo(sso, cert);
							sql.pushCertInfo(info);
						}else{
							logger.d(this, "");
						}
					} catch (CertificateException e) {
						e.printStackTrace();
						logger.d(this, e.getMessage());
					}
		    	}else{
		    		logger.d(this, "Error while getting cert. Please try again.");
		    	}
		    }else{
		    	logger.d(this, result);
		    }
			in.close();
			out.close();
		}
		
		private void doFileRequest() throws IOException{
			String fileName = mPayLoad[1];
			PrintWriter out =
			        new PrintWriter(mSocket.getOutputStream(), true);
		    BufferedReader in =
		        new BufferedReader(
		            new InputStreamReader(mSocket.getInputStream()));
	    	StringBuilder payload = new StringBuilder(mReqType);
	    	payload.append(joinStrs(mPayLoad, DELIMITER));
		    out.write(payload.toString());
		    String result = in.readLine();
		    if(result.startsWith(RESPONSE_FILE_SIZE)){
		    	String[] payloads = result.split(DELIMITER);
		    	if(payloads.length > 3){
			    	long fileSize = Long.parseLong(payloads[1]);
			    	byte[] fileContents = StaticUtil.base64ToBytes(payloads[2]);
			    	int fileType = Integer.parseInt(payloads[3]);
		    		if(fileSize == fileContents.length){
		    			Date now = new Date();
		    			String today = StaticUtil.daysAfter(now, 0);
		    			String expiresOn = StaticUtil.daysAfter(now, 1);
		    			sql.pushFileInfo(Paths.get(mHomeDirectory + "/" + fileName),
		    					mSso, today, expiresOn,
		    					fileType,
		    					SHA256Helper.getHashStringFromBytes(fileContents));
		    			StaticUtil.saveToFile(mHomeDirectory + fileName, fileContents);
		    		}else{
			    		logger.d(this, "File received is currupt. Please try again.");
		    		}
		    	}else{
		    		logger.d(this, "Error while getting file. Please try again.");
		    	}
		    }else{
		    	logger.d(this, result);			    	
		    }
			in.close();
			out.close();
		}
		
		private void doReportViolation() throws IOException {
			PrintWriter out =
			        new PrintWriter(mSocket.getOutputStream(), true);
		    BufferedReader in =
		        new BufferedReader(
		            new InputStreamReader(mSocket.getInputStream()));
	    	StringBuilder payload = new StringBuilder(mReqType);
			
		}
	}
	
	
	private static String joinStrs(String[] strings, String delimiter)
	{
		StringBuilder result = new StringBuilder();
		for(String item : strings){
			result.append(item).append(delimiter);
		}
		result.deleteCharAt(result.length()-1);
		return result.toString();
	}

	
}
