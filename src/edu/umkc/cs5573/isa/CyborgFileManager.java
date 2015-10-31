package edu.umkc.cs5573.isa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Used for changing the file permission
 * @author Younghwan
 *
 */
public class CyborgFileManager {
	final static int READ		= 0x00000100;
	final static int WRITE		= 0x00000010;
	final static int EXECUTE 	= 0x00000001;
	
	public CyborgFileManager(){
	}
	
	/**
	 * Changes permission.
	 * @param filePath File Path
	 * @param permissionString Permission String. Like chmod. "755" or "600"...
	 * @return true if success, false otherwise
	 */
	public static boolean setPermissions(String filePath, String permissionString){
		Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
		int l_owner = Integer.parseInt(permissionString.substring(0,1));
		int l_group = Integer.parseInt(permissionString.substring(1,2));
		int l_other = Integer.parseInt(permissionString.substring(2,3));
		//add owners permission
		if((l_owner & READ) == READ) perms.add(PosixFilePermission.OWNER_READ);
		if((l_owner & WRITE) == WRITE) perms.add(PosixFilePermission.OWNER_WRITE);
		if((l_owner & EXECUTE) == EXECUTE) perms.add(PosixFilePermission.OWNER_EXECUTE);
		//add group permissions
		if((l_group & READ) == READ) perms.add(PosixFilePermission.GROUP_READ);
		if((l_group & WRITE) == WRITE) perms.add(PosixFilePermission.GROUP_WRITE);
		if((l_group & EXECUTE) == EXECUTE) perms.add(PosixFilePermission.GROUP_EXECUTE);
		//add others permissions
		if((l_other & READ) == READ) perms.add(PosixFilePermission.OTHERS_READ);
		if((l_other & WRITE) == WRITE) perms.add(PosixFilePermission.OTHERS_WRITE);
		if((l_other & EXECUTE) == EXECUTE) perms.add(PosixFilePermission.OTHERS_EXECUTE);
		try {
			Files.setPosixFilePermissions(Paths.get(filePath), perms);
			return true;
		} catch (IOException | UnsupportedOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public void listf(String directoryName, ArrayList<File> files) {
	    File directory = new File(directoryName);

	    // get all the files from a directory
	    File[] fList = directory.listFiles();
	    for (File file : fList) {
	        if (file.isFile()) {
	            files.add(file);
	        } else if (file.isDirectory()) {
	            listf(file.getAbsolutePath(), files);
	        }
	    }
	}

}
