package com.metalwihen.demo.eventqa;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import bolts.Continuation;
import bolts.Task;

import com.ibm.mobile.services.data.IBMDataException;
import com.ibm.mobile.services.data.IBMDataObject;
import com.ibm.mobile.services.data.IBMQuery;
import com.ibm.mqa.MQA;
import com.ibm.mqa.MQA.Mode;
import com.ibm.mqa.config.Configuration;

public class MainActivity extends Activity {

	List<Item> itemList;
	EventApplication blApplication;
	ArrayAdapter<Item> lvArrayAdapter;
	int listItemPosition;

	public static final String CLASS_NAME = "MainActivity";

	public String event_hashtag = "";

	Context ct;

	Mode currentMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		getActionBar().setBackgroundDrawable(
				(new ColorDrawable(Color.parseColor("#000000"))));

		blApplication = (EventApplication) getApplication();
		itemList = blApplication.getItemList();

		Intent i = getIntent();
		event_hashtag = i.getExtras().getString("eventhashtag").toLowerCase()
				.trim();
		getActionBar().setTitle("" + event_hashtag);

		ct = this;

		/* Set up the array adapter for items list view. */
		ListView itemsLV = (ListView) findViewById(R.id.itemsList);
		lvArrayAdapter = new ArrayAdapter<Item>(this, R.layout.list_item_1,
				itemList);
		itemsLV.setAdapter(lvArrayAdapter);

		/* Refresh the list. */
		READ_ITEM();

		/* Set on item click listener = vote */
		itemsLV.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				listItemPosition = arg2;

				voteOnItem(itemList.get(listItemPosition));
			}
		});

		/* Set on item long click listener. */
		itemsLV.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> adapter, View view,
					int position, long rowId) {

				listItemPosition = position;

				getRidOfItem(itemList.get(listItemPosition));

				return true;
			}
		});

		/* Set on button click listener. */
		Button b_ask = (Button) findViewById(R.id.acceptText);
		b_ask.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				CREATE_ITEM(v);
			}
		});

		EditText itemToAdd = (EditText) findViewById(R.id.itemToAdd);
		itemToAdd.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					CREATE_ITEM(v);
					return true;
				}
				return false;
			}
		});

		// Change to Mode.QA to actually test out features!
		currentMode = Mode.QA;

		// Mobile Quality Assurance
		Properties prop = EventApplication.getKeysFromPropertyFile(this);
		String APP_KEY = (String) prop.get(EventApplication.APP_MQA_KEY);

		// MQA configuration
		Configuration configuration = new Configuration.Builder(this)
				.withAPIKey(APP_KEY).withMode(currentMode)
				.withReportOnShakeEnabled(false).build();

		MQA.startNewSession(MainActivity.this, configuration);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.send_report:
			if (currentMode == Mode.MARKET)
				Toast.makeText(ct, "Enable QA Mode in code!",
						Toast.LENGTH_SHORT).show();
			else
				MQA.showReportScreen();
			return true;
		case R.id.refresh_list:
			READ_ITEM();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		/* If an edit has been made, notify that the data set has changed. */
		case EventApplication.EDIT_ACTIVITY_RC:
			sortItems(itemList);
			lvArrayAdapter.notifyDataSetChanged();
			break;
		}
	}

	public void voteOnItem(final Item item) {

		AlertDialog.Builder alert = new AlertDialog.Builder(ct);
		alert.setTitle("Upvote this question?");
		alert.setPositiveButton("Upvote",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						UPDATE_ITEM(item, 1);
					}
				});

		alert.setNegativeButton("Downvote",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						UPDATE_ITEM(item, -1);
					}
				});

		alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {

			}
		});

		alert.show();

	}

	public void getRidOfItem(final Item item) {

		AlertDialog.Builder alert = new AlertDialog.Builder(ct);
		alert.setTitle("Delete this question?");

		alert.setPositiveButton("Delete",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						DELETE_ITEM(item);
					}
				});

		alert.setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});

		alert.show();

	}

	public void READ_ITEM() {

		try {
			if (!Common.checkNetworkStatus(ct)) {
				Toast.makeText(ct, "No Network Available", Toast.LENGTH_SHORT)
						.show();
				return;
			}

			Log.i(CLASS_NAME, "Loading ITEMS");

			IBMQuery<Item> query = IBMQuery.queryForClass(Item.class);
			query.whereKeyEqualsTo(Item.EVENT, event_hashtag);

			// Query all the Item objects from the server.

			final ProgressDialog progressDialog = Common.createDialog(ct);
			try {
				progressDialog.setMessage("Loading Questions...");
				progressDialog.show();
			} catch (Exception e) {
				e.printStackTrace();
			}

			query.find().continueWith(new Continuation<List<Item>, Void>() {

				public Void then(Task<List<Item>> task) throws Exception {

					try {
						progressDialog.dismiss();
					} catch (Exception e) {
						e.printStackTrace();
					}

					Log.i(CLASS_NAME, "Loading ITEMS - THEN()");

					final List<Item> objects = task.getResult();
					// Log if the find was cancelled.
					if (task.isCancelled()) {
						Log.e(CLASS_NAME, "Exception : Task " + task.toString()
								+ " was cancelled.");
					}
					// Log error message, if the find task fails.
					else if (task.isFaulted()) {
						Log.e(CLASS_NAME, "Exception : "
								+ task.getError().getMessage());
					}

					// If the result succeeds, load the list.
					else {
						// Clear local itemList.
						// We'll be reordering and repopulating from
						// DataService.
						itemList.clear();

						for (IBMDataObject item : objects) {

							itemList.add((Item) item);
							Log.i("item - " + itemList.size(), ""
									+ ((Item) item).getVotes());

						}

						sortItems(itemList);
						lvArrayAdapter.notifyDataSetChanged();
					}
					return null;
				}
			}, Task.UI_THREAD_EXECUTOR);

		} catch (IBMDataException error) {
			Log.e(CLASS_NAME, "Exception : " + error.getMessage());

			error.printStackTrace();
		}
	}

	public void CREATE_ITEM(View v) {

		if (!Common.checkNetworkStatus(ct)) {
			Toast.makeText(ct, "No Network Available", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		Log.i(CLASS_NAME, "Adding ITEM");

		final EditText itemToAdd = (EditText) findViewById(R.id.itemToAdd);
		final String toAdd = itemToAdd.getText().toString().trim();

		AlertDialog.Builder alert = new AlertDialog.Builder(ct);

		alert.setTitle("Are you Sure?");
		alert.setMessage("Question: " + toAdd);

		alert.setPositiveButton("Yes, Ask Question!",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

						if (toAdd.length() < 2) {
							Toast.makeText(ct, "Please add a Valid Question!",
									Toast.LENGTH_SHORT).show();
							return;
						}

						Item item = new Item();
						if (!toAdd.equals("")) {
							item.setName(toAdd);
							item.setVotes(0);
							item.setEvent(event_hashtag);

							final ProgressDialog progressDialog = Common
									.createDialog(ct);
							;
							try {
								progressDialog.setMessage("Adding Question...");
								progressDialog.show();
							} catch (Exception e) {
								e.printStackTrace();
							}

							// Use the IBMDataObject to create and persist the
							// Item object.
							item.save().continueWith(
									new Continuation<IBMDataObject, Void>() {

										public Void then(
												Task<IBMDataObject> task)
												throws Exception {

											try {
												progressDialog.dismiss();
											} catch (Exception e) {
												e.printStackTrace();
											}

											// Log if the save was cancelled.
											if (task.isCancelled()) {
												Log.e(CLASS_NAME,
														"Exception : Task "
																+ task.toString()
																+ " was cancelled.");
											}
											// Log error message, if the save
											// task fails.
											else if (task.isFaulted()) {
												Log.e(CLASS_NAME,
														"Exception : "
																+ task.getError()
																		.getMessage());
											}

											// If the result succeeds, load the
											// list.
											else {
												READ_ITEM();
											}

											return null;
										}

									}, Task.UI_THREAD_EXECUTOR);

							// Set text field back to empty after item is added.
							itemToAdd.setText("");
						}
					}
				});

		alert.setNegativeButton("No, Cancel.",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {

					}
				});

		alert.show();

	}

	public void DELETE_ITEM(Item item) {

		if (!Common.checkNetworkStatus(ct)) {
			Toast.makeText(ct, "No Network Available", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		final ProgressDialog progressDialog = Common.createDialog(ct);

		try {
			progressDialog.setMessage("Deleting Question...");
			progressDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}

		itemList.remove(listItemPosition);

		item.delete().continueWith(new Continuation<IBMDataObject, Void>() {

			public Void then(Task<IBMDataObject> task) throws Exception {

				try {
					progressDialog.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Log if the delete was cancelled.
				if (task.isCancelled()) {
					Log.e(CLASS_NAME, "Exception : Task " + task.toString()
							+ " was cancelled.");
				}

				// Log error message, if the delete task fails.
				else if (task.isFaulted()) {
					Log.e(CLASS_NAME, "Exception : "
							+ task.getError().getMessage());
				}

				// If the result succeeds, reload the list.
				else {
					lvArrayAdapter.notifyDataSetChanged();
				}
				return null;
			}
		}, Task.UI_THREAD_EXECUTOR);

		lvArrayAdapter.notifyDataSetChanged();
	}

	public void UPDATE_ITEM(Item item, int addvotes) {

		if (!Common.checkNetworkStatus(ct)) {
			Toast.makeText(ct, "No Network Available", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		final ProgressDialog progressDialog = Common.createDialog(ct);

		try {
			progressDialog.setMessage("Voting Question...");
			progressDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}

		int votes = item.getVotes();
		votes += addvotes;
		item.setVotes(votes);

		item.save().continueWith(new Continuation<IBMDataObject, Void>() {

			public Void then(Task<IBMDataObject> task) throws Exception {

				try {
					progressDialog.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (task.isCancelled()) {
					Log.e(CLASS_NAME, "Exception : " + task.toString()
							+ " was cancelled.");
				}

				else if (task.isFaulted()) {
					Log.e(CLASS_NAME, "Exception : "
							+ task.getError().getMessage());
				}

				else {

					READ_ITEM();

				}
				return null;
			}

		}, Task.UI_THREAD_EXECUTOR);

	}

	private void sortItems(List<Item> theList) {
		// Sort collection by case insensitive alphabetical order.

		// First, Alphabetically.
		Collections.sort(theList, new Comparator<Item>() {
			public int compare(Item lhs, Item rhs) {

				String lhsName = lhs.getName();
				String rhsName = rhs.getName();
				return lhsName.compareToIgnoreCase(rhsName);
			}
		});

		// Then, on basis of votes - descending
		Collections.sort(theList, new Comparator<Item>() {
			public int compare(Item lhs, Item rhs) {

				int lhsName = lhs.getVotes();
				int rhsName = rhs.getVotes();
				return (rhsName - lhsName);
			}
		});
	}

}
