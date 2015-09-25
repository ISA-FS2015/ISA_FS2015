package edu.umkc.cs5573.isa;

import java.nio.file.Path;
import java.util.List;

public interface IWatchDirHandler {
	public void onRegisterCallback(Path dir);
	public void onRegisterCallback(List<Path> dirs);
	public void onFileCreated(Path child);
	public void onFileModified(Path child);
	public void onFileDeleted(Path child);
}
