package edu.umkc.cs5573.isa;

public class Logger {
	final static boolean DEBUG = true;
	public static void d(String msg){
		if(DEBUG) System.out.println(msg);
	}
}
