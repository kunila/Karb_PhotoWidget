package com.karbens.photowidget;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IntroActivity extends Activity{

	private static final String LOG_TAG = "Introduction";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.aboutus);

		Button backButton = (Button) findViewById(R.id.ButtonBack);
		backButton.setOnClickListener((new View.OnClickListener(){
				public void onClick(View v) 
				{
					IntroActivity.this.finish();
				}
			}));

		PackageInfo pInfo = null;
		try 
		{
			pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), PackageManager.GET_META_DATA);
		} 
		catch (NameNotFoundException e) 
		{
			Log.e(LOG_TAG, e.toString());
		}
		
		if (pInfo != null) {
			TextView version = (TextView) this.findViewById(R.id.versionText);
			version.setText(String.format(this.getText(R.string.AboutUs_Version).toString(),
				pInfo.versionName, pInfo.versionCode));
		}
		
	}
	
    
}
