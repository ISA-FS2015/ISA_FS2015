package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;

import com.almworks.sqlite4java.SQLiteException;

public class CyborgController implements IWatchDirHandler{
	// Singleton - Start
	
	private volatile static CyborgController mController;
	
	public static CyborgController getInstance(String userName) throws IOException, SQLiteException{
		if(mController == null){
			synchronized(CyborgController.class){
				if(mController == null){
					mController = new CyborgController(userName);
				}
			}
		}
		return mController;
	}
	
	// Singleton - End

	CyborgUdpThread udpThread;
	CyborgFtpServer ftpServer;
	WatchDir watchDir;
    SQLiteInstance sql;
	private boolean isInit = false;
	
	
	private CyborgController(String userName) throws IOException, SQLiteException{
		sql = SQLiteInstance.getInstance();
		ftpServer = new CyborgFtpServer();
		watchDir = new WatchDir(Resources.WORK_DIR, true, this);
		udpThread = new CyborgUdpThread("UDPThread", userName, "wlan0");
	}
	
	public void init(){
		if(isInit){
			ftpServer.start();
			watchDir.start();
			udpThread.start();
			udpThread.reqJoinUser();
			isInit = true;
		}else{
			Logger.d(this, "Already initialized. Skipping...");
		}
	}
	
	public void closeService(){
		if(isInit){
			udpThread.stopThread();
			ftpServer.stopFtpServer();
			watchDir.stopService();
		}
	}
	
	public void cli(PrintStream out, InputStream in) throws IOException{
		if(!isInit){
			Logger.d(this, "Already initialized. Skipping...");
			return;
		}
		boolean session = true;
		while(session){
			// Get into CLI mode
			String[] cmds = getCommands(out, in);
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
		Logger.d(this, "Exitting...... Byebye!");
	}
	
	public static String[] getCommands(PrintStream out, InputStream in) throws IOException{
		out.print(">");
		InputStreamReader cin = new InputStreamReader(in);
		char[] buf = new char[1024];
		cin.read(buf);
		String inputLine = new String(buf);
		return inputLine.trim().split(" ");
	}
	


	@Override
	public void onRegisterCallback(Path dir) {
		// TODO Auto-generated method stub
	}


	@Override
	public void onFileCreated(Path child) {
    	String filePath = child.toString();
		Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
		try {
	        SQLiteInstance sql = SQLiteInstance.getInstance();
	        if(sql.getFileHash(child) == null){
	        	sql.pushFileInfo(child, Resources.CYBORG_FILE_TYPE_ORIGINAL);
	        }
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}


	@Override
	public void onFileModified(Path child) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onFileDeleted(Path child) {
		// TODO Auto-generated method stub
		
	}
}
