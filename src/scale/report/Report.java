package scale.report;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import lib.JtsCalendar;
import lib.OStream;
import lib.S;

/** Calculates pnl. */
public class Report {
	enum Index { Stansberry, Metals, Volatility, Open, Core, Pairs, Closed, Commod, Oex, None, };

	static final String PNL_DAY		 	= "c:\\fin\\pnlDay.csv";
	static final String PNL_WEEK 		= "c:\\fin\\pnlWeek.csv";
	static final String PNL_MONTH 		= "c:\\fin\\pnlMonth.csv";
	static final String PNL_YEAR 		= "c:\\fin\\pnlYear.csv";
	static final String ALL 			= "c:\\fin\\pnlAll.csv";
	static final String CLOSING 		= "c:\\fin\\pnlClosing.csv";
	static final String SUMMARY 		= "c:\\fin\\pnlSummary.csv";
	static final String EXERCISE 		= "c:\\fin\\exercise.csv";
	static final String ASSIGNMENT 		= "c:\\fin\\assignment.csv";
	
	static final Report INSTANCE = new Report();
	public static final String C = ",";

	OStream m_osDay;
	OStream m_osWeek;
	OStream m_osMonth;
	OStream m_osYear;

	private Trades m_trades = new Trades();
	private static String m_secType;													// for filtering
	private static Map<String,Integer> m_conids = new HashMap<String,Integer>();		// map symbol to conid
	private HashSet<String> m_accounts = new HashSet<String>();
	
	Trades trades() 		{ return m_trades; }
	static String secType() { return m_secType; }
	
	static String fmt( double val) { return S.fmt2( val); }
	void addAccount( String acct) { m_accounts.add( acct); }
	
	public static void main(String[] args) {
		if( S.isNotNull( System.getProperty("http.proxyHost") ) ) {
        	S.err( String.format( "proxy is %s:%s", 
        			System.getProperty("http.proxyHost"),
        			System.getProperty("http.proxyPort") ) );
		}

		INSTANCE.run();
	}
	
	static boolean monitor(String underSymbol, String secType) {
		if( secType.equals( "CASH")) {
			return false;
		}
		if( underSymbol.equals( "WFC")) { // this was a bond this was transferred in
			return false;
		}
		return true;
	}

	void run() {
		deleteFiles();
		
		JButton b1 = new JButton( "Boxer");
		b1.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onReadBoxer();
			}
		});

		JButton b2 = new JButton( "Coder");
		b2.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onReadCoder();
			}
		});
		
		b1.setPreferredSize( new Dimension( 200, 200 ) );
		b2.setPreferredSize( new Dimension( 200, 200 ) );

		JPanel p = new JPanel();
		p.setLayout( new BoxLayout( p, BoxLayout.X_AXIS) );
		p.add( b1);
		p.add( b2);
		
		JFrame f = new JFrame();
		f.add( p);
		f.setSize( 200, 200);
		f.setVisible( true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//		try {
//			m_trades.readTrades( "c:\\temp\\test.csv");
//			continueProcessing( "c:\\temp\\test.t");
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
	}
	
	private void onReadBoxer() {
		try {
			m_trades.readTrades( "c:\\fin\\2008\\2008_all_ib_trades.csv");
			m_trades.readTrades( "c:\\fin\\2009\\2009_all_ib_trades.csv");
			m_trades.readTrades( "c:\\fin\\2010\\2010_all_ib_trades.csv");
			m_trades.readTrades( "c:\\fin\\2011\\2011_all_ib_trades.csv");
			m_trades.readTrades( "c:\\sync\\boxer\\2012.csv");
			m_trades.adjustForSplit();
//			m_trades.readCashSettlement( "c:\\fin\\2009\\2009 cash settlement.csv");
//			m_trades.readCashSettlement( "c:\\fin\\2010\\2010 cash settlement.csv");
			continueProcessing( "c:\\sync\\boxer\\report.t");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}

	private void onReadCoder() {
		try {
			m_trades.readTrades( "c:\\sync\\coder\\2011.csv");
			m_trades.readTrades( "c:\\sync\\coder\\2012.csv");
			continueProcessing( "c:\\sync\\coder\\report.t");
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	
	void continueProcessing(String filename) {
		// VXX had a conid change; map them both to the same Scale
		// STILL ONE PROBLEM: we split 10 shares and then ignore .5 trade; make it 12 shares in the file and calculate correct price
		// YOU MAY ALSO HAVE AN ISSUE WITH QQQ VS QQQQ; CHECK IF THERE WAS A CONID CHANGE
		// CHANGE SYMBOL FILE TO HAVE SAME COLUMNS AS DAILY PNL: Mkt Val	Pnl	Real	Pnl dif	Real dif
		// close is definitely wrong; pnl goes from 46->178 in one day

		//VXX AND FOR ALL I KNOW ALL OTHER SPLITS STOP UPDATING AFTER THE SPLIT;
		//THAT IS, THE POSITIONS, IN BOTH THE SUMMARY AND THE SYMBOL FILE
		//MODIFY TRADES FILE TO ACCOUNT FOR .5 SHARE
		
		// sort by date/time
		m_trades.sort();
		
		// create underlyings; eventually move this into FilterPanel. ps
		HashSet<String> unders = new HashSet<String>();
		for( Trade trade : m_trades) {
			unders.add( trade.underSymbol() );
		}
		
		ArrayList<String> list = new ArrayList<String>( unders);
		Collections.sort( list);
		
		ReportFrame frame = new ReportFrame( this, filename);
		frame.refresh( list);
		S.err( "done");
	}
	
	private void deleteFiles() {
		S.clearDir( "c:\\fin\\unders");
		S.deleteFile( PNL_DAY);
		S.deleteFile( PNL_WEEK);
		S.deleteFile( PNL_MONTH);
		S.deleteFile( PNL_YEAR);
		S.deleteFile( SUMMARY);
		S.deleteFile( CLOSING);
		S.deleteFile( ALL);
		S.deleteFile( EXERCISE);
	}
	
	interface ISame {
		boolean isSame( JtsCalendar d1, JtsCalendar d2);
	}
	
	static void report( String filename, String... fields) {
		String str = S.concat( fields);
		S.out( filename, str); 
	}
	
	static String pnlClosing( String secType) {
		return "c:\\fin\\pnlClosing" + secType + ".csv";
	}
	
	public void addConid(String symbol, int conid, String secType, JtsCalendar dateTime) {
		m_conids.put( symbol, conid);
	}

	public String[] getAccounts() {
		return m_accounts.toArray( new String[0]);
	}
}


/* Notes
 * 
 * Losers
 * On 6/18/10, I bought BP calls by mistake instead of selling them. On 6/29, I corrected the mistake, sold those calls and then sold others.
 * FAS was a scale that was going great, then just headed south. You sold calls and made back 4k, but still ended with loss.
 * I have no fucking clue what FTR was all about.
 * No idea about JNK. You did some covered calls and then closed them out the next day.
 * LZ and RAI and RGC looks like mistakes. Bought the stock one day, sold it the next.
 * NOK you sold puts and then the price dropped $3; you have a long way to go to make it up selling calls
 * TLT is the long term US debt you are selling short 
 * OEX and XEO matches perfectly with 1256 form 
 */

// TODO: marks should be for real pnl only; unreal pnl should be reported only on time pnl pages

// TODO: loop at all closed positions and compare those that sold options with those that didn't
// confirm oex and xeo



// in the underlying report, show pnl for stock and all options at the end of each line, not pnl for contract traded
// get historical prices and add them in to calcs 
// create a secType enum
// INTC IS FUCKING UP THE PNL OF CLOSED GROUP ON 
// check all ps and ???

//exercise call: add amount to cost basis (spent)
//exercise put: deduct amount from sale price (spent)
//assigned on call: add amount to sale price
//assigned on put: deduct price from purchase price (cost casis)
//allow launching chart from selected or first trade row
//give a way to clear removed traded, and show that some are removed (checkbox)
//let reports link to charts 
//let start dates default to 00:00 and end dates to 23:59
//main screen for all shows big unreal pnl loss but detail screen doesn't show it?
//problem: real plus unreal from detail screen does not equal total from main screen; only doesn't work when there is starting unreal pnl before reporting
//bug: VXX total from details does not match pnl total from main screen
//check qqq vs qqqq, conids and underconids
