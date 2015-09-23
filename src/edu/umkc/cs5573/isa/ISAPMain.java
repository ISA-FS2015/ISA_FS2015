package edu.umkc.cs5573.isa;

import java.io.File;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;

public class ISAPMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String tobeHashed = "Test!!";
		System.out.println("Hash of " + tobeHashed + " : " + SHA256Helper.getHashString(tobeHashed));

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
		serverFactory.setUserManager(userManagerFactory.createUserManager());
		// start the server
		FtpServer server = serverFactory.createServer(); 
		try {
			server.start();
		} catch (FtpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
