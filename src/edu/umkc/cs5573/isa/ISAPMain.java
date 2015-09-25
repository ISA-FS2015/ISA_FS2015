package edu.umkc.cs5573.isa;

import java.io.IOException;

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
			cyborg.init();
			cyborg.cli(System.out, System.in);
			cyborg.closeService();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public static void localPrint(String msg){
		System.out.println(msg);
	}
}
