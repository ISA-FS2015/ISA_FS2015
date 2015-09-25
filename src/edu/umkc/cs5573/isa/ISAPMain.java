package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchService;

import com.almworks.sqlite4java.SQLiteException;

public class ISAPMain {
	final static String WORK_DIR = Resources.WORK_DIR;

	public static void main(String[] args) {
		//String tobeHashed = "Test!!";
		//System.out.println("Hash of " + tobeHashed + " : " + SHA256Helper.getHashString(tobeHashed));
		localPrint("Cyborg test program.");
		CyborgController cyborg;
		try {
			if(args.length > 0){
				cyborg = CyborgController.getInstance(args[0]);
			}else{
				String userName = System.getProperty("user.name");
				cyborg = CyborgController.getInstance(userName);
			}
		} catch (IOException | SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public static void localPrint(String msg){
		System.out.println(msg);
	}
}
