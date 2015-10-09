package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.almworks.sqlite4java.SQLiteException;
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
    private Logger logger;
    private boolean isRunning = false;
    private String userName;
    private String homeDirectory;
    private String localIpAddress;
    private String broadcastIpAddress;
    /**
     * For managing peer list. Key will be username and value will be ip address
     */
    protected Map<String, String> userList = null;
    /**
     * Creates UDP communication thread.
     * @param threadName The name of thread
     * @param userName User name to be userd
     * @param ifName Name of the interface such as eth0 or wlan0. wlan0 is mainly used.
     * @throws IOException
     */
    public CyborgUdpThread(String threadName, String userName, String ifName, String homeDirectory) throws IOException {
        super(threadName + "_" + userName);
        this.logger = Logger.getInstance();
        for(Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();){
        	NetworkInterface iface = e.nextElement();
        	if(iface.getName().equals(ifName)){
        		logger.d(this, iface.getName());
            	for(InterfaceAddress addr : iface.getInterfaceAddresses()){
            		logger.d(this, "\t" + addr.getAddress().getHostAddress());
            		if(addr.getAddress().getHostAddress().contains(".")){
                    	System.out.println("\t" + addr.getAddress().getHostAddress());
                    	System.out.println("\t" + addr.getBroadcast().getHostAddress());
                        localIpAddress = addr.getAddress().getHostAddress();
                        broadcastIpAddress = addr.getBroadcast().getHostAddress();
            		}
            	}
        	}
        }
        if(localIpAddress == null) throw new IOException("No interface named \"" + ifName + "\" exists ");
        logger.d(this, "Starting UDP Service...");
        socket = new DatagramSocket(PORT_NO);
        logger.d(this, "My Local IP is :" + localIpAddress);
//    	String[] ipSlice = localIpAddress.split(".");
//    	if(ipSlice.length == 4){
//        	ipSlice[3] = "255";
//        	String broadCast = ipSlice[0] + "." + ipSlice[1] + "." + ipSlice[2] + "." + ipSlice[3];
//    	}else{
//    		throw new IOException("IP Error. Something wrong!!");
//    	}
        isRunning = true;
        this.homeDirectory = homeDirectory;
        userList = new HashMap<String, String>();
        userList.put(userName, localIpAddress);
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
				if(!localIpAddress.equals(address.getHostAddress())) {
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
    	logger.d(this, "Stopping UDP Thread...");
    	isRunning = false;
    }
    
    public String processInput(String input){
    	try {
        	CTP ctp = new CTP(input);
        	if(ctp.getType() == CTP.CTP_TYPE_REQ){
        		logger.d(this, "REQ: " + ctp.getDataType());
        		if(CTP.REQ_USERLIST.equals(ctp.getDataType())){
        			return CTP.buildRes_PeerList(userList);
        		}else if(CTP.REQ_JOINUSER.equals(ctp.getDataType())){
        			return ctp.buildRes_JoinUser(userList);
        		}else if(CTP.REQ_PROBE.equals(ctp.getDataType())){
        			return ctp.buildRes_Probe(userList);
        		}else if(CTP.REQ_FILE_PROBE.equals(ctp.getDataType())){
        			return ctp.buildRes_File_Probe(homeDirectory, userName, localIpAddress);
        		}else{
        			return CTP.buildErr_Unrecognized();
        		}
        	}else if (ctp.getType() == CTP.CTP_TYPE_RES){
        		// process response. return must be null
        		if(CTP.RES_OK.equals(ctp.getDataType())){
        			logger.d(this, "Received: " + ctp.getMessage());
        		}else if(CTP.RES_USERLIST.equals(ctp.getDataType())){
        			ctp.putPeerList(userList);
        		}else if(CTP.RES_FILE_PROBE.equals(ctp.getDataType())){
        			logger.d(this, "Received: " + ctp.getMessage());
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
    public void reqFileProbe(String fileName){
    	new UdpRequestThread(UdpRequestThread.REQUEST_FILE_PROBE, socket).addArgument(fileName).start();
    }
    public void reqUserList(){
    	new UdpRequestThread(UdpRequestThread.REQUEST_USER_LIST, socket).start();
    }
    public void reqJoinUser(){
    	new UdpRequestThread(UdpRequestThread.REQUEST_JOIN_USER, socket).start();
    }
    public void reqProbe(){
    	new UdpRequestThread(UdpRequestThread.REQUEST_PROBE, socket).start();
    }
    
    
    // Outgoing process functions - Start

    // Outgoing process functions - End
    
    // Incoming process functions - Start

    // Incoming process functions - End
    
    class UdpRequestThread extends Thread{
		final static int REQUEST_USER_LIST = 0;
    	final static int REQUEST_JOIN_USER = 1;
    	final static int REQUEST_PROBE = 2;
    	final static int BROADCAST_FILE_LIST = 3;
    	public static final int REQUEST_FILE_PROBE = 4;
    	/**
    	 * Some network does not allow using broadcast IP address. In that case, we need to 
    	 * emulate the broadcasting(Just sending data from IP x.x.x.1 to x.x.x.254
    	 */
    	private boolean softBroadcast = true;
    	private int reqType;
    	private String arg = null;
    	private DatagramSocket socket = null;
    	private byte[] buf = null;
    	public UdpRequestThread(int reqType, DatagramSocket sock){
        	buf = new byte[BUFFER_SIZE];
    		this.reqType = reqType;
    		this.socket = sock;
    	}
    	public UdpRequestThread addArgument(String arg){
    		this.arg = arg;
    		return this;
    	}
    	public void setSoftBroadcast(boolean softBroadcast){
    		this.softBroadcast = softBroadcast;
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
	        		case REQUEST_PROBE:
	        		{
	        			probe(userName);
	        			break;
	        		}
	        		case REQUEST_FILE_PROBE:
	        		{
	        			fileProbe(arg);
	        			break;
	        		}
	        		case BROADCAST_FILE_LIST:
	        		{
	        			try {
							broadcastFileList();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	        			break;
	        		}
        		}
        	}
        }

        private void fileProbe(String fileName) {
        	broadcastPacket(CTP.buildReq_File_Probe(fileName));
		}
		@Deprecated
        public void reqPeerList(){
        	broadcastPacket(CTP.buildReq_PeerList());
        }
        
        public void joinUser(String userName){
        	broadcastPacket(CTP.buildReq_JoinUser(userName, localIpAddress));
        }
        
        public void probe(String userName){
        	broadcastPacket(CTP.buildReq_Probe(userName, localIpAddress));
        }
        
        public void broadcastFileList() throws IOException{
            try {
				SQLiteInstance sql = SQLiteInstance.getInstance();
				CTP.build_FileList(userName, sql.getFileInfoes());
			} catch (SQLiteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
        }

        
        public void sendPacketTo(String packet, String ipAddress){
        	sendPacket(packet, ipAddress);
        }
        public void broadcastPacket(String packet){
        	if(softBroadcast){
        		String[] ipSets = localIpAddress.split("\\.");
        		for(int i= 1; i <= 255 ; i++){
        			String targetIp = String.format("%s.%s.%s.%s", ipSets[0], ipSets[1], ipSets[2], Integer.toString(i));
        			if(!targetIp.equals(localIpAddress) && !targetIp.equals(broadcastIpAddress)){
                    	sendPacket(packet, targetIp);
        			}
        		}
        	}else{
            	sendPacket(packet, broadcastIpAddress);
        	}
        }
        private void sendPacket(String packet, String ipAddress){
            try {
            	buf = packet.getBytes(CHARSET_UTF8);
            	InetAddress address = InetAddress.getByName(ipAddress);
                DatagramPacket sPacket = new DatagramPacket(buf, buf.length, address, PORT_NO);
                this.socket.send(sPacket);
    		} catch (IOException | JSONException e) {
    			e.printStackTrace();
    		}

        }
    }
}
