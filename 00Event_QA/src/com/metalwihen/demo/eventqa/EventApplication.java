/*
 * Copyright 2014 IBM Corp. All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.metalwihen.demo.eventqa;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import bolts.Continuation;
import bolts.Task;

import com.ibm.mobile.services.core.IBMBluemix;
import com.ibm.mobile.services.data.IBMData;
import com.ibm.mobile.services.push.IBMPush;
import com.ibm.mobile.services.push.IBMPushNotificationListener;
import com.ibm.mobile.services.push.IBMSimplePushNotification;

public final class EventApplication extends Application {
	private static final String PROPS_FILE = "bluemixkeys.properties";
	private static final String APP_ID = "applicationID";
	private static final String APP_SECRET = "applicationSecret";
	private static final String APP_ROUTE = "applicationRoute";
	public static final String APP_MQA_KEY = "MQA_KEY";

	public static IBMPush push = null;
	private IBMPushNotificationListener notificationListener = null;
	private Activity mActivity;

	public static final int EDIT_ACTIVITY_RC = 1;
	private static final String CLASS_NAME = EventApplication.class
			.getSimpleName();

	List<Item> itemList;

	public EventApplication() {
		registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
			public void onActivityCreated(Activity activity,
					Bundle savedInstanceState) {
				Log.d(CLASS_NAME,
						"Activity created: " + activity.getLocalClassName());
				mActivity = activity;

				// Define IBMPushNotificationListener behavior on push
				// notifications.
				notificationListener = new IBMPushNotificationListener() {
					public void onReceive(
							final IBMSimplePushNotification message) {
						mActivity.runOnUiThread(new Runnable() {
							public void run() {
								Class<? extends Activity> actClass = mActivity
										.getClass();
								if (actClass == MainActivity.class) {

									((MainActivity) mActivity).READ_ITEM();

									Log.e(CLASS_NAME,
											"Notification message received: "
													+ message.toString());

									// Present the message when sent from Push
									// notification console.

									if (!message.getAlert().contains(
											"ItemList was updated")) {

										mActivity.runOnUiThread(new Runnable() {
											public void run() {

												new AlertDialog.Builder(
														mActivity)
														.setTitle(
																"Push notification received")
														.setMessage(
																message.getAlert())
														.setPositiveButton(
																android.R.string.ok,
																new DialogInterface.OnClickListener() {
																	public void onClick(
																			DialogInterface dialog,
																			int whichButton) {
																	}
																}).show();

											}
										});

									}
								}
							}
						});
					}
				};
			}

			public void onActivityStarted(Activity activity) {
				mActivity = activity;
				Log.d(CLASS_NAME,
						"Activity started: " + activity.getLocalClassName());
			}

			public void onActivityResumed(Activity activity) {
				mActivity = activity;
				Log.d(CLASS_NAME,
						"Activity resumed: " + activity.getLocalClassName());
				if (push != null) {
					push.listen(notificationListener);
				}
			}

			public void onActivitySaveInstanceState(Activity activity,
					Bundle outState) {
				Log.d(CLASS_NAME,
						"Activity saved instance state: "
								+ activity.getLocalClassName());
			}

			public void onActivityPaused(Activity activity) {
				if (push != null) {
					push.hold();
				}
				Log.d(CLASS_NAME,
						"Activity paused: " + activity.getLocalClassName());
				if (activity != null && activity.equals(mActivity))
					mActivity = null;
			}

			public void onActivityStopped(Activity activity) {
				Log.d(CLASS_NAME,
						"Activity stopped: " + activity.getLocalClassName());
			}

			public void onActivityDestroyed(Activity activity) {
				Log.d(CLASS_NAME,
						"Activity destroyed: " + activity.getLocalClassName());
			}
		});
	}

	@Override
	public void onCreate() {
		super.onCreate();

		itemList = new ArrayList<Item>();
		// Read from properties file.

		Context context = getApplicationContext();
		Properties props = getKeysFromPropertyFile(context);

		// Initialize the IBM core backend-as-a-service.
		IBMBluemix.initialize(this, props.getProperty(APP_ID),
				props.getProperty(APP_SECRET), props.getProperty(APP_ROUTE));

		// >> IBM Data Service.
		IBMData.initializeService();
		Item.registerSpecialization(Item.class);

		// >> IBM Push service.
		IBMPush.initializeService();
		push = IBMPush.getService();
		push.register("device-android", "consumer-android").continueWith(
				new Continuation<String, Void>() {

					public Void then(Task<String> task) throws Exception {
						if (task.isCancelled()) {
							Log.e(CLASS_NAME,
									"Exception : Task " + task.toString()
											+ " was cancelled.");
						} else if (task.isFaulted()) {
							Log.e(CLASS_NAME, "Exception : "
									+ task.getError().getMessage());
						} else {
							Log.d(CLASS_NAME, "Device Successfully Registered");
						}

						return null;
					}

				});
	}

	public static Properties getKeysFromPropertyFile(Context context) {
		Properties props = new java.util.Properties();
		try {
			AssetManager assetManager = context.getAssets();
			props.load(assetManager.open(PROPS_FILE));
			Log.i(CLASS_NAME, "Found configuration file: " + PROPS_FILE);
			return props;

		} catch (FileNotFoundException e) {
			Log.e(CLASS_NAME, "The bluelist.properties file was not found.", e);
		} catch (IOException e) {
			Log.e(CLASS_NAME,
					"The bluelist.properties file could not be read properly.",
					e);
		}
		return null;
	}

	public List<Item> getItemList() {
		return itemList;
	}
}