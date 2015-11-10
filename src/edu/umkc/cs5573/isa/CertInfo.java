package edu.umkc.cs5573.isa;

public class CertInfo {
	private String sso;
	private String cert;
	private String privateKey;
	public String getSso() {
		return sso;
	}
	public void setSso(String sso) {
		this.sso = sso;
	}
	public String getCert() {
		return cert;
	}
	public void setCert(String cert) {
		this.cert = cert;
	}
	
	/**
	 * @return the privateKey
	 */
	public String getPrivateKey() {
		return privateKey;
	}
	/**
	 * @param privateKey the privateKey to set
	 */
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}
	public CertInfo(String sso, String cert, String privateKey) {
		super();
		this.sso = sso;
		this.cert = cert;
		this.privateKey = privateKey;
	}
	
}
