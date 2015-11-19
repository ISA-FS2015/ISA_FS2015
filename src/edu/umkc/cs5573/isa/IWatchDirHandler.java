package edu.umkc.cs5573.isa;

import java.io.File;
import java.util.List;
/**
 * Watch Directory Interface which handles the file change event
 * @author Younghwan
 *
 */
public interface IWatchDirHandler {
	/**
	 * Called when directory or file is registered in WatchDir service
	 * @param dir The directory or file path
	 */
	public void onRegisterCallback(File file);
	/**
	 * Called when directory or file set is registered in WatchDir service
	 * @param dirs the list of directory or file path
	 */
	public void onRegisterCallback(List<File> files);
	/**
	 * Called when file has been created
	 * @param child The file or directory path created
	 */
	public void onFileCreated(File child);
	/**
	 * Called when file has been modified. Also called when file has been created on some platforms or OSes.
	 * @param child The file or directory path modified
	 */
	public void onFileModified(File child);
	/**
	 * Called when file has been deleted
	 * @param child The file or directory path deleted
	 */
	public void onFileDeleted(File child);
}
