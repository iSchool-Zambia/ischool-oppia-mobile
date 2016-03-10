/* 
 * This file is part of OppiaMobile - https://digital-campus.org/
 * 
 * OppiaMobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OppiaMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OppiaMobile. If not, see <http://www.gnu.org/licenses/>.
 */

package org.digitalcampus.oppia.activity;

import android.content.Intent;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;

import org.ischool.zambia.oppia.R;
import org.ischool.zambia.oppia.application.ISchool;
import org.ischool.zambia.oppia.exceptions.ISchoolLoginException;
import org.ischool.zambia.oppia.exceptions.UserIdFormatException;
import org.digitalcampus.oppia.application.DbHelper;
import org.digitalcampus.oppia.application.ScheduleReminders;
import org.digitalcampus.oppia.exception.UserNotFoundException;
import org.digitalcampus.oppia.model.User;
import org.digitalcampus.oppia.application.MobileLearning;
import org.digitalcampus.oppia.application.SessionManager;


public class AppActivity extends FragmentActivity {
	
	public static final String TAG = AppActivity.class.getSimpleName();

    /**
	 * @param activities: list of activities to show on the ScheduleReminders section
	 */
	public void drawReminders(ArrayList<org.digitalcampus.oppia.model.Activity> activities){
        ScheduleReminders reminders = (ScheduleReminders) findViewById(R.id.schedule_reminders);
        if (reminders != null){
            reminders.initSheduleReminders(activities);
        }
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
		}
		return true;
	}

	
	@Override
	protected void onResume(){
		super.onResume();
		/* ischool specific start */
		try {
			ISchool.loginUser(this);
		} catch (ISchoolLoginException isle){
			return;
		} catch (UserIdFormatException usfe) {
			return;
		}
		
		//We check if the user session time has expired to log him out
        if (MobileLearning.SESSION_EXPIRATION_ENABLED){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            long now = System.currentTimeMillis()/1000;
            long lastTimeActive = prefs.getLong(PrefsActivity.LAST_ACTIVE_TIME, now);
            long timePassed = now - lastTimeActive;

            prefs.edit().putLong(PrefsActivity.LAST_ACTIVE_TIME, now).apply();
            if (timePassed > MobileLearning.SESSION_EXPIRATION_TIMEOUT){
                Log.d(TAG, "Session timeout (passed " + timePassed + " seconds), logging out");
                logoutAndRestartApp();
            }
        }
        
		try {
			TextView uTV = (TextView) this.findViewById(R.id.ischool_username);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			DbHelper db = DbHelper.getInstance(this);
			try {
				User u = db.getUser(prefs.getString(PrefsActivity.PREF_USER_NAME, ""));
				
				String username;
				if (u.getFirstname() != null && !u.getFirstname().equals("")){
					username = u.getFirstname() + " " + u.getLastname();
				} else {
					username = u.getUsername();
				}
				uTV.setText(this.getString(R.string.ischool_username, username));
			} catch (UserNotFoundException unfe){
				
			}
			
		} catch (NullPointerException npe){
			
		}
		/* ischool specific end */
	}

    @Override
    public void onPause(){
        super.onPause();
        if (MobileLearning.SESSION_EXPIRATION_ENABLED){
            long now = System.currentTimeMillis()/1000;
            PreferenceManager
                .getDefaultSharedPreferences(this).edit()
                .putLong(PrefsActivity.LAST_ACTIVE_TIME, now).apply();
        }
    }

    public void logoutAndRestartApp(){

        SessionManager.logoutCurrentUser(this);

        Intent restartIntent = new Intent(this, StartUpActivity.class);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(restartIntent);
        this.finish();
    }

}
