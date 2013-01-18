package scale.report;

import lib.JtsCalendar;
import lib.OStream;

public class PnlCollector {
	static final String C = ",";

	private final Marks m_marks = new Marks();
	private final Pnls m_cumPnls = new Pnls();
	int m_i;

	/** pnls is the incremental pnl; we add this to the cum to get the
	 *  new cum and then add a mark with the new cum. */
	public void mark(Trade trade, Pnls pnls) {
		m_cumPnls.increment( pnls);
		m_marks.add( new Mark( trade.dateTime(), m_cumPnls) );
	}

	public void reset() {
		m_i = 0;
	}

	public void displayPnl( OStream os, JtsCalendar dateTime) {
		// if there are no marks or first mark is past dateTime, make null entry
		if( m_marks.size() == 0 || m_marks.get( 0).dateTime().getTimeInMillis() > dateTime.getTimeInMillis() ) {
			writePnlGroupEntry( os, Pnls.NULL, 0, 0); 
			return;
		}

		// loop until we find last mark in this time period
		for( int i = m_i; i < m_marks.size(); i++) {
			Mark mark = m_marks.get( i);
			Mark next = i < m_marks.size() - 1 ? m_marks.get( i + 1) : null;
			if( next == null || next.dateTime().getTimeInMillis() > dateTime.getTimeInMillis() ) {
				Mark last = m_marks.get( m_i);
				double pnlDif = mark.m_pnl - last.m_pnl;
				double realDif = mark.m_realPnl - last.m_realPnl;
				writePnlGroupEntry( os, mark, pnlDif, realDif);
				m_i = i;
				break;
			}
		}
	}
	
	private static void writePnlGroupEntry( OStream os, Pnls pnls, double pnlDif, double realDif) {
		os.report( pnls.getReportStr(), pnlDif, realDif, "");
	}

	public static void writeHeader1(OStream os, String name) {
		os.write( getPnlGroupHeader1( name) );
	}

	public static void writeHeader2(OStream os) {
		os.write( getPnlGroupHeader2() );
	}
	
	private static String getPnlGroupHeader1( String name) {
		return name + C + ",,,,,";
	}
	
	private static String getPnlGroupHeader2() {
		return Pnls.getReportHeader() + C + "Pnl dif" + C + "Real dif" + C + C;
	}

}
