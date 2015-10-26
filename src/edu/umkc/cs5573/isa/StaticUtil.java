package edu.umkc.cs5573.isa;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
	public static String encodeFileToBase64Binary(String fileName)
			throws IOException {

		File file = new File(fileName);
		byte[] bytes = loadFile(file);
		return byteToBase64(bytes);
	}

	private static byte[] loadFile(File file) throws IOException {
	    InputStream is = new FileInputStream(file);

	    long length = file.length();
	    if (length > Integer.MAX_VALUE) {
	        // File is too large
	    }
	    byte[] bytes = new byte[(int)length];
	    
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }

	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file "+file.getName());
	    }

	    is.close();
	    return bytes;
	}

}
