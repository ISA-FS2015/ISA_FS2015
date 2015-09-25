package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CyborgController implements IWatchDirHandler{
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
	final static int EXPIRES_DAYS_COPIED = 1;
	final static int EXPIRES_DAYS_OWNER = 3650; // 10 years for the owner

	// Singleton - Start
	
	private volatile static CyborgController mController;
	
	public static CyborgController getInstance(String userName) throws IOException{
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
	private boolean isDisposed = false;
	
	
	private CyborgController(String userName) throws IOException{
//		sql = SQLiteInstance.getInstance();
		ftpServer = new CyborgFtpServer();
		watchDir = new WatchDir(Resources.WORK_DIR, true, this);
		udpThread = new CyborgUdpThread("UDPThread", userName, "wlan0");
		sql = new SQLiteInstance();
	}
	
	public void init(){
		if(!isInit){
			ftpServer.start();
			watchDir.start();
			udpThread.start();
			udpThread.reqJoinUser();
			isInit = true;
			//new SQLiteInstance().init();
		}else if(isDisposed){
			Logger.d(this, "Already disposed. Skipping...");
		}else{
			Logger.d(this, "Already initialized. Skipping...");
		}
	}
	
	public void closeService(){
		if(isInit){
			udpThread.stopThread();
			ftpServer.stopFtpServer();
			watchDir.stopService();
			try {
				sql.dispose();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isDisposed = true;
		}
	}
	
	public void cli(PrintStream out, InputStream in) throws IOException{
		if(!isInit || isDisposed){
			Logger.d(this, "Not initialized or already disposed. Skipping...");
			return;
		}
		boolean session = true;
		while(session){
			// Get into CLI mode
			String[] cmds = getCommands(out, in);
			if(cmds.length == 0) continue;
			if("byby".equals(cmds[0])){
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
    			sql.execSql(state.toString());
//	    		try {
//	    	        SQLiteInstance sql = SQLiteInstance.getInstance();
//	    	        sql.execSql(state.toString());
//	    		} catch (SQLiteException e) {
//	    			e.printStackTrace();
//	    		}
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
	public void onFileCreated(Path child) {
    	//String filePath = child.toString();
		Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.add(Calendar.DATE, EXPIRES_DAYS_OWNER);
		Date expiresOn = cal.getTime();
		//new SQLiteInstance().pushFileInfo(child,
		FileInfo info = sql.getFileInfo(child);
		if(info == null || info.getType() == Resources.CYBORG_FILE_TYPE_ORIGINAL){
			sql.pushFileInfo(child,
					dateFormat.format(now),
					dateFormat.format(expiresOn),
					Resources.CYBORG_FILE_TYPE_ORIGINAL|Resources.CYBORG_FILE_WRITE_ALLOWED,
					SHA256Helper.getHashStringFromFile(child));
			Logger.d(this, "Fileinfo successfully created");
		}
		
		// TODO Make files permission 000 so that no one except Superuser can read, write, or execute it.
		// For Later step!!
	}


	@Override
	public void onFileModified(Path child) {
    	if(child.toFile().exists()){
        	//String filePath = child.toString();
    		Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
    		FileInfo info = sql.getFileInfo(child);
    		if(info == null){
    			Logger.d(this, "No fileinfo. Creating...");
    			Date now = new Date();
    			Calendar cal = Calendar.getInstance();
    			cal.setTime(now);
    			cal.add(Calendar.DATE, EXPIRES_DAYS_OWNER);
    			Date expiresOn = cal.getTime();
    			//new SQLiteInstance().pushFileInfo(child,
    			sql.pushFileInfo(child,
    					dateFormat.format(now),
    					dateFormat.format(expiresOn),
    					Resources.CYBORG_FILE_TYPE_ORIGINAL|Resources.CYBORG_FILE_WRITE_ALLOWED,
    					SHA256Helper.getHashStringFromFile(child));
    		}else{
    			if(!SHA256Helper.getHashStringFromFile(child).equals(info.getHash())){
    				int copied_and_protected = Resources.CYBORG_FILE_TYPE_COPIED | Resources.CYBORG_FILE_WRITE_PROTECTED;
    				if((info.getType()&copied_and_protected) == copied_and_protected){
    					// The user violates the access rule! File should be deleted!!
        				Logger.d(this, "Alert! File contents has been attemped to be changed!! Deleting");
        				child.toFile().delete();
        				// TODO Report this message to the original owner!!
    				}
    			}
    		}
    	}		
	}


	@Override
	public void onFileDeleted(Path child) {
    	if(child.toFile().exists()){
    		Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
    		sql.deleteFileInfo(child);
    	}		
	}

	@Override
	public void onRegisterCallback(Path dir) {
		//new SQLiteInstance().pushFileInfo(dir, Resources.CYBORG_FILE_TYPE_ORIGINAL);
//		try {
//	        SQLiteInstance sql = SQLiteInstance.getInstance();
//	        if(sql.getFileHash(dir) == null){
//	        	sql.pushFileInfo(dir, Resources.CYBORG_FILE_TYPE_ORIGINAL);
//	        }
//		} catch (SQLiteException e) {
//			e.printStackTrace();
//		}
	}


	@Override
	public void onRegisterCallback(List<Path> dirs) {
		// TODO Auto-generated method stub
		
	}
}
