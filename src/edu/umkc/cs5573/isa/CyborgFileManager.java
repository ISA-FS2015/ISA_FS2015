package edu.umkc.cs5573.isa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

public class CyborgFileManager {
	final static int READ = 4;
	final static int WRITE = 2;
	final static int EXECUTE = 1;
	
	public CyborgFileManager(){
	}
	
	/**
	 * Changes permission.
	 * @param filePath File Path
	 * @param permissionString Permission String. Like chmod. "755" or "600"...
	 * @return true if success, false otherwise
	 */
	public boolean setPermissions(String filePath, String permissionString){
		Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
		int l_owner = Integer.parseInt(permissionString.substring(0,1));
		int l_group = Integer.parseInt(permissionString.substring(0,1));
		int l_other = Integer.parseInt(permissionString.substring(0,1));
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

}
