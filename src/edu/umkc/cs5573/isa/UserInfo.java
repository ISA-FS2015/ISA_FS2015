package edu.umkc.cs5573.isa;

public class UserInfo {
	public final static int TYPE_STUDENT = 0;
	public final static int TYPE_EMPLOYEE = 1;
	
	private String sso;
	private int type;
	private String name;
	private String organization;
	private String email;
	private String phoneNumber;
	private String privateKey;
	private String publicKey;
	private int score;
	
	public UserInfo(String sso, int type, String name, String organization, String email, String phoneNumber, int score, String privateKey, String publicKey) {
		super();
		this.sso = sso;
		this.type = type;
		this.name = name;
		this.organization = organization;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.score = score;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}
	public String getSso() {
		return sso;
	}
	public void setSso(String sso) {
		this.sso = sso;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public String getPhoneNumber() {
		return phoneNumber;
	}
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}
	public String getPrivateKey() {
		return privateKey;
	}
	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}
	public String getPublicKey() {
		return publicKey;
	}
	public void setPublicKey(String publicKey) {
		this.publicKey = publicKey;
	}
	
}
