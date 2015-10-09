package org.ischool.zambia.oppia.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.digitalcampus.oppia.activity.PrefsActivity;
import org.digitalcampus.oppia.application.DatabaseManager;
import org.digitalcampus.oppia.application.DbHelper;
import org.digitalcampus.oppia.model.User;
import org.digitalcampus.oppia.utils.storage.FileUtils;
import org.ischool.zambia.oppia.exceptions.ISchoolLoginException;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

public class ISchool extends Application {
	
	public static final String TAG = ISchool.class.getSimpleName();

	public static String deviceId = "";
	
	/* Set of locations to look for the login file/data */
	private static String[] userDataFileLocation = new String[] {
															"/storage/sdcard1/ischool.txt",
															""};
	
	public static void loginUser(Context ctx) throws ISchoolLoginException {
		// read data from the text file on system
		
		String userData = null;
		for (String location: userDataFileLocation){
			try {
				userData = FileUtils.readFile(location);
				Log.d(TAG,"data file content:" + userData);
				break;
			} catch (IOException ioe ){
				// just continue
				Log.d(TAG, "No file found at: " + location);
				continue;
			}
		}
		
		if (userData == null){
			throw new ISchoolLoginException();
		}
		
		// check data in correct format
		// format of info in text file: 
		// user_ID;name_surname;device_ID
		User u = new User();
		try {
			String[] userDataArray = userData.split(";");
			u.setUsername(userDataArray[0].replace('/', '_'));
			String[] userNameArray = userDataArray[1].split("_");
			u.setFirstname(userNameArray[0]);
			u.setLastname(userNameArray[1]);
			ISchool.deviceId = userDataArray[2];
		} catch (ArrayIndexOutOfBoundsException aioobe ){
			throw new ISchoolLoginException();
		}
		
		// find if user already already registered/logged in on the device
		DbHelper db = new DbHelper(ctx);
		long userId = db.isUser(u.getUsername());
		if (userId == -1) {
			// generate password
			UUID guid = java.util.UUID.randomUUID();
			u.setPassword(guid.toString());
			db.addUser(u);
		}
		DatabaseManager.getInstance().closeDatabase();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		// now log user into app
		Editor editor = prefs.edit();
    	editor.putString(PrefsActivity.PREF_USER_NAME, u.getUsername());
    	editor.commit();
	}
	
}
