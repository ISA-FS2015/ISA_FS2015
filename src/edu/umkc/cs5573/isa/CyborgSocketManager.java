package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class CyborgSocketManager {
	Socket mSock;
	ServerSocket mServerSk;
	
	public CyborgSocketManager(){
		
	}
	
	void serverSide(String ip, int portNumber){
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(portNumber);
			Socket clientSocket = serverSocket.accept();
			PrintWriter out =
			    new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(
			    new InputStreamReader(clientSocket.getInputStream()));
			mSock = serverSocket.accept();
			String inputLine, outputLine;
            
		    // Initiate conversation with client
		    CTP kkp = new CTP("C:/Test");
		    outputLine = kkp.processInput(null);
		    out.println(outputLine);

		    while ((inputLine = in.readLine()) != null) {
		        outputLine = kkp.processInput(inputLine);
		        out.println(outputLine);
		        if (outputLine.equals("Bye."))
		            break;
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void clientSice(String hostName, int portNumber){
 
        try (
            Socket kkSocket = new Socket(hostName, portNumber);
            PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(kkSocket.getInputStream()));
        ) {
            BufferedReader stdIn =
                new BufferedReader(new InputStreamReader(System.in));
            String fromServer;
            String fromUser;
 
            while ((fromServer = in.readLine()) != null) {
                System.out.println("Server: " + fromServer);
                if (fromServer.equals("Bye."))
                    break;
                 
                fromUser = stdIn.readLine();
                if (fromUser != null) {
                    System.out.println("Client: " + fromUser);
                    out.println(fromUser);
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        }
	}
}
