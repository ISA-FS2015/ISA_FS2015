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
public class CyborgController implements IWatchDirHandler{
	
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	final static int EXPIRES_DAYS_COPIED = 1;
	final static int EXPIRES_DAYS_OWNER = 3650; // 10 years for the owner
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
	SQLiteInstance sql;
	private boolean isInit = false;
	private boolean isDisposed = false;
	private int certReqTriggered = 0;
	private int reactReqTriggered = 0;
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
		this.mSocketManager = new CyborgSocketManager(this, userName, ifName, homeDirectory, Resources.UDP_PORT, Resources.TCP_PORT, false);
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
			logger.d(this, "Already disposed. Skipping...");
		}else{
			logger.d(this, "Already initialized. Skipping...");
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
				sql.dispose();
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
			logger.d(this, "Not initialized or already disposed. Skipping...");
			return;
		}
		boolean session = true;
		while(session){
			// Get into CLI mode
			logger.d(this, "Type 'help' anytime for getting the command list.");
			String[] cmds = getCommands(out, in);
			if(cmds.length == 0) continue;
			if("byby".equals(cmds[0])){
				session = false;
			}else if ("userlist".equals(cmds[0])){
				doUserList();
			}else if ("whohas".equals(cmds[0])){
				doRequestFileProbe(cmds);
			}else if ("requestFile".equals(cmds[0])){
				doRequestFile(cmds);
			}else if ("requestCert".equals(cmds[0])){
				doRequestCert(cmds);
			}else if ("setmyinfo".equals(cmds[0])){
				doSetMyInfo(cmds);
			}else if ("setfile".equals(cmds[0])){
				doSetFile(cmds);
			}else if ("maketestfile".equals(cmds[0])){
				makeTestFile();
			}else if ("cert".equals(cmds[0])){
				if(certReqTriggered > 0) mQueue.queue(StaticUtil.joinWith(cmds, " "));
			}else if ("react".equals(cmds[0])){
				if(reactReqTriggered > 0) mQueue.queue(StaticUtil.joinWith(cmds, " "));
			}else if("sql".equals(cmds[0])){
				doSql(cmds);
			}else if ("help".equals(cmds[0])){
				doCommandList();
			}
		}
		logger.d(this, "Exitting...... Byebye!");
		logger.resetOutputStream();
	}
	
	private void doCommandList() {
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		sb.append("---Available command list---\n")
			.append("byby\n")
			.append("userlist\n")
			.append("whohas <filename>")
			.append("requestFile <SSO> <filename>")
			.append("requestCert <SSO>")
			.append("setmyinfo <Student/Employee> <name> <organization> <e-mail> <phoneNumber>\n")
			.append("setfile <filename> <type:readwrite/readonly>\n");
		logger.i(this, sb.toString());
		
	}

	private void doUserList() {
		Map<String, String> userList = mSocketManager.getUserList();
    	for(Entry<String, String> entry : userList.entrySet()){
        	logger.d(this, entry.getKey() + ":" + entry.getValue());
    	}
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
				logger.d(this, "My Info has not been set. Please set my info first using 'setmyinfo' command");
			}else{
				String sso = cmds[1];
				mSocketManager.reqCert(sso, myInfo);
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
				FileInfo info = sql.getFileInfo(file);
				if(info != null){
					if((info.getType()&FileInfo.TYPE_ORIGINAL) == FileInfo.TYPE_ORIGINAL){
						info.setType(info.getType() | FileInfo.WRITE_ALLOWED);
						sql.updateFileInfo(file, info.getExpiresOnStr(),info.getType(),info.getHash(), FileInfo.UNLOCK);
					}
					else{
						logger.d(this, "Prohibited access. Reporting to the owner...");
        				mSocketManager.reportViolation(userName, fileName, "ProhibitedModeChange");
					}
				}else{
					logger.d(this, "No file info. please check your filename or permission.");
				}
			}else if(permission.equals("readonly")){
				Path file = Paths.get(homeDirectory + "/" + fileName);
				FileInfo info = sql.getFileInfo(file);
				if(info != null){
					if((info.getType() & FileInfo.TYPE_ORIGINAL) == FileInfo.TYPE_ORIGINAL){
						info.setType(info.getType() & FileInfo.WRITE_PROTECTED);
						sql.updateFileInfo(file, info.getExpiresOnStr(),info.getType(),info.getHash(), FileInfo.UNLOCK);
					}
					else{
						logger.d(this, "The owner allowed write permission, but do you really need to make it readonly??");
					}
				}else{
					logger.d(this, "No file info. please check your filename or permission.");
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
			logger.d(this, "Usage: setmyinfo <Type: Student/Employee> <Name> <Organization> <e-mail> <PhoneNumber>");
		}
	}

	/**
	 * <p>Make test file represents the copied from other peer.</p>
	 * <p>Note that it is used for testing</p>
	 * @throws IOException
	 */
	public void makeTestFile() throws IOException
	{
		Path testPath = Paths.get(homeDirectory + "/test.txt");
		Files.write(testPath, "This is for test! It represents the copied file and should not be changed.".getBytes());
		Date now = new Date();
		String today = StaticUtil.daysAfter(now, 0);
		String expiresOn = StaticUtil.daysAfter(now, EXPIRES_DAYS_COPIED);
		//new SQLiteInstance().pushFileInfo(child,
		FileInfo info = sql.getFileInfo(testPath);
		if(info != null) sql.deleteFileInfo(testPath);
		sql.pushFileInfo(testPath, "TestOwner",
				today,
				expiresOn,
				FileInfo.TYPE_COPIED|FileInfo.WRITE_PROTECTED,
				SHA256Helper.getHashStringFromFile(testPath));
		logger.d(this, "Test Fileinfo successfully created");
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
	public void onFileCreated(Path child) {
    	//String filePath = child.toString();
		logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
		Date now = new Date();
		String today = StaticUtil.daysAfter(now, 0);
		String expiresOn = StaticUtil.daysAfter(now, EXPIRES_DAYS_OWNER);
		//new SQLiteInstance().pushFileInfo(child,
		FileInfo info = sql.getFileInfo(child);
		if(info == null || info.getType() == FileInfo.TYPE_ORIGINAL){
			sql.pushFileInfo(child, userName, today, expiresOn,
					FileInfo.TYPE_ORIGINAL|FileInfo.WRITE_ALLOWED,
					SHA256Helper.getHashStringFromFile(child));
			logger.d(this, "Fileinfo successfully created.\nPlease type 'setfile " + child.toFile().getName() + " readwrite' to make it write-allowed for other peers.");
		}
		// Set file permission as read-only. Commenting for now
//		CyborgFileManager.setPermissions(child.toString(), "600");
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
    			String today = StaticUtil.daysAfter(now, 0);
    			String expiresOn = StaticUtil.daysAfter(now, EXPIRES_DAYS_OWNER);
    			//new SQLiteInstance().pushFileInfo(child,
    			sql.pushFileInfo(child, userName, today, expiresOn,
    					FileInfo.TYPE_ORIGINAL|FileInfo.WRITE_ALLOWED,
    					SHA256Helper.getHashStringFromFile(child));
    		}else{
    			if(!SHA256Helper.getHashStringFromFile(child).equals(info.getHash())){
    				int copied_and_protected = FileInfo.TYPE_COPIED | FileInfo.WRITE_PROTECTED;
    				// If the file is not mine and copy protected
    				if(!info.getOwner().equals(userName) &&
    						(info.getType()&copied_and_protected) == copied_and_protected){
    					// The user violates the access rule! File should be deleted!!
    					logger.d(this, "Alert! File contents has been attemped to be changed!! Reporting to the owner...");
    					// Lock the file
    					sql.updateFileInfo(child, info.getExpiresOnStr(), info.getType(), SHA256Helper.getHashStringFromFile(child), FileInfo.LOCK);
    					CyborgFileManager.setPermissions(child.toString(), "000");
        				mSocketManager.reportViolation(userName, child.toFile().getName(), "WritingReadOnly");
    				}else{
    					// This file is original or write-allowed. Update the file info
    					if(info.getLock() == FileInfo.LOCK){
        					logger.d(this, "Alert! This file is locked. Access Denied.");
    					}else{
        					sql.updateFileInfo(child, info.getExpiresOnStr(), info.getType(), SHA256Helper.getHashStringFromFile(child), FileInfo.UNLOCK);
    					}
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
	
	// File change events - end
}
