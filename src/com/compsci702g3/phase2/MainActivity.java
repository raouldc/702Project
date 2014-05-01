package com.compsci702g3.phase2;

import java.io.File;
import java.util.List;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;
import android.view.View.OnClickListener;
import android.bluetooth.BluetoothAdapter;

public class MainActivity extends Activity {

	Button button;
	private BluetoothAdapter btAdapter;
	private File filetotransfer;
	private Context context;
	private String mFileName;
	
	
	private static final int DISCOVER_DURATION = 300;
	// our request code (must be greater than zero)
	private static final int REQUEST_BLU = 1;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.mFileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/secretlol.3gp";
		filetotransfer = new File(this.mFileName);
		addListenerOnButton();
	}

	public void addListenerOnButton() {

		context = this;
		button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(btnListener);

	}

	public void enableBlu() {
		// enable device discovery - this will automatically enable Bluetooth
		Intent discoveryIntent = new Intent(
				BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);

		discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
				DISCOVER_DURATION);

		startActivityForResult(discoveryIntent, REQUEST_BLU);
	}

	private OnClickListener btnListener = new OnClickListener() {
		public void onClick(View arg0) {

			// Check if bluetooth is supported
			btAdapter = BluetoothAdapter.getDefaultAdapter();

			if (btAdapter == null) {
				// Device does not support Bluetooth
				// Inform user that we're done.
			}

			enableBlu();

			// bring up Android chooser
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_STREAM,
					Uri.fromFile(filetotransfer));
			// ...
			startActivity(intent);

			// list of apps that can handle our intent
			PackageManager pm = getPackageManager();
			List appsList = pm.queryIntentActivities(intent, 0);

			if (appsList.size() > 0) {
				// proceed
				// select bluetooth
				String packageName = null;
				String className = null;
				boolean found = false;

				for (int i = 0; i < appsList.size(); i++) {
					ResolveInfo info = (ResolveInfo) appsList.get(i);
					packageName = info.activityInfo.packageName;
					if (packageName.equals("com.android.bluetooth")) {
						className = info.activityInfo.name;
						found = true;
						break;// found
					}
				}

				if (!found) {
					Toast.makeText(context, R.string.blu_notfound_inlist,
							Toast.LENGTH_SHORT).show();
					// exit
					System.exit(0);
				}

				// set our intent to launch Bluetooth
				intent.setClassName(packageName, className);
				startActivity(intent);
			}
		}
	};
}