package edu.umkc.cs5573.isa;

import java.nio.file.Path;

public interface IWatchExpHandler {
	/**
	 * Called when file has been created
	 * @param child The file or directory path created
	 */
	public void onFileExpired(Path child);
}
