package edu.umkc.cs5573.isa;


/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
 
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

import com.almworks.sqlite4java.SQLiteException;
 
/**
 * Example to watch a directory (or tree) for changes to files.
 */
 
public class WatchDir extends Thread{
 
    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean trace = false;
    private boolean isRunning = false;
 
    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
 
    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else {
                if (!dir.equals(prev)) {
                    System.out.format("update: %s -> %s\n", prev, dir);
                }
            }
        }
        if(!dir.toFile().isDirectory()){
    		try {
    	        SQLiteInstance sql = SQLiteInstance.getInstance();
    	        if(sql.getFileHash(dir) == null){
    	        	sql.pushFileInfo(dir, Resources.CYBORG_FILE_TYPE_ORIGINAL);
    	        }
    		} catch (SQLiteException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        }
        keys.put(key, dir);
    }
 
    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException
            {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
 
    /**
     * Creates a WatchService and registers the given directory
     */
    WatchDir(String dirPath, boolean recursive) throws IOException {
		Path dir = Paths.get(dirPath);
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
        this.recursive = recursive;
        this.isRunning = true;
        if (recursive) {
            System.out.format("Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println("Done.");
        } else {
            register(dir);
        }
 
        // enable trace after initial registration
        this.trace = true;
    }
    
    /**
     * Process all events for keys queued to the watcher
     */
    public void run() {
        while (isRunning) {
 
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }
 
            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }
 
            for (WatchEvent<?> event: key.pollEvents()) {
                Kind<?> kind = event.kind();
 
                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }
 
                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);
 
                // print out event
                System.out.format("%s: %s\n", event.kind().name(), child);
 
                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }else{
                        	handleCreatEvent(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readbale
                    }
                }else if(recursive && (kind == ENTRY_MODIFY)) {
                	handleModifyEvent(child);
                }else if(recursive && (kind == ENTRY_DELETE)) {
                	handleDeleteEvent(child);
                }
            }
 
            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
 
                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }
    
    public void stopService(){
    	System.out.println("Stopping Watchdir service...");
    	isRunning = false;
    }
 
    // Now handle every file event so that we can control our security!! - Start
    
    public void handleCreatEvent(Path child){
    	String filePath = child.toString();
		Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
		try {
	        SQLiteInstance sql = SQLiteInstance.getInstance();
	        if(sql.getFileHash(child) == null){
	        	sql.pushFileInfo(child, Resources.CYBORG_FILE_TYPE_ORIGINAL);
	        }
		} catch (SQLiteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void handleModifyEvent(Path child){
    	if(child.toFile().exists()){
        	String filePath = child.toString();
    		Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
    		try {
    	        SQLiteInstance sql = SQLiteInstance.getInstance();
    	        if(sql.getFileHash(child) == null){
    	        	sql.pushFileInfo(child, Resources.CYBORG_FILE_TYPE_ORIGINAL);
    	        }
    		} catch (SQLiteException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}
    }
    
    public void handleDeleteEvent(Path child){
    	if(child.toFile().exists()){
    		Logger.d(this, "Abs Path:" + child.toAbsolutePath().toString());
    		Logger.d(this, "HASH:" + SHA256Helper.getHashStringFromFile(child));
    	}

    }
    

    // Now handle every file event so that we can control our security!! - End
 
    static void usage() {
        System.err.println("usage: java WatchDir [-r] dir");
        System.exit(-1);
    }
    
    
 
//    public static void main(String[] args) throws IOException {
//        // parse arguments
//        if (args.length == 0 || args.length > 2)
//            usage();
//        boolean recursive = false;
//        int dirArg = 0;
//        if (args[0].equals("-r")) {
//            if (args.length < 2)
//                usage();
//            recursive = true;
//            dirArg++;
//        }
// 
//        // register directory and process its events
//        Path dir = Paths.get(args[dirArg]);
//        new WatchDir(dir, recursive).processEvents();
//    }
}
