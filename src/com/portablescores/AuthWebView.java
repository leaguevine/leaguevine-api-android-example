/**
 * Copyright 2011 Mark Wyszomierski
 * Modified HEAVILY by Bob Baddeley, 2012, for use with Leaguevine
 */
package com.portablescores;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * https://developer.foursquare.com/docs/oauth.html
 * https://foursquare.com/oauth/
 * 
 * @date May 17, 2011
 * @author Mark Wyszomierski (markww@gmail.com)
 * @author Bob Baddeley (bob@portablescores.com) - modified HEAVILY for use with Leaguevine
 */
public class AuthWebView extends Activity 
{
    private static final String TAG = "ActivityWebView";
    private AuthWebView myself;
    
    public static final String REDIRECT_URI = "http%3A%2F%2Fportablescores.com%2F%0A";
    /**
     * Get these values after registering your oauth app at leaguevine
     */
    public static final String CLIENT_SECRET = "c21d500510d0bab4224c25cde6655e";
    public static final String CLIENT_ID = "29d505f10c47b26edc8d0fe0101b06";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authwebview);
        myself = this;
        String url =
            "https://www.leaguevine.com/oauth2/authorize/" + 
                "?client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=" + REDIRECT_URI +
                "&scope=universal";
        // We can override onPageStarted() in the web client and grab the token out.
        WebView webview = (WebView)findViewById(R.id.webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient() {
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
            	//when we load the page, see if there's a code parameter in the URL.
            	//If there is, then the user has attempted to log in. Otherwise, they're
            	//looking at the login page.
            	String fragment = "?code=";
                int start = url.indexOf(fragment);
                if (start > -1) {
                    //now they've logged in. Great. Take the code and make the second
                	//call to get the access token
                	String code = url.substring(start + fragment.length(), url.length());
                    url = "https://www.leaguevine.com/oauth2/token/" + 
                                "?client_id=" + CLIENT_ID +
                                "&client_secret=" + CLIENT_SECRET +
                                "&grant_type=authorization_code" +
                                "&redirect_uri=" + REDIRECT_URI +
                                "&code="+ code;
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpResponse response;
                    String accessToken = "";
					try {
						//get the response from the second call
						response = httpclient.execute(new HttpGet(url));
	                    StatusLine statusLine = response.getStatusLine();
	                    //if it was successful...
	                    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	                        ByteArrayOutputStream out = new ByteArrayOutputStream();
	                        response.getEntity().writeTo(out);
	                        out.close();
	                        String responseString = out.toString();
	                        try {
	                        	//now take the response as a JSON string and tokenize it.
		                        JSONObject object = (JSONObject)new JSONTokener(responseString).nextValue();
		                        accessToken = object.getString("access_token");
		                        //We're going to pass the token back to the application in an intent
		                        Intent resultData = new Intent();
	    						resultData.putExtra("Token", accessToken);
	    						setResult(Activity.RESULT_OK, resultData);
	                        } catch (JSONException e) {
    							e.printStackTrace();
    						}
	                        //and then close the activity. The user never even sees the redirect_uri
	                        //You could remove this line and then have the redirect_uri let the user
	                        //know that authentication was successful... meh.
	                        myself.finish();
	                    } else{
	                        //Closes the connection.
	                        response.getEntity().getContent().close();
	                        throw new IOException(statusLine.getReasonPhrase());
	                    }
					} catch (ClientProtocolException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
                }
            }
        });
        webview.loadUrl(url);
    }
}