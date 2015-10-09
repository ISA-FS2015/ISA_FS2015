package edu.umkc.cs5573.isa;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.almworks.sqlite4java.SQLiteException;


/**
 * Handles the request and response messages using JSON format
 * @author Younghwan
 *
 */
public class CTP {
	final static String KEY_REQ_TYPE = "req_type";
	final static String KEY_USER = "user";
	final static String KEY_IP = "ip";
	final static String KEY_FILENAME = "file_name";
	final static String KEY_FILETYPE = "file_type";
	final static String KEY_MSG_LENGTH = "length";
	final static String KEY_REQ_MSG = "req_message";
	final static String KEY_RES_MSG = "res_message";
	final static String REQ_USERLIST = "user_list";
	final static String REQ_JOINUSER = "join_user";
	final static String REQ_PROBE = "probe_req";
	final static String REQ_FILE_PROBE = "file_probe_req";
	final static String REQ_LEAVEUSER = "leave_user";
	final static String REQ_FILELIST = "file_list";
	final static String REQ_FILE = "file";
	final static String KEY_RES_TYPE = "res_type";
	final static String RES_USERLIST = "user_list";
	final static String RES_FILELIST = "file_list";
	final static String RES_FILE_PROBE = "file_probe_res";
	final static String RES_PROBE = "probe_res";
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
    	jObj.put(KEY_REQ_TYPE, REQ_USERLIST);
    	return jObj.toString();
    }
    public static String buildReq_JoinUser(String userName, String ipAddress){
		Map<String, String> params = new HashMap<String, String>();
		params.put(KEY_USER, userName);
		params.put(KEY_IP, ipAddress);
    	return build_req(REQ_JOINUSER, params);
    }
    
    public static String buildReq_Probe(String userName, String ipAddress){
		Map<String, String> params = new HashMap<String, String>();
		params.put(KEY_USER, userName);
		params.put(KEY_IP, ipAddress);
    	return build_req(REQ_PROBE, params);
    }

	public static String build_FileList(String userName, List<FileInfo> fileInfoes) {
    	JSONArray jArr = new JSONArray();
		JSONObject obj = new JSONObject();
		for(FileInfo item : fileInfoes){
			obj.put(KEY_USER, userName);
			obj.put(KEY_FILENAME, item.getFileName());
			obj.put(KEY_FILETYPE, item.getType());
			jArr.put(obj);
		}
		obj = new JSONObject();
		
		obj.put(KEY_RES_TYPE, RES_USERLIST);
		obj.put(KEY_RES_MSG, jArr);
		obj.put(KEY_MSG_LENGTH, jArr.toString().length());
		return obj.toString();
	}
	
    public String buildRes_JoinUser(Map<String, String> userList){
    	if(msgLength == payload.length()){
    		try{
        		JSONObject obj = new JSONObject(payload).getJSONObject(KEY_REQ_MSG);
        		if(userList.containsKey(obj.get(KEY_USER))){
        			if(userList.get(obj.get(KEY_USER)).equals(obj.get(KEY_IP))){
        				userList.put(obj.getString(KEY_USER), obj.getString(KEY_IP));
            			return buildRes_Ok("Successfully added.");
        			}else{
            			return buildRes_Err("Already Exists. Try other username.");
        			}
        		}else{
    				userList.put(obj.getString(KEY_USER), obj.getString(KEY_IP));
        			return buildRes_Ok("Successfully added.");
        		}
    		}
    		catch (JSONException e){
    			e.printStackTrace();
    		}
    	}
    	return buildErr_Unrecognized();
    }
    
    public String buildRes_Probe(Map<String, String> userList){
    	if(msgLength == payload.length()){
    		try{
        		JSONObject obj = new JSONObject(payload).getJSONObject(KEY_REQ_MSG);
        		if(userList.containsKey(obj.get(KEY_USER))){
        			if(userList.get(obj.get(KEY_USER)).equals(obj.get(KEY_IP))){
            			return buildRes_Ok("You already joined.");
        			}else{
            			return buildRes_Err("Already Exists. Try other username.");
        			}
        		}else{
    				userList.put(obj.getString(KEY_USER), obj.getString(KEY_IP));
        			return buildRes_Ok("You can use the name.");
        		}
    		}
    		catch (JSONException e){
    			e.printStackTrace();
    		}
    	}
    	return buildErr_Unrecognized();
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
		obj.put(KEY_RES_TYPE, RES_USERLIST);
		obj.put(KEY_RES_MSG, jArr);
		obj.put(KEY_MSG_LENGTH, jArr.toString().length());
		return obj.toString();
    }
    
    public static String buildRes_Err(String msg){
    	return new JSONObject()
		.put(KEY_RES_TYPE, RES_ERROR)
		.put(KEY_RES_MSG, msg)
		.put(KEY_MSG_LENGTH, msg.length()).toString();
    }
    
    public static String buildErr_Unrecognized(){
    	return new JSONObject()
		.put(KEY_RES_TYPE, RES_ERROR)
		.put(KEY_RES_MSG, "Unrecognized")
		.put(KEY_MSG_LENGTH, "Unrecognized".length()).toString();
    }
    
    public static String buildRes_Ok(String msg){
		return new JSONObject()
				.put(KEY_RES_TYPE, RES_OK)
				.put(KEY_RES_MSG, msg)
				.put(KEY_MSG_LENGTH, msg.length()).toString();
    }
    
    public String getMessage(){
    	return payload;
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
        				Logger.d(this, "IP Parsing error: " + obj.getString(KEY_IP));
        			}
        		}
        		return buildRes_Ok("Successfully added.");
    		}
    		catch (JSONException e){
    			e.printStackTrace();
    		}
    	}
    	Logger.d(this, "Parsing error");
    	return null;
    }
	public static String buildReq_File_Probe(String fileName) {
		Map<String, String> params = new HashMap<String, String>();
		params.put(KEY_FILENAME, fileName);
    	return build_req(REQ_FILE_PROBE, params);
	}
	
	private static String build_req(String reqType, Map<String, String> params)
	{
    	JSONObject jObj = new JSONObject();
    	JSONObject msgObj = new JSONObject();
    	for(Entry<String, String> entry : params.entrySet()){
        	msgObj.put(entry.getKey(), entry.getValue());
    	}
    	jObj.put(KEY_REQ_TYPE, reqType);
    	jObj.put(KEY_REQ_MSG, msgObj);
    	jObj.put(KEY_MSG_LENGTH, msgObj.toString().length());
    	return jObj.toString();
	}
	private static String build_res(String resType, Map<String, String> params)
	{
    	JSONObject jObj = new JSONObject();
    	JSONObject msgObj = new JSONObject();
    	for(Entry<String, String> entry : params.entrySet()){
        	msgObj.put(entry.getKey(), entry.getValue());
    	}
    	jObj.put(KEY_RES_TYPE, resType);
    	jObj.put(KEY_REQ_MSG, msgObj);
    	jObj.put(KEY_MSG_LENGTH, msgObj.toString().length());
    	return jObj.toString();
	}
	public String buildRes_File_Probe(String homeDirectory, String userName, String ipAddress) {
    	if(msgLength == payload.length()){
    		try{
        		JSONObject obj = new JSONObject(payload).getJSONObject(KEY_REQ_MSG);
        		String fileName = obj.getString(KEY_FILENAME);
        		SQLiteInstance sql = SQLiteInstance.getInstance();
        		FileInfo info = sql.getFileInfo(Paths.get(homeDirectory + "/" + fileName));
        		if(info == null){
        			return null;
        		}else{
        			Map<String, String> params = new HashMap<String, String>();
        			params.put(KEY_FILENAME, fileName);
        			params.put(KEY_USER, userName);
        			params.put(KEY_IP, ipAddress);
        	    	return build_res(RES_FILE_PROBE, params);
        		}
    		}
    		catch (JSONException | SQLiteException e){
    			e.printStackTrace();
    		}
    	}
    	return buildErr_Unrecognized();
	}
}