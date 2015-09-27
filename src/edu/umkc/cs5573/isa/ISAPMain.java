package edu.umkc.cs5573.isa;

import java.io.IOException;

import com.almworks.sqlite4java.SQLiteException;

public class ISAPMain {
	final static String WORK_DIR = Resources.WORK_DIR;

	public static void main(String[] args) {
		//String tobeHashed = "Test!!";
		//System.out.println("Hash of " + tobeHashed + " : " + SHA256Helper.getHashString(tobeHashed));
		localPrint("Cyborg test program.");
		localPrint("Usage: ISAPMain <username> <interfacefname> <HomeDirectory>");
		localPrint("E.g: ISAPMain user01 wlan0 ./cyborgman");
		CyborgController cyborg;
		try {
			if(args.length > 2){
				cyborg = CyborgController.getInstance(args[0], args[1], args[2]);
			}else{
				String userName = System.getProperty("user.name");
				cyborg = CyborgController.getInstance(userName, "wlan0", "res/home/cyborgman");
			}
			cyborg.init();
			cyborg.cli(System.out, System.in);
			cyborg.closeService();
		} catch (IOException | SQLiteException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public static void localPrint(String msg){
		System.out.println(msg);
	}
}
