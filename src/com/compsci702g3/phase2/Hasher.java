package com.compsci702g3.phase2;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {

	// computes the SHA-256 hash of an input file and prints it to the console
	public String computeHash(String filename) throws NoSuchAlgorithmException,
			IOException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		FileInputStream fis = new FileInputStream(filename);

		byte[] dataBytes = new byte[1024];

		int nread = 0;
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		}
		;
		byte[] mdbytes = md.digest();

		// convert the byte to hex format method 
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
					.substring(1));
		}
		System.out.println("Hex format : " + sb.toString());
		fis.close();
		return sb.toString();
	}

}
