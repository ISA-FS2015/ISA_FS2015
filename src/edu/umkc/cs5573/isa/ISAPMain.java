package edu.umkc.cs5573.isa;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;

public class ISAPMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String tobeHashed = "Test!!";
		System.out.println("Hash of " + tobeHashed + " : " + SHA256Helper.getHashString(tobeHashed));
		FtpServerFactory serverFactory = new FtpServerFactory();
		FtpServer server = serverFactory.createServer();
		// start the server
		try {
			server.start();
		} catch (FtpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
