package edu.umkc.cs5573.isa;

import java.nio.file.Path;

public interface IWatchDirHandler {
	public void onRegisterCallback(Path dir);
	public void onFileCreated(Path child);
	public void onFileModified(Path child);
	public void onFileDeleted(Path child);
}
