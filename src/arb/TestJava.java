package arb;

import java.net.HttpURLConnection;
import java.net.URL;

import lib.S;


public class TestJava {
	public static void main(String[] args) {
		try {
			main_();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main_() throws Exception {
		S.err( System.getProperty( "java.version") );
		
		System.setProperty("https.proxyHost", "192.168.121.254");
		System.setProperty("https.proxyPort", "3128");
		System.setProperty("http.proxyHost", "192.168.121.254");
		System.setProperty("http.proxyPort", "3128");

		String urlStr = "https://www.interactivebrokers.com/sso/AuthenticateTWS";

		URL url = new URL(urlStr);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		int code = conn.getResponseCode();
		S.err( "" + code);

	}
}
