package edu.umkc.cs5573.isa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * FileInfo Bean
 * @author Younghwan
 *
 */
public class FileInfo {
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
//	+ "id integer primary key autoincrement, "
//	+ "Filename text not null, "
//	+ "LastModified text not null, "
//	+ "Type int not null, "
//	+ "Hash text not null"
	private int id;
	private String fileName;
	private Date createdOn;
	private Date expiresOn;
	private int type;
	private String hash;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getHash() {
		return hash;
	}
	public void setHash(String hash) {
		this.hash = hash;
	}
	
	public Date getExpiresOn() {
		return expiresOn;
	}
	public void setExpiresOn(Date expiresOn) {
		this.expiresOn = expiresOn;
	}
	public FileInfo(int id, String fileName, String createdOn, String expiresOn, int type, String hash) {
		super();
		this.id = id;
		this.fileName = fileName;
		try {
			this.createdOn = dateFormat.parse(createdOn);
			this.expiresOn = dateFormat.parse(expiresOn);
		} catch (ParseException e) {
			this.createdOn = null;
			this.expiresOn = null;
		}
		this.type = type;
		this.hash = hash;
	}
	
	public FileInfo(String fileName, String createdOn, String expiresOn, int type, String hash) {
		super();
		this.id = 0;
		this.fileName = fileName;
		try {
			this.createdOn = dateFormat.parse(createdOn);
			this.expiresOn = dateFormat.parse(expiresOn);
		} catch (ParseException e) {
			this.createdOn = null;
			this.expiresOn = null;
		}
		this.type = type;
		this.hash = hash;
	}

}
