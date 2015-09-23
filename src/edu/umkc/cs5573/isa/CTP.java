package edu.umkc.cs5573.isa;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Handles the request and response messages using JSON format
 * @author Younghwan
 *
 */
public class CTP {
	final static String KEY_REQ_TYPE = "req_type";
	final static String KEY_USER = "user";
	final static String KEY_IP = "ip";
	final static String KEY_MSG_LENGTH = "length";
	final static String KEY_REQ_MSG = "req_message";
	final static String KEY_RES_MSG = "res_message";
	final static String REQ_USERLIST = "user_list";
	final static String REQ_JOINUSER = "join_user";
	final static String REQ_LEAVEUSER = "leave_user";
	final static String REQ_FILELIST = "file_list";
	final static String REQ_FILE = "file";
	final static String KEY_RES_TYPE = "res_type";
	final static String RES_USERLIST = "user_list";
	final static String RES_ERROR = "error";
	final static String RES_OK = "ok";
	
	final static int CTP_TYPE_REQ = 0;
	final static int CTP_TYPE_RES = 1;
	
    
    private int ctpType;
    private String dataType;
    private String payload;
    private int msgLength;
    
    public CTP(String rawMsg) throws JSONException{
    	JSONObject jObj = new JSONObject(rawMsg);
    	if(jObj.has(KEY_REQ_TYPE)) {
    		ctpType = CTP_TYPE_REQ;
    		// process request
    		this.dataType = jObj.getString(KEY_REQ_TYPE);
    		this.payload = jObj.getString(KEY_REQ_MSG);
    		this.msgLength = jObj.getInt(KEY_MSG_LENGTH);
    	}else if(jObj.has(KEY_RES_TYPE)) {
    		ctpType = CTP_TYPE_RES;
    		// process request
    		this.dataType = jObj.getString(KEY_RES_TYPE);    		
    		this.payload = jObj.getString(KEY_RES_MSG);
    		this.msgLength = jObj.getInt(KEY_MSG_LENGTH);
    	}else{
    		throw new JSONException("Unsupported CTP Type");
    	}
    }
    public int getType(){
    	return this.ctpType;
    }
    public String getDataType(){
    	return this.dataType;
    }
    public static String buildReq_PeerList(){
    	JSONObject jObj = new JSONObject();
    	jObj.append(KEY_REQ_TYPE, REQ_USERLIST);
    	return jObj.toString();
    }
    public static String buildReq_JoinUser(String userName, String ipAddress){
    	JSONObject jObj = new JSONObject();
    	JSONObject msgObj = new JSONObject();
    	msgObj.append(KEY_USER, userName);
    	msgObj.append(KEY_IP, ipAddress);
    	jObj.append(KEY_REQ_TYPE, REQ_JOINUSER);
    	jObj.append(KEY_RES_MSG, msgObj);
    	jObj.append(KEY_MSG_LENGTH, msgObj.toString());
    	return jObj.toString();
    }
    
    public static String buildRes_PeerList(Map<String, String> userList){
    	JSONArray jArr = new JSONArray();
		JSONObject obj = new JSONObject();
		for(Map.Entry<String, String> entry : userList.entrySet()){
			obj.put(KEY_USER, entry.getKey());
			obj.put(KEY_IP, entry.getValue());
			jArr.put(obj);
		}
		obj = new JSONObject();
		obj.append(KEY_RES_TYPE, RES_USERLIST);
		obj.append(KEY_RES_MSG, jArr);
		obj.put(KEY_MSG_LENGTH, jArr.toString().length());
		return obj.toString();
    }
    public static String buildErr_Unrecognized(){
    	return new JSONObject()
		.append(KEY_RES_TYPE, RES_ERROR)
		.append(KEY_RES_MSG, "Unrecognized")
		.append(KEY_MSG_LENGTH, "Unrecognized".length()).toString();
    }
    public static String buildRes_Ok(String msg){
		return new JSONObject()
				.append(KEY_RES_TYPE, RES_OK)
				.append(KEY_RES_MSG, msg)
				.append(KEY_MSG_LENGTH, msg.length()).toString();
    }
    
    public String putPeerList(Map<String, String> userList){
    	if(msgLength == payload.length()){
    		try{
        		JSONArray jArr = new JSONArray(payload);
        		for(int i=0; i < jArr.length() ; i++){
        			JSONObject obj = jArr.getJSONObject(i);
        			if(obj.getString(KEY_IP).split(".").length == 4){
        				userList.put(obj.getString(KEY_USER), obj.getString(KEY_IP));
        			}else{
        				Logger.d("IP Parsing error: " + obj.getString(KEY_IP));
        			}
        		}
        		return buildRes_Ok("Successfully added.");
    		}
    		catch (JSONException e){
    			e.printStackTrace();
    		}
    	}
    	Logger.d("Parsing error");
    	return null;
    }
}