package com.metalwihen.demo.eventqa;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

import com.ibm.mobile.services.data.IBMDataException;
import com.ibm.mobile.services.data.IBMDataObject;
import com.ibm.mobile.services.data.IBMQuery;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class Common {

	public static boolean checkNetworkStatus(Context ct) {

		final ConnectivityManager connMgr = (ConnectivityManager) ct
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		final android.net.NetworkInfo wifi = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		final android.net.NetworkInfo mobile = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		if (wifi.isAvailable() && wifi.isConnected()) {
			return true;

		} else if (mobile.isAvailable() && mobile.isConnected()) {

			return true;
		} else {
			return false;
		}

	}

	public static ProgressDialog createDialog(Context ct) {
		ProgressDialog progressDialog;

		progressDialog = new ProgressDialog(ct);
		progressDialog.setIndeterminate(true);
		progressDialog.setCancelable(true);
		progressDialog.setCanceledOnTouchOutside(true);

		return progressDialog;
	}

}
