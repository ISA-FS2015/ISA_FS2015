package edu.umkc.cs5573.isa;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.almworks.sqlite4java.SQLiteException;

/**
 * <p>The wrapper class of actual Cyborg Security logic process.</p>
 * <p>It implements {@code IWatchDirHandler} so that the file change event will be handled here</p>
 * @see IWatchDirHandler
 * @author Younghwan
 *
 */
public class CyborgController implements IWatchDirHandler, IWatchExpHandler{
	
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	final static int EXPIRES_DAYS_COPIED = 1;
	final static int EXPIRES_DAYS_OWNER = 3650; // 10 years for the owner
	private ICyborgEventHandler mHandler = null;
	private static UserInfo myInfo;
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
	CyborgSocketManager mSocketManager;
//	CyborgFtpServer ftpServer;
	WatchDir watchDir;
	WatchFileExpiration watchFileExpiration;
	SQLiteInstanceAbstract sql;
	private boolean isInit = false;
	private boolean isDisposed = false;
	private int certReqTriggered = 0;
	private int reactReqTriggered = 0;
	/**
	 * The user name(SSO) which corresponds to my name
	 */
	private String userName;
	private String homeDirectory;
	private Logger logger;
	private MessageQueue mQueue;

	/**
	 * Constructor
	 * @param userName User Name(SSO)
	 * @param ifName Interface name(e.g. wlan0)
	 * @param homeDirectory Directory of files saved
	 * @throws IOException
	 * @throws SQLiteException
	 */
	private CyborgController(String userName, String ifName, String homeDirectory)
			throws IOException, SQLiteException{
		this.logger = Logger.getInstance();
		this.sql = SQLiteInstance.getInstance();
		//this.ftpServer = new CyborgFtpServer();
		this.watchDir = new WatchDir(homeDirectory, true, this);
		this.watchFileExpiration = new WatchFileExpiration("FileExpirationWatcher", sql);
		this.userName = userName;
		this.homeDirectory = homeDirectory;
		this.mSocketManager = new CyborgSocketManager(this, userName, ifName, homeDirectory, Resources.UDP_PORT, Resources.TCP_PORT, false, sql);
		this.mQueue = MessageQueue.getInstance();
	}
	
	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the homeDirectory
	 */
	public String getHomeDirectory() {
		return homeDirectory;
	}

	/**
	 * @param homeDirectory the homeDirectory to set
	 */
	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}
	
	

	/**
	 * @return the certReqTriggered
	 */
	public int getCertReqTriggered() {
		return certReqTriggered;
	}

	/**
	 * @param certReqTriggered the certReqTriggered to set
	 */
	public void setCertReqTriggered(int certReqTriggered) {
		this.certReqTriggered = certReqTriggered;
	}
	public int increaseCertReq(){
		return ++certReqTriggered;
	}
	public int decreaseCertReq(){
		return --certReqTriggered;
	}

	/**
	 * @return the reactReqTriggered
	 */
	public int getReactReqTriggered() {
		return reactReqTriggered;
	}

	/**
	 * @param reactReqTriggered the reactReqTriggered to set
	 */
	public void setReactReqTriggered(int reactReqTriggered) {
		this.reactReqTriggered = reactReqTriggered;
	}
	public int increaseReactReq(){
		return ++reactReqTriggered;
	}
	public int decreaseReactReq(){
		return --reactReqTriggered;
	}

	/**
	 * @return the mHandler
	 */
	public ICyborgEventHandler getEventHandler() {
		return mHandler;
	}

	/**
	 * @param mHandler the mHandler to set
	 */
	public void setEventHandler(ICyborgEventHandler mHandler) {
		this.mHandler = mHandler;
	}

	/**
	 * Initialization
	 */
	public void init(){
		if(!isInit){
//			ftpServer.start();
			watchDir.start();
			watchFileExpiration.start();
			mSocketManager.init();
			isInit = true;
			//new SQLiteInstance().init();
		}else if(isDisposed){
			logger.i(this, "Already disposed. Skipping...");
		}else{
			logger.i(this, "Already initialized. Skipping...");
		}
	}
	
	/**
	 * Closes services
	 */
	public void closeService(){
		if(isInit){
			mSocketManager.stopServices();
//			ftpServer.stopFtpServer();
			watchDir.stopService();
			watchFileExpiration.stopThread();
			try {
				((SQLiteInstance) sql).dispose();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isDisposed = true;
		}
	}
	
	/**
	 * Enters command line interface
	 * @param out stream from keyboard(or client socket)
	 * @param in stream to display(or client socket)
	 * @throws IOException
	 */
	public void cli(PrintStream out, InputStream in) throws IOException{
		logger.setOutputStream(out);
		if(!isInit || isDisposed){
			logger.i(this, "Not initialized or already disposed. Skipping...");
			if(mHandler != null) mHandler.onResultFailed(ICyborgEventHandler.NOT_INITIALIZED);
			return;
		}
		boolean session = true;
		logger.i(this, "Type 'help' anytime for getting the command list.");
		while(session){
			// Get into CLI mode
			String[] cmds = getCommands(out, in);
			if(cmds.length == 0) continue;
			if("byby".equals(cmds[0])){
				session = false;
			}else if ("userlist".equals(cmds[0])){
				doUserList();
			}else if ("whohas".equals(cmds[0].toLowerCase())){
				doRequestFileProbe(cmds);
			}else if ("requestfile".equals(cmds[0].toLowerCase())){
				doRequestFile(cmds);
			}else if ("requestcert".equals(cmds[0].toLowerCase())){
				doRequestCert(cmds);
			}else if ("setmyinfo".equals(cmds[0].toLowerCase())){
				doSetMyInfo(cmds);
			}else if ("setfile".equals(cmds[0].toLowerCase())){
				doSetFile(cmds);
			}else if ("maketestfile".equals(cmds[0].toLowerCase())){
				try {
					makeTestFile();
				} catch (FileTooBigException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if ("cert".equals(cmds[0].toLowerCase())){
				if(certReqTriggered > 0) mQueue.queue(StaticUtil.joinWith(cmds, " "));
			}else if ("react".equals(cmds[0].toLowerCase())){
				if(reactReqTriggered > 0) mQueue.queue(StaticUtil.joinWith(cmds, " "));
			}else if("sql".equals(cmds[0].toLowerCase())){
				if(Logger.DEBUG){
					doSql(cmds);
				}else{
					logger.i(this, "Permission denied.");
				}
			}else if ("help".equals(cmds[0].toLowerCase())){
				doCommandList();
			}else if ("".equals(cmds[0].toLowerCase())){
			}else{
				logger.i(this, "Unknown command.");
			}
		}
		logger.i(this, "Exitting...... Byebye!");
		if(mHandler != null) mHandler.onConnectionEnds();
		logger.resetOutputStream();
	}
	
	private void doCommandList() {
		StringBuilder sb = new StringBuilder();
		sb.append("---Available command list---\n")
			.append("byby\n")
			.append("userlist\n")
			.append("whohas <filename>\n")
			.append("requestFile <SSO> <filename>\n")
			.append("requestCert <SSO>\n")
			.append("setmyinfo <Student/Employee> <name> <organization> <e-mail> <phoneNumber>\n")
			.append("setfile <filename> <type:readwrite/readonly>\n");
		logger.i(this, sb.toString());
	}

	private void doUserList() {
		Map<String, String> userList = mSocketManager.getUserList();
    	for(Entry<String, String> entry : userList.entrySet()){
        	logger.i(this, entry.getKey() + ":" + entry.getValue());
    	}
		if(mHandler != null) mHandler.onUserListResult(userList);
	}

	/**
	 * Broadcast the file probe into the network. The response will be handled in {@code CyborgUdpService}
	 * @param cmds Commands
	 */
	public void doRequestFileProbe(String[] cmds)
	{
		if(cmds.length >= 2) mSocketManager.reqFileProbe(cmds[1]);
	}
	
	/**
	 * Request files into the destination The response will be handled in {@code CybordTcpService}
	 * @param cmds SSO, FileName
	 */
	private void doRequestFile(String[] cmds)
	{
		if(cmds.length >= 3){
			String sso = cmds[1];
			String fileName = cmds[2];
			mSocketManager.reqFile(sso, fileName);
		}
	}

	/**
	 * Request trust to destination
	 * @param cmds SSO
	 */
	private void doRequestCert(String[] cmds)
	{
		if(cmds.length >= 2){
			if(myInfo == null){
				logger.i(this, "My Info has not been set. Please set my info first using 'setmyinfo' command");
			}else{
				// Request x509 cert
				String sso = cmds[1];
				CertInfo info = sql.getCertInfo(sso);
				if(info == null){
					mSocketManager.reqCert(sso, myInfo);
				}else{
					Logger.getInstance().d(this, "The certificate is already exist. Skipping...");
				}
			}
		}
	}

	/**
	 * Sets my file's mode. The violation report will be sent if the prohibited action performed
	 * @param cmds FileName, Permission
	 */
	private void doSetFile(String[] cmds)
	{
		if(cmds.length >= 3){
			String fileName = cmds[1];
			String permission = cmds[2];
			if(permission.equals("readwrite")){
				Path file = Paths.get(homeDirectory + "/" + fileName);
				FileInfo info = sql.getFileInfo(file.toString());
				if(info != null){
					if((info.getType()&FileInfo.TYPE_ORIGINAL) == FileInfo.TYPE_ORIGINAL){
						info.setType(info.getType() | FileInfo.WRITE_ALLOWED);
						sql.updateFileInfo(file.toString(), info.getExpiresOnStr(),info.getType(),info.getHash(), FileInfo.UNLOCK);
					}
					else{
						logger.i(this, "Prohibited access. Reporting to the owner...");
						
        				mSocketManager.reportViolation(info.getOwner(), fileName, "ProhibitedModeChange");
					}
				}else{
					logger.i(this, "No file info. please check your filename or permission.");
				}
			}else if(permission.equals("readonly")){
				Path file = Paths.get(homeDirectory + "/" + fileName);
				FileInfo info = sql.getFileInfo(file.toString());
				if(info != null){
					if((info.getType() & FileInfo.TYPE_ORIGINAL) == FileInfo.TYPE_ORIGINAL){
						info.setType(info.getType() & FileInfo.WRITE_PROTECTED);
						sql.updateFileInfo(file.toString(), info.getExpiresOnStr(),info.getType(),info.getHash(), FileInfo.UNLOCK);
					}
					else{
						logger.i(this, "The owner allowed write permission, but do you really need to make it readonly??");
					}
				}else{
					logger.i(this, "No file info. please check your filename or permission.");
				}
			}
		}
	}

	/**
	 * Sets my information. It needs to be done before requesting certificates
	 * @param cmds Type, Name, Organization, Email, PhoneNumber
	 */
	private void doSetMyInfo(String[] cmds)
	{
		if(cmds.length >= 6){
			int type = 0;
			if(cmds[1].equals("Student")){
				type = UserInfo.TYPE_STUDENT;
			}else{
				type = UserInfo.TYPE_EMPLOYEE;
			}
			String name = cmds[2];
			String organization = cmds[3];
			String email = cmds[4];
			String phoneNumber = cmds[5];
			myInfo = new UserInfo(userName, type, name, organization, email, phoneNumber, 0, phoneNumber, phoneNumber);
		}else{
			logger.i(this, "Usage: setmyinfo <Type: Student/Employee> <Name> <Organization> <e-mail> <PhoneNumber>");
		}
	}

	/**
	 * <p>Make test file represents the copied from other peer.</p>
	 * <p>Note that it is used for testing</p>
	 * @throws IOException
	 * @throws FileTooBigException 
	 */
	public void makeTestFile() throws IOException, FileTooBigException
	{
		Path testPath = Paths.get(homeDirectory + "/test.txt");
		Files.write(testPath, "This is for test! It represents the copied file and should not be changed.".getBytes());
		Date now = new Date();
		String today = StaticUtil.daysAfter(now, 0);
		String expiresOn = StaticUtil.daysAfter(now, EXPIRES_DAYS_COPIED);
		//new SQLiteInstance().pushFileInfo(child,
		FileInfo info = sql.getFileInfo(testPath.toString());
		if(info != null) sql.deleteFileInfo(testPath.toString());
		sql.pushFileInfo(testPath.toString(), "TestOwner",
				today,
				expiresOn,
				FileInfo.TYPE_COPIED|FileInfo.WRITE_PROTECTED,
				SHA256Helper.getHashStringFromFile(testPath.toFile()));
		logger.i(this, "Test Fileinfo successfully created");
	}
	
	/**
	 * <p>Executes SQL query into SQLite DB.</p>
	 * <p>Note that it is used for debugging.</p>
	 * @param cmds SQL statements
	 */
	private void doSql(String[] cmds)
	{
		StringBuilder state = new StringBuilder();
		for(int i = 1; i < cmds.length ; i++){
			state.append(cmds[i]);
			if(i != cmds.length -1){
				state.append(" ");
			}
		}
		sql.execSql(state.toString());
	}

	/**
	 * Parses commands and split them into a String array
	 * @param out OutStream from keyboard or socket
	 * @param in InStream to display or socket
	 * @return The string array splitted by space
	 * @throws IOException
	 */
	private static String[] getCommands(PrintStream out, InputStream in)
			throws IOException{
		out.print(">");
		InputStreamReader cin = new InputStreamReader(in);
		char[] buf = new char[1024];
		cin.read(buf);
		String inputLine = new String(buf);
		return inputLine.trim().split(" ");
	}

	// File change events - start
	
	@Override
	public void onFileCreated(File child) {
    	//String filePath = child.toString();
		try {
			logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child.getPath()));
			Date now = new Date();
			String today = StaticUtil.daysAfter(now, 0);
			String expiresOn = StaticUtil.daysAfter(now, EXPIRES_DAYS_OWNER);
			//new SQLiteInstance().pushFileInfo(child,
			FileInfo info = sql.getFileInfo(child.getPath());
			if(info == null || info.getType() == FileInfo.TYPE_ORIGINAL){
				sql.pushFileInfo(child.getPath(), userName, today, expiresOn,
						FileInfo.TYPE_ORIGINAL|FileInfo.WRITE_ALLOWED,
						SHA256Helper.getHashStringFromFile(child.getPath()));
				logger.i(this, "Fileinfo successfully created.\nPlease type 'setfile " + child.getName() + " readwrite' to make it write-allowed for other peers.");
			}
		} catch (FileTooBigException e) {
			// TODO Auto-generated catch block
			logger.d(this, e.getMessage());
			e.printStackTrace();
		}
		// Set file permission as read-only. Commenting for now
//		CyborgFileManager.setPermissions(child.toString(), "600");
	}


	@Override
	public void onFileModified(File child) {
    	if(child.exists()){
        	//String filePath = child.toString();
    		try {
				logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child.getPath()));
	    		FileInfo info = sql.getFileInfo(child.toString());
	    		if(info == null){
	    			logger.d(this, "No fileinfo. Creating...");
	    			Date now = new Date();
	    			String today = StaticUtil.daysAfter(now, 0);
	    			String expiresOn = StaticUtil.daysAfter(now, EXPIRES_DAYS_OWNER);
	    			//new SQLiteInstance().pushFileInfo(child,
	    			sql.pushFileInfo(child.getPath(), userName, today, expiresOn,
	    					FileInfo.TYPE_ORIGINAL|FileInfo.WRITE_ALLOWED,
	    					SHA256Helper.getHashStringFromFile(child.getPath()));
	    		}else{
	    			if(!SHA256Helper.getHashStringFromFile(child.getPath()).equals(info.getHash())){
	    				int copied_and_protected = FileInfo.TYPE_COPIED | FileInfo.WRITE_PROTECTED;
	    				// If the file is not mine and copy protected
	    				if(!info.getOwner().equals(userName) &&
	    						(info.getType()&copied_and_protected) == copied_and_protected){
	    					// The user violates the access rule! File should be deleted!!
	    					logger.i(this, "Alert! File contents has been attemped to be changed!! Reporting to the owner...");
	    					// Lock the file
	    					sql.updateFileInfo(child.toString(), info.getExpiresOnStr(), info.getType(), SHA256Helper.getHashStringFromFile(child.getPath()), FileInfo.LOCK);
	    					CyborgFileManager.setPermissions(child.toString(), "000");
	        				mSocketManager.reportViolation(info.getOwner(), child.getName(), "WritingReadOnly");
	    				}else{
	    					// This file is original or write-allowed. Update the file info
	    					if(info.getLock() == FileInfo.LOCK){
	        					logger.i(this, "Alert! This file is locked. Access Denied.");
	    					}else{
	        					sql.updateFileInfo(child.toString(), info.getExpiresOnStr(), info.getType(), SHA256Helper.getHashStringFromFile(child.getPath()), FileInfo.UNLOCK);
	    					}
	    				}
	    			}
	    		}
			} catch (FileTooBigException e) {
				logger.d(this, e.getMessage());
				e.printStackTrace();
			}
    	}		
	}


	@Override
	public void onFileDeleted(File child) {
    	if(!child.exists()){
    		//Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
    		sql.deleteFileInfo(child.toString());
    	}
	}

	@Override
	public void onRegisterCallback(File dir) {
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
	public void onRegisterCallback(List<File> dirs) {
		
	}

	@Override
	public void onFileExpired(File child) {
		child.delete();
		sql.deleteFileInfo(child.getPath());
		logger.i(this, "The file " + child.getPath() + " has been expired. deleting...");
		
	}
	
	// File change events - end
}
