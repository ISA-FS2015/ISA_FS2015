package edu.umkc.cs5573.isa;

import java.io.File;

public interface IWatchExpHandler {
	/**
	 * Called when file has been created
	 * @param child The file or directory path created
	 */
	public void onFileExpired(File child);
}
