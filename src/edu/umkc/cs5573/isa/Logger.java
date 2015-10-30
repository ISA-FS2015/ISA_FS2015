package edu.umkc.cs5573.isa;

import java.io.PrintStream;

public class Logger {
	final static boolean DEBUG = true;
	private PrintStream out;
	private volatile static Logger mLogger;
	public static Logger getInstance(){
		if(mLogger == null){
			synchronized(Logger.class){
				if(mLogger == null){
					mLogger = new Logger();
				}
			}
		}
		return mLogger;
	}
	private Logger(){
		this.out = System.out;
	}
	public void setOutputStream(PrintStream out){
		this.out = out;
	}
	public void resetOutputStream(){
		this.out = System.out;
	}
	public void d(Object obj, String msg){
		if(DEBUG) out.println(obj.getClass().getSimpleName() + " Says: " + msg);
	}
	public void i(Object obj, String msg){
		out.println(obj.getClass().getSimpleName().substring(0, 8) + ": " + msg);
	}
}
