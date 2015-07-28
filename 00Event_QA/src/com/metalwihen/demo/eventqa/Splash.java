package com.metalwihen.demo.eventqa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Splash extends Activity {

	public static final String CLASS_NAME = "SplashActivity";
	Context ct;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getActionBar().hide();
		setContentView(R.layout.activity_splash);

		ct = this;
		Button go = (Button) findViewById(R.id.button1);
		go.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				AlertDialog.Builder alert = new AlertDialog.Builder(ct);

				final EditText edittext = new EditText(ct);
				edittext.setHint(getString(R.string.event_text_hint));
				edittext.setBackgroundColor(Color.parseColor("#00000000"));
				edittext.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
				// alert.setMessage("Enter Your Message");

				alert.setTitle("Enter Event TAG:");

				alert.setView(edittext);

				alert.setPositiveButton("Yes, Continue!",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								String YouEditTextValue = edittext.getText()
										.toString().toLowerCase().trim();

								if (YouEditTextValue.length() < 2
										|| YouEditTextValue.contains(" "))
									Toast.makeText(
											ct,
											"Please enter a Valid Single Word Event TAG!",
											Toast.LENGTH_SHORT).show();
								else {
									Intent i = new Intent(ct,
											MainActivity.class);
									i.putExtra("eventhashtag", YouEditTextValue);
									startActivity(i);
								}
							}
						});

				alert.setNegativeButton("No, Cancel.",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {

								Toast.makeText(ct, "No Event Mentioned",
										Toast.LENGTH_SHORT).show();
							}
						});

				alert.show();

			}
		});

	}

}
