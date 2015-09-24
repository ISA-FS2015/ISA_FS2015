package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;

import com.almworks.sqlite4java.SQLiteException;

public class ISAPMain {
	final static String WORK_DIR = Resources.WORK_DIR;

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
			watchDir = new WatchDir(Resources.WORK_DIR, true);
			watchDir.start();
			localPrint("Starting UDP Service...");
			if(args[0] == null){
				String userName = System.getProperty("user.name");
				udpThread = new CyborgUdpThread("UDPThread", userName , "wlan0");
			}else{
				udpThread = new CyborgUdpThread("UDPThread", args[0], "wlan0");
			}
			udpThread.start();
			udpThread.reqJoinUser();
			boolean session = true;
			while(session){
				// Get into CLI mode
				String[] cmds = getCommands(System.out, System.in);
				if("byebye".equals(cmds[0])){
					session = false;
				}
				else if ("connect".equals(cmds[0])){
					
				}else if("sql".equals(cmds[0])){
					StringBuilder state = new StringBuilder();
					for(int i = 1; i < cmds.length ; i++){
						state.append(cmds[i]);
						if(i != cmds.length -1){
							state.append(" ");
						}
					}
		    		try {
		    	        SQLiteInstance sql = SQLiteInstance.getInstance();
		    	        sql.execSql(state.toString());
		    		} catch (SQLiteException e) {
		    			// TODO Auto-generated catch block
		    			e.printStackTrace();
		    		}

				}
			}
			udpThread.stopThread();
			ftpServer.stopFtpServer();
			watchDir.stopService();
			localPrint("Exitting...... Byebye!");
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static String[] getCommands(PrintStream out, InputStream in) throws IOException{
		out.print(">");
		InputStreamReader cin = new InputStreamReader(in);
		char[] buf = new char[1024];
		cin.read(buf);
		String inputLine = new String(buf);
		return inputLine.trim().split(" ");
	}
	
	public static void localPrint(String msg){
		System.out.println(msg);
	}
}
