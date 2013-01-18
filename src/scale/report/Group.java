///**
// * 
// */
//package scale.report;
//
//import java.io.FileNotFoundException;
//import java.util.Set;
//
//import lib.JtsCalendar;
//import lib.OStream;
//
//
//class Group {
//	static final String C = ",";
//
//	private Set<Underlying> m_underlyings = new OrderdSet<Underlying>();
//	private String m_name;
//	private final PnlCollector m_collector = new PnlCollector();
//	private OStream m_os; // e.g. c:\fin\groups\Core_daily.csv
//	
//	String name() { return m_name; }
//
//	Group( String name) {
//		m_name = name;
//	}
//	
//	void add( Underlying underlying) {
//		m_underlyings.add( underlying);
//	}
//
//	public Pnls showSummary( String filename) {
//		Report.report( filename, m_name);
//		
//		Pnls total = new Pnls();
//
//		for( Underlying under : m_underlyings) {
//			Pnls pnls = under.showSummary( filename, true);
//			total.increment( pnls);
//		}
//		
//		Report.report( filename, "Total", "", total.getReportStr() );
//		Report.report( filename, "");
//		
//		return total;
//	}
//
//	/** Shows a single row for each pair. */
//	public Pnls showPairSummary( String filename) {
//		Pnls total = new Pnls();
//
//		for( Underlying under : m_underlyings) {
//			Pnls pnls = under.showSummary( filename, false);
//			total.increment( pnls);
//		}
//		
//		Report.report( filename, m_name, "", total.getReportStr() );
//		
//		return total;
//	}
//
//	public void mark(Trade trade, Pnls pnls) {
//		m_collector.mark( trade, pnls);
//	}
//
//	public void writeHeader1(OStream os, String time) {
//		PnlCollector.writeHeader1( os, m_name);
//		
//		os( time).write( ",,,");
//		for( Underlying under : m_underlyings) {
//			under.writeHeader1( os( time) );
//		}
//		os( time).writeln();
//	}	
//
//	public void writeHeader2(OStream os, String time) {
//		PnlCollector.writeHeader2( os);
//		
//		os( time).write( ",,");
//		for( Underlying under : m_underlyings) {
//			under.writeHeader2( os( time) );
//		}
//		os( time).writeln();
//	}
//
//	public void displayPnl( OStream os, String time, JtsCalendar dateTime) {
//		m_collector.displayPnl( os, dateTime);
//
//		// write row for underlying in c:\fin\groups\group_time.csv
//		os(time).write( dateTime.getYYYYMMDD() + C + C);
//		for( Underlying under : m_underlyings) {
//			under.displayPnl( os( time), dateTime);
//		}
//		os( time).writeln();
//	}
//	
//	private OStream os( String time) {
//		if( m_os == null) {
//			String filename = String.format( "c:\\fin\\groups\\%s_%s.csv", m_name, time);
//			try {
//				m_os = new OStream( filename, false);
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			} 
//		}
//		return m_os;
//	}
//
//	public void reset() {
//		m_collector.reset();
//		
//		for( Underlying under : m_underlyings) {
//			under.reset();
//		}
//		m_os.close();
//		m_os = null;
//	}
//}