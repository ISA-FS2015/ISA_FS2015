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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import com.almworks.sqlite4java.SQLiteException;

/**
 * The actual TCP communication is processed here.
 * @author Younghwan
 *
 */
public class CyborgTcpService extends Thread {
	/**
	 * The default score for trust.
	 */
	private final static int BASE_SCORE = 100;
	/**
	 * The score increment. Inversely used as decrement by inversion
	 */
	private final static int SCORE_INCREMENT = 10;
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
	private CyborgController controller;
	private ServerSocket mServerSocket;
	private String mUserName;
	private String mHomeDirectory;
	private SQLiteInstance sql;
	private Logger logger;
	private MessageQueue mQueue;
	public CyborgTcpService(String threadName, CyborgController controller, int portNum)
			throws IOException, SQLiteException{
		super(threadName);
        this.controller = controller;
		this.mServerSocket = new ServerSocket(portNum);
		this.mUserName = controller.getUserName();
		this.mHomeDirectory = controller.getHomeDirectory();
		this.isRunning = true;
		this.sql = SQLiteInstance.getInstance();
		this.logger = Logger.getInstance();
		this.mQueue = MessageQueue.getInstance();
	}
	
	public void run(){
		while(isRunning && !mServerSocket.isClosed()){
			try {
				Socket clientSocket = mServerSocket.accept();
				// For Multi-socket programming
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
	            if(recvd == null) return;
	            if(recvd.length() >= PREFIX){
	            	String[] reqs = recvd.split(DELIMITER);
	            	if(REQTYPE_FILE.equals(reqs[0])){
	            		os.writeBytes(doFileTransferProcess(reqs) + "\n");
	        		    os.flush();
	            	}else if(REQTYPE_TRST.equals(reqs[0])){
	            		os.writeBytes(doIssueCertificates(reqs) + "\n");
	        		    os.flush();
	            	}else if(REQTYPE_REPORT_VIOLATION.equals(reqs[0])){
	            		os.writeBytes(doReaction(reqs) + "\n");
	        		    os.flush();
	            	}
	            }
			} catch (IOException | GeneralSecurityException e) {
				e.printStackTrace();
				try {
					os.writeBytes(RESPONSE_ERROR + DELIMITER + e.getMessage() + "\n");
				} catch (IOException e1) {
					e1.printStackTrace();
					isRunning = false;
				}
			}
		}
		
		/**
		 * <p>Make a reaction of the violation report</p>
		 * <p>If owner react for deleting, deletes file remotely, decrementing the trust score.</p>
		 * <p>If owner react for restoring, restores file remotely, slightly decrementing the trust score.</p>
		 * <p>If owner react for nothing, just allows the file modified, no score changed</p>
		 * @param reqs
		 * @return
		 */
		private String doReaction(String[] reqs)
		{
			String sso = reqs[0];
			String name = reqs[1];
			String fileName = reqs[2];
			String access = reqs[3];
			String msg = "The prohibited access detected from the following user:"
					+ "\nSSO: " + sso
					+ "\nName: " + name
					+ "\nAccess attempted: " + access
					+ "\nPlease type 'react delete " + fileName + " from " + sso + "' to delete remotely,"
					+ "\nor 'react restore " + fileName + " from " + sso + "' to restore remotely,"
					+ "\nor 'react allow " + fileName + " from " + sso + "' to do nothing.";
			logger.d(this, msg);
			controller.increaseReactReq();
			msg = null;
			String response = "";
			while(msg == null){
				try {
					msg = mQueue.getFirstMessage();
					if(msg == null) continue;
					if(msg.startsWith("react delete ")){
						synchronized(mQueue){
							mQueue.deque();
							controller.decreaseReactReq();
						}
						String[] cmds = msg.split(" ");
						if(cmds.length > 4){
							if(cmds[2].equals(fileName) &&
								cmds[3].equals("from") &&
								cmds[4].equals(sso)){
				    			// Decrements the user's trust
				    			UserInfo info = sql.getUserInfo(sso);
				    			info.setScore(info.getScore() - SCORE_INCREMENT);
				    			sql.updateUserInfo(info);
								// Delete the file remotely
								response = RESPONSE_REACTION + DELIMITER + REACTION_DELETE + DELIMITER + fileName;
							}
						}
					}else if(msg.startsWith("react restore ")){
						synchronized(mQueue){
							mQueue.deque();
							controller.decreaseReactReq();
						}
						String[] cmds = msg.split(" ");
						if(cmds.length > 4){
							if(cmds[2].equals(fileName) &&
								cmds[3].equals("from") &&
								cmds[4].equals(sso)){
				    			// Decrements the user's trust
				    			UserInfo info = sql.getUserInfo(sso);
				    			info.setScore(info.getScore() - SCORE_INCREMENT/2);
				    			sql.updateUserInfo(info);
								// Restore the file remotely
								String fileBase64;
								try {
									fileBase64 = StaticUtil.encodeFileToBase64Binary(fileName);
									File file = new File(mHomeDirectory + "/" + fileName);
									response = RESPONSE_REACTION + DELIMITER
											+ REACTION_RESTORE + DELIMITER
											+ fileName + DELIMITER
											+ Long.toString(file.length()) + DELIMITER
											+ fileBase64;
								} catch (IOException e) {
									e.printStackTrace();
									logger.d(this, e.getMessage());
								}
							}
						}
					}else if(msg.startsWith("react allow ")){
						synchronized(mQueue){
							mQueue.deque();
							controller.decreaseReactReq();
						}
						String[] cmds = msg.split(" ");
						if(cmds.length > 4){
							if(cmds[2].equals(fileName) &&
								cmds[3].equals("from") &&
								cmds[4].equals(sso)){
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
		
		/**
		 * <p>File Transfer process</p>
		 * <p>The owner checks the X509 certificate's validation, if the trust score's sufficient, finally gives the file</p>
		 * @param reqs
		 * @return
		 * @throws CertificateException
		 * @throws IOException
		 */
		public String doFileTransferProcess(String[] reqs)
				throws CertificateException, IOException{
    		// Get filename
    		String fileName = reqs[1];
    		// Do some trust process
    		String trust = reqs[2];
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
    		// Send the file to the requestor
    		File file = new File(mHomeDirectory + "/" + fileName);
    		if(file.exists()){
    			if(info.getScore() >= BASE_SCORE){
        			FileInfo fileInfo = sql.getFileInfo(file.toPath());
    	            return RESPONSE_FILE_SIZE + DELIMITER
    	            		+ Long.toString(file.length()) + DELIMITER
    	            		+ StaticUtil.encodeFileToBase64Binary(file.toPath().toString()) +DELIMITER
    	            		+ fileInfo.getType();
    			}else{
    				return RESPONSE_ERROR+DELIMITER+"Insufficient Trust Score";
    			}
    		}else{
	            return RESPONSE_ERROR+DELIMITER+"NoFile";
    		}
		}
		
		/**
		 * Issueing Certification process
		 * @param reqs
		 * @return
		 * @throws IOException
		 * @throws GeneralSecurityException
		 */
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
		
		/**
		 * Inserts user info into DB
		 * @param reqs
		 * @param prk Private Key. Not used for now, but just in case...
		 * @param pbk Public Key
		 */
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
		
		/**
		 * Validates if the user is correct or not.
		 * @param reqs
		 * @return
		 */
		private boolean validateUser(String[] reqs)
		{
			// the index start from 1 because 0 is used by prefix
			if(reqs.length == 7){
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
						+ "\nPlease type 'cert allow " + sso + "' to allow,"
						+ "\notherwise 'cert revoke " + sso + "' to revoke.";
				logger.d(this, msg);
				controller.increaseCertReq();
				msg = null;
				while(msg == null){
					try {
						msg = mQueue.getFirstMessage();
						if(msg == null) continue;
						if(msg.startsWith("cert allow ")){
							synchronized(mQueue){
								mQueue.deque();
								controller.decreaseCertReq();
							}
							String[] cmds = msg.split(" ");
							if(cmds.length > 1){
								String requestor = cmds[2];
								if(sso.equals(requestor)){
									return true;
								}
							}
						}else if(msg.startsWith("cert revoke ")){
							synchronized(mQueue){
								mQueue.deque();
								controller.decreaseCertReq();
							}
							String[] cmds = msg.split(" ");
							if(cmds.length > 1){
								String requestor = cmds[1];
								if(sso.equals(requestor)){
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
	
	public void reqCert(String ipAddress, int portNum, String sso, UserInfo myInfo)
	{
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
	
	public void reqFile(String ipAddress, int portNum, String sso, String fileName)
	{
		CertInfo cert = sql.getCertInfo(sso);
		if (cert == null){
			logger.d(this, "No certification corresponding to the owner. Please get a trust first.");
		}else{
			String certStr = cert.getCert();
			Requestor req = new Requestor(ipAddress, portNum, sso);
			StringBuilder payload = new StringBuilder();
			payload.append(fileName).append(DELIMITER)
					.append(certStr);
			req.setRequest(REQTYPE_FILE, payload.toString().split(DELIMITER));
			req.start();
		}
	}
	
	public void reportViolation(String ipAddress, int portNum, String sso, String fileName, String violation)
	{
		StringBuilder payload = new StringBuilder();
		payload.append(fileName).append(DELIMITER)
				.append(sso).append(DELIMITER)
				.append(violation);
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
		/**
		 * The client socket
		 */
		private Socket mSocket;
		/**
		 * The destination IP Address
		 */
		private String mIpAddress;
		/**
		 * The SSO ID of the destination(Not yours)
		 */
		private String mSso;
		/**
		 * The destination TCP port number
		 */
		private int mPortNum;
		/**
		 * The request type
		 */
		private String mReqType;
		/**
		 * The data payload
		 */
		private String[] mPayLoad;
		/**
		 * Logger
		 */
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

		/**
		 * Process the request
		 * @throws IOException
		 */
		private void processReq() throws IOException {
		    if(mReqType.equals(REQTYPE_TRST)){
		    	doCertRequest();
		    }else if(mReqType.equals(REQTYPE_FILE)){
		    	doFileRequest();
		    }else if(mReqType.equals(REQTYPE_REPORT_VIOLATION)){
		    	doReportViolation();
		    }
		}
		

		/**
		 * Certification Request method
		 * @throws IOException
		 */
		private void doCertRequest() throws IOException
		{
			PrintWriter out =
			        new PrintWriter(mSocket.getOutputStream(), true);
		    BufferedReader in =
		        new BufferedReader(
		            new InputStreamReader(mSocket.getInputStream()));
	    	StringBuilder payload = new StringBuilder(mReqType).append(DELIMITER);
	    	payload.append(joinStrs(mPayLoad, DELIMITER)).append("\n");
	    	logger.d(this, "Sending:" + payload.toString());
		    out.write(payload.toString());
		    out.flush();
		    String result = in.readLine();
	    	logger.d(this, "Received:" + result);
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
		
		/**
		 * <p>File Request method</p>
		 * <p>If file is received, if the user info exists, raise the trust score for the owner's sharing the file</p>
		 * @throws IOException
		 */
		private void doFileRequest() throws IOException
		{
			String fileName = mPayLoad[0];
			PrintWriter out =
			        new PrintWriter(mSocket.getOutputStream(), true);
		    BufferedReader in =
		        new BufferedReader(
		            new InputStreamReader(mSocket.getInputStream()));
	    	StringBuilder payload = new StringBuilder(mReqType).append(DELIMITER);
	    	payload.append(joinStrs(mPayLoad, DELIMITER)).append("\n");
	    	logger.d(this, "Sending:" + payload.toString());
		    out.write(payload.toString());
		    out.flush();
		    String result = in.readLine();
	    	logger.d(this, "Received:" + result);
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
		    			StaticUtil.saveToFile(mHomeDirectory + "/" + fileName, fileContents);
		    			// Retrieving file finished. If we have the sso who gave file,
		    			// Then we will raise his/her score by 10
		    			UserInfo info = sql.getUserInfo(mSso);
		    			if(info != null){
			    			info.setScore(info.getScore() + SCORE_INCREMENT);
			    			sql.updateUserInfo(info);
		    			}
		    		}else{
			    		logger.d(this, "File received is currupted. Please try again.");
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
		
		/**
		 * Report Violation method
		 * @throws IOException
		 */
		private void doReportViolation() throws IOException
		{
			PrintWriter out =
			        new PrintWriter(mSocket.getOutputStream(), true);
		    BufferedReader in =
		        new BufferedReader(
		            new InputStreamReader(mSocket.getInputStream()));
	    	StringBuilder payload = new StringBuilder(mReqType).append(DELIMITER);
	    	payload.append(joinStrs(mPayLoad, DELIMITER)).append("\n");
	    	logger.d(this, "Sending:" + payload.toString());
		    out.write(payload.toString());
		    out.flush();
		    String result = in.readLine();
	    	logger.d(this, "Received:" + result);
		    if(result.startsWith(RESPONSE_REACTION)){
		    	String[] reaction = result.split(DELIMITER);
		    	if(reaction.length > 1){
		    		if(reaction[1].equals(REACTION_DELETE)){
		    			String fileName = reaction[2];
		    			File file = new File(mHomeDirectory + "/" + fileName);
		    			file.delete();
		    		}else if(reaction[1].equals(REACTION_RESTORE)){
		    			String fileName = reaction[2];
		    			Path filePath = Paths.get(mHomeDirectory + "/" + fileName);
		    			//long fileSize = Long.parseLong(reaction[3]);
		    			byte[] fileContent = StaticUtil.base64ToBytes(reaction[4]);
		    			FileInfo info = sql.getFileInfo(filePath);
		    			sql.updateFileInfo(filePath,
		    					info.getExpiresOnStr(), info.getType(),
		    					SHA256Helper.getHashStringFromBytes(fileContent), FileInfo.UNLOCK);
		    			Files.write(filePath, fileContent, StandardOpenOption.WRITE);
		    		}else if(reaction[1].equals(REACTION_ALLOW)){
		    			// Just unlock file and update the hash
		    			String fileName = reaction[2];
		    			Path filePath = Paths.get(mHomeDirectory + "/" + fileName);
		    			FileInfo info = sql.getFileInfo(filePath);
		    			sql.updateFileInfo(filePath, info.getExpiresOnStr(), info.getType(),
		    					SHA256Helper.getHashStringFromFile(filePath), FileInfo.UNLOCK);
		    		}else{
				    	logger.d(this, result);
		    		}
		    	}
		    }else{
		    	logger.d(this, result);
		    }
		}
	}
	
	/**
	 * Join string arrays into one string separated by a delimiter
	 * @param strings
	 * @param delimiter
	 * @return
	 */
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
