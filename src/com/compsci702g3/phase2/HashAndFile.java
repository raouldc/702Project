package com.compsci702g3.phase2;

//Class that represents a File encoded as a Base64 string and its hash
public class HashAndFile {

	public String hash;
	public String b64file;
	
	public HashAndFile(String hash, String b64file)
	{
		this.hash = hash;
		this.b64file = b64file;
	}
	
}
