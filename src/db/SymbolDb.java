package db;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;

import lib.JtsCalendar;
import lib.OStream;
import lib.S;
import db.PriceDb.BadSymbol;
import db.PriceDb.BarType;

/** All prices for a single symbol. */
public class SymbolDb {
	public static final String YESTERDAY = JtsCalendar.getYesterday().getYYYYMMDD();
	
	private String m_firstDate;
	private String m_lastDate;
	private String m_symbol;
	private BarType m_time;
	private ArrayList<Price> m_prices = new ArrayList<Price>(); // maps date to price
	private String m_filename;
	private boolean m_read;
	//private String[] currencies = { 

	private boolean fx() {
//		for( String cur : currencies) {
//			if( m_symbol.equals( cur)  {
//				return true;
//			}
//		}
		return false;
	}

	public SymbolDb( String symbol, BarType time) {
		m_symbol = symbol;
		m_time = time;
		m_filename = String.format( "%s\\%s_%s.csv", S.getTempDir(), fix( m_symbol), time.toString() );
	}

	private String fix(String str) { 
		str.replaceAll( "=", "");
		return str.charAt( 0) == '^' ? str.substring( 1) : str;
	}

	/** Get prices from date, either from file or from download.
	 *  @param startDate is YYYYMMDD */ 
	public Collection<Price> getAllPrices( String startDate) throws BadSymbol {
		return getPrices( startDate, YESTERDAY); 
	}

	public Collection<Price> getPrices( String startDate, String endDate) throws BadSymbol {
		// read date from file
		if( !m_read) {
			readFromFile();
		}
		
		// if not enough data was available in file, download from server
		if( m_prices.isEmpty() || startDate.compareTo( m_firstDate ) < 0 || endDate.compareTo( m_lastDate ) > 0) {
			m_prices.clear(); // this is needed so start date in file is accurate
			downloadFromYahoo( startDate, endDate);
			writeToFile();
		}

		// return all prices?
		if( m_firstDate.equals( startDate) && m_lastDate.equals( endDate) ) { // could use compareTo to pick up more cases. ps
			return m_prices;
		}

		// return subset of prices
		ArrayList<Price> prices = new ArrayList<Price>();
		for( Price price : m_prices) {
			if( price.date().compareTo( startDate) >= 0 && 
			    (endDate == YESTERDAY || price.date().compareTo( endDate) <= 0) ) { // use == compare for better performance
				prices.add( price);
			}
		}
		return prices;
	}
	
	void downloadFromYahoo( String startDate, String endDate) throws BadSymbol {
		if( fx() ) {
			downloadFxFrom( startDate);
		}
		else {
			downloadStockFrom( startDate, endDate); // wrong, need end date. ps
		}
	}
	
	void downloadFxFrom( String startDate) {
//		String str = String.format( "http://www.global-view.com/forex-trading-tools/forex-history/exchange_csv_report.html?" +
//				"CLOSE_1=ON&" +
//				"start_date=09/11/2010&" +
//				"stop_date=09/11/2011&" +
//				"Submit=Get%20Daily%20Stats", 
	}

	void downloadStockFrom( String startDate, String endDate) throws BadSymbol {
		try {
			//PriceDb.out( "querying " + m_symbol + " " + startDate + " " + endDate + " " + m_time);
			String year = startDate.substring( 0, 4);
			int month = Integer.parseInt( startDate.substring( 4, 6) ) - 1;
			String day = startDate.substring( 6, 8);
	
			String eyear = endDate.substring( 0, 4);
			int emonth = Integer.parseInt( endDate.substring( 4, 6) ) - 1;
			String eday = endDate.substring( 6, 8);
	
			// g=d, w, or m (daily, weekly, monthly)
			String str = String.format( "http://ichart.finance.yahoo.com/table.csv?s=%s&a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&g=%s&ignore=.csv",
				m_symbol, month, day, year, emonth, eday, eyear, m_time.getChar() );
			URL url = new URL( str);
			HttpURLConnection c = (HttpURLConnection)url.openConnection();
			BufferedReader br = new BufferedReader( new InputStreamReader( c.getInputStream() ) );
			int lc = 0;
			String line;
			while( (line=br.readLine() ) != null) {
				if( lc++ == 0) { // skip header
					continue;
				}
				Price price = new Price( line);
				m_prices.add( price);
			}

			Collections.sort( m_prices);
			m_firstDate = startDate;
			m_lastDate = JtsCalendar.getYesterday().getYYYYMMDD();

			downloadDividedsFrom( month, day, year, emonth, eday, eyear);
		}
		catch( Exception e) {
			//S.err( "Throwing bad symbol " + m_symbol);
			throw new BadSymbol();
		}
	}

	private void downloadDividedsFrom(int month, String day, String year, int emonth, String eday, String eyear) {
		try {
			// get dividends
			String str = String.format( "http://ichart.finance.yahoo.com/table.csv?s=%s&a=%s&b=%s&c=%s&d=%s&e=%s&f=%s&g=v&ignore=.csv",
					m_symbol, month, day, year, emonth, eday, eyear);
	
			URL url = new URL( str);
			HttpURLConnection c = (HttpURLConnection)url.openConnection();
			BufferedReader br = new BufferedReader( new InputStreamReader( c.getInputStream() ) );
			int lc = 0;
			String line;
			while( (line=br.readLine() ) != null) {
				if( lc++ == 0) { // skip header
					continue;
				}
				StringTokenizer st = new StringTokenizer( line, ",");
				String date = Price.parseDate( st.nextToken() );
				double div = Double.parseDouble( st.nextToken() ); 

				Price temp = new Price( date, 0, 0, 0, 0, 0, 0, 0);
				int i = Collections.binarySearch( m_prices, temp);
				if( i < 0) {
					i = -(i + 1);
				}
				if( i < m_prices.size() ) {
					Price price = m_prices.get( i);
					price.dividend( div);
				}
				else {
					S.err( "error: no price for " + date);
				}
			}
		}
		catch( Exception e) {
			S.err( "Error " + e);
		}
	}

	void readFromFile() {
		try {
			BufferedReader br = new BufferedReader( new FileReader( m_filename) );
			
			m_firstDate = br.readLine();
			m_lastDate = br.readLine();
			
			String line;
			while( (line=br.readLine() ) != null) {
				StringTokenizer st = new StringTokenizer( line, ",");
				String date = st.nextToken();
				String high = st.nextToken();
				String low = st.nextToken();
				String open = st.nextToken();
				String close = st.nextToken();
				String adjusted = st.nextToken();
				String dividend = st.nextToken();
				String vol = st.nextToken();
				Price price = new Price( date, Double.parseDouble( high), Double.parseDouble( low), Double.parseDouble( open), Double.parseDouble( close), Double.parseDouble( adjusted), Double.parseDouble( dividend), Double.parseDouble( vol) );
				m_prices.add( price); 
			}
			m_read = true;
		} 
		catch( FileNotFoundException e) {
		}
		catch( Exception e) {
			e.printStackTrace();
			S.deleteFile(m_filename);
		}
	}
	
	void writeToFile() {
		try {
			OStream os = new OStream( m_filename, false);
			
			os.writeln( m_firstDate);
			os.writeln( m_lastDate);
			
			for( Price price : m_prices) {
				String line = String.format( "%s,%s,%s,%s,%s,%s,%s,%s", price.date(), price.high(), price.low(), price.open(), price.close(), price.adjusted(), price.dividend(), price.vol() ); 
				os.writeln( line);
			}
			
			m_read = true;
		} 
		catch( Exception e) {
			e.printStackTrace();
		}
	}
}
