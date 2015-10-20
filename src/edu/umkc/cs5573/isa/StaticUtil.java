package edu.umkc.cs5573.isa;

import java.util.Base64;

public class StaticUtil {
	/**
	 * Converts bytes to printable strings.
	 * @param data
	 * @return Base64 encoded string
	 */
	public static String byteToBase64(byte[] data){
		return Base64.getEncoder().encodeToString(data);
	}
	
	/**
	 * Converts printable base64 strings to bytes.
	 * @param data
	 * @return Base64 encoded string
	 */
	public static byte[] base64ToBytes(String data){
		return Base64.getDecoder().decode(data);
	}

}
