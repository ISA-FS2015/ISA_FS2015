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

public class CyborgTcpService extends Thread {
	private boolean isRunning = false;
	private ServerSocket mServerSocket;
	public CyborgTcpService(int portNum) throws IOException{
		super("TCP Thread");
		this.mServerSocket = new ServerSocket(portNum);
		this.isRunning = true;
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
    		X509Util x509Util = new X509Util(trust);
			x509Util.checkValidity();
    		// Get filename
    		String fileName = reqs[2];
    		// Send the file to the requestor
    		File file = new File(fileName);
    		if(file.exists()){
	            os.writeBytes("Size:" + Long.toString(file.length()));
	            String resp = is.readLine();
	            if("OK".equals(resp)){
	            	Files.copy(file.toPath(), os);
	            }
    		}else{
	            os.writeBytes("Error:NoFile");
    		}
		}
		public void doIssueCertificates(BufferedReader is, DataOutputStream os, String[] reqs) throws IOException, GeneralSecurityException{
    		boolean isTrustworthy = false;
    		// Ask the required information from the trustee
    		
    		if(isTrustworthy){
        		// If qualified, issue a X509 Cert.
    			KeyPairGenerator keyGen;
				keyGen = KeyPairGenerator.getInstance(CyborgSecurity.ALGORITHM);
    			keyGen.initialize(1024);
    			final KeyPair pair = keyGen.generateKeyPair();
    			X509Certificate issuedX509 = X509Util.issueCertificate("CN=CyborgCoop, L=Kansas City, C=US",
    					pair, 31, "SHA1withRSA");
    			PrivateKey prk = pair.getPrivate();
    			PublicKey pbk = pair.getPublic();
    			os.writeBytes("X509:");
    			os.write( issuedX509.getEncoded() );
    			os.flush();
    		}
		}

	}
}
