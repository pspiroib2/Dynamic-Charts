/**
 * 
 */
package scale.report;

import lib.JtsCalendar;

class Daily {
	private JtsCalendar m_dateTime;
	private double m_totalReal;
	private double m_totalPnl;
	
	public JtsCalendar dateTime() 	{ return m_dateTime; }
	double totalReal() 				{ return m_totalReal; }
	double totalPnl() 				{ return m_totalPnl; }
	
	Daily( JtsCalendar dateTime, double totalReal, double totalPnl) {
		m_dateTime = dateTime;
		m_totalReal = totalReal;
		m_totalPnl = totalPnl;
	}
}
