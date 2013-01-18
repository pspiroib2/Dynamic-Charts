/**
 * 
 */
package scale.report;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import lib.S;

// add ability to exclude trades with shift-click
// add ability to drill-down
// add comments to a filter
// ability to see weekly/monthly?

class Filter implements Serializable {
	private static final long serialVersionUID = 1;
	
	static final int STK = 1;
	static final int OPT = 2;
	static final int FUT = 4;
	
	private String m_name;
	private ArrayList<String> m_unders;
	private String m_start;
	private String m_startReporting;
	private String m_end;
	private HashSet<String> m_exclude = new HashSet<String>();
	private int m_secType;
	private String m_account;

	String name()				{ return m_name; }
	String startTrading()		{ return m_start; }
	String startReporting()		{ return m_startReporting; }
	String end()				{ return m_end; }
	String account()			{ return m_account; }
	ArrayList<String> unders() 	{ return m_unders; }
	
	
	void name( String v) 			{ m_name = v; }
	void startTrading( String v)	{ m_start = v; }
	void startReporting( String v)	{ m_startReporting = v; }
	void end( String v)				{ m_end = v; }
	void account( String v)			{ m_account = v; }
	void exclude( String time) 		{ m_exclude.add( time); }

	Filter( String name, ArrayList<String> unders) {
		m_name = name;
		m_unders = unders;
	}
	
	boolean pass( Trade trade) {
		String time = trade.dateTimeStr();
		
		if( m_unders.size() > 0 && !m_unders.contains( trade.underSymbol() ) ) {
			return false;
		}
		
		if( S.isNotNull( m_start) && trade.dateTimeStr().compareTo( m_start) < 0) {
			return false;
		}

		if( S.isNotNull( m_end) && trade.dateTimeStr().compareTo( m_end) > 0) {
			return false;
		}
		
		if( S.isNotNull( m_account) && !m_account.equals( trade.account() ) ) {
			return false;
		}
		
		if( m_exclude.contains( time) ) {
			return false;
		}
		
		if( m_secType != 0) {
			return trade.secType() == "STK" && (m_secType & STK) > 0 ||
				   trade.secType() == "OPT" && (m_secType & OPT) > 0 ||
				   trade.secType() == "FUT" && (m_secType & FUT) > 0;
		}
		
		return true;
	}

	public boolean report(Trade trade) {
		if( S.isNotNull( m_startReporting) && trade.dateTimeStr().compareTo( m_startReporting) < 0) {
			return false;
		}
		return true;
	}
	
	
	/** Return comma-delimited text. */
	public static String getCDText(ArrayList<String> strs) {
		StringBuilder sb = new StringBuilder();
		for( String str : strs) {
			sb.append( str);
			sb.append( ',');
		}
		return sb.toString();
	}

	public void setUnders(String text) {
		m_unders.clear();
		StringTokenizer st = new StringTokenizer( text, ",");
		while( st.hasMoreTokens() ) {
			m_unders.add( st.nextToken() );
		}
	}
	
	public void secType(boolean stk, boolean opt, boolean fut) {
		m_secType = 0;
		if( stk) {
			m_secType |= STK;
		}
		if( opt) {
			m_secType |= OPT;
		}
		if( fut) {
			m_secType |= FUT;
		}
	}
	
	public boolean hasSecType(int bit) {
		return (m_secType & bit) > 0;
	}
}
