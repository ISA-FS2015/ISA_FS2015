package edu.umkc.cs5573.isa;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;

public class X509Util{
	CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
	X509Certificate x509cert;
	//CertificateFactory certificatefactory;
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
	public X509Util(File file) throws CertificateException, IOException {
		FileInputStream fileinputstream = new FileInputStream(file);
		// Generate a certificate from the data in the file.
		this.x509cert =
		  (X509Certificate)certificatefactory.generateCertificate(fileinputstream);
		fileinputstream.close();
	}
	public X509Util(byte[] byteInformation) throws CertificateException {
		InputStream stream = new ByteArrayInputStream(byteInformation);
		this.x509cert =
		  (X509Certificate)certificatefactory.generateCertificate(stream);
	}
	public X509Certificate getCertificate() {
		return this.x509cert;
	}
	public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
		x509cert.checkValidity();
	}
	public String getBriefKeyInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append("---Certificate---")
			.append("type = " + x509cert.getType())
			.append("version = " + x509cert.getVersion())
			.append("subject = " + x509cert.getSubjectDN().getName())
			.append("valid from = " + x509cert.getNotBefore())
			.append("valid to = " + x509cert.getNotAfter())
			.append("serial number = " + x509cert.getSerialNumber().toString(16))
			.append("issuer = " + x509cert.getIssuerDN().getName())
			.append("signing algorithm = " + x509cert.getSigAlgName())
			.append("public key algorithm = " + x509cert.getPublicKey().getAlgorithm())
			.append("---Extensions---");
		Set setCritical = x509cert.getCriticalExtensionOIDs();
		if (setCritical != null && setCritical.isEmpty() == false)
		for (Iterator iterator = setCritical.iterator(); iterator.hasNext(); )
			sb.append(iterator.next().toString() + " *critical*");
		Set setNonCritical = x509cert.getNonCriticalExtensionOIDs();
		if (setNonCritical != null && setNonCritical.isEmpty() == false)
		for (Iterator iterator = setNonCritical.iterator(); iterator.hasNext(); )
			sb.append(iterator.next().toString());
		// We're done.
		sb.append("---");
		return sb.toString();
	}
}