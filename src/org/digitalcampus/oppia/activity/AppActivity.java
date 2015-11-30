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

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;

import org.ischool.zambia.oppia.R;
import org.ischool.zambia.oppia.application.ISchool;
import org.ischool.zambia.oppia.exceptions.ISchoolLoginException;
import org.ischool.zambia.oppia.exceptions.UserIdFormatException;
import org.digitalcampus.oppia.application.DatabaseManager;
import org.digitalcampus.oppia.application.DbHelper;
import org.digitalcampus.oppia.application.ScheduleReminders;
import org.digitalcampus.oppia.exception.UserNotFoundException;
import org.digitalcampus.oppia.model.User;


public class AppActivity extends FragmentActivity {
	
	public static final String TAG = AppActivity.class.getSimpleName();
	
	private ScheduleReminders reminders;

	
	/**
	 * @param activities
	 */
	public void drawReminders(ArrayList<org.digitalcampus.oppia.model.Activity> activities){
		try {
			reminders = (ScheduleReminders) findViewById(R.id.schedule_reminders);
			reminders.initSheduleReminders(activities);
		} catch (NullPointerException npe) {
			// do nothing
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
		
		try {
			TextView uTV = (TextView) this.findViewById(R.id.ischool_username);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			DbHelper db = new DbHelper(this);
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
			DatabaseManager.getInstance().closeDatabase();
			
		} catch (NullPointerException npe){
			
		}
		/* ischool specific end */
	}
}
