package readurl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Main {
	public static void main(String[] args) {
		System.setProperty( "http.proxyHost", "proxy.ibllc.net");
		System.setProperty( "http.proxyPort", "3128");
		
		for( int page = 1; page <= 16; page++) {
			try {
				err( "");
				err( "Page " + page);
				readPage_( page);
				Thread.sleep( 500);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	static void readPage_( int page) throws Exception {
		String s = "http://www.elitetrader.com/vb/showthread.php?s=&threadid=157925&perpage=6&pagenumber=" + page;
		URL url = new URL( s);   
		HttpURLConnection c = (HttpURLConnection)url.openConnection();
		InputStreamReader r1 = new InputStreamReader( c.getInputStream() );
		BufferedReader r2 = new BufferedReader( r1);
		
		String line = r2.readLine();
		
		while( line != null) {
			r2.readLine();
			err( line);
			line = r2.readLine();
		}
		
		r2.close();
		r1.close();
	}

	public static void err(String string) {
		System.out.println( string);
	}
}
