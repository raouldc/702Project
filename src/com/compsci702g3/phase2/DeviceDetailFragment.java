/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.compsci702g3.phase2;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.compsci702g3.phase2.DeviceListFragment.DeviceActionListener;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
public class DeviceDetailFragment extends Fragment implements
		ConnectionInfoListener {

	protected static final int CHOOSE_FILE_RESULT_CODE = 20;
	private static View mContentView = null;
	private WifiP2pDevice device;
	private WifiP2pInfo info;
	ProgressDialog progressDialog = null;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mContentView = inflater.inflate(R.layout.device_detail, null);
		mContentView.findViewById(R.id.btn_connect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						WifiP2pConfig config = new WifiP2pConfig();
						config.deviceAddress = device.deviceAddress;
						config.wps.setup = WpsInfo.PBC;
						if (progressDialog != null
								&& progressDialog.isShowing()) {
							progressDialog.dismiss();
						}
						progressDialog = ProgressDialog.show(getActivity(),
								"Press back to cancel", "Connecting to :"
										+ device.deviceAddress, true, true
						// new DialogInterface.OnCancelListener() {
						//
						// @Override
						// public void onCancel(DialogInterface dialog) {
						// ((DeviceActionListener)
						// getActivity()).cancelDisconnect();
						// }
						// }
								);
						((DeviceActionListener) getActivity()).connect(config);

					}
				});

		mContentView.findViewById(R.id.btn_disconnect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						((DeviceActionListener) getActivity()).disconnect();
					}
				});

		mContentView.findViewById(R.id.btn_start_client).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// Allow user to pick an image from Gallery or other
						// registered apps
						// Intent intent = new
						// Intent(Intent.ACTION_GET_CONTENT);
						// intent.setType("audio/3gp");
						// startActivityForResult(intent,
						// CHOOSE_FILE_RESULT_CODE);
						onActivityResult();
					}
				});

		return mContentView;
	}

	// @Override
	public void onActivityResult() {// int requestCode, int resultCode, Intent
									// data) {

		// User has picked an image. Transfer it to group owner i.e peer using
		// FileTransferService.
		String mFileName = "/storage/emulated/0/com.compsci702g3.phase2/recording.3gp";// Environment.getExternalStorageDirectory().getAbsolutePath()
																						// +
																						// "/recording.3gp";
		Uri uri = Uri.parse(mFileName);// data.getData();
		TextView statusText = (TextView) mContentView
				.findViewById(R.id.status_text);
		statusText.setText("Sending: " + uri);
		Log.d(WiFiDirectActivity.TAG, "Intent----------- " + uri);
		Intent serviceIntent = new Intent(getActivity(),
				FileTransferService.class);
		serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
		serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH,
				uri.toString());
		serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
				info.groupOwnerAddress.getHostAddress());
		serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT,
				8988);
		getActivity().startService(serviceIntent);
	}

	@Override
	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		this.info = info;
		this.getView().setVisibility(View.VISIBLE);

		// The owner IP is now known.
		TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(getResources().getString(R.string.group_owner_text)
				+ ((info.isGroupOwner == true) ? getResources().getString(
						R.string.yes) : getResources().getString(R.string.no)));

		// InetAddress from WifiP2pInfo struct.
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText("Group Owner IP - "
				+ info.groupOwnerAddress.getHostAddress());

		// After the group negotiation, we assign the group owner as the file
		// server. The file server is single threaded, single connection server
		// socket.
		if (info.groupFormed && info.isGroupOwner) {
			new FileServerAsyncTask(getActivity(),
					mContentView.findViewById(R.id.status_text)).execute();
		} else if (info.groupFormed) {
			// The other device acts as the client. In this case, we enable the
			// get file button.
			mContentView.findViewById(R.id.btn_start_client).setVisibility(
					View.VISIBLE);
			((TextView) mContentView.findViewById(R.id.status_text))
					.setText(getResources().getString(R.string.client_text));
		}

		// hide the connect button
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.GONE);
	}

	/**
	 * Updates the UI with device data
	 * 
	 * @param device
	 *            the device to be displayed
	 */
	public void showDetails(WifiP2pDevice device) {
		this.device = device;
		this.getView().setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView
				.findViewById(R.id.device_address);
		view.setText(device.deviceAddress);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(device.toString());

	}

	/**
	 * Clears the UI fields after a disconnect or direct mode disable operation.
	 */
	public void resetViews() {
		mContentView.findViewById(R.id.btn_connect).setVisibility(View.VISIBLE);
		TextView view = (TextView) mContentView
				.findViewById(R.id.device_address);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.device_info);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.group_owner);
		view.setText(R.string.empty);
		view = (TextView) mContentView.findViewById(R.id.status_text);
		view.setText(R.string.empty);
		mContentView.findViewById(R.id.btn_start_client).setVisibility(
				View.GONE);
		this.getView().setVisibility(View.GONE);
	}

	/**
	 * A simple server socket that accepts connection and writes some data on
	 * the stream.
	 */
	public static class FileServerAsyncTask extends
			AsyncTask<Void, Void, String> {

		private Context context;
		private TextView statusText;

		/**
		 * @param context
		 * @param statusText
		 */
		public FileServerAsyncTask(Context context, View statusText) {
			this.context = context;
			this.statusText = (TextView) statusText;
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				ServerSocket serverSocket = new ServerSocket(8988);
				Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");
				Socket client = serverSocket.accept();
				Log.d(WiFiDirectActivity.TAG, "Server: connection done");

				Log.d(WiFiDirectActivity.TAG,
						"server: copying files " );
				InputStream inputstream = client.getInputStream();
				HashAndFile hashF = receiveHashandFile(inputstream);
				TextView view = (TextView) mContentView.findViewById(R.id.group_owner);
				
				Log.d(WiFiDirectActivity.TAG, "Received Hash is: "+hashF.hash);
				view.setText("Received Hash is: " +hashF.hash);
				serverSocket.close();
				if(hashF.b64file!=null)
				{
					try {
						TextView view2 = (TextView) mContentView.findViewById(R.id.device_info);
					
						String computedHash = new Hasher().computeHash(hashF.b64file);
						view2.setText("Received File Hash is: "+computedHash);
						//hash.equals();
						if(!computedHash.equals(hashF.hash))
						{
							throw new RuntimeException("hashes do not match");
						}
						
						Log.d(WiFiDirectActivity.TAG, "Received File Hash is: "+computedHash);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				
				return hashF.b64file;
			} catch (IOException e) {
				Log.e(WiFiDirectActivity.TAG, e.getMessage());
				return null;
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(String result) {
			if (result != null) {
				statusText.setText("File copied - " + result);
				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(Uri.parse("file://" + result),
						"audio/3gp");
				context.startActivity(intent);
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			statusText.setText("Opening a server socket");
		}

	}

	public static boolean copyFile(InputStream inputStream, OutputStream out) {

		byte buf[] = new byte[1024];
		int len;
		try {
			while ((len = inputStream.read(buf)) != -1) {
				out.write(buf, 0, len);

			}
			out.close();
			inputStream.close();
		} catch (IOException e) {
			Log.d(WiFiDirectActivity.TAG, e.toString());
			return false;
		}
		return true;
	}

	public static boolean sendHashandFile(InputStream inputStream,
			OutputStream out, String hash, String b64file) throws IOException {

		OutputStreamWriter outwr = new OutputStreamWriter(out);
		BufferedWriter buffwr = new BufferedWriter(outwr);
		buffwr.write(hash);
		buffwr.newLine();
		
		Log.d(WiFiDirectActivity.TAG, "Sent B64 is: "+b64file);
		
		buffwr.write(b64file);
		buffwr.newLine();
		
		buffwr.close();
		outwr.close();
		
		inputStream.close();
		out.close();
		
		return true;

	}

	public static HashAndFile receiveHashandFile(InputStream inputStream) throws IOException {
		InputStreamReader inp = new InputStreamReader(inputStream);
		BufferedReader buffI = new BufferedReader(inp);
		String hash = buffI.readLine();
		String line ="";
		String b64file = null;
		b64file = buffI.readLine();
		
		Log.d(WiFiDirectActivity.TAG, "Received B64 is: "+b64file);
		
		
		byte[] data = Base64.decode(b64file, Base64.DEFAULT);
		
		String path = "/storage/emulated/0/com.compsci702g3.phase2/recordingreceived.3gp";
		File file = new File(path);
		file.createNewFile();
		FileOutputStream fos = null;

		try {
			
			fos = new FileOutputStream(file);
			
			// Writes bytes from the specified byte array to this file output stream 
			fos.write(data);

		}
		catch (FileNotFoundException e) {
			System.out.println("File not found" + e);
		}
		catch (IOException ioe) {
			System.out.println("Exception while writing file " + ioe);
		}
		finally {
			// close the streams using close method
			try {
				if (fos != null) {
					fos.close();
				}
			}
			catch (IOException ioe) {
				System.out.println("Error while closing stream: " + ioe);
			}

		}
		
		buffI.close();
		inp.close();
		
		inputStream.close();		
		HashAndFile f = new HashAndFile(hash, path);
		return f;
	}
	

}
