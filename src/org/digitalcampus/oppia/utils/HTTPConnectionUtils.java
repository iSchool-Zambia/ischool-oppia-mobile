package org.digitalcampus.oppia.utils;

import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.ischool.zambia.oppia.R;
import org.digitalcampus.oppia.activity.PrefsActivity;
import org.digitalcampus.oppia.application.MobileLearning;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class HTTPConnectionUtils extends DefaultHttpClient {
	
	private HttpParams httpParameters;
	private SharedPreferences prefs;
	private Context ctx;
	
	public HTTPConnectionUtils(Context ctx){
		this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		this.ctx = ctx;
		this.httpParameters = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(
				httpParameters,
				Integer.parseInt(prefs.getString(PrefsActivity.PREF_SERVER_TIMEOUT_CONN,
						ctx.getString(R.string.prefServerTimeoutConnectionDefault))));
		HttpConnectionParams.setSoTimeout(
				httpParameters,
				Integer.parseInt(prefs.getString(PrefsActivity.PREF_SERVER_TIMEOUT_CONN,
						ctx.getString(R.string.prefServerTimeoutResponseDefault))));
		
		// add user agent 
		String v = "0";
		try {
			v = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
        
		super.setParams(httpParameters);
		super.getParams().setParameter(CoreProtocolPNames.USER_AGENT, MobileLearning.USER_AGENT + v);
	}
	
	public BasicHeader getAuthHeader(String username, String apiKey){
		return new BasicHeader("Authorization","ApiKey " + username + ":" + apiKey);
	}
	
	public String getFullURL(String apiPath){
		return prefs.getString(PrefsActivity.PREF_SERVER, ctx.getString(R.string.prefServerDefault)) + apiPath;
	}

	public String createUrlWithCredentials(String baseUrl, String username, String apiKey){
		List<NameValuePair> pairs = new LinkedList<NameValuePair>();
		pairs.add(new BasicNameValuePair("username", username));
		pairs.add(new BasicNameValuePair("api_key", apiKey));
		pairs.add(new BasicNameValuePair("format", "json"));
		String paramString = URLEncodedUtils.format(pairs, "utf-8");
		if(!baseUrl.endsWith("?"))
			baseUrl += "?";
		baseUrl += paramString;
		return baseUrl;
	}

}
