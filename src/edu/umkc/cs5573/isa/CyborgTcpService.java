package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.Queue;

import com.almworks.sqlite4java.SQLiteException;

public class CyborgTcpService extends Thread {
	private final static int BASE_SCORE = 10;
	private boolean isRunning = false;
	private ServerSocket mServerSocket;
	private String mUserName;
	private String mHomeDirectory;
	private SQLiteInstance sql;
	public CyborgTcpService(int portNum, String userName, String homeDirectory)
			throws IOException, SQLiteException{
		super("TCP Thread");
		this.mServerSocket = new ServerSocket(portNum);
		this.mUserName = userName;
		this.mHomeDirectory = homeDirectory;
		this.isRunning = true;
		this.sql = SQLiteInstance.getInstance();
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
		private static final int BUF_SIZE = 8192;
		private static final int PREFIX = 8;
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
	            	String[] reqs = recvd.split("#");
	            	if("REQ_FILE".equals(reqs[0])){
	            		doFileTransferProcess(is, os, reqs);
	            	}else if("REQ_TRST".equals(reqs[0])){
	            		doIssueCertificates(is, os, reqs);
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
		public void doFileTransferProcess(BufferedReader is, DataOutputStream os, String[] reqs) throws CertificateException, IOException{
    		// Do some trust process
    		String trust = reqs[1];
    		X509Util x509Util = new X509Util(StaticUtil.base64ToBytes(trust));
    		// Checking if the user is trusted by me
    		String sso = X509Util.parseDN(x509Util.getCertificate().getSubjectDN().getName())[0];
    		UserInfo info = sql.getUserInfo(sso);
    		if(info == null){
                os.writeBytes("Error#InvalidTrustee");
                return;
    		}
    		// Checking if the cert is valid
			if(!x509Util.isCertValid(x509Util.getCertificate().getPublicKey(), mUserName, info.getSso())){
                os.writeBytes("Error#InvalidCert");
                return;
			}
    		// Get filename
    		String fileName = reqs[2];
    		// Send the file to the requestor
    		File file = new File(fileName);
    		if(file.exists()){
	            os.writeBytes("Size#" + Long.toString(file.length()) + "#" + StaticUtil.encodeFileToBase64Binary(fileName));
    		}else{
	            os.writeBytes("Error#NoFile");
    		}
		}
		public void doIssueCertificates(BufferedReader is, DataOutputStream os, String[] reqs) throws IOException, GeneralSecurityException{
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
    			os.writeBytes("X509#");
    			//os.write( issuedX509.getEncoded() );
    			os.writeBytes(StaticUtil.byteToBase64(issuedX509.getEncoded()));
    			os.flush();
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
				//if the info is true{
				return true;
				//}else{
				// return false;
				//}
			}
			else{
				return false;
			}
		}
	}
}
