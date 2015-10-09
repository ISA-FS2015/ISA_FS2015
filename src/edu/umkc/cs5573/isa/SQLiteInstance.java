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
		queue.execute(new SQLiteJob<Object>() {
			@Override
			protected Object job(SQLiteConnection connection) throws Throwable {
				String sqlState = "create table " + TABLE_FILE_INFO + " ("
						+ "id integer primary key autoincrement, "
						+ "Filename text not null, "
						+ "CreatedOn text not null, "
						+ "ExpiresOn text not null, "
						+ "Type int not null, "
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
	}
	
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
						String createdOn = st.columnString(2);
						String expiresOn = st.columnString(3);
						int type = st.columnInt(4);
						String hash = st.columnString(5);
						FileInfo info = new FileInfo(id, fileName, createdOn, expiresOn, type, hash);
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
						String createdOn = st.columnString(2);
						String expiresOn = st.columnString(3);
						int type = st.columnInt(4);
						String hash = st.columnString(5);
						list.add(new FileInfo(id, fileName, createdOn, expiresOn, type, hash));
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
	
	public boolean pushFileInfo(Path path, String createdOn, String expiresOn, int type, String hash){
		//Check if there are any items with the same file
//		FileInfo info = getFileInfo(path);
//		if(info == null){
//			
//		}
		final String sqlState = "insert into " + TABLE_FILE_INFO
				+ "(Filename, CreatedOn, ExpiresOn, Type, Hash)"
				+ " values ("
				+ "\"" + path.toString() + "\"" + ","
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
						String createdOn = st.columnString(2);
						String expiresOn = st.columnString(3);
						int type = st.columnInt(4);
						String hash = st.columnString(5);
						FileInfo info = new FileInfo(id, fileName, createdOn, expiresOn, type, hash);
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
