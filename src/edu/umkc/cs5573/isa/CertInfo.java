package edu.umkc.cs5573.isa;

public class CertInfo {
	private String sso;
	private String cert;
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
	public CertInfo(String sso, String cert) {
		super();
		this.sso = sso;
		this.cert = cert;
	}
	
}
