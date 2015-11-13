package edu.umkc.cs5573.isa;

import java.io.File;

public class FileTooBigException extends Exception {
	private File file;
	public FileTooBigException(File file){
		this.file = file;
	}
	
	@Override
	public String getMessage(){
		return "File " + file.getName() + " is too big.";
		
	}
}
