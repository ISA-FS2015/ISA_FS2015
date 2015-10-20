package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.security.cert.CertificateException;
import java.util.LinkedList;
import java.util.Queue;

public class CyborgTcpService extends Thread {
	private boolean isRunning = false;
	private ServerSocket mServerSocket;
	public CyborgTcpService(int portNum) throws IOException{
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
			            	}else if("REQ_TRST".equals(reqs[0])){
			            		
			            	}
			            }
			} catch (IOException | CertificateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					os.writeBytes("Error:" + e.getMessage());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}
}
