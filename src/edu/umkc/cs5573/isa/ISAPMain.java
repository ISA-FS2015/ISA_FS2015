package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;

public class ISAPMain {

	public static void main(String[] args) {
		localPrint("Cyborg test program.");

		CyborgUdpThread udpThread;
		CyborgFtpServer ftpServer;
		WatchDir watchDir;
		//String tobeHashed = "Test!!";
		//System.out.println("Hash of " + tobeHashed + " : " + SHA256Helper.getHashString(tobeHashed));
		localPrint("Starting FTP server...");
		ftpServer = new CyborgFtpServer();
		ftpServer.start();
		try {
			localPrint("Starting Directory Watcher...");
			watchDir = new WatchDir("res/home/cyborgman", true);
			watchDir.start();
			localPrint("Starting UDP Service...");
			udpThread = new CyborgUdpThread("UDPThread", args[1]);
			udpThread.start();
			udpThread.reqJoinUser();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	public static void localPrint(String msg){
		System.out.println(msg);
	}
}
