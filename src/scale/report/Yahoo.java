package scale.report;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.StringTokenizer;

import lib.S;


public class Yahoo {
	public static void main(String[] args) {
		S.err( "" + fetch( "IBM", 6, 13, 2011) );
		S.err( "" + fetch( "IBM", 6, 14, 2011) );
		S.err( "" + fetch( "IBM", 6, 15, 2011) );
		S.err( "" + fetch( "IBM", 6, 16, 2011) );
		S.err( "" + fetch( "IBM", 6, 17, 2011) );
	}
	
	static double fetch(String symbol, int month, int day, int year) {
		try {
			month--;
			
			String str = String.format( "http://ichart.finance.yahoo.com/table.csv?" +
					"s=%s&a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&g=d&ignore=.csv", 
					symbol, "" + month, "" + day, "" + year, "" + month, "" + day, "" + year);

			String str2 = "http://ichart.finance.yahoo.com/table.csv?s=IBM&a=05&b=13&c=2011&d=05&e=13&f=2011&g=d&ignore=.csv";
			
			URL url = new URL( str); 
			HttpURLConnection c = (HttpURLConnection)url.openConnection();
			
			if( c.getResponseCode() == HttpURLConnection.HTTP_OK) {
				  BufferedReader br = new BufferedReader( new InputStreamReader( c.getInputStream()));
				  String header = br.readLine();
				  String line = br.readLine();
				  if( line != null) {
					  StringTokenizer st = new StringTokenizer( line, ",");
					  String date = st.nextToken();
					  String open = st.nextToken();
					  String high = st.nextToken();
					  String low = st.nextToken();
					  String close = st.nextToken();
					  
					  return Double.parseDouble( close);
				  }
			}
			else {
				S.err( "Error: " + c.getResponseCode() );
			}
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	private static String pad(int month) {
		return "" + month;
		//return month >= 10 ? ("" + month) : ("0" + month); 
	}
}
/*
06:47:09	main	http://ichart.finance.yahoo.com/table.csv?s=IBM&a=05&b=13&c=2010&d=05&e=13&f=2010&g=d&ignore=.csv
06:47:09	main	http://ichart.finance.yahoo.com/table.csv?s=IBM&a=05&b=13&c=2011&d=05&e=13&f=2011&g=d&ignore=.csv

*
*/