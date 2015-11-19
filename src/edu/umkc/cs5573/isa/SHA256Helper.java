package edu.umkc.cs5573.isa;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class helps to make a SHA256 hash from text, inputstream, or files
 * @author Younghwan
 *
 */
public class SHA256Helper {
	/**
	 * Make a SHA256 hash into bytes from the text
	 * @param text
	 * @return hashed bytes
	 */
	public static byte[] getHash(String text){
		try {
			return hashSHA256(text.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (NullPointerException e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws FileTooBigException
	 * @param filePath
	 * @return
	 * @throws FileTooBigException 
	 */
	public static byte[] getHashFromFile(String filePath) throws FileTooBigException {
		try {
			byte[] data = StaticUtil.loadFile(new File(filePath));
			return hashSHA256(data);
		} catch (IOException e) {
			// Maybe file is not found
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Retrieves hash string from byte array
	 * @param data
	 * @return
	 */
	public static String getHashStringFromBytes(byte[] data){
		return StaticUtil.byteToBase64(hashSHA256(data));
	}
	
	/**
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws FileTooBigException
	 * @param file
	 * @return
	 * @throws FileTooBigException 
	 */
	public static byte[] getHashFromFile(File file) throws FileTooBigException {
		try {
			byte[] data = StaticUtil.loadFile(file);
			return hashSHA256(data);
		} catch (IOException e) {
			// Maybe file is not found
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws FileTooBigException
	 * @param filePath
	 * @return
	 * @throws FileTooBigException 
	 */
	public static String getHashStringFromFile(String filePath) throws FileTooBigException {
		return StaticUtil.byteToBase64(getHashFromFile(filePath));
	}
	
	/**
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws FileTooBigException
	 * @param path
	 * @return
	 * @throws FileTooBigException 
	 */
	public static String getHashStringFromFile(File file) throws FileTooBigException {
		return StaticUtil.byteToBase64(getHashFromFile(file));
	}
	
	/**
	 * Make a SHA256 hash into printable string from the text
	 * @param text
	 * @return
	 */
	public static String getHashString(String text){
		return StaticUtil.byteToBase64(getHash(text));
	}
	
	/**
	 * This is a private method. Do not use this in public
	 * @param raw raw bytes
	 * @return hashed bytes
	 */
	private static byte[] hashSHA256(byte[] raw){
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(raw);
			return hash;
		} catch (NoSuchAlgorithmException e) {
			// No algorithm is found. Never called here
			e.printStackTrace();
		}
		return null;
	}
	
}
