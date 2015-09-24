package edu.umkc.cs5573.isa;

import java.io.File;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

public class CyborgFtpServer extends Thread{
	private boolean isRunning = false;
	private FtpServer server;
	public CyborgFtpServer(){
		super("CyborgFtpServer");
		isRunning = true;
	}
	public void initServer(){
		// Running FTP server!
		FtpServerFactory serverFactory = new FtpServerFactory();
		ListenerFactory factory = new ListenerFactory();
		// set the port of the listener
		factory.setPort(2221);
		// define SSL configuration
		//SslConfigurationFactory ssl = new SslConfigurationFactory();
		//ssl.setKeystoreFile(new File("src/test/resources/ftpserver.jks"));
		//ssl.setKeystorePassword("password");
		// set the SSL configuration for the listener
		//factory.setSslConfiguration(ssl.createSslConfiguration());
		//factory.setImplicitSsl(true);
		// replace the default listener
		serverFactory.addListener("default", factory.createListener());
		PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
		userManagerFactory.setFile(new File("res/conf/users.properties"));
//				UserManager um = userManagerFactory.createUserManager();
//				BaseUser user = new BaseUser();
//				user.setName("cyborgman");
//				user.setPassword("isafs2015");
//				user.setHomeDirectory("res/home/cyborgman");
//				try {
//					um.save(user);
//				} catch (FtpException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
		serverFactory.setUserManager(userManagerFactory.createUserManager());
		server = serverFactory.createServer(); 

	}
	
	public void run(){
		startFtpServer();
	}
	
	public void stopFtpServer(){
		if(server !=null){
			Logger.d(this, "Stopping FTP server...");
			server.stop();
		}
	}
	
	
	public void startFtpServer(){
		if(server !=null){
			// start the server
			try {
				server.start();
			} catch (FtpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
