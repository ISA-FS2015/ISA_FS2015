package edu.umkc.cs5573.isa;

public class Logger {
	final static boolean DEBUG = true;
	public static void d(Object obj, String msg){
		if(DEBUG) System.out.println(obj.getClass().getName() + " Says: " + msg);
	}
}
