/**
 * 
 */
package scale.report;

public class Pnls implements Cloneable {
	static final Pnls NULL = new Pnls();
	
	double m_mktVal;
	double m_pnl;
	double m_realPnl;

	public Pnls() {
	}

	public Pnls( Pnls pnls) {
		this( pnls.m_mktVal, pnls.m_pnl, pnls.m_realPnl);
	}

	public Pnls(double mktVal, double pnl, double realPnl) {
		m_mktVal = mktVal;
		m_pnl = pnl;
		m_realPnl = realPnl;
	}
	
	public static String getReportHeader() {
		return "Mkt Val,Pnl,Real";
	}

	public String getReportStr() {
		return Report.fmt( m_mktVal) + Report.C + Report.fmt( m_pnl) + Report.C + Report.fmt( m_realPnl);
	}

	public void increment(Pnls pnls) {
		m_pnl += pnls.m_pnl;
		m_realPnl += pnls.m_realPnl;
		m_mktVal += pnls.m_mktVal;
	}

	public void add(double mktVal, double pnl, double realPnl) {
		m_mktVal += mktVal;
		m_pnl += pnl;
		m_realPnl += realPnl;
	}

	public void add(double settlementIncome) {
		m_pnl += settlementIncome;
		m_realPnl += settlementIncome;
	}
	
	@Override protected Pnls clone() {
		try {
			return (Pnls)super.clone();
		} 
		catch (CloneNotSupportedException e) {
			return null;
		}
	}
}