package org.ischool.zambia.oppia.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.digitalcampus.oppia.activity.PrefsActivity;
import org.digitalcampus.oppia.application.DatabaseManager;
import org.digitalcampus.oppia.application.DbHelper;
import org.digitalcampus.oppia.application.MobileLearning;
import org.digitalcampus.oppia.model.User;
import org.digitalcampus.oppia.task.Payload;
import org.digitalcampus.oppia.utils.HTTPConnectionUtils;
import org.digitalcampus.oppia.utils.MetaDataUtils;
import org.ischool.zambia.oppia.R;
import org.ischool.zambia.oppia.listeners.RegisterISchoolUserListener;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.splunk.mint.Mint;

public class RegisterISchoolUsersTask extends AsyncTask<Payload, Object, Payload> {

	public static final String TAG = RegisterISchoolUsersTask.class.getSimpleName();

	private Context ctx;
	private SharedPreferences prefs;
	private RegisterISchoolUserListener mStateListener;
	
	public RegisterISchoolUsersTask(Context ctx) {
		this.ctx = ctx;
		prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	}
	
	@Override
	protected Payload doInBackground(Payload... params) {
		
		Payload payload = new Payload();
		
		HTTPConnectionUtils client = new HTTPConnectionUtils(ctx);

		String url = prefs.getString(PrefsActivity.PREF_SERVER, ctx.getString(R.string.prefServerDefault))
				+ MobileLearning.REGISTER_PATH;
		DbHelper db = new DbHelper(ctx);
		ArrayList<User> unregisteredUsers = db.getUnregisteredUsers();
		DatabaseManager.getInstance().closeDatabase();
		
		if (unregisteredUsers.size() == 0){
			payload.setResult(true);
			Log.d(TAG,"There are no users to register");
			return payload;
		}
		for (User u: unregisteredUsers){
			
			HttpPost httpPost = new HttpPost(url);
			try {
				
				// create password and email
				// actual password is irrelevant as won't be used
				// email only needs to be unique within the OppiaServer, again actual email is irrelevant
				UUID guid = java.util.UUID.randomUUID();
				u.setEmail(u.getUsername() + "@ischool.zm");
				u.setPassword(guid.toString());
				
				JSONObject json = new JSONObject();
				json.put("username", u.getUsername());
	            json.put("password", u.getPassword());
	            json.put("passwordagain", u.getPassword());
	            json.put("email",u.getEmail());
	            json.put("firstname",u.getFirstname());
	            json.put("lastname",u.getLastname());
	            StringEntity se = new StringEntity(json.toString(),"utf8");
	            Log.d(TAG,json.toString());
	            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
	            httpPost.setEntity(se);
	
				// make request
				HttpResponse response = client.execute(httpPost);
				
				// read response
				InputStream content = response.getEntity().getContent();
				BufferedReader buffer = new BufferedReader(new InputStreamReader(content), 4096);
				String responseStr = "";
				String s = "";
	
				while ((s = buffer.readLine()) != null) {
					responseStr += s;
				}
	
				Log.d(TAG, responseStr);
				
				switch (response.getStatusLine().getStatusCode()){
					case 400: // unauthorised
						payload.setResult(false);
						payload.setResultResponse(responseStr);
						break;
					case 201: // logged in
						JSONObject jsonResp = new JSONObject(responseStr);
						u.setApiKey(jsonResp.getString("api_key"));
						try {
							u.setPoints(jsonResp.getInt("points"));
							u.setBadges(jsonResp.getInt("badges"));
						} catch (JSONException e){
							u.setPoints(0);
							u.setBadges(0);
						}
						try {
							u.setScoringEnabled(jsonResp.getBoolean("scoring"));
							u.setBadgingEnabled(jsonResp.getBoolean("badging"));
						} catch (JSONException e){
							u.setScoringEnabled(true);
							u.setBadgingEnabled(true);
						}
						try {
							JSONObject metadata = jsonResp.getJSONObject("metadata");
					        MetaDataUtils mu = new MetaDataUtils(ctx);
					        mu.saveMetaData(metadata, prefs);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						
						// add or update user in db
						DbHelper db1 = new DbHelper(ctx);
						db1.addOrUpdateUser(u);
						DatabaseManager.getInstance().closeDatabase();
						
						payload.setResult(true);
						payload.setResultResponse(ctx.getString(R.string.register_complete));
						break;
					default:
						payload.setResult(false);
						payload.setResultResponse(ctx.getString(R.string.error_connection));
				}
			} catch (UnsupportedEncodingException e) {
				payload.setResult(false);
				payload.setResultResponse(ctx.getString(R.string.error_connection));
			} catch (ClientProtocolException e) {
				payload.setResult(false);
				payload.setResultResponse(ctx.getString(R.string.error_connection));
			} catch (IOException e) {
				payload.setResult(false);
				payload.setResultResponse(ctx.getString(R.string.error_connection));
			} catch (JSONException e) {
				Mint.logException(e);
				e.printStackTrace();
				payload.setResult(false);
				payload.setResultResponse(ctx.getString(R.string.error_processing_response));
			} 
		}
		return payload;
	}

	@Override
	protected void onPostExecute(Payload response) {
		synchronized (this) {
			if (mStateListener != null) {
				mStateListener.registerRequestComplete(response);
			}
		}
	}

	public void setRegisterListener(RegisterISchoolUserListener riul) {
		synchronized (this) {
			mStateListener = riul;
		}
	}
}