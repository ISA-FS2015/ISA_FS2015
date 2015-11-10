package edu.umkc.cs5573.isa;

import java.util.Map;

public interface ICyborgEventHandler {

	public final static int NOT_INITIALIZED = 0;
	
	public final static int REACT_VALIDATION_USER = 0;
	public final static int REACT_VIOLATION = 1;
	
	public void onResultFailed(int reason);

	public void onConnectionEnds();

	public void onUserListResult(Map<String, String> userList);
	
	public void onFileProbeResult(String payload);
	
	public void onOkReceived(String payload);
	
	public void onReactionRequired(int code, String message);
	
	public void onReactionPerformed(String message);
	
	public void onTcpReqFailed(String message);
	
	public void onTcpReqSuccess(String message);

}
