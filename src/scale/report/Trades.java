package scale.report;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.StringTokenizer;

import lib.JtsCalendar;
import lib.S;
import lib.STokenizer;


class Trades extends ArrayList<Trade> {
	enum Type { QUEUE, STACK, TAXES, BEST };
	
	Type m_type = Type.STACK;//BEST;
	
	Trade lastTrade() 	{ return size() == 0 ? null : get( size() - 1); }
	
	public Trades() {
	}
	
	void readCashSettlement( String filename) throws Exception {
		S.err( "reading " + filename);
		FileInputStream fis = new FileInputStream( filename);
		InputStreamReader isr = new InputStreamReader( fis);
		BufferedReader reader = new BufferedReader( isr);

		String line;
		while( (line=reader.readLine()) != null) {
			processCashSettlementLine( line);
		}
	}
	
	private void processCashSettlementLine(String line) {
		STokenizer st = new STokenizer( line);
		String dateTimeIn = st.nextToken();
		String action = st.nextToken();
		String underSymbol = st.nextToken();
		String expiry = st.nextToken();
		String strike = st.nextToken();
		String right = st.nextToken();
		String strIncome = st.nextToken();
		
		// skip header row
		if( S.isNull( dateTimeIn) || dateTimeIn.startsWith( "Date")) {
			return;
		}
		
		// skip this symbol?
		if( !Report.monitor( underSymbol, "") ) {
			return;
		}
		
		// parse date/time
		StringTokenizer st2 = new StringTokenizer( dateTimeIn);
		String date = st2.nextToken();
		String time = st2.nextToken();
		String strDateTime = date.substring( 0, 4) + date.substring( 5, 7) + date.substring( 8, 10) + " " + time + " EST";
		JtsCalendar dateTime = JtsCalendar.createFromYYYYMMDD_HHMMSS( strDateTime);
		
		double income = Double.parseDouble( strIncome);
		Trade trade = new Trade( dateTime, Side.SETTLE, underSymbol, income);
		add( trade);
	}

	void readTrades( String filename) throws Exception {
		S.err( "reading " + filename);
		FileInputStream fis = new FileInputStream( filename);
		InputStreamReader isr = new InputStreamReader( fis);
		BufferedReader reader = new BufferedReader( isr);

		String line = reader.readLine();
		processHeader( line);
		
		while( (line=reader.readLine()) != null) {
			processTrade( line);
		}
	}

	String[] fields = { 
			"Date/Time",
			"Symbol",
			"AssetClass",		// secType
			"Description",
			"Conid",
			"UnderlyingConid",
			"Multiplier",
			"Strike",
			"Expiry",        // add order ref. ps
			"Put/Call",
			"Buy/Sell",
			"Quantity",
			"Price",
			"Commission",
			"TransactionType",
			"ClientAccountID" 
	};
	
	int[] ar = new int[fields.length];
	
	private void processHeader(String line) {
		ArrayList<String> list = new ArrayList<String>();
		STokenizer st = new STokenizer( line);
		String str;
		while( (str=st.nextToken() ) != null) {
			list.add( str);
		}
		
		for( int i = 0; i < fields.length; i++) {
			ar[i] = list.indexOf( fields[i] );
		}
	}
	
	private void processTrade(String line) {
		String[] strs = new String[30];
		int i = 0;
		
		// parse lines into strings
		STokenizer st = new STokenizer( line);
		String str;
		while( (str=st.nextToken() ) != null) {
			strs[i++] = str;
		}

		int n = 0;
		String dateTimeIn = strs[ar[n++]];
		String symbol = strs[ar[n++]];
		String secType = strs[ar[n++]].intern(); // move the intern or make enum. ps
		String description = strs[ar[n++]];
		String strConid = strs[ar[n++]];
		String strUnderConid = strs[ar[n++]];
		String strMult = strs[ar[n++]];
		String strike = strs[ar[n++]];
		String expiry = strs[ar[n++]];
		String right = strs[ar[n++]];
		String sideIn = strs[ar[n++]];
		String strSize = strs[ar[n++]];
		String strPrice = strs[ar[n++]];
		String strComm = strs[ar[n++]];
		String transType = fetch( strs, ar[n++]);  // use fetch for optional columns
		String account = fetch( strs, ar[n++]);
		
		if( S.isNull( strUnderConid) ) {
			strUnderConid = strConid;
		}
		
		String underSymbol = secType.equals( "STK") ? symbol : new StringTokenizer( description, " ").nextToken();
		
		symbol = notQQQQ( symbol);
		underSymbol =notQQQQ( underSymbol);
		
		// skip header row
		if( dateTimeIn.equals( "Date/Time")) {
			return;
		}
		
		// skip this symbol?
		if( !Report.monitor( underSymbol, secType) ) {
			return;
		}
		
		// add account to list
		if( account != null) {
			Report.INSTANCE.addAccount( account);
		}

		// combine two VXX trades near split into single trade to avoid using decimal sizes
//		if( symbol.equals( "VXX") ) {
//			if( strSize.equals( "-10")) {
//				Report.out( "skipping VXX trade");
//				return;
//			}
			if( strSize.equals( "0.5") ) {
				return;
//				strSize = "-2";
//				strPrice = "45.9075";
			}
//		}

		// parse date/time
		StringTokenizer st2 = new StringTokenizer( dateTimeIn, " ;");
		String date = st2.nextToken().replaceAll( "-", "");
		String time = st2.nextToken();
		
		// change 120000 to 12:00:00
		if (time.indexOf( ':') == -1) {
			time = String.format( "%s:%s:%s", time.substring( 0, 2), time.substring( 2, 4), time.substring( 4, 6) );
		}
		
		String strDateTime = date + " " + time + " EST";
		JtsCalendar dateTime = JtsCalendar.createFromYYYYMMDD_HHMMSS( strDateTime);
		
		// parse other values
		int size = Integer.parseInt( strSize);
		double price = Double.parseDouble( strPrice);
		double comm = -Double.parseDouble( strComm);  // comm comes in negative; we flip it to positive
		int conid = Integer.parseInt( strConid);
		int underConid = Integer.parseInt( strUnderConid);
		int mult = Integer.parseInt( strMult);
		Side side = Side.create( sideIn, size);
		
		// VXX had conid change after split
		if( conid == 57582212) {
			conid = 80789235;
		}
					
		Trade trade = new Trade( dateTime, side, size, conid, underConid, mult, symbol, underSymbol, secType, price, comm, transType, account);
		addAndConsolidate( trade);
		
		if( secType.equals( "OPT") ) {
			symbol += " " + mult;
		}
		
		Report.INSTANCE.addConid( symbol, conid, secType, dateTime);
	}
	
	private static String notQQQQ(String sym) {
		return sym.equals( "QQQQ") ? "QQQ" : sym;
	}

	private static String fetch(String[] strs, int i) {
		return i != -1 ? strs[i] : null;
	}

	// this has to be done for real trades only, not split trades; test "opening trades" after re-enabling. ps
	public boolean addAndConsolidate(Trade trade) {
		Trade last = lastTrade();
		if( last != null && last.matches( trade) ) {
			remove( size() - 1);
			return super.add( last.combineWith( trade) );
		}
		return super.add( trade);
	}

	public void sort() {
		Collections.sort( this);
	}

	/** Return trade with index i+1 or null. */
	public Trade getNext(int i) {
		return i + 1 < size() ? get( i + 1) : null;
	}

	/** If pairing with the farthest back trade would make it long-term,
	 *  use that one; otherwise, use most recent trade. */ 
	public Trade findBestMatch(Trade closingTrade) {
		if( size() == 0) {
			return null;
		}
		
		int index = -1;
		
		Type type;
		
		if( m_type == Type.TAXES) {
			int year = closingTrade.dateTime().get( Calendar.YEAR);
			if( year == 2008) {
				type = Type.QUEUE;
			}
			else if( year == 2009) {
				type = Type.STACK; //wrong!!!!!
			}
			else {
				type = Type.QUEUE;
			}
		}
		else {
			type = m_type;
		}
		
		switch( type) {
			case QUEUE:
				index = 0; // write some test cases. ???
				break;
				
			case STACK:
				index = size() - 1;
				break;
				
			// once you settle on long or short, you should take the one that
			// will give the smallest gain. ???
			case BEST: {
				if( isLongTerm( get( 0).dateTime(), closingTrade.dateTime() ) ) {
					S.err( "yes------------------");
				}
				index = isLongTerm( get( 0).dateTime(), closingTrade.dateTime() )
					? 0	: size() -1;
				break;
			}
		}
		
		Trade openingTrade = get( index);
		
		if( closingTrade.remainingSize() >= openingTrade.remainingSize() ) {
			remove( index);
		}
		
		return openingTrade;
	}
	
	public void pop() {
		remove( last() );
	}

	Trade peek() {
		int index = last();
		return index != -1 ? get( index) : null;
	}
	
	private int last() {
		return m_type == Type.QUEUE ? 0 : size() - 1;
	}

	
	
	/** Return true if t2 is at least one year past t1. */
	public static boolean isLongTerm( JtsCalendar t1, JtsCalendar t2) {
		int year1  = t1.get( Calendar.YEAR);
		int month1 = t1.get( Calendar.MONTH);
		int day1   = t1.get( Calendar.DATE);
		
		int year2  = t2.get( Calendar.YEAR);
		int month2 = t2.get( Calendar.MONTH);
		int day2   = t2.get( Calendar.DATE);
		
		return year2 - year1 >= 2 ||
		       year2 - year1 == 1 && month2 > month1 ||
		       year2 - year1 == 1 && month2 == month1 && day2 >= day1;
	}

	void dump() {
		for( Trade trade : this) {
			S.err( trade.toString() );
		}
	}

	/** Do this after trades are added so similar trades are already combined. */
	public void adjustForSplit() {
		S.err( "adjusting for splits");
		for( Trade trade : this) {
			trade.adjustForSplit();
		}
	}

//	class Iter implements Iterator<Trade> {
//		Iterator<Trade> m_iter;
//		Trade m_next;
//		Trade m_peek;
//		private FilterPanel m_filter;
//		
//		Iter( FilterPanel filter) {
//			m_filter = filter;
//			m_iter = iterator();
//		}
//
//		public boolean hasNext() {
//			loadNext();
//			return m_next != null;
//		}
//
//		public Trade next() {
//			try { 
//				return peek();
//			}
//			finally {
//				m_next = null;
//			}
//		}
//		
//		Trade peek() {
//			loadNext();
//			return m_next;
//		}
//
//		private void loadNext() {
//			if( m_next == null) {
//				while( m_iter.hasNext() ) {
//					Trade trade = m_iter.next();
//					if( m_filter.pass( trade) ) {
//						m_next = trade;
//						break;
//					}
//				}
//			}
//		}
//
//		public void remove() {
//		}
//	}
//
//	public Iter iter(FilterPanel filter) {
//		return new Iter( filter);
//	}
}
