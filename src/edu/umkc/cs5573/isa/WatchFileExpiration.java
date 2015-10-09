package edu.umkc.cs5573.isa;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WatchFileExpiration extends Thread{
	private final static long INTERVAL = 30 * 60 * 1000;
	private SQLiteInstance sql;
	private Logger logger;
	private boolean isRunning;
	

	public WatchFileExpiration(String threadName, SQLiteInstance sql) {
		super(threadName);
		this.sql = sql;
		this.logger = Logger.getInstance();
		this.isRunning = true;
	}
	
	public boolean isThreadRunning(){
		return isRunning;
	}
	public void stopThread(){
		isRunning = false;
	}
	
	public void run(){
		while(isRunning){
			List<FileInfo> infoes = sql.getExpiredFileInfoes();
			for (FileInfo info : infoes){
				Path path = Paths.get(info.getFileName());
				path.toFile().delete();
				sql.deleteFileInfo(path);
				logger.d(this, "The file " + path.toString() + " has been expired. deleting...");
			}
			try {
				// Runs every 30 min.
				Thread.sleep(INTERVAL);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
