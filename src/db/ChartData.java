/**
 * 
 */
package db;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import lib.IStream;
import lib.JtsCalendar;
import lib.S;
import lib.Util;
import db.DayEntry.Entry;
import db.DayEntry.PriceType;
import db.PriceDb.BadSymbol;
import db.PriceDb.BarType;

/** Maps date to Entry which contains prices for that date. */
class ChartData { // change to has-a
	public static final String AGG = "Agg";
	public static final String SPY = "SPY";
	public static final int MAX_DAILY_BARS = 800;

	private final ArrayList<Attribs> m_attribs = new ArrayList<Attribs>(); // stores data for table in upper-left corner; no entry is added for SPY
	private double[][] m_corr = new double[0][0];
	private BarType m_barType;
	private final TreeMap<String,DayEntry> m_data = new TreeMap<String,DayEntry>();
	private ArrayList<Color> m_colors = new ArrayList<Color>( Arrays.asList( Colors.colors) );
	
	public int getSize() 				{ return m_attribs.size(); } // return size, not counting SPY by counting agg
	public String getSymbol( int i) 	{ return m_attribs.get( i).symbol(); }
	public Color getColor( int i)		{ return m_attribs.get( i).color(); }
	public Attribs getAttribs( int i)	{ return m_attribs.get( i); }
	public String firstDate()			{ return m_data.size() > 0 ? m_data.firstKey() : null; }
	public String lastDate()			{ return m_data.size() > 0 ? m_data.lastKey() : null; }
	TreeMap<String,DayEntry> data()		{ return m_data; }
	double getCorr( int i, int j) 		{ return m_corr[i][j]; }
	public BarType barType()			{ return m_barType; }
	
	void reset() {
		// remove agg from attribs
		if( getSize() > 0) {
			int last = m_attribs.size() - 1;
			if( m_attribs.get( last).isAgg() ) {
				removeAttribs( last);
			}
		}
		
		// clear all chart data
		m_data.clear();
	}

	public double getDailyVol(int i) { 
		double vol = m_attribs.get( i).vol();
		if( m_barType == BarType.WEEKLY) {
			vol /= Math.sqrt( 5); // calculate this across many years; what is real value for num of days? ps
		}
		return vol;
	}

	public void getOrQueryPrices(String startDate, boolean adjusted) throws BadSymbol {
		boolean agg = hasAgg();
		
		// set bar type
		JtsCalendar cal = JtsCalendar.createFromYYYYMMDD( startDate);
		long dif = System.currentTimeMillis() - cal.getTimeInMillis();
		long days = dif / JtsCalendar.MILLIS_IN_DAY;
		m_barType = days > MAX_DAILY_BARS ? BarType.WEEKLY : BarType.DAILY;
		
		// add chart data for normal symbols
		int i;
		for( i = 0; i < m_attribs.size(); i++) {
			Attribs attribs = m_attribs.get( i);
			
			String symbol = attribs.symbol();
			try {
				if( attribs.filename() != null) {
					addPricesFromIB( attribs.filename(), i, startDate, agg);
				}
				else {
					addChartData( symbol, i, startDate, adjusted, agg);
				}
			}
			catch( BadSymbol e) {
				S.err( "Bad symbol " + symbol);
				m_attribs.remove( i--);
				throw e;
			}
		}
		removeInvalidEntries( i);
		
		// no data?
		if( m_data.size() == 0) {
			S.err( "Error: no data");
			return;
		}

		// add aggregate
		if( agg) {
			addAgg( i++);
		}
		
		// add SPY (no row is added to m_attribs)
		addChartData( SPY, i++, startDate, adjusted, agg);
		removeInvalidEntries( i);
		
		calcPercentages();
		calcVol();
		calcCorr();
		calcBeta();
	}
	
	public void onAggHeaderClicked() {
		setAgg( hasAgg() ? false : true);
	}
	
	private void setAgg(boolean v) {
		for( Attribs attribs : m_attribs) {
			attribs.agg( v);
		}
	}

	private boolean hasAgg() {
		for( Attribs attribs : m_attribs) {
			if( attribs.agg() ) {
				return true;
			}
		}
		return false;
	}
	
	private void addChartData(String symbol, int i, String startDate, boolean adjusted, boolean agg) throws BadSymbol {
		Collection<Price> prices = PriceDb.INSTANCE.getOrQueryAllPrices( symbol, startDate, m_barType);
		for( Price price : prices) {
			DayEntry dayEntry = getOrCreate( price.date(), agg);
			double close = price.get( adjusted);
			dayEntry.set( i, close, price);
		}
		
		// some data is missing?
		if( prices.size() > 0 && i < m_attribs.size() ) {
			JtsCalendar c1 = JtsCalendar.createFromYYYYMMDD( prices.iterator().next().date() );
			JtsCalendar c2 = JtsCalendar.createFromYYYYMMDD( startDate);
			long dif = c1.getTimeInMillis() - c2.getTimeInMillis();
			double days = dif / JtsCalendar.MILLIS_IN_DAY;
			m_attribs.get( i).missing( days > 5);
		}
	}

	/** Reads prices from file downloaded from IB. Format is YYYY-MM-DD,NAV. */
	private void addPricesFromIB( String filename, int i, String startDate, boolean agg) {
		try {
			IStream is = new IStream( filename);
			String line;

			while( (line=is.readln()) != null) {
				// skip header row
				if( line.indexOf( "ReportDate") != -1) {
					continue;
				}
				
				StringTokenizer st = new StringTokenizer( line, ",-\"");
				String year = st.nextToken();
				String month = st.nextToken();
				String day = st.nextToken();
				double nav = Double.parseDouble( st.nextToken() );

				String date = year + pad( month) + pad( day);
				
				if( date.compareTo( startDate) >= 0) {
					Price price = new Price( date, nav, nav, nav, nav, nav, 0, 0);
					DayEntry dayEntry = getOrCreate( date, agg);
					dayEntry.set( i, nav, price);
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	String pad( String str) {
		return str.length() == 1 ? "0" + str : str;
	}

	DayEntry getOrCreate( String date, boolean agg) {
		DayEntry a = m_data.get( date);
		if( a == null) {
			int size = getSize() + 1 + (agg ? 1 : 0);
			a = new DayEntry( date, size);  // add +1 for SPY, 1 for agg
			m_data.put( date, a);
		}
		return a;
	}

	public void calcPercentages() {
		DayEntry first = m_data.values().iterator().next();

		DayEntry prev = null;
		for( DayEntry dayEntry : m_data.values() ) {
			dayEntry.calcPercentages( first, prev);
			prev = dayEntry;
		}
	}

	public void addAgg( int i) {
		// calculate how many shares of each stock we want to buy to get $1000 worth
		// (will be ignored if user has entered custom value)
		DayEntry first = m_data.entrySet().iterator().next().getValue();
		for( int j = 0; j < getSize(); j++) {
			Attribs attribs = getAttribs( j);
			double price = first.get( j).close();
			attribs.aggShares( 1000 / price);
		}

		// calculate the agg amount of each day 
		for( DayEntry dayEntry : m_data.values() ) {
			dayEntry.calcAgg( m_attribs, i);
		}

		// add AGG Attribs
		Attribs attrib = new Attribs( AGG, "Aggregate", getNextColor());
		m_attribs.add( attrib);
	}
	
	private Color getNextColor() {
		return m_colors.size() > 0 ? m_colors.remove( 0) : Color.black;
	}
	
	public void removeInvalidEntries( int before) {
		for( Iterator<DayEntry> iter = m_data.values().iterator(); iter.hasNext(); ) {
			DayEntry dayEntry = iter.next();
			if( !dayEntry.valid( before) ) {
				iter.remove();
			}
		}
	}

	public void calcVol() {
		for( int i = 0; i < getSize(); i++) {
			calcVol( i);
		}
	}
	
	void calcVol( int i) {
		ArrayList<Double> vals = new ArrayList<Double>();
		
		DayEntry last = null;
		for( DayEntry dayEntry : m_data.values() ) {
			if( last != null) {
				vals.add( Math.log( dayEntry.get( i).close() / last.get( i).close() ) );
			}
			last = dayEntry;
		}
		
		double vol = Util.stddev( vals);
		double sharpe = getTotal( i) / vol;
	
		Attribs attribs = m_attribs.get( i);
		attribs.vol( vol);
		attribs.sharpe( sharpe);
	}

	/** Return min/max value for all contracts not in the agg
	 *  (since those won't be drawn on the chart. */
	public double getMinMaxValue( PriceType priceType) {
		double minMax = priceType.getStartingVal();
		for( DayEntry dayEntry : m_data.values() ) {
			double value = dayEntry.getMinMaxValue( m_attribs, priceType);
			minMax = priceType.getMinMax( minMax, value); 
		}
		return minMax;
	}
	
	public void calcBeta() {
		for( int i = 0; i < getSize(); i++) {
			calcBeta( i);
		}
	}
	
	/** Returns the y values for the first and last points. */
	double[] calcSlope( int i) {
		int size = m_data.size();
		
		if( size < 10) {
			return new double[] { 0, 0 };
		}
		
		// calculate averages
		double totalStock = 0;
		double totalTime = 0;
		int x = 0;
		for( DayEntry dayEntry : m_data.values() ) {
			totalStock += dayEntry.get( i).changeFromFirst();
			totalTime += x;
			x++;
		}
		double avgStock = totalStock / size;
		double avgTime = totalTime / size;

		// calc slope
		double totalCovarIndex = 0;
		double totalVarIndex = 0;
		x = 0;

		for( DayEntry dayEntry : m_data.values() ) {
			Entry entry = dayEntry.get( i);
			
			double difStock = entry.changeFromFirst() - avgStock;
			double difTime = x - avgTime;
			totalCovarIndex += difStock * difTime;
			totalVarIndex += difTime * difTime;
			x++;
		}

		int count = m_data.size();
		double covar = totalCovarIndex / count;
		double var = totalVarIndex / count;
		double m = covar / var;
		double b = avgStock - m * avgTime;

		double y1 = b;
		double y2 = m * (x-1) + b;
		return new double[] { y1, y2 };
	}
	
	private void calcBeta(int i) {
		int i2 = getSize(); // index of SPY
		int size = m_data.size() - 1; // skip the first because there is no pct change

		// calculate averages
		double totalIndex = 0;
		double totalStock = 0;
		DayEntry past = null;
		for( DayEntry dayEntry : m_data.values() ) {
			if( past != null) {
				totalStock += dayEntry.get( i).changeFromPrev();
				totalIndex += dayEntry.get( i2).changeFromPrev();
			}
			past = dayEntry;
		}
		double avgStock = totalStock / size;
		double avgIndex = totalIndex / size;
		
		// calc beta
		double totalDifIndexSq = 0;
		double totalStockIndex = 0;
		past = null;
		for( DayEntry dayEntry : m_data.values() ) {
			if( past != null) {
				double difStock = dayEntry.get( i).changeFromPrev() - avgStock;
				double difIndex = dayEntry.get( i2).changeFromPrev() - avgIndex;
				totalDifIndexSq += difIndex * difIndex;
				totalStockIndex += difStock * difIndex;
			}
			past = dayEntry;
		}

		double covar = totalStockIndex / size;
		double var = totalDifIndexSq / size;
		double beta = covar / var;
		double dayAlpha = avgStock - avgIndex * beta;
		double yearAlpha = Math.pow( 1 + dayAlpha, m_barType.barsInYear() ) - 1;
		
		Attribs attribs = m_attribs.get( i);
		attribs.beta( beta);
		attribs.alpha( yearAlpha);  
	}
	
	public void calcCorr() {
		int num = getSize();
		
		m_corr = new double[num][num + 1]; // +1 for SPY
		for( int i = 0; i < num; i++) {
			Arrays.fill( m_corr[i], Double.MAX_VALUE); 
		}

		for( int i = 0; i < num; i++) {
			for( int j = 0; j <= num; j++) {
				if( i != j) {
					m_corr[i][j] = calcCorr( i, j);
				}
			}
			
			Attribs attribs = m_attribs.get( i);
			attribs.rSquared( m_corr[i][num]);
		}
	}
	
	public double calcCorr( int i, int j) {
		double sx = 0;
		double sy = 0;
		double sxy = 0;
		double sx2 = 0;
		double sy2 = 0;
		DayEntry past = null;

		for( DayEntry dayEntry : m_data.values() ) {
			if( past != null) {
				DayEntry.Entry pastI = past.get( i);
				DayEntry.Entry pastJ = past.get( j);
				DayEntry.Entry curI = dayEntry.get( i);
				DayEntry.Entry curJ = dayEntry.get( j);

				double retI = Math.log( curI.close() / pastI.close() );
				double retJ = Math.log( curJ.close() / pastJ.close());

				sx += retI;
				sy += retJ;
				sxy += retI * retJ;
				sx2 += retI * retI;
				sy2 += retJ * retJ;
			}
			past = dayEntry;
		}

		int n = m_data.size() - 1; // use n-1 because we skip the first entry
		double sxsq = sx * sx;
		double sysq = sy * sy;
		double corr = (n * sxy - sx * sy) / Math.sqrt( (n * sx2 - sxsq) * (n * sy2 - sysq) );
		return corr;
	}

	public double getAnnual(int i) {
		JtsCalendar start = JtsCalendar.createFromYYYYMMDD( m_data.firstEntry().getKey() );
		JtsCalendar end = JtsCalendar.createFromYYYYMMDD( m_data.lastEntry().getKey() );
		double millis = end.getTimeInMillis() - start.getTimeInMillis();
		double numDays = millis / JtsCalendar.MILLIS_IN_DAY;
		
		double first = m_data.firstEntry().getValue().get( i).close();
		double last = m_data.lastEntry().getValue().get( i).close();
		double totalCont = Math.log( last / first);
		double annualCont = totalCont / numDays * 365;
		double annual = Math.exp( annualCont) - 1;
		return annual;
	}
	
	public double getTotal(int i) {
		return m_data.lastEntry().getValue().get( i).changeFromFirst();
	}

	public void addSymbol(String symbol, String name) {
		if( !containsSymbol( symbol) ) {
			Attribs attribs = new Attribs( symbol, name, getNextColor());
			m_attribs.add( attribs);
		}
	}
	
	public void addFromIBFile(String symbol, String filename) {
		Attribs attribs = new Attribs( symbol, null, getNextColor());
		attribs.filename( filename);
		m_attribs.add( attribs);
	}

	private boolean containsSymbol(String symbol) {
		for( Attribs attribs : m_attribs) {
			if( attribs.symbol().equals( symbol) ) {
				return true;
			}
		}
		return false;
	}
	
	public void removeAttribs(int i) {
		Attribs attribs = m_attribs.remove( i);
		m_colors.add( 0, attribs.color() );
	}

	/** Remove agg if it is there. */
	public void removeAgg() {
		int last = getSize() - 1;
		if( last >= 0 && m_attribs.get( last).symbol() == AGG) {
			m_attribs.remove( last);
		}
	}
	
	String getYears() {
		String begin = firstDate();
		String end = lastDate();
		return JtsCalendar.getYears( begin, end);
	}

	/** Return true if any symbol has a negative factor (such as with a pair). */
	public boolean usePrices() { // called? ps
		for( Attribs attribs : m_attribs) {
			if( attribs.agg() && attribs.aggShares() < 0) {
				return true;
			}
		}
		
		return getSize() == 1;
	}

	/** Return date that is pct way through. */
	String getDate( double pct) { 
		int i = (int)Math.round( pct * m_data.size() );
		
		int n = 0;
		for( String date : m_data.keySet() ) {
			if( n++ == i) {
				return date;
			}
		}
		return m_data.size() > 0 ? m_data.firstKey() : null;
	}

	public DayEntry getBar(int index) {
		int i = 0;
		for( DayEntry dayEntry : m_data.values() ) {
			if (i++ == index) {
				return dayEntry;
			}
		}
		return null;
	}
}
