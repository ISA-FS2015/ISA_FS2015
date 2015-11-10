package edu.umkc.cs5573.isa;

import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public abstract class SQLiteInstanceAbstract {
	final static String TABLE_FILE_INFO = "FileInfo";
	final static String TABLE_USER_INFO = "UserInfo";
	final static String TABLE_CERT_INFO = "CertInfo";
	final static String SQL_CREATE_FILE_INFO = "create table " + TABLE_FILE_INFO + " ("
			+ "id integer primary key autoincrement, "
			+ "Filename text not null, "
			+ "Owner text not null, "
			+ "CreatedOn text not null, "
			+ "ExpiresOn text not null, "
			+ "Type integer not null, "
			+ "Hash text not null,"
			+ "Lock integer text"
			+ ")";
	final static String SQL_CREATE_USER_INFO = "create table " + TABLE_USER_INFO + " ("
			+ "SSO text primary key not null, "
			+ "Type integer not null, "
			+ "Name text not null, "
			+ "Organization text not null, "
			+ "Email text not null, "
			+ "PhoneNumber text not null, "
			+ "Score integer not null, "
			+ "PrivateKey text not null, "
			+ "PublicKey text not null"
			+ ")";
	final static String SQL_CREATE_CERT_INFO = "create table " + TABLE_CERT_INFO + " ("
			+ "SSO text primary key not null, "
			+ "Cert text not null, "
			+ "PrivateKey text not null"
			+ ")";
	/**
	 * Retrieves filehash corresponding to the path
	 * @param path
	 * @return
	 */
	public abstract String getFileHash(Path path);
	
	/**
	 * Retrieves the file info from the path
	 * @param path
	 * @return
	 */
	public abstract FileInfo getFileInfo(Path path);
	
	/**
	 * Get the list of all files
	 * @return
	 */
	public abstract List<FileInfo> getFileInfoes();
	
	/**
	 * Update the file info
	 * @param path
	 * @param expiresOn
	 * @param type
	 * @param hash
	 * @param lock
	 */
	public abstract void updateFileInfo(Path path, String expiresOn, int type, String hash, int lock);
	
	/**
	 * Deletes the file info
	 * @param path
	 * @return
	 */
	public abstract boolean deleteFileInfo(Path path);
	
	/**
	 * Inserts the file info
	 * @param path
	 * @param owner
	 * @param createdOn
	 * @param expiresOn
	 * @param type
	 * @param hash
	 * @return
	 */
	public abstract boolean pushFileInfo(Path path, String owner, String createdOn, String expiresOn, int type, String hash);
	/**
	 * Retrieves the list of file infoes only expired
	 * @return
	 */
	public abstract List<FileInfo> getExpiredFileInfoes();
	public abstract Date getLastModified(String fileName);
	/**
	 * Inserts the user info
	 * @param info
	 * @return
	 */
	public abstract boolean pushUserInfo(UserInfo info);
	/**
	 * Updates the user info
	 * @param info
	 */
	public abstract void updateUserInfo(UserInfo info);
	/**
	 * Retrieves the User info
	 * @param sso
	 * @return
	 */
	public abstract UserInfo getUserInfo(final String sso);
	/**
	 * Inserts the cert info
	 * @param info
	 * @return
	 */
	public abstract boolean pushCertInfo(CertInfo info);
	
	/**
	 * retrieves the cert info
	 * @param sso
	 * @return
	 */
	public abstract CertInfo getCertInfo(final String sso);
	
	/**
	 * Executes the SQL statements. Used for debugging
	 * @param sql
	 */
	public abstract void execSql(final String sql);
}
