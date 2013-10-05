package com.karbens.photowidget;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;

public abstract class RegularSizeWidgetBase extends AppWidgetProvider{

	public abstract String getUriSchemaId();
    
    public abstract String getOnClickAction();
    
    private static Map<String, File[]> folder2filesMap = new HashMap<String, File[]>();
    
    private static final String LOG_TAG = "ImagesWidgetProvider";
    
    
    public static File[] getPicFileList(String folder) 
    {
    	File folderFile = new File(folder);
    	if (!folderFile.exists() || !folderFile.isDirectory())
    		return null;
    	
    	if (folder2filesMap.containsKey(folder))
    		return folder2filesMap.get(folder);
    	
    	synchronized (LOG_TAG) {
        	if (folder2filesMap.containsKey(folder))
        		return folder2filesMap.get(folder);
        	
    		File[] pics = folderFile.listFiles(sImagesFileFilter);
    		folder2filesMap.put(folder, pics);
    		return folder2filesMap.get(folder);
    	}
    }
    
    @Override
    public void onEnabled(Context context) {
        // This is only called once, regardless of the number of widgets of this
        // type
        // We do not have any global initialization
        //Log.i(LOG_TAG, "onEnabled()");
        super.onEnabled(context);
    }
    
    private void onUpdateOne(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences config = context.getSharedPreferences(ConfigActivityBase.PREFS_NAME, 0);

        // 
    	int layoutResId = 0;
        String frameTypeKey = String.format(ConfigActivityBase.PREFS_FOLDER_FRAME_TYPE_FIELD_PATTERN, appWidgetId);
        
        switch (config.getInt(frameTypeKey, 0)) {
        case 1:
        	layoutResId = R.layout.widget_black_thin;
        	//layoutResId = R.layout.widget_white_thick;
        	break;
        case 2:
        	//layoutResId = R.layout.widget_black_thin;
        	break;
        case 3:
        	//layoutResId = R.layout.widget_black_thick;
        	break;
        case 0:
    	default:
        	layoutResId = R.layout.widget_white_thin;
        	break;
        }
        
        final RemoteViews remoteView = new RemoteViews(context.getPackageName(), layoutResId);
        
        //index
        String currentIndexKey = String.format(ConfigActivityBase.PREFS_UPDATE_CURRENT_PIC_INDEX_FIELD_PATTERN, appWidgetId);
        int currentIndex = config.getInt(currentIndexKey, -1);
        
        //
        String picFolderKey = String.format(ConfigActivityBase.PREFS_FOLDER_PATH_FIELD_PATTERN, appWidgetId);
        String picFolder = config.getString(picFolderKey, null);
        
        if (null != picFolder) 
        {
        	File[] pics = getPicFileList(picFolder);
        	
        	if (null != pics && pics.length > 0) 
        	{
        		int nTried = 0;
        		File currentFile = null;
        		
        		while (null == currentFile) 
        		{
            		currentIndex ++;
            		nTried ++;
            		if (currentIndex >= pics.length) 
            		{
            			currentIndex = 0;
            		}
            		
            		if (pics[currentIndex].exists() && pics[currentIndex].isFile())
            		{
            			currentFile = pics[currentIndex];
            		}
            		
            		if (nTried >= pics.length)
            		{
            			break; // 
            		}
        		}            		
        		
        		if (null != currentFile) {
        			config.edit().putInt(currentIndexKey, currentIndex).commit();
        			
        			BitmapFactory.Options options = new BitmapFactory.Options();
        			options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(currentFile.getPath(), options);
                    
                    BitmapFactory.Options options2 = null;
                    int inSampleSize = 1+ Math.max(Math.max(
                    	Double.valueOf(options.outHeight / 4800).intValue(),
                    	Double.valueOf(options.outWidth / 320).intValue()),
                    	Double.valueOf(Math.sqrt(currentFile.length() / (64*1024))).intValue());
                    
                    Log.d(LOG_TAG, String.format("File=%s, Height=%s, Width=%s, inSampleSize=%s",
                    	currentFile.getAbsoluteFile(), options.outHeight, options.outWidth, inSampleSize));
                    
                    if (inSampleSize > 1) 
                    {
                        options2 = new BitmapFactory.Options();
                        options2.inSampleSize = inSampleSize;
                    }

                    Bitmap bm = BitmapFactory.decodeFile(currentFile.getPath(), options2);
                    if (null != bm)
                    {
                    	remoteView.setImageViewBitmap(R.id.image, bm);
                    }
                    
                    
        		}
        	}
        }
        
		final Intent intentClick = new Intent(this.getOnClickAction());
		intentClick.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		intentClick.setData(Uri.withAppendedPath(Uri.parse(this.getUriSchemaId() + "://widget/id/"), String.valueOf(appWidgetId)));
		final PendingIntent pendingIntentPhoto = PendingIntent.getBroadcast(context, 0, intentClick, 0);
		
		
		Intent configIntent = new Intent(context, ConfigActivity_2x2.class);
		configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		configIntent.setData(Uri.withAppendedPath(Uri.parse(this.getUriSchemaId() + "://widget/id/"), String.valueOf(appWidgetId)));
		configIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
		final PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
		
		remoteView.setOnClickPendingIntent(R.id.image, pendingIntentPhoto);
		remoteView.setOnClickPendingIntent(R.id.configBtn, configPendingIntent);
		
		
        appWidgetManager.updateAppWidget(appWidgetId, remoteView);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(LOG_TAG, "onUpdate(): ");

        for (int appWidgetId : appWidgetIds) {
        	this.onUpdateOne(context, appWidgetManager, appWidgetId);
        }
        
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(LOG_TAG, "onDelete()");

        for (int appWidgetId : appWidgetIds) {

            // stop alarm
            Intent widgetUpdate = new Intent();
            widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(this.getUriSchemaId() + "://widget/id/"), String.valueOf(appWidgetId)));
            PendingIntent newPending = PendingIntent.getBroadcast(context, 0, widgetUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarms.cancel(newPending);

            // remove preference
            Log.d(LOG_TAG, "Removing preference for id " + appWidgetId);
            SharedPreferences config = context.getSharedPreferences(ConfigActivityBase.PREFS_NAME, 0);
            SharedPreferences.Editor configEditor = config.edit();

            configEditor.remove(String.format(ConfigActivityBase.PREFS_UPDATE_RATE_FIELD_PATTERN, appWidgetId));
            configEditor.remove(String.format(ConfigActivityBase.PREFS_FOLDER_PATH_FIELD_PATTERN, appWidgetId));
            configEditor.remove(String.format(ConfigActivityBase.PREFS_FOLDER_FRAME_TYPE_FIELD_PATTERN, appWidgetId));
            configEditor.remove(String.format(ConfigActivityBase.PREFS_UPDATE_CURRENT_PIC_INDEX_FIELD_PATTERN, appWidgetId));
            configEditor.commit();
        }

        super.onDeleted(context, appWidgetIds);
    }

    
    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        Log.d(LOG_TAG, "OnReceive:Action: " + action);
        if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
            final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                this.onDeleted(context, new int[] { appWidgetId });
            }
        } else if (this.getOnClickAction().equals(action)) {
            final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            Log.d(LOG_TAG, "OnReceive:OnClick:Id: " + appWidgetId);
            
            this.onUpdateOne(context, AppWidgetManager.getInstance(context), appWidgetId);
        	registerAlarmService(context, appWidgetId);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
            if (!this.getUriSchemaId().equals(intent.getScheme())) {
                // if the scheme doesn't match, that means it wasn't from the
                // alarm
                // either it's the first time in (even before the configuration
                // is done) or after a reboot or update

            	AppWidgetManager manager = AppWidgetManager.getInstance(context);
                final int[] appWidgetIds = manager.getAppWidgetIds(new ComponentName(context, this.getClass()));
                for (int appWidgetId : appWidgetIds) {
                	registerAlarmService(context, appWidgetId);
                }
            }
            super.onReceive(context, intent);
        } else {
            super.onReceive(context, intent);
        }
    }
    
    private void registerAlarmService(Context context, int appWidgetId) {
        // get the user settings for how long to schedule the update
        // time for
        SharedPreferences config = context.getSharedPreferences(ConfigActivityBase.PREFS_NAME, 0);
        int updateRateSeconds = config.getInt(String.format(ConfigActivityBase.PREFS_UPDATE_RATE_FIELD_PATTERN, appWidgetId), -1);
        if (updateRateSeconds > 0) {
            Log.i(LOG_TAG, "Starting recurring alarm for id " + appWidgetId);
            Intent widgetUpdate = new Intent();
            widgetUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            widgetUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });

            // make this pending intent unique by adding a scheme to
            // it
            widgetUpdate.setData(Uri.withAppendedPath(Uri.parse(this.getUriSchemaId() + "://widget/id/"), String.valueOf(appWidgetId)));
            PendingIntent newPending = PendingIntent.getBroadcast(context, 0, widgetUpdate, PendingIntent.FLAG_UPDATE_CURRENT);

            // schedule the updating
            AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarms.setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + updateRateSeconds * 1000,
                    updateRateSeconds * 1000, newPending);
        }
    }
    
    private static final FileFilter sImagesFileFilter = new FileFilter() {  
    	@Override  
    	public boolean accept(File f) {
    		if (f.isDirectory())
    			return false;
    		
    		int dotIndex = f.getName().lastIndexOf('.');
    		if (-1 == dotIndex)
    			return false;
    		
    		String ext = f.getName().substring(dotIndex + 1);
    		if (null == ext || ext.length() == 0)
    			return false;
    		
    		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
    		if (null == mimeType) {
    			Log.d(LOG_TAG, "X Ignore file: ");
    			return false;
    		}

    		if (mimeType.toLowerCase().startsWith("image/")) {
    			Log.d(LOG_TAG, String.format("Found file: %s. Mimetype: %s.", f.getName(), mimeType));
    			return true;
    		} else {
    			Log.d(LOG_TAG, String.format("X Ignore file: %s. Mimetype: %s.", f.getName(), mimeType));
    			return false;
    		}
    	}  
    };
    
	
}
