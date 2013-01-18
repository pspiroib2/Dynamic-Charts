package scale.report;

import lib.JtsCalendar;


class Mark extends Pnls {
	public static Mark NULL = new Mark();
	
	private JtsCalendar m_dateTime;

	public JtsCalendar dateTime() 	{ return m_dateTime; }
	
	private Mark() {
	}
	
	public Mark(JtsCalendar dateTime, Pnls pnls) {
		super( pnls);
		m_dateTime = dateTime;
	}

//	public String getStr() {
//		return m_dateTime.getExcelDate() + Report.C + OStream.fmt2( m_mktVal) + Report.C + OStream.fmt2( m_pnl) + Report.C + OStream.fmt2( m_realPnl);
//	}
}
