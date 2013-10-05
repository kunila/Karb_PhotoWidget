package com.karbens.photowidget;

import java.io.File;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public abstract class ConfigActivityBase extends Activity implements OnClickListener ,DialogInterface.OnClickListener {

	public static final String PREFS_NAME = "ImagesWidgetPrefs";
    public static final String PREFS_UPDATE_RATE_FIELD_PATTERN = "UpdateRate-%d";
    public static final String PREFS_FOLDER_PATH_FIELD_PATTERN = "FolderPath-%d";
    public static final String PREFS_FOLDER_FRAME_TYPE_FIELD_PATTERN = "FrameType-%d";
    public static final String PREFS_UPDATE_CURRENT_PIC_INDEX_FIELD_PATTERN = "CurrentPicIndex-%d";
    private static final int PREFS_UPDATE_RATE_DEFAULT = 5;
    
    private static final String LOG_TAG = "ConfigurationActivity";
    
    private FolderPicker mFolderDialog;
	private View mPickFolder;
	private TextView mFolderPath;
	private TextView mPicsHint;
	private Button mSaveButton;
	private Spinner mSelectFrameSpinner;
	
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	public abstract String getUriSchemaId();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// get any data we were launched with
        Intent launchIntent = getIntent();
        Bundle extras = launchIntent.getExtras();
        if (extras != null) 
        {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            Intent cancelResultValue = new Intent();
            cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_CANCELED, cancelResultValue);
        } 
        else 
        {
            // only launch if it's for configuration
            // Note: when you launch for debugging, this does prevent this
            // activity from running. We could also turn off the intent
            // filtering for main activity.
            // But, to debug this activity, we can also just comment the
            // following line out.
            finish();
        }

		
		setContentView(R.layout.configuration_main);
		
		mPicsHint = (TextView)findViewById(R.id.config_folder_path_hint);
        mPickFolder = findViewById(R.id.config_pick_folder);
        mPickFolder.setOnClickListener(this);
        mFolderPath = (TextView)findViewById(R.id.config_folder_path);
        mSelectFrameSpinner = (Spinner)findViewById(R.id.select_frame_background);
        
        String dirName= "mnt/sdcard/dcim/camera";
        
    	System.out.println("FolderPath :"+dirName);
    	
    	mFolderPath.setText(dirName);
    	setFolderHint(dirName);
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.frame_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSelectFrameSpinner.setAdapter(adapter);
        
        final SharedPreferences config = getSharedPreferences(PREFS_NAME, 0);
        final EditText updateRateEntry = (EditText) findViewById(R.id.update_rate_entry);

        updateRateEntry.setText(String.valueOf(config.getInt(String.format(PREFS_UPDATE_RATE_FIELD_PATTERN, appWidgetId), PREFS_UPDATE_RATE_DEFAULT)));

        mSaveButton = (Button) findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	// 
            	CharSequence folderPath = mFolderPath.getText();
            	if (null == folderPath || 0 == folderPath.length()) 
            	{
            		Toast.makeText(ConfigActivityBase.this, R.string.no_image_picked, Toast.LENGTH_SHORT).show();
            		return;
            	} 
            	else 
            	{
            		File[] pics = RegularSizeWidgetBase.getPicFileList(folderPath.toString());
            		if (null == pics || 0 == pics.length) {
                		Toast.makeText(ConfigActivityBase.this, R.string.no_image_picked, Toast.LENGTH_SHORT).show();
                		return;
            		}
            	}
            	
            	// 
            	CharSequence updateRate = updateRateEntry.getText();
            	int updateRateSeconds = -1;
            	if (updateRate != null && updateRate.length() >= 0) {
            		try
            		{
            			updateRateSeconds = Integer.parseInt(updateRate.toString());
            		} catch (NumberFormatException e) {
            			Log.e(LOG_TAG, "bad input: " + e.toString());
            		}
            	}
            	
            	if (0 > updateRateSeconds) 
            	{
            		Toast.makeText(ConfigActivityBase.this, R.string.no_update_rate, Toast.LENGTH_SHORT).show();
            		return;
            	}
            	
            	
            	/*
            	Cursor cursor = getContentResolver().query(Media.EXTERNAL_CONTENT_URI, new String[]{Media.DATA, Media.DATE_ADDED, MediaStore.Images.ImageColumns.ORIENTATION}, Media.DATE_ADDED, null, "date_added ASC");
            	if(cursor != null && cursor.moveToFirst())
            	{
            	    do {
            	    	Uri uri = Uri.parse(cursor.getString(cursor.getColumnIndex(Media.DATA)));
            	        String photoPath = uri.toString();
            	    }while(cursor.moveToNext());
            	    cursor.close();
            	}
            	*/

            		
                // Store off the user setting for update timing
                SharedPreferences.Editor configEditor = config.edit();

                configEditor.putInt(String.format(PREFS_UPDATE_RATE_FIELD_PATTERN, appWidgetId), updateRateSeconds);
                configEditor.putString(String.format(PREFS_FOLDER_PATH_FIELD_PATTERN, appWidgetId), folderPath.toString());
                configEditor.putInt(String.format(PREFS_FOLDER_FRAME_TYPE_FIELD_PATTERN, appWidgetId), mSelectFrameSpinner.getSelectedItemPosition());
                configEditor.commit();

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) 
                {

                    // tell the app widget manager that we're now configured
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    setResult(RESULT_OK, resultValue);

                    Intent widgetUpdate = new Intent();
                    widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });

                    // make this pending intent unique
                    widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(
                    	ConfigActivityBase.this.getUriSchemaId() + "://widget/id/"), String.valueOf(appWidgetId)));
                    PendingIntent newPending = PendingIntent.getBroadcast(getApplicationContext(), 0, widgetUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

                    // schedule the new widget for updating
                    AlarmManager alarms = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                    alarms.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), updateRateSeconds * 1000, newPending);
                }

                // activity is now done
                finish();
            }
        });
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	public void onClick(View v) {
		if (v == mPickFolder) {
			mFolderDialog = new FolderPicker(this, this, 0);
			mFolderDialog.show();
		}
	}

	public void onClick(DialogInterface dialog, int which) {
		
		if (dialog == mFolderDialog) 
		{
        	String folderPath = mFolderDialog.getPath();
        	if (null == folderPath || 0 == folderPath.length()) 
        	{
        		folderPath = "";
        	} 
        	else 
        	{
        		File picFolder = new File(folderPath);
        		folderPath = picFolder.getAbsolutePath();
        		if (!picFolder.isDirectory() || !picFolder.exists()) 
        		{
        			mPicsHint.setText(R.string.no_image_picked);
        		}
        		File[] pics = RegularSizeWidgetBase.getPicFileList(folderPath);
        		if (null == pics || 0 == pics.length) 
        		{
        			mPicsHint.setText(R.string.no_image_picked);
        		} 
        		else 
        		{
        			String hint = String.format(
        				ConfigActivityBase.this.getText(R.string.count_image_picked).toString(),
        				pics.length);
        			mPicsHint.setText(hint);
        		}
        	}
        	
			mFolderPath.setText(folderPath);
		}
	}
	
    
	public void setFolderHint(String folderPath) 
	{
		File picFolder = new File(folderPath);
		folderPath = picFolder.getAbsolutePath();
		if (!picFolder.isDirectory() || !picFolder.exists()) 
		{
			mPicsHint.setText(R.string.no_image_picked);
		}
		File[] pics = RegularSizeWidgetBase.getPicFileList(folderPath);
		if (null == pics || 0 == pics.length) 
		{
			mPicsHint.setText(R.string.no_image_picked);
		} 
		else 
		{
			String hint = String.format(
				ConfigActivityBase.this.getText(R.string.count_image_picked).toString(),
				pics.length);
			mPicsHint.setText(hint);
		}
	}
	

}
