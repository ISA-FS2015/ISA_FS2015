package edu.umkc.cs5573.isa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

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
	
	public static File decodeBase64BinaryToFile(String path, String fileName, String contents)
			throws IOException {
		File file = saveToFile(path + "/" + fileName, base64ToBytes(contents));
		return file;
	}
	public static File saveToFile(String filePath, byte[] contents) throws IOException{
		File newFile = new File(filePath);
		OutputStream os = new FileOutputStream(newFile);
		os.write(contents);
		os.close();
		return newFile;
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
	    	is.close();
	        throw new IOException("Could not completely read file "+file.getName());
	    }

	    is.close();
	    return bytes;
	}
	public static String daysAfter(Date date, int after){
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, after);
		return dateFormat.format(cal.getTime());
	}
}
