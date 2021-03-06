package edu.umkc.cs5573.isa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class CyborgSecurity {
	/**
	 * String to hold name of the encryption algorithm.
	 */
	public static final String ALGORITHM = "RSA";

	/**
	 * String to hold the name of the private key file.
	 */
	public static final String PRIVATE_KEY_FILE = "C:/keys/private.key";

	/**
	 * String to hold name of the public key file.
	 */
	public static final String PUBLIC_KEY_FILE = "C:/keys/public.key";
	
	private Cipher cipher;
	public CyborgSecurity() throws NoSuchAlgorithmException, NoSuchPaddingException{
		this.cipher = Cipher.getInstance(ALGORITHM);
	}
	public void getPrivateKey(){
		
	}
	public static PrivateKey getPrivateKey(byte[] data) throws GeneralSecurityException{
		KeyFactory kf = KeyFactory.getInstance(ALGORITHM); // or "EC" or whatever
		PrivateKey prk = kf.generatePrivate(new PKCS8EncodedKeySpec(data));
		return prk;
	}
	public void getPublicKey(){
	}
	public static PublicKey getPublicKey(byte[] data) throws GeneralSecurityException{
		KeyFactory kf = KeyFactory.getInstance(ALGORITHM); // or "EC" or whatever
		PublicKey pbk = kf.generatePublic(new X509EncodedKeySpec(data));
		return pbk;
	}
	/**
	   * Generate key which contains a pair of private and public key using 1024
	   * bytes. Store the set of keys in Prvate.key and Public.key files.
	   * 
	   * @throws NoSuchAlgorithmException
	   * @throws IOException
	   * @throws FileNotFoundException
	   */
	  public static void generateKey() {
	    try {
	      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
	      keyGen.initialize(1024);
	      final KeyPair key = keyGen.generateKeyPair();

	      File privateKeyFile = new File(PRIVATE_KEY_FILE);
	      File publicKeyFile = new File(PUBLIC_KEY_FILE);

	      // Create files to store public and private key
	      if (privateKeyFile.getParentFile() != null) {
	        privateKeyFile.getParentFile().mkdirs();
	      }
	      privateKeyFile.createNewFile();

	      if (publicKeyFile.getParentFile() != null) {
	        publicKeyFile.getParentFile().mkdirs();
	      }
	      publicKeyFile.createNewFile();

	      // Saving the Public key in a file
	      ObjectOutputStream publicKeyOS = new ObjectOutputStream(
	          new FileOutputStream(publicKeyFile));
	      publicKeyOS.writeObject(key.getPublic());
	      publicKeyOS.close();

	      // Saving the Private key in a file
	      ObjectOutputStream privateKeyOS = new ObjectOutputStream(
	          new FileOutputStream(privateKeyFile));
	      privateKeyOS.writeObject(key.getPrivate());
	      privateKeyOS.close();
	    } catch (Exception e) {
	      e.printStackTrace();
	    }
	  }
	  /**
	   * The method checks if the pair of public and private key has been generated.
	   * 
	   * @return flag indicating if the pair of keys were generated.
	   */
	  public static boolean areKeysPresent() {

	    File privateKey = new File(PRIVATE_KEY_FILE);
	    File publicKey = new File(PUBLIC_KEY_FILE);

	    if (privateKey.exists() && publicKey.exists()) {
	      return true;
	    }
	    return false;
	  }
	  /**
	   * Encrypt the plain text using public key.
	   * 
	   * @param text
	   *          : original plain text
	   * @param key
	   *          :The public key
	   * @return Encrypted text
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	   */
	public byte[] encrypt(String text, PublicKey key) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		try {
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] bytes = text.getBytes("UTF-8");
			byte[] encrypted = blockCipher(bytes,Cipher.ENCRYPT_MODE);
		    return encrypted;
		} catch (UnsupportedEncodingException e) {
			// Will never get here
			e.printStackTrace();
			return null;
		}

	  }
	  
	  /**
	   * Decrypt text using private key.
	   * 
	   * @param text
	   *          :encrypted text
	   * @param key
	   *          :The private key
	   * @return plain text
	 * @throws InvalidKeyException 
	 * @throws BadPaddingException 
	 * @throws IllegalBlockSizeException 
	   * @throws java.lang.Exception
	   */
	public String decrypt(byte[] text, PrivateKey key) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		try {
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] decrypted = blockCipher(text, Cipher.DECRYPT_MODE);
			return new String(decrypted,"UTF-8").trim();
		} catch (UnsupportedEncodingException e) {
			// Will never get here
			e.printStackTrace();
			return null;
		}
	}
	
	private byte[] blockCipher(byte[] bytes, int mode) throws IllegalBlockSizeException, BadPaddingException{
		// string initialize 2 buffers.
		// scrambled will hold intermediate results
		byte[] scrambled = new byte[0];

		// toReturn will hold the total result
		byte[] toReturn = new byte[0];
		// if we encrypt we use 100 byte long blocks. Decryption requires 128 byte long blocks (because of RSA)
		int length = (mode == Cipher.ENCRYPT_MODE)? 100 : 128;

		// another buffer. this one will hold the bytes that have to be modified in this step
		byte[] buffer = new byte[length];

		for (int i=0; i< bytes.length; i++){

			// if we filled our buffer array we have our block ready for de- or encryption
			if ((i > 0) && (i % length == 0)){
				//execute the operation
				scrambled = cipher.doFinal(buffer);
				// add the result to our total result.
				toReturn = append(toReturn,scrambled);
				// here we calculate the length of the next buffer required
				int newlength = length;

				// if newlength would be longer than remaining bytes in the bytes array we shorten it.
				if (i + length > bytes.length) {
					 newlength = bytes.length - i;
				}
				// clean the buffer array
				buffer = new byte[newlength];
			}
			// copy byte into our buffer.
			buffer[i%length] = bytes[i];
		}

		// this step is needed if we had a trailing buffer. should only happen when encrypting.
		// example: we encrypt 110 bytes. 100 bytes per run means we "forgot" the last 10 bytes. they are in the buffer array
		scrambled = cipher.doFinal(buffer);

		// final step before we can return the modified data.
		toReturn = append(toReturn,scrambled);

		return toReturn;
	}
	
	private static byte[] append(byte[] prefix, byte[] suffix){
		byte[] toReturn = new byte[prefix.length + suffix.length];
		for (int i=0; i< prefix.length; i++){
			toReturn[i] = prefix[i];
		}
		for (int i=0; i< suffix.length; i++){
			toReturn[i+prefix.length] = suffix[i];
		}
		return toReturn;
	}
	
	
	  /**
	   * Test the EncryptionUtil
	   */
	public static void demo(String[] args) {
		CyborgSecurity sec;
		try {
			sec = new CyborgSecurity();
		      // Check if the pair of keys are present else generate those.
		      if (!areKeysPresent()) {
		        // Method generates a pair of keys using the RSA algorithm and stores it
		        // in their respective files
		        generateKey();
		      }

		      final String originalText = "Text to be encrypted ";
		      ObjectInputStream inputStream = null;

		      // Encrypt the string using the public key
		      inputStream = new ObjectInputStream(new FileInputStream(PUBLIC_KEY_FILE));
		      final PublicKey publicKey = (PublicKey) inputStream.readObject();
		      final byte[] cipherText = sec.encrypt(originalText, publicKey);
		      inputStream.close();

		      // Decrypt the cipher text using the private key.
		      inputStream = new ObjectInputStream(new FileInputStream(PRIVATE_KEY_FILE));
		      final PrivateKey privateKey = (PrivateKey) inputStream.readObject();
		      final String plainText = sec.decrypt(cipherText, privateKey);

		      // Printing the Original, Encrypted and Decrypted Text
		      System.out.println("Original: " + originalText);
		      System.out.println("Encrypted: " + cipherText.toString());
		      System.out.println("Decrypted: " + plainText);
		      inputStream.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
}
