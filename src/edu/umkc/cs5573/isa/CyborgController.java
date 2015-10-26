package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.almworks.sqlite4java.SQLiteException;

public class CyborgController implements IWatchDirHandler{
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	final static int EXPIRES_DAYS_COPIED = 1;
	final static int EXPIRES_DAYS_OWNER = 3650; // 10 years for the owner
	final static int UDP_PORT = 55730;
	final static int TCP_PORT = 55731;

	// Singleton - Start
	
	private volatile static CyborgController mController;
	
	public static CyborgController getInstance(String userName, String ifName, String homeDirectory)
			throws IOException, SQLiteException{
		if(mController == null){
			synchronized(CyborgController.class){
				if(mController == null){
					mController = new CyborgController(userName, ifName, homeDirectory);
				}
			}
		}
		return mController;
	}
	
	// Singleton - End

	CyborgUdpThread udpThread;
	CyborgTcpService tcpService;
//	CyborgFtpServer ftpServer;
	WatchDir watchDir;
	WatchFileExpiration watchFileExpiration;
	SQLiteInstance sql;
	private boolean isInit = false;
	private boolean isDisposed = false;
	private String homeDirectory;
	private Logger logger;

	
	private CyborgController(String userName, String ifName, String homeDirectory)
			throws IOException, SQLiteException{
		this.logger = Logger.getInstance();
		this.sql = SQLiteInstance.getInstance();
		//this.ftpServer = new CyborgFtpServer();
		this.watchDir = new WatchDir(homeDirectory, true, this);
		this.watchFileExpiration = new WatchFileExpiration("FileExpirationWatcher", sql);
		this.homeDirectory = homeDirectory;
		this.udpThread = new CyborgUdpThread("UDPThread", userName, ifName, homeDirectory);
		this.tcpService = new CyborgTcpService(TCP_PORT);
	}
	
	public void init(){
		if(!isInit){
//			ftpServer.start();
			watchDir.start();
			watchFileExpiration.start();
			udpThread.start();
			tcpService.start();
			isInit = true;
			//new SQLiteInstance().init();
		}else if(isDisposed){
			logger.d(this, "Already disposed. Skipping...");
		}else{
			logger.d(this, "Already initialized. Skipping...");
		}
	}
	
	public void closeService(){
		if(isInit){
			udpThread.stopThread();
			tcpService.stopService();
//			ftpServer.stopFtpServer();
			watchDir.stopService();
			watchFileExpiration.stopThread();
			try {
				sql.dispose();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isDisposed = true;
		}
	}
	
	public void cli(PrintStream out, InputStream in) throws IOException{
		logger.setOutputStream(out);
		if(!isInit || isDisposed){
			logger.d(this, "Not initialized or already disposed. Skipping...");
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
			else if ("whohas".equals(cmds[0])){
				if(cmds.length >= 2) udpThread.reqFileProbe(cmds[1]);
			}
			else if ("requestFile".equals(cmds[0])){
				if(cmds.length >= 3){
					String ipAddress = cmds[1];
					String fileName = cmds[2];
					// TODO Using TCPClient get file!!
				}
			}
			else if ("maketestfile".equals(cmds[0])){
				makeTestFile();
			}else if("sql".equals(cmds[0])){
				StringBuilder state = new StringBuilder();
				for(int i = 1; i < cmds.length ; i++){
					state.append(cmds[i]);
					if(i != cmds.length -1){
						state.append(" ");
					}
				}
    			sql.execSql(state.toString());
			}
		}
		logger.d(this, "Exitting...... Byebye!");
		logger.resetOutputStream();
	}
	
	public void makeTestFile() throws IOException{
//		File testFile = new File(homeDirectory + "/test.txt");
		Path testPath = Paths.get(homeDirectory + "/test.txt");
		Files.write(testPath, "This is for test! It represents the copied file and should not be changed.".getBytes());
		Date now = new Date();
		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.add(Calendar.DATE, 1);
		Date expiresOn = cal.getTime();
		//new SQLiteInstance().pushFileInfo(child,
		FileInfo info = sql.getFileInfo(testPath);
		if(info != null) sql.deleteFileInfo(testPath);
		sql.pushFileInfo(testPath,
				dateFormat.format(now),
				dateFormat.format(expiresOn),
				Resources.CYBORG_FILE_TYPE_COPIED|Resources.CYBORG_FILE_WRITE_PROTECTED,
				SHA256Helper.getHashStringFromFile(testPath));
		logger.d(this, "Test Fileinfo successfully created");
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
		logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
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
			logger.d(this, "Fileinfo successfully created");
		}
		
		// TODO For Later step!!
		
	}


	@Override
	public void onFileModified(Path child) {
    	if(child.toFile().exists()){
        	//String filePath = child.toString();
    		logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
    		FileInfo info = sql.getFileInfo(child);
    		if(info == null){
    			logger.d(this, "No fileinfo. Creating...");
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
    					logger.d(this, "Alert! File contents has been attemped to be changed!! Deleting");
        				child.toFile().delete();
        				// TODO Report this message to the original owner!!
        				
    				}else{
    					// This file is original. Update the file info
    					sql.updateFileInfo(child, dateFormat.format(info.getExpiresOn()), info.getType(), SHA256Helper.getHashStringFromFile(child));
    				}
    			}
    		}
    	}		
	}


	@Override
	public void onFileDeleted(Path child) {
    	if(!child.toFile().exists()){
    		//Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
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
		
	}
}
