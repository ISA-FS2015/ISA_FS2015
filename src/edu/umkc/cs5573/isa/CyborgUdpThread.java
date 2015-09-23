package edu.umkc.cs5573.isa;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class CyborgUdpThread extends Thread {
	protected final static int BUFFER_SIZE = 1024;
	protected final static int PORT_NO = 55731;
	protected final static String KEY_USER = "user";
	protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    protected boolean isRunning = false;
    protected Map<String, String> userList = null;
    
    public CyborgUdpThread(String name) throws IOException {
        super(name);
        socket = new DatagramSocket(PORT_NO);
        isRunning = true;
        userList = new HashMap<String, String>();
    }
	public boolean isRunning() {
		return isRunning;
	}
	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
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
				String received = new String(packet.getData());
				String reply = processInput(received);
				InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }
    
    public String processInput(String input){
    	JSONObject jObj = new JSONObject(input);
    	String reqType = jObj.getString(CTP.JSON_KEY_REQ_TYPE);
		if(CTP.JSON_REQ_USERLIST.equals(reqType)){
			JSONArray jArr = new JSONArray();
			for(Map.Entry<String, String> entry : userList.entrySet()){
				JSONObject obj = new JSONObject();
				obj.put(CTP.JSON_REQ_USERLIST, entry.getValue());
				jArr.put(obj);
			}
			return jArr.toString();
		}else if(CTP.JSON_KEY_REQ_TYPE.equals(reqType)){
		}else{
			return new JSONObject()
					.append(CTP.JSON_KEY_RES_TYPE, CTP.JSON_RES_ERROR)
					.append(CTP.JSON_KEY_RES_MSG, "Unrecognized").toString();
		}
		return reqType;
    }
    
    public Map<String, String> remoteGetPeerList(){
    	byte[] buf = new byte[BUFFER_SIZE];
    	JSONObject jObj = new JSONObject();
    	jObj.append(CTP.JSON_KEY_REQ_TYPE, CTP.JSON_REQ_USERLIST);
    	buf = jObj.toString().getBytes();
        // receive request
        try {
        	InetAddress address = InetAddress.getByName("255.255.255.255");
            DatagramPacket sPacket = new DatagramPacket(buf, buf.length, address, PORT_NO);
			socket.send(sPacket);
			DatagramPacket rPacket = new DatagramPacket(buf, buf.length);
			JSONArray jArr = new JSONArray(new String(buf));
			for(int i=0; i < jArr.length() ; i++){
				JSONObject obj = jArr.getJSONObject(i);
				userList.put(KEY_USER, obj.getString(CTP.JSON_KEY_USER));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return userList;
    }
}
