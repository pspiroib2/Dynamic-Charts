/**
 * 
 */
package db;

import java.util.StringTokenizer;

import lib.JtsCalendar;
import lib.S;


/** This is a record in the price database, PriceDb. */
public class Price implements Comparable<Price> {
	public static Price NULL = new Price( "null", 0, 0, 0, 0, 0, 0, 0); // used for Agg
	private static String TIME = " 16:00:00";
	
	private final String m_date;
	private final double m_high;
	private final double m_low;
	private final double m_open;
	private final double m_close;
	private final double m_adjusted;
	private double m_dividend;
	private double m_vol;
	
	public String date() 			{ return m_date; }
	public double high() 			{ return m_high; }
	public double low() 			{ return m_low; }
	public double open() 			{ return m_open; }
	public double close() 			{ return m_close; }
	public double adjusted() 		{ return m_adjusted; }
	public double dividend()		{ return m_dividend; }
	public JtsCalendar dateTime() 	{ return JtsCalendar.createFromYYYYMMDD_HHMMSS( m_date + TIME); }
	
	public double vol() { return m_vol; }

	public void dividend(double v) 	{ m_dividend = v; }
	
	public Price(String date, double high, double low, double open, double close, double adjusted, double dividend, double vol) {
		m_date = date;
		m_high = high;
		m_low = low;
		m_open = open;
		m_close = close;
		m_adjusted = adjusted;
		m_dividend = dividend;
		m_vol = vol;
	}

	Price( String line) {
		StringTokenizer st = new StringTokenizer( line, ",");
		String date = st.nextToken();
		String open = st.nextToken();
		String high = st.nextToken();
		String low = st.nextToken();
		String close = st.nextToken();
		String volume = st.nextToken();
		String adjClose = st.nextToken();
		
		m_date = parseDate( date);
		m_high = Double.parseDouble( high);
		m_low = Double.parseDouble( low);
		m_open = Double.parseDouble( open);
		m_close = Double.parseDouble( close);
		m_adjusted = Double.parseDouble( adjClose);
		m_vol = Double.parseDouble( volume);
	}
	
	/** @param date mm/dd/yyyy
	 *  @return yyyymmdd */
	public static String parseDate( String date) {
		String year = date.substring( 0, 4);
		String month = date.substring( 5, 7);
		String day = date.substring( 8, 10);
		return year + month + day;
	}
	
	@Override public String toString() {
		return m_date + " " + m_close + " " + m_dividend;
	}

	public double get(boolean adjusted) {
		return adjusted ? m_adjusted : m_close;
	}

	public double divPct() {
		return m_dividend == 0 ? 0 : m_dividend * 4 / m_close;
	}

	@Override public int compareTo(Price o) {
		return m_date.compareTo( o.m_date);
	}

	public String getDividendForDisplay() {
		return String.format( "%s (%s)", S.fmt( m_dividend), S.fmtPct( divPct() ) );
		//return S.fmt( m_dividend);
	}
}
