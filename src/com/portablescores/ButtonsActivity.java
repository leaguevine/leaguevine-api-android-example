package com.portablescores;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class ButtonsActivity extends Activity{

	Activity myself;
	int score_1 = 0;
	int score_2 = 0;
	public static final String PREFS_NAME = "PortableScoresPrefs";

	SharedPreferences settings;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.buttons);
		myself = this;
		settings = getSharedPreferences(PREFS_NAME, 0);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.buttons);
	}

	public void ConnectToLeaguevine(View view){
		//do we already have an access token for this user?
		String leaguevine_access_token = settings.getString("leaguevine_access_token","");
		//if not, then open up the AuthWebView activity and have the user log in!
		if (leaguevine_access_token==""){
			Intent intent = new Intent(ButtonsActivity.this, AuthWebView.class);
			startActivityForResult(intent,1);  
		}
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent data){
		//make sure an access token was passed
		if (data == null)
		{
			Log.d ("data", "data was null");
			return;

		}
		Bundle extras = data.getExtras();
		String name = extras.getString("Token");
		SharedPreferences.Editor editor = settings.edit();
		//store the token
		editor.putString("leaguevine_access_token", name);
		//can't forget to commit those changes!
		editor.commit();
	}

	public void LeftUp(View view){
		score_1++;
		UpdateScore(score_1,score_2);
	}
	public void LeftDown(View view){
		if (score_1>0)score_1--;
		UpdateScore(score_1,score_2);
	}
	public void RightUp(View view){
		score_2++;
		UpdateScore(score_1,score_2);
	}
	public void RightDown(View view){
		if (score_2>0)score_2--;
		UpdateScore(score_1,score_2);
	}

	public void UpdateScore(int score_1, int score_2){
		String url = "https://api.leaguevine.com/v1/game_scores/";
		HttpPost httpost = new HttpPost(url);
		//		try{
		//			httpost.setEntity(new StringEntity("{\"filters\":true}"));
		//		} catch(Exception e) {}
		//set the headers
		httpost.setHeader("Accept", "application/json");
		httpost.setHeader("Content-Type","application/json");
		//grab the token and use that
		httpost.setHeader("Authorization", "bearer "+settings.getString("leaguevine_access_token",""));
		//set up the request with the appropriate parameters
		JSONObject jsonObj = new JSONObject();
		try{
			jsonObj.put("game_id","81256");
			jsonObj.put("team_1_score",score_1);
			jsonObj.put("team_2_score",score_2);
			jsonObj.put("is_final","False");
			StringEntity entity = new StringEntity(jsonObj.toString());
			entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE,"application/json"));
			httpost.setEntity(entity);
		} catch(Exception e){e.printStackTrace();}
		//we do this special httpclient because we need to make sure https works
		HttpClient httpclient = createHttpClient();
		HttpResponse response;
		try {
			response = httpclient.execute(httpost);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			response.getEntity().writeTo(out);
			out.close();
			//now grab the response
			String responseString = out.toString();
			try {
				JSONObject object = (JSONObject)new JSONTokener(responseString).nextValue();
				//at this point object has all the data that was returned from the server
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} catch(Exception e){}
	}

	private HttpClient createHttpClient()
	{
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
		HttpProtocolParams.setUseExpectContinue(params, true);

		SchemeRegistry schReg = new SchemeRegistry();
		schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

		return new DefaultHttpClient(conMgr, params);
	}
}