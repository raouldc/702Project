// Copyright 2011 Google Inc. All Rights Reserved.

package com.compsci702g3.phase2;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

	private static final int SOCKET_TIMEOUT = 5000;
	public static final String ACTION_SEND_FILE = "com.compsci702g3.phase2.SEND_FILE";
	public static final String EXTRAS_FILE_PATH = "file_url";
	public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
	public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
	public static final String HASH_FILE_DIR = "/storage/emulated/0/com.compsci702g3.phase2/recording.hash";

	public FileTransferService(String name) {
		super(name);
	}

	public FileTransferService() {
		super("FileTransferService");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.IntentService#onHandleIntent(android.content.Intent)
	 */
	@Override
	protected void onHandleIntent(Intent intent) {

		Context context = getApplicationContext();
		// compute the hash before sending the file
		if (intent.getAction().equals(ACTION_SEND_FILE)) {
			String fileUri = "file:///storage/emulated/0/com.compsci702g3.phase2/recording.3gp";
			String audioHash = "";
			try {
				audioHash = new Hasher()
						.computeHash("/storage/emulated/0/com.compsci702g3.phase2/recording.3gp");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Sending Audio File....");
			sendFile(intent, context, fileUri, audioHash);

		}
	}

	private void sendFile(Intent intent, Context context, String fileUri,
			String hash) {
		String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
		Socket socket = new Socket();
		int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

		try {
			
			//setup connection
			Log.d(WiFiDirectActivity.TAG, "Opening client socket - ");
			socket.bind(null);
			socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

			Log.d(WiFiDirectActivity.TAG,
					"Client socket - " + socket.isConnected());
			OutputStream stream = socket.getOutputStream();
			ContentResolver cr = context.getContentResolver();

			InputStream is = null;
			try {
				is = cr.openInputStream(Uri.parse(fileUri));
			} catch (FileNotFoundException e) {
				Log.d(WiFiDirectActivity.TAG, e.toString());
			}

			// convert file to byte array

			BufferedInputStream bs = new BufferedInputStream(is);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();

			// encode byte array to Base64 String

			String encodedfile = Base64.encodeToString(buffer.toByteArray(),
					Base64.NO_WRAP);

			DeviceDetailFragment.sendHashandFile(is, stream, hash, encodedfile);

			Log.d(WiFiDirectActivity.TAG, "Client: Data written");
		} catch (IOException e) {
			Log.e(WiFiDirectActivity.TAG, e.getMessage());
		} finally {
			if (socket != null) {
				if (socket.isConnected()) {
					try {
						socket.close();
					} catch (IOException e) {
						// Give up
						e.printStackTrace();
					}
				}
			}
		}
	}
}
