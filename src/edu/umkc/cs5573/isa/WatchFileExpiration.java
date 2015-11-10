package edu.umkc.cs5573.isa;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class WatchFileExpiration extends Thread implements Runnable {
	private final static long INTERVAL = 30 * 60 * 1000;
	private SQLiteInstanceAbstract sql;
	private boolean isRunning;
	private IWatchExpHandler handler;
	

	public WatchFileExpiration(String threadName, SQLiteInstanceAbstract sql) {
		super(threadName);
		this.sql = sql;
		Logger.getInstance();
		this.isRunning = true;
	}
	
	public boolean isThreadRunning(){
		return isRunning;
	}
	public void stopThread(){
		isRunning = false;
	}
	public void setExpirationHandler(IWatchExpHandler handler){
		this.handler = handler;
	}
	@Override
	public void run(){
		while(isRunning){
			List<FileInfo> infoes = sql.getExpiredFileInfoes();
			for (FileInfo info : infoes){
				Path path = Paths.get(info.getFileName());
				if(handler != null) handler.onFileExpired(path);
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
