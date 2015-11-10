package edu.umkc.cs5573.isa;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;

/**
 * SQLite interfacing class which manipulates local DB
 * @author Younghwan
 *
 */
public class SQLiteInstance extends SQLiteInstanceAbstract {
	SQLiteQueue queue = null;
	private Logger logger;
	
	/**
	 * Private constructor. It is singleton so it needs to be called internally
	 */
	private SQLiteInstance(){
//		db = new SQLiteConnection(new File("res/db/database.db"));
		super();
		this.queue = new SQLiteQueue(new File("res/db/database.db"));
		this.logger = Logger.getInstance();
		init();
	}

	// Singleton - Start
	/**
	 * Singleton
	 */
	private volatile static SQLiteInstance sqLiteInstance;
	
	/**
	 * Singleton
	 * @param in 
	 * @param out 
	 * @return
	 * @throws SQLiteException 
	 */
	public static SQLiteInstance getInstance() throws SQLiteException{
		if(sqLiteInstance == null){
			synchronized(SQLiteInstance.class){
				if(sqLiteInstance == null){
					sqLiteInstance = new SQLiteInstance();
				}
			}
		}
		return sqLiteInstance;
	}
	
	// Singleton - End
	
	/**
	 * Initialization. Creating tables if not exist
	 */
	void init(){
		queue.start();
		logger.d(this, "SQLite Queue running...");
		// File Info Table Initialization
		queue.execute(new SQLiteJob<Object>() {
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				String sqlState = SQL_CREATE_FILE_INFO;
				try{
					connection.exec(sqlState);
				}catch(SQLiteException e){
					logger.d(this, e.getMessage());
				}
				return null;
			}
		});
		// User Info Table Initialization
		queue.execute(new SQLiteJob<Object>() {
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				String sqlState = SQL_CREATE_USER_INFO;
				try{
					connection.exec(sqlState);
				}catch(SQLiteException e){
					logger.d(this, e.getMessage());
				}
				return null;
			}
		});
		// User Info Table Initialization
		queue.execute(new SQLiteJob<Object>() {
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				String sqlState = SQL_CREATE_CERT_INFO;
				try{
					connection.exec(sqlState);
				}catch(SQLiteException e){
					logger.d(this, e.getMessage());
				}
				return null;
			}
		});
	}
	
	/* Following methods are for file info DB */
	
	@Override
	public String getFileHash(Path path){
		final String sql = "SELECT Hash FROM " + TABLE_FILE_INFO + " WHERE Filename = \"" + path.toString() + "\"";
		String hash = queue.execute(new SQLiteJob<String>(){
			@Override
			protected String job(SQLiteConnection connection) throws Throwable {
				connection.open(true);
				SQLiteStatement st = connection.prepare(sql);
				try{
					while (st.step()) {
						return st.columnString(0);
					}
				}catch(SQLiteException e){
					
				}finally{
					st.dispose();
				}
				//  orders.add(st.columnLong(0));
				return null;
			}
		}).complete();
		return hash;
	}
	
	@Override
	public FileInfo getFileInfo(Path path){
		final String sql = "SELECT * FROM " + TABLE_FILE_INFO + " WHERE Filename = \"" + path.toString() + "\"";
		FileInfo item = queue.execute(new SQLiteJob<FileInfo>(){
			@Override
			protected FileInfo job(SQLiteConnection connection) throws Throwable {
				try {
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
						int id = st.columnInt(0);
						String fileName = st.columnString(1);
						String owner = st.columnString(2);
						String createdOn = st.columnString(3);
						String expiresOn = st.columnString(4);
						int type = st.columnInt(5);
						String hash = st.columnString(6);
						int lock = st.columnInt(7);
						FileInfo info = new FileInfo(id, fileName, owner, createdOn, expiresOn, type, hash, lock);
						return info;
					}
					st.dispose();
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
		return item;
	}
	
	@Override
	public List<FileInfo> getFileInfoes(){
		final String sql = "SELECT * FROM " + TABLE_FILE_INFO;
		List<FileInfo> item = queue.execute(new SQLiteJob<List<FileInfo>>(){
			@Override
			protected List<FileInfo> job(SQLiteConnection connection) throws Throwable {
				try {
					List<FileInfo> list = new ArrayList<FileInfo>();
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
						int id = st.columnInt(0);
						String fileName = st.columnString(1);
						String owner = st.columnString(2);
						String createdOn = st.columnString(3);
						String expiresOn = st.columnString(4);
						int type = st.columnInt(5);
						String hash = st.columnString(6);
						int lock = st.columnInt(7);
						list.add(new FileInfo(id, fileName, owner, createdOn, expiresOn, type, hash, lock));
					}
					st.dispose();
					return list;
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
		return item;
	}
	
	@Override
	public void updateFileInfo(Path path, String expiresOn, int type, String hash, int lock){
		final String sql = "UPDATE " + TABLE_FILE_INFO + " set "
				+ "ExpiresOn=\"" + expiresOn + "\"" + ","
				+ "Type=" + type + ","
				+ "Hash=\"" + hash + "\"" + ","
				+ "Lock=" + lock
				+ " WHERE Filename = \"" + path.toString() + "\"";
		queue.execute(new SQLiteJob<Void>(){
			@Override
			protected Void job(SQLiteConnection connection) throws Throwable {
				try {
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
					}
					st.dispose();
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
	}
	
	@Override
	public boolean deleteFileInfo(Path path){
		final String sqlState = "delete from " + TABLE_FILE_INFO
				+ " where Filename= " + "\"" + path.toString() + "\"";
		return queue.execute(new SQLiteJob<Boolean>(){
			@Override
			protected Boolean job(SQLiteConnection connection) throws Throwable {
				try {
					connection.open();
					SQLiteStatement st = connection.prepare(sqlState);
					logger.d(this, "Result: " + st.columnCount());
					while (st.step()) {
						logger.d(this, "Delete Result: " + st.columnString(0));
					}
					st.dispose();
					return true;
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return false;
			}
		}).complete();
	}
	
	@Override
	public boolean pushFileInfo(Path path, String owner, String createdOn, String expiresOn, int type, String hash){
		//Check if there are any items with the same file
//		FileInfo info = getFileInfo(path);
//		if(info == null){
//			
//		}
		final String sqlState = "insert into " + TABLE_FILE_INFO
				+ "(Filename, Owner, CreatedOn, ExpiresOn, Type, Hash, Lock)"
				+ " values ("
				+ "\"" + path.toString() + "\"" + ","
				+ "\"" + owner + "\"" + ","
				+ "\"" + createdOn + "\"" + ","
				+ "\"" + expiresOn + "\"" + ","
				+ Integer.toString(type) + ","
				+ "\"" + hash + "\"" + ","
				+ FileInfo.UNLOCK
				+ ")";
		return queue.execute(new SQLiteJob<Boolean>(){
			@Override
			protected Boolean job(SQLiteConnection connection) throws Throwable {
				try {
					connection.open();
					SQLiteStatement st = connection.prepare(sqlState);
					logger.d(this, "Result: " + st.columnCount());
					while (st.step()) {
						logger.d(this, "Insert Result: " + st.columnString(0));
					}
					st.dispose();
					return true;
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return false;
			}
			
		}).complete();
	}
	
	@Override
	public List<FileInfo> getExpiredFileInfoes(){
		final String sql = "SELECT * FROM " + TABLE_FILE_INFO;
		List<FileInfo> item = queue.execute(new SQLiteJob<List<FileInfo>>(){
			@Override
			protected List<FileInfo> job(SQLiteConnection connection) throws Throwable {
				try {
					List<FileInfo> list = new ArrayList<FileInfo>();
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
						int id = st.columnInt(0);
						String fileName = st.columnString(1);
						String owner = st.columnString(2);
						String createdOn = st.columnString(3);
						String expiresOn = st.columnString(4);
						int type = st.columnInt(5);
						String hash = st.columnString(6);
						int lock = st.columnInt(7);
						FileInfo info = new FileInfo(id, fileName, owner, createdOn, expiresOn, type, hash, lock);
						if(info.isExpired()){
							list.add(info);
						}
					}
					st.dispose();
					return list;
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
		return item;
	}
	@Override
	public Date getLastModified(String fileName){
		return null;
		
	}
	
	/* Following methods are for user info DB */
	
	@Override
	public boolean pushUserInfo(UserInfo info){
		//Check if there are any items with the same file
//		FileInfo info = getFileInfo(path);
//		if(info == null){
//			
//		}
		final String sqlState = "insert into " + TABLE_USER_INFO
				+ "(SSO, Type, Name, Organization, Email, PhoneNumber, Score, PrivateKey, PublicKey)"
				+ " values ("
				+ "\"" + info.getSso() + "\"" + ","
				+ Integer.toString(info.getType()) + ","
				+ "\"" + info.getName() + "\"" + ","
				+ "\"" + info.getOrganization() + "\"" + ","
				+ "\"" + info.getEmail() + "\"" + ","
				+ "\"" + info.getPhoneNumber() + "\"" + ","
				+ Integer.toString(info.getScore()) + ","
				+ "\"" + info.getPrivateKey() + "\"" + ","
				+ "\"" + info.getPublicKey() + "\""
				+ ")";
		return queue.execute(new SQLiteJob<Boolean>(){
			@Override
			protected Boolean job(SQLiteConnection connection) throws Throwable {
				try {
					connection.open();
					SQLiteStatement st = connection.prepare(sqlState);
					logger.d(this, "Result: " + st.columnCount());
					while (st.step()) {
						logger.d(this, "Insert Result: " + st.columnString(0));
					}
					st.dispose();
					return true;
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return false;
			}
		}).complete();
	}

	@Override
	public void updateUserInfo(UserInfo info)
	{
		final String sql = "UPDATE " + TABLE_USER_INFO + " set "
				+ "Name=\"" + info.getName() + "\"" + ","
				+ "Organization=\"" + info.getOrganization() + "\"" + ","
				+ "Email=\"" + info.getEmail() + "\"" + ","
				+ "PhoneNumber=\"" + info.getPhoneNumber() + "\"" + ","
				+ "Score=" + Integer.toString(info.getScore()) + ","
				+ "PrivateKey=\"" + info.getPrivateKey() + "\"" + ","
				+ "PublicKey=\"" + info.getPublicKey() + "\""
				+ " WHERE SSO = \"" + info.getSso() + "\"";
		queue.execute(new SQLiteJob<Void>(){
			@Override
			protected Void job(SQLiteConnection connection) throws Throwable {
				try {
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
					}
					st.dispose();
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
	}
	
	/**
	 * Deletes the user info
	 * @param info
	 * @return
	 */
	public boolean deleteUserInfo(UserInfo info){
		final String sqlState = "delete from " + TABLE_USER_INFO
				+ " WHERE SSO = \"" + info.getSso() + "\"";
		return queue.execute(new SQLiteJob<Boolean>(){
			@Override
			protected Boolean job(SQLiteConnection connection) throws Throwable {
				try {
					connection.open();
					SQLiteStatement st = connection.prepare(sqlState);
					logger.d(this, "Result: " + st.columnCount());
					while (st.step()) {
						logger.d(this, "Delete Result: " + st.columnString(0));
					}
					st.dispose();
					return true;
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return false;
			}
		}).complete();
	}
	
	@Override
	public UserInfo getUserInfo(final String sso){
		final String sql = "SELECT * FROM " + TABLE_USER_INFO + " WHERE SSO=\"" + sso + "\"";
		UserInfo item = queue.execute(new SQLiteJob<UserInfo>(){
			@Override
			protected UserInfo job(SQLiteConnection connection) throws Throwable {
				try {
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
						int type = st.columnInt(1);
						String name = st.columnString(2);
						String organization = st.columnString(3);
						String email = st.columnString(4);
						String phoneNumber = st.columnString(5);
						int score = st.columnInt(6);
						String privateKey = st.columnString(7);
						String publicKey = st.columnString(8);
						UserInfo info = new UserInfo(sso, type, name, organization, email, phoneNumber, score, privateKey, publicKey);
						return info;
					}
					st.dispose();
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
		return item;
	}

	@Override
	public boolean pushCertInfo(CertInfo info){
		//Check if there are any items with the same file
		final String sqlState = "insert into " + TABLE_CERT_INFO
				+ "(SSO, Cert, PrivateKey)"
				+ " values ("
				+ "\"" + info.getSso() + "\"" + ","
				+ "\"" + info.getCert() + "\"" + ","
				+ "\"" + info.getPrivateKey() + "\""
				+ ")";
		return queue.execute(new SQLiteJob<Boolean>(){
			@Override
			protected Boolean job(SQLiteConnection connection) throws Throwable {
				try {
					connection.open();
					SQLiteStatement st = connection.prepare(sqlState);
					logger.d(this, "Result: " + st.columnCount());
					while (st.step()) {
						logger.d(this, "Insert Result: " + st.columnString(0));
					}
					st.dispose();
					return true;
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return false;
			}
		}).complete();
	}
	
	@Override
	public CertInfo getCertInfo(final String sso){
		final String sql = "SELECT * FROM " + TABLE_CERT_INFO + " WHERE SSO=\"" + sso + "\"";
		CertInfo item = queue.execute(new SQLiteJob<CertInfo>(){
			@Override
			protected CertInfo job(SQLiteConnection connection) throws Throwable {
				try {
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
						String cert = st.columnString(1);
						String prk = st.columnString(2);
						CertInfo info = new CertInfo(sso, cert, prk);
						return info;
					}
					st.dispose();
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
		return item;
	}
	
	@Override
	public void execSql(final String sql){
		String result = queue.execute(new SQLiteJob<String>(){
			@Override
			protected String job(SQLiteConnection connection) throws Throwable {
				try {
					connection.open();
					SQLiteStatement st = connection.prepare(sql);
					StringBuilder buf = new StringBuilder();
					int index = 0;
					while(st.step()){
						if(index == 0){
							for(int i=0; i < st.columnCount(); i++){
								buf.append(st.getColumnName(i)).append('\t');
							}
							buf.append('\n');
						}
						for(int i=0; i < st.columnCount(); i++){
							buf.append(st.columnString(i)).append('\t');
						}
						buf.append('\n');
					}
					st.dispose();
					return buf.toString();
				} catch (SQLiteException e) {
					e.printStackTrace();
				}
				return null;
			}
		}).complete();
		logger.d(this, result);
	}
	
	public void dispose() throws InterruptedException{
		queue.stop(true).join();
	}
}
