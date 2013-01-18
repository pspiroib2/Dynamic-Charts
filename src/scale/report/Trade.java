/**
 * 
 */
package scale.report;

import java.util.ArrayList;

import lib.JtsCalendar;
import lib.S;


class Trade implements Comparable<Trade>, Cloneable {
	static final char C = ',';
	static int s_counter = 1;
	
	private JtsCalendar m_dateTime;
	private int m_num = s_counter++;
	private Side m_side;
	private int m_size; 			// always positive
	private String m_symbol;
	private String m_underSymbol;
	private String m_secType;
	private double m_price;
	private double m_comm;  		// positive
	private int m_remainingSize; 	// always positive 
	private int m_conid;
	private int m_underConid;		// not currently used or needed; not set for settlement trades
	private int m_mult;
	private double m_settlementIncome;
	private String m_transType;     // interned
	private String m_account;
	
	// set from Scale
	private double m_realPnl;		// for dividends, this is the amount of the dividend received
	private double m_mktVal; 		// market value of this contract as of this trade
	private int m_position;			// position of this contract as of this trade
	
	// get
	public JtsCalendar dateTime() 	{ return m_dateTime; }
	public String dateTimeStr() 	{ return m_dateTime.getYYYYMMDDHHMMSS(); }
	public String dateStr() 		{ return m_dateTime.getYYYYMMDD(); }
	public Side side()				{ return m_side; }
	public String symbol()			{ return m_symbol; }
	public int size()				{ return m_size; }
	public int remainingSize()		{ return m_remainingSize; }
	public int signedSize() 		{ return isBuy() ? m_size : -m_size; }
	public int signedRemainingSize(){ return isBuy() ? m_remainingSize : -m_remainingSize; }
	public String underSymbol()		{ return m_underSymbol; }
	public double price() 			{ return m_price; }
	public double comm() 			{ return m_comm; }
	public double realPnl() 		{ return m_realPnl; }
	public double mktVal()			{ return m_mktVal; }
	public int conid() 				{ return m_conid; }
	public String secType() 		{ return m_secType; }
	public int mult()				{ return m_mult; }
	public double settlementIncome(){ return m_settlementIncome; }
	public int position() 			{ return m_position; }
	public String account() 		{ return m_account; }

	// set
	public void realPnl(double v) 	{ m_realPnl = v; }
	public void mktVal(double v)  	{ m_mktVal = v; }
	public void position(int v)		{ m_position = v; }
	
	// helpers
	public boolean isBuy() 			{ return m_side == Side.BUY; }
	public boolean isSell() 		{ return m_side == Side.SELL; }
	public boolean isSettlement() 	{ return m_side == Side.SETTLE; } 
	public boolean isPrice() 		{ return m_side == Side.PRICE; }
	public boolean isDividend()		{ return m_side == Side.DIVIDEND; }

	/** For normal trade or price only. */
	Trade(JtsCalendar dateTime, Side side, int size, int conid, int underConid, int mult, String symbol, String underSymbol, String secType, double price, double comm, String transType, String account) {
		m_dateTime = dateTime;
		m_side = side;
		m_size = Math.abs( size);
		m_remainingSize = m_size;
		m_conid = conid;
		m_underConid = underConid;
		m_mult = mult;
		m_symbol = symbol;
		m_underSymbol = underSymbol;
		m_secType = secType.intern();
		m_price = price;
		m_comm = comm;
		m_transType = transType;
		m_account = account;
	}

	/** For cash settlement. */
	public Trade(JtsCalendar dateTime, Side side, String underSymbol, double income) {
		m_dateTime = dateTime;
		m_side = side;
		m_underSymbol = underSymbol;
		m_settlementIncome = income;
		if( income == 0) {
			S.err( "0");
		}
	}
	
	/** Sort by date/time. */
	public int compareTo(Trade other) {
		int rc = m_dateTime.compareTo( other.m_dateTime);
		return rc != 0 ? rc : compare( m_num, other.m_num);
	}

	private static int compare(int n1, int n2) {
		return n1 < n2 ? -1 : n1 > n2 ? 1 : 0;
	}
	
	public void reduceBy(int size) { 
		m_remainingSize -= size; 
	}
	
	/** Used for closing trades file and per-conid files. */
	public static String getHeader() {
		return "Date,Side,Size,Sym,Type,Price,Comm,Spent,Real pnl";
	}
	
	public String getStr() {
		int size = isSell() ? -m_size : m_size;
		return S.concat( m_dateTime.getExcelDateTime(), m_side, size, m_symbol, m_secType, S.fmt2( m_price), S.fmt2( m_comm), S.fmt2( spent() ), S.fmt2( m_realPnl) );
	}
	
	public String toString() {
		return m_dateTime + " " + m_side + " " + m_size + " " + m_symbol + " @ " + S.fmt2( m_price) + " " + S.fmt2( m_comm);
	}
	
	/** Return positive for buy orders. */
	public double spent() {
		if( isSettlement() ) {
			return -m_settlementIncome;
		}
		if( isDividend() ) {
			return -m_realPnl;
		}
		return signedSize() * m_price * m_mult + m_comm;
	}
	
	public double spent2() {
		return spent() * m_remainingSize / m_size;
	}
	
	/** Return positive for sell orders. */
	public double income() {
		return -spent();
	}
	
	/** Returns size/m_size percent of total income.
	 *  Used when trades are split during pnl calculations. */
	public double income(int size) {
		return income() * size / m_size;
	}
	
	public Trade splitTrade(int size) {
		Trade trade = new Trade( m_dateTime, m_side, size, m_conid, m_underConid, m_mult, m_symbol, m_underSymbol, m_secType, m_price, m_comm * size / m_size, m_transType, m_account);
		trade.m_num = m_num;
		return trade;
	}
	
	public void adjustForSplit() {
		for( Split split : m_splits) {
			if( m_symbol.equals( split.m_symbol) && m_dateTime.compareTo( split.m_date) == -1) {
				int size = m_size / split.m_oldShares * split.m_newShares;
				m_size = size;
				m_remainingSize = m_size;
				m_price = m_price * split.m_oldShares / split.m_newShares;
				break;
			}
		}
	}
	
	static class Split {
		final String m_symbol;
		private int m_oldShares;
		final int m_newShares;
		final JtsCalendar m_date;

		/** Date is first day after split. All trades before date will be adjusted. */
		public Split(String symbol, int oldShares, int newShares, int year, int month, int day) {
			m_symbol = symbol;
			m_oldShares = oldShares;
			m_newShares = newShares;
			m_date = new JtsCalendar( year, month, day);
		}
	}
	
	static Splits m_splits = new Splits();
	
	static class Splits extends ArrayList<Split> {
		Splits() {
			add( new Split( "DRN", 1, 4, 2010, 5, 5) );
			add( new Split( "FAS", 1, 3, 2010, 5, 5) );
			add( new Split( "VXX", 4, 1, 2010, 11, 9) );
		}
	}

	/** Return true if the trades could be combined together and
	 *  considered as one. */
	public boolean matches(Trade other) {
		if( m_account != null && !m_account.equals( other.m_account) ) {
			return false;
		}
		
		return m_dateTime.equals( other.m_dateTime) &&		      
			   m_side.equals( other.m_side) && 
			   m_conid == other.m_conid &&
			   m_price == other.m_price &&
			   !isSettlement();
	}

	/** Returns a new trade which combines the two. */
	public Trade combineWith(Trade other) {
		return new Trade( m_dateTime, m_side, m_size + other.m_size, m_conid, m_underConid, m_mult, m_symbol, m_underSymbol, m_secType, m_price, m_comm + other.m_comm, m_transType, m_account); 
	}

	public boolean isExercise() {
		return m_price == 0 && isSell(); 
	}
	
	public boolean isAssignment() {
		return m_price == 0 && isBuy();
	}

	public String day() {
		return m_dateTime.getYYYYMMDD();
	}

	public boolean needsCashSettlement() {
		return m_price == 0 && isIndexOption();
	}
	
	private boolean isIndexOption() {
		return m_underSymbol.equals( "XEO") || m_underSymbol.equals( "OEX");
	}

	/** Return true if this is a stock that was bought or sold due to option exercise. */
	public boolean stockExercise() {
		return m_secType == "STK" && m_transType == "BookTrade"; // seems not to be working for SGOL
	}
	
	protected Trade clone() {
		try {
			return (Trade)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public Object getSizeForDisplay(boolean showRemaining) {
		if( showRemaining) {
			return Integer.valueOf( m_remainingSize);
		}
		return m_size != 0 ? Integer.valueOf( m_size) : null;
	}
}
