package edu.umkc.cs5573.isa;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;

public class SQLiteInstance {
//	private SQLiteConnection db;
	SQLiteQueue queue = null;
	private final static String TABLE_FILE_INFO = "FileInfo";
	private final static String TABLE_USER_INFO = "UserInfo";
	private final static String TABLE_CERT_INFO = "CertInfo";
	private Logger logger;
	
	private SQLiteInstance(){
//		db = new SQLiteConnection(new File("res/db/database.db"));
		this.queue = new SQLiteQueue(new File("res/db/database.db"));
		this.logger = Logger.getInstance();
		init();
	}

	// Not anymore singleton due to thread issue
	// Singleton - Start
	
	private volatile static SQLiteInstance sqLiteInstance;
//
//	private SQLiteInstance() throws SQLiteException{
//		db = new SQLiteConnection(new File("res/db/database.db"));
//		dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
//		db.open(true);
//		init();
////		db.dispose();
//	}
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
	
//	private SimpleDateFormat dateFormat = null;
	
	void init(){
		queue.start();
		logger.d(this, "SQLite Queue running...");
		// File Info Table Initialization
		queue.execute(new SQLiteJob<Object>() {
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				String sqlState = "create table " + TABLE_FILE_INFO + " ("
						+ "id integer primary key autoincrement, "
						+ "Filename text not null, "
						+ "Owner text not null, "
						+ "CreatedOn text not null, "
						+ "ExpiresOn text not null, "
						+ "Type integer not null, "
						+ "Hash text not null"
						+ ")";
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
				String sqlState = "create table " + TABLE_USER_INFO + " ("
						+ "SSO text primary key not null, "
						+ "Type integer not null, "
						+ "Name text not null, "
						+ "Organization text not null, "
						+ "Email text not null, "
						+ "PhoneNumber text not null, "
						+ "Score integer not null, "
						+ "PrivateKey text not null, "
						+ "PublicKey text not null"
						+ ")";
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
				String sqlState = "create table " + TABLE_CERT_INFO + " ("
						+ "SSO text primary key not null, "
						+ "Cert text not null"
						+ ")";
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
	
	public String getFileHash(Path path){
		final String sql = "SELECT Hash FROM " + TABLE_FILE_INFO + " WHERE Filename = \"" + path.toString() + "\"";
		String hash = queue.execute(new SQLiteJob<String>(){
			@Override
			protected String job(SQLiteConnection connection) throws Throwable {
				// TODO Auto-generated method stub
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
						FileInfo info = new FileInfo(id, fileName, owner, createdOn, expiresOn, type, hash);
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
						list.add(new FileInfo(id, fileName, owner, createdOn, expiresOn, type, hash));
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
	
	public void updateFileInfo(Path path, String expiresOn, int type, String hash){
		final String sql = "UPDATE from " + TABLE_FILE_INFO + " set "
				+ "ExpiresOn=\"" + expiresOn + "\""
				+ "Type=" + Integer.toString(type)
				+ "Hash=\"" + hash + "\""
				+ "WHERE Filename = \"" + path.toString() + "\"";
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
	
	public boolean pushFileInfo(Path path, String owner, String createdOn, String expiresOn, int type, String hash){
		//Check if there are any items with the same file
//		FileInfo info = getFileInfo(path);
//		if(info == null){
//			
//		}
		final String sqlState = "insert into " + TABLE_FILE_INFO
				+ "(Filename, Owner, CreatedOn, ExpiresOn, Type, Hash)"
				+ " values ("
				+ "\"" + path.toString() + "\"" + ","
				+ "\"" + owner + "\"" + ","
				+ "\"" + createdOn + "\"" + ","
				+ "\"" + expiresOn + "\"" + ","
				+ Integer.toString(type) + ","
				+ "\"" + hash + "\""
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
						FileInfo info = new FileInfo(id, fileName, owner, createdOn, expiresOn, type, hash);
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

	
	public Date getLastModified(String fileName){
		return null;
		
	}
	
	/* Following methods are for user info DB */
	
	public boolean pushUserInfo(UserInfo info){
		//Check if there are any items with the same file
//		FileInfo info = getFileInfo(path);
//		if(info == null){
//			
//		}
		final String sqlState = "insert into " + TABLE_USER_INFO
				+ "(SSO, Type, Name, Organization, Email, PhoneNumber, Score)"
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

	public void updateUserInfo(UserInfo info){
		final String sql = "UPDATE from " + TABLE_USER_INFO + " set "
				+ "Name=\"" + info.getName() + "\""
				+ "Organization=\"" + info.getOrganization() + "\""
				+ "Email=\"" + info.getEmail() + "\""
				+ "PhoneNumber=\"" + info.getPhoneNumber() + "\""
				+ "Score=\"" + Integer.toString(info.getScore())
				+ "PrivateKey=\"" + info.getPrivateKey() + "\""
				+ "PublicKey=\"" + info.getPublicKey() + "\""
				+ "WHERE SSO = \"" + info.getSso() + "\"";
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
	
	public boolean deleteUserInfo(UserInfo info){
		final String sqlState = "delete from " + TABLE_USER_INFO
				+ "WHERE SSO = \"" + info.getSso() + "\"";
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
	
	public UserInfo getUserInfo(final String sso){
		final String sql = "SELECT * FROM " + TABLE_USER_INFO + "WHERE SSO=\"" + sso + "\"";
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

	public boolean pushCertInfo(CertInfo info){
		//Check if there are any items with the same file
//		FileInfo info = getFileInfo(path);
//		if(info == null){
//			
//		}
		final String sqlState = "insert into " + TABLE_CERT_INFO
				+ "(SSO, Cert)"
				+ " values ("
				+ "\"" + info.getSso() + "\"" + ","
				+ "\"" + info.getCert() + "\""
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
	
	public CertInfo getCertInfo(final String sso){
		final String sql = "SELECT * FROM " + TABLE_CERT_INFO + "WHERE SSO=\"" + sso + "\"";
		CertInfo item = queue.execute(new SQLiteJob<CertInfo>(){
			@Override
			protected CertInfo job(SQLiteConnection connection) throws Throwable {
				try {
					SQLiteStatement st = connection.prepare(sql);
					while (st.step()) {
						String cert = st.columnString(1);
						CertInfo info = new CertInfo(sso, cert);
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
//		db.dispose();
//		db = null;
	}
	
//	void sqlTest() {
//		try {
//			SQLiteStatement st;
//			st = db.prepare("SELECT order_id FROM orders WHERE quantity >= ?");
//			//st.bind(1, minimumQuantity);
//			while (st.step()) {
//			//  orders.add(st.columnLong(0));
//			}
//			st.dispose();
////			db.dispose();
//		} catch (SQLiteException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
}
