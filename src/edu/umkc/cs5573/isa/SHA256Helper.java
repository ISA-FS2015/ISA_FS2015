package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
	static byte[] getHash(String text){
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
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws OutOfMemoryError
	 * @param filePath
	 * @return
	 * @throws OutOfMemoryError
	 */
	static byte[] getHashFromFile(String filePath) throws OutOfMemoryError {
		Path path = Paths.get(filePath);
		try {
			byte[] data = Files.readAllBytes(path);
			return hashSHA256(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws OutOfMemoryError
	 * @param path
	 * @return
	 * @throws OutOfMemoryError
	 */
	static byte[] getHashFromFile(Path path) throws OutOfMemoryError {
		try {
			byte[] data = Files.readAllBytes(path);
			return hashSHA256(data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws OutOfMemoryError
	 * @param filePath
	 * @return
	 * @throws OutOfMemoryError
	 */
	static String getHashStringFromFile(String filePath) throws OutOfMemoryError {
		return StaticUtil.byteToBase64(getHashFromFile(filePath));
	}
	
	/**
	 * Make a SHA256 hash from the file. If the file is larger than 2GB it throws OutOfMemoryError
	 * @param path
	 * @return
	 * @throws OutOfMemoryError
	 */
	static String getHashStringFromFile(Path path) throws OutOfMemoryError {
		return StaticUtil.byteToBase64(getHashFromFile(path));
	}
	
	/**
	 * Make a SHA256 hash into printable string from the text
	 * @param text
	 * @return
	 */
	static String getHashString(String text){
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
