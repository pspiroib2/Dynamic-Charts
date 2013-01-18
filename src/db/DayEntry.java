/**
 * 
 */
package db;

import java.util.ArrayList;


import lib.S;


/** This is one row in the table. */
class DayEntry {
	private final String m_date;
	private Entry[] m_entries; // one entry per column, including AGG (maybe) and SPY (always)
	static String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", };

	Entry get(int i) 	{ return m_entries[i]; }
	String date() 		{ return m_date; }
	String year() 		{ return m_date.substring( 0, 4); }
	
	String month() { 
		int i = Integer.parseInt( m_date.substring( 4, 6) ) - 1;
		return MONTHS[i];
	}

	public DayEntry(String date, int numEntries) {
		m_date = date;
		m_entries = new Entry[numEntries];
	}

	/** Check entries before before. */
	public boolean valid( int before) {
		for( int i = 0; i < before; i++) {
			if( m_entries[i] == null) {
				return false;
			}
		}
		return true;
	}
	
	public void calcPercentages( DayEntry firstDay, DayEntry prevDay) {
		for( int i = 0; i < m_entries.length; i++) {
			Entry entry = m_entries[i];
			Entry first = firstDay.get( i);
			Entry prev = prevDay != null ? prevDay.get( i) : null;
			entry.calcChanges( first, prev);
		}
	}

	/** Return min/max value (either close or pctChange) for all contracts not in the agg
	 *  (since those won't be drawn on the chart. 
	 * @param maxType */
	public double getMinMaxValue( ArrayList<Attribs> attribsList, PriceType priceType) {
		double minMax = priceType.getStartingVal();
		
		for( int i = 0; i < m_entries.length - 1; i++) { // skip the last one which is SPY
			if( !attribsList.get( i).agg() ) { // don't draw lines part of the agg
				Entry entry = m_entries[i];
				double value = entry.getValue( priceType);				
				minMax = priceType.getMinMax( minMax, value);
			}
		}
		
		return minMax;
	}
	
	public void set(int i, double close, Price price) {
		Entry entry = new Entry( close, price);
		m_entries[i] = entry;
	}

	/** Calculate aggregate price for this day and add to this DayEntry. */
	public void calcAgg(ArrayList<Attribs> attribsList, int i) {
		// calculate agg amount, only counting included contracts to the left of agg; note that SPY is to the right of agg
		double total = 0;
		for( int j = 0; j < i; j++) {
			Attribs attribs = attribsList.get( j);
			Entry entry = m_entries[j];
			if( attribs.agg() && entry != null) {  // if entry is null the whole DayEntry will be removed in removeInvalidEntries()
				double val = entry.close() * attribs.aggShares();
				total += val;
			}
		}
		
		// add agg entry to the array
		Entry aggEntry = new Entry( total, Price.NULL);
		m_entries[i] = aggEntry;
	}
	
	enum PriceType { 
		MIN_LOW, MAX_HIGH, MIN_CLOSE, MAX_CLOSE, MIN_CHANGE, MAX_CHANGE, MIN_VOL, MAX_VOL, MIN_DIV, MAX_DIV;
		
		boolean min() {
			return this == MIN_LOW || this == MIN_VOL || this == MIN_DIV || this == MIN_CLOSE || this == MIN_CHANGE;
		}
		
		public double getStartingVal() { 
			return min() ? Double.MAX_VALUE : -10000.0;
		}

		public double getMinMax(double v1, double v2) {
			// don't return min div of zero
			if( this == MIN_DIV && v2 == 0) {
				return v1;
			}
			
			return min() ? Math.min( v1, v2) : Math.max( v1, v2);
		}
	}

	static class Entry {
		private Price m_price;
		private double m_close; 			// close or adjusted close;
		private double m_changeFromFirst; 	// percentage change from first  this is not needed, we get the same chart just plotting the prices. ps
		private double m_changeFromPrev;  	// percentage change from previous (used to calculate beta)
		private long m_time;

		Price price()				{ return m_price; }
		double div()				{ return m_price.dividend(); }
		double divPct() 			{ return m_price.divPct(); }
		double close() 				{ return m_close; }
		double changeFromFirst() 	{ return m_changeFromFirst; }
		double changeFromPrev() 	{ return m_changeFromPrev; }
		
		Entry( double close, Price price) {
			m_close = close;
			m_price = price;
		}
		
		double getValue( PriceType type) {
			switch( type) {
				case MIN_LOW:		return m_price.low();
				case MAX_HIGH:		return m_price.high();
				case MIN_CLOSE:		return m_close;
				case MAX_CLOSE:		return m_close;
				case MIN_CHANGE:	return m_changeFromFirst;
				case MAX_CHANGE:	return m_changeFromFirst;
				case MIN_VOL:		return m_price.vol();
				case MAX_VOL:		return m_price.vol();
				case MIN_DIV:		return m_price.dividend();
				case MAX_DIV:		return m_price.dividend();
				default: 			S.err( "ERROR"); return 0;
			}
		}
		
		double getVal( PriceType type) {
			return type == PriceType.MAX_CLOSE ? m_close : m_changeFromFirst;
		}
		
		public void calcChanges( Entry first, Entry prev) {
			m_changeFromFirst = (m_close - first.m_close) / first.m_close;

			if( prev != null) {
				m_changeFromPrev = Math.log( m_close / prev.m_close);
			}
		}
		
		long time() {
			if( m_time == 0) {
				m_time = m_price.dateTime().getTimeInMillis();
			}
			return m_time;
		}
	}
}
