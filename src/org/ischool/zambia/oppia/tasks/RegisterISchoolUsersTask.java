package org.ischool.zambia.oppia.tasks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.digitalcampus.oppia.activity.PrefsActivity;
import org.digitalcampus.oppia.application.DbHelper;
import org.digitalcampus.oppia.application.MobileLearning;
import org.digitalcampus.oppia.model.User;
import org.digitalcampus.oppia.task.Payload;
import org.digitalcampus.oppia.utils.HTTPClientUtils;
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
		
		 OkHttpClient client = HTTPClientUtils.getClient(ctx);

		String url = prefs.getString(PrefsActivity.PREF_SERVER, ctx.getString(R.string.prefServerDefault))
				+ MobileLearning.REGISTER_PATH;
		DbHelper db = DbHelper.getInstance(ctx);
		ArrayList<User> unregisteredUsers = db.getUnregisteredUsers();
		
		if (unregisteredUsers.size() == 0){
			payload.setResult(true);
			Log.d(TAG,"There are no users to register");
			return payload;
		}
		for (User u: unregisteredUsers){
		
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
	            
	
	            Request request = new Request.Builder()
                	.url(HTTPClientUtils.getFullURL(ctx, MobileLearning.REGISTER_PATH))
                	.post(RequestBody.create(HTTPClientUtils.MEDIA_TYPE_JSON, json.toString()))
                	.build();


	            	// make request
	            Response response = client.newCall(request).execute();
	            if (response.isSuccessful()){
	            	JSONObject jsonResp = new JSONObject(response.body().string());
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
					DbHelper db1 = DbHelper.getInstance(ctx);
					db1.addOrUpdateUser(u);
					
					payload.setResult(true);
					payload.setResultResponse(ctx.getString(R.string.register_complete));
					break;
	            }
	            else{
	                switch (response.code()) {
	                    case 400:
	                        payload.setResult(false);
	                        payload.setResultResponse(response.body().string());
	                        break;
	                    default:
	                        payload.setResult(false);
	                        payload.setResultResponse(ctx.getString(R.string.error_connection));
	                }
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
