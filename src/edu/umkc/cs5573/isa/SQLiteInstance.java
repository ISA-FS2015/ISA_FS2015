package edu.umkc.cs5573.isa;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

public class SQLiteInstance {

	// Singleton - Start
	
	private volatile static SQLiteInstance sqLiteManager;

	private SQLiteInstance() throws SQLiteException{
		db = new SQLiteConnection(new File("res/db/database.db"));
		dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
		db.open(true);
		init();
//		db.dispose();
	}
	/**
	 * Singleton
	 * @return
	 * @throws SQLiteException 
	 */
	public static SQLiteInstance getInstance() throws SQLiteException{
		if(sqLiteManager == null){
			synchronized(SQLiteInstance.class){
				if(sqLiteManager == null){
					sqLiteManager = new SQLiteInstance();
				}
			}
		}
		return sqLiteManager;
	}
	
	// Singleton - End
	
	private SQLiteConnection db;
	private final static String TABLE_FILE_INFO = "FileInfo";
	private SimpleDateFormat dateFormat = null;
	
	private void init(){
		try {
			String sqlState = "create table " + TABLE_FILE_INFO + " ("
					+ "id integer primary key autoincrement, "
					+ "Filename text not null, "
					+ "LastModified text not null, "
					+ "Type int not null, "
					+ "Hash text not null"
					+ ")";
			db.exec(sqlState);
		} catch (SQLiteException e) {
			Logger.d(this, e.getMessage());
		}
	}
	
	public String getFileHash(Path path){
		SQLiteStatement st;
		String hash = null;
		try {
//			db.open(true);
			st = db.prepare("SELECT Hash FROM " + TABLE_FILE_INFO + " WHERE Filename = \"" + path.toString() + "\"");
			while (st.step()) {
				hash = st.columnString(0);
			//  orders.add(st.columnLong(0));
			}
			st.dispose();
//			db.dispose();
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return hash;
	}
	
	public boolean pushFileInfo(Path path, int type){
		Date now = new Date();
		String sqlState = "insert into " + TABLE_FILE_INFO
				+ "(Filename, LastModified, Type, Hash)"
				+ " values ("
				+ "\"" + path.toString() + "\"" + ","
				+ "\"" + dateFormat.format(now) + "\"" + ","
				+ Integer.toString(type) + ","
				+ "\"" + SHA256Helper.getHashStringFromFile(path)+ "\""
				+ ")";
		try {
//			db.open();
			SQLiteStatement st = db.prepare(sqlState);
			Logger.d(this, "Result: " + st.columnCount());
			while (st.step()) {
				Logger.d(this, "Result: " + st.columnString(0));
			}
			st.dispose();
//			db.dispose();
			return true;
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public Date getLastModified(String fileName){
		return null;
		
	}
	
	public void execSql(String sql){
		try {
//			db.open();
			SQLiteStatement st = db.prepare(sql);
			while(st.step()){
				Logger.d(this, st.toString());
			}
			st.dispose();
//			db.dispose();
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void dispose(){
		db.dispose();
	}
	void sqlTest() {
		try {
			SQLiteStatement st;
			st = db.prepare("SELECT order_id FROM orders WHERE quantity >= ?");
			//st.bind(1, minimumQuantity);
			while (st.step()) {
			//  orders.add(st.columnLong(0));
			}
			st.dispose();
//			db.dispose();
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
