package edu.umkc.cs5573.isa;


public class ISAPMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String tobeHashed = "Test!!";
		System.out.println("Hash of " + tobeHashed + " : " + SHA256Helper.getHashString(tobeHashed));
	}
}
