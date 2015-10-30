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
	/**
	 * Converts file contents into base64 format
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String encodeFileToBase64Binary(String fileName)
			throws IOException {

		File file = new File(fileName);
		byte[] bytes = loadFile(file);
		return byteToBase64(bytes);
	}
	
	/**
	 * Converts base64 encoded bytes into the file
	 * @param path
	 * @param fileName
	 * @param contents
	 * @return
	 * @throws IOException
	 */
	public static File decodeBase64BinaryToFile(String path, String fileName, String contents)
			throws IOException {
		File file = saveToFile(path + "/" + fileName, base64ToBytes(contents));
		return file;
	}
	
	/**
	 * Saves the byte contents into a file
	 * @param filePath
	 * @param contents
	 * @return
	 * @throws IOException
	 */
	public static File saveToFile(String filePath, byte[] contents) throws IOException{
		File newFile = new File(filePath);
		OutputStream os = new FileOutputStream(newFile);
		os.write(contents);
		os.close();
		return newFile;
	}
	/**
	 * Gets byte array from the file content
	 * @param file
	 * @return
	 * @throws IOException
	 */
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
	/**
	 * Gets the date after the specific days
	 * @param date
	 * @param after
	 * @return
	 */
	public static String daysAfter(Date date, int after){
		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.DATE, after);
		return dateFormat.format(cal.getTime());
	}
}
