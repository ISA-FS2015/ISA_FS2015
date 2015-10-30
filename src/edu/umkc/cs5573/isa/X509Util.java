package edu.umkc.cs5573.isa;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * Class which manipulates X509 certificates easily
 * @author Younghwan
 *
 */
@SuppressWarnings("restriction")
public class X509Util{
	CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
	X509Certificate x509cert;
	//CertificateFactory certificatefactory;
	/**
	 * Constructor. Builds X509 certificates from base64-encoded bytes
	 * @param base64Bytes
	 * @throws CertificateException
	 * @throws IOException
	 */
	public X509Util(String base64Bytes) throws CertificateException, IOException {
		// Each file specified on the command line must contain a single
		// DER-encoded X.509 certificate.  The DER-encoded certificate
		// can be in either binary or ASCII format.
		// Open the file.
		InputStream is = new ByteArrayInputStream(StaticUtil.base64ToBytes(base64Bytes));
		// Generate a certificate from the data in the file.
		this.x509cert =
		  (X509Certificate)certificatefactory.generateCertificate(is);
		is.close();
	}
	/**
	 * Constructor. Builds X509 from .cer file
	 * @param file
	 * @throws CertificateException
	 * @throws IOException
	 */
	public X509Util(File file) throws CertificateException, IOException {
		FileInputStream fileinputstream = new FileInputStream(file);
		// Generate a certificate from the data in the file.
		this.x509cert =
		  (X509Certificate)certificatefactory.generateCertificate(fileinputstream);
		fileinputstream.close();
	}
	/**
	 * Constructor. Builds X509 from byte information
	 * @param byteInformation
	 * @throws CertificateException
	 */
	public X509Util(byte[] byteInformation) throws CertificateException {
		InputStream stream = new ByteArrayInputStream(byteInformation);
		this.x509cert =
		  (X509Certificate)certificatefactory.generateCertificate(stream);
	}
	/**
	 * Retrieves the X509 certificate
	 * @return
	 */
	public X509Certificate getCertificate() {
		return this.x509cert;
	}
	/**
	 * Checks if the certification is valid
	 * @param pbk
	 * @param issuer
	 * @param user
	 * @return true if the cert is valid, issuer is valid, user is valid, public key is valud, false otherwise
	 */
	public boolean isCertValid(PublicKey pbk, String issuer, String user){
		try {
			x509cert.checkValidity();
			// For now, we skip if the issuer is same or not
			//if(!issuer.equals(parseDN(x509cert.getIssuerDN().getName())[0])) return false;
			if(!user.equals(parseDN(x509cert.getSubjectDN().getName())[0])) return false;
			if(!x509cert.getPublicKey().equals(pbk)) return false;
			
			return true;
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	/**
	 * Retrieves the brief key info
	 * @return
	 */
	public String getBriefKeyInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("---Certificate---")
			.append("type = " + x509cert.getType()).append("\n")
			.append("version = " + x509cert.getVersion()).append("\n")
			.append("subject = " + x509cert.getSubjectDN().getName()).append("\n")
			.append("valid from = " + x509cert.getNotBefore()).append("\n")
			.append("valid to = " + x509cert.getNotAfter()).append("\n")
			.append("serial number = " + x509cert.getSerialNumber().toString(16)).append("\n")
			.append("issuer = " + x509cert.getIssuerDN().getName()).append("\n")
			.append("signing algorithm = " + x509cert.getSigAlgName()).append("\n")
			.append("public key algorithm = " + x509cert.getPublicKey().getAlgorithm()).append("\n")
			.append("---Extensions---");
		Set<String> setCritical = x509cert.getCriticalExtensionOIDs();
		if (setCritical != null && setCritical.isEmpty() == false)
		for (Iterator<String> iterator = setCritical.iterator(); iterator.hasNext(); )
			sb.append(iterator.next().toString() + " *critical*").append("\n");
		Set<String> setNonCritical = x509cert.getNonCriticalExtensionOIDs();
		if (setNonCritical != null && setNonCritical.isEmpty() == false)
		for (Iterator<String> iterator = setNonCritical.iterator(); iterator.hasNext(); )
			sb.append(iterator.next().toString()).append("\n");
		// We're done.
		sb.append("---");
		return sb.toString();
	}
	
	/** 
	 * <p>Create a self-signed X.509 Certificate</p>
	 * <p>Example: X509Certificate x509 = issueCertificate("CN=Test", key, 31, "SHA1withRSA");</p>
	 * @param dn the X.509 Distinguished Name, eg "CN=Test, L=London, C=GB"
	 * @param pair the KeyPair
	 * @param days how many days from now the Certificate is valid for
	 * @param algorithm the signing algorithm, eg "SHA1withRSA"
	 */ 
	public static X509Certificate issueCertificate(String issuerDN, String subjectDN, KeyPair pair, int days, String algorithm)
	  throws GeneralSecurityException, IOException
	{
	  PrivateKey privkey = pair.getPrivate();
	  X509CertInfo info = new X509CertInfo();
	  Date from = new Date();
	  Date to = new Date(from.getTime() + days * 86400000l);
	  CertificateValidity interval = new CertificateValidity(from, to);
	  BigInteger sn = new BigInteger(64, new SecureRandom());
	  X500Name owner = new X500Name(issuerDN);
	  X500Name subject = new X500Name(subjectDN);
	 
	  info.set(X509CertInfo.VALIDITY, interval);
	  info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
	  info.set(X509CertInfo.SUBJECT, subject);
	  info.set(X509CertInfo.ISSUER, owner);
	  info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
	  info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
	  AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
	  info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
	 
	  // Sign the cert to identify the algorithm that's used.
	  X509CertImpl cert = new X509CertImpl(info);
	  cert.sign(privkey, algorithm);
	 
	  // Update the algorith, and resign.
	  algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
	  info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
	  cert = new X509CertImpl(info);
	  cert.sign(privkey, algorithm);
	  return cert;
	}
	
	/**
	 * Saves into .cer file.
	 * @param filePath
	 * @throws CertificateEncodingException
	 * @throws IOException
	 */
	public void saveToCerFile(String filePath) throws CertificateEncodingException, IOException{
		FileOutputStream fos = new FileOutputStream(filePath);
		fos.write( x509cert.getEncoded() );
		fos.flush();
		fos.close();		
	}
	
	/**
	 * Make the Distinguished Name from the given arguments
	 * @param commonName CN
	 * @param organization O
	 * @param location L
	 * @param country C
	 * @return
	 */
	public static String makeDN(String commonName, String organization, String location, String country){
		return String.format("CN=%s, O=%s, L=%s, C=%s", commonName, organization, location, country);
	}
	/**
	 * Retrieves the keywords from distinguished name
	 * @param dn
	 * @return index- 0:CN, 1:O, 2:L, 3:C
	 */
	public static String[] parseDN(String dn){
		String[] info = new String[4];
		String[] keystrings = dn.split(",");
		for(String keystring : keystrings){
			String[] keywords = keystring.split("=");
			if("CN".equals(keywords[0])){
				info[0] = keywords[1];
			}else if("O".equals(keywords[0])){
				info[1] = keywords[1];
			}else if("L".equals(keywords[0])){
				info[2] = keywords[1];
			}else if("C".equals(keywords[0])){
				info[3] = keywords[1];
			}
		}
		return info;
	}
}