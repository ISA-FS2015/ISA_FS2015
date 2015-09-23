package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
/**
 * This class manages peer list through UDP broadcasting.
 * @author Younghwan
 *
 */
public class CyborgUdpThread extends Thread {
	protected final static int BUFFER_SIZE = 1024;
	protected final static int PORT_NO = 55730;
	private final static String CHARSET_UTF8 = "UTF-8";
	//protected final static String KEY_USER = "user";
	protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    private boolean isRunning = false;
    private String userName;
    /**
     * For managing peer list. Key will be username and value will be ip address
     */
    protected Map<String, String> userList = null;
    
    public CyborgUdpThread(String threadName, String userName) throws IOException {
        super(threadName + "_" + userName);
        socket = new DatagramSocket(PORT_NO);
        isRunning = true;
        userList = new HashMap<String, String>();
        userList.put(userName, socket.getLocalAddress().getHostAddress());
    }
    // Getter
	public boolean isRunning() {
		return isRunning;
	}
	public Map<String, String> getUserList(){
		return userList;
	}
	public String getIpAddress(String userName){
		return userList.get(userName);
	}
	public boolean isUserExists(String userName){
		return userList.containsKey(userName);
	}
	
	/**
	 * Actually runs here!
	 */
    public void run() {
    	while(isRunning){
            try {
        		byte[] buf = new byte[BUFFER_SIZE];
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				InetAddress address = packet.getAddress();
				if(!socket.getLocalAddress().getHostAddress().equals(address.getHostAddress())) {
					String received = new String(packet.getData(), CHARSET_UTF8);
					String reply = processInput(received);
					if(reply != null) {
						buf = reply.getBytes();
		                int port = packet.getPort();
		                packet = new DatagramPacket(buf, buf.length, address, port);
		                socket.send(packet);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
    	socket.close();
    }
    
    public void stopThread() {
    	isRunning = false;
    }
    
    public String processInput(String input){
    	try {
        	CTP ctp = new CTP(input);
        	if(ctp.getType() == CTP.CTP_TYPE_REQ){
        		if(CTP.REQ_USERLIST.equals(ctp.getDataType())){
        			return CTP.buildRes_PeerList(userList);
        		}else if(CTP.REQ_JOINUSER.equals(ctp.getDataType())){
        			return CTP.buildRes_PeerList(userList);
        		}else{
        			return CTP.buildErr_Unrecognized();
        		}
        	}else if (ctp.getType() == CTP.CTP_TYPE_RES){
        		// process response. return must be null
        		if(CTP.RES_OK.equals(ctp.getDataType())){
        			
        		}else if(CTP.RES_USERLIST.equals(ctp.getDataType())){
        			ctp.putPeerList(userList);
        		}else {
        			
        		}
        		return null;
        	}
        	
    	}catch(JSONException e) {
    		return null;
    	}
		return null;

    }
    
    
    
    // Request Functions
    public void reqUserList(){
    	new UdpRequestThread(UdpRequestThread.REQUEST_USER_LIST, socket).start();
    }
    public void reqJoinUser(){
    	new UdpRequestThread(UdpRequestThread.REQUEST_JOIN_USER, socket).start();
    }
    
    
    // Outgoing process functions - Start

    // Outgoing process functions - End
    
    // Incoming process functions - Start

    // Incoming process functions - End
    
    class UdpRequestThread extends Thread{
    	final static int REQUEST_USER_LIST = 0;
    	final static int REQUEST_JOIN_USER = 1;
    	private int reqType;
    	private DatagramSocket socket = null;
    	private byte[] buf = null;
    	public UdpRequestThread(int reqType, DatagramSocket sock){
        	buf = new byte[BUFFER_SIZE];
    		this.reqType = reqType;
    		this.socket = sock;
    	}
    	/**
    	 * Actually runs here!
    	 */
        public void run() {
        	if(!socket.isClosed()){
        		switch(reqType){
	        		case REQUEST_USER_LIST:
	        		{
	        			reqPeerList();
		        		break;
		        	}
	        		case REQUEST_JOIN_USER:
	        		{
	        			joinUser(userName);
	        			break;
	        		}
        		}
        	}
        }
        
        public void reqPeerList(){
        	sendPacket(CTP.buildReq_PeerList());
        }
        
        public void joinUser(String userName){
        	sendPacket(CTP.buildReq_JoinUser(userName, socket.getLocalAddress().getHostAddress()));
        }
        
        public void sendPacket(String packet){
            try {
            	buf = packet.getBytes(CHARSET_UTF8);
            	InetAddress address = InetAddress.getByName("255.255.255.255");
                DatagramPacket sPacket = new DatagramPacket(buf, buf.length, address, PORT_NO);
                this.socket.send(sPacket);
    		} catch (IOException | JSONException e) {
    			e.printStackTrace();
    		}

        }
    }
}
