package edu.umkc.cs5573.isa;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
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
 *   - Neither the name of Oracle or the names of its
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