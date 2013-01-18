/**
 * 
 */
package correlation;

import lib.OStream;

class PairData {
	static PairData NULL = new PairData();
	
	double m_corr;		// closing prices correlation
	double m_corr2;		// daily return correlation
	double m_beta;
	int m_size;			// # of closing prices (days)
	double m_avg; 		// avg change of price dif
	double m_vol; 		// 1d vol of price dif
	double m_vol5; 		// 5 day vol of price dif 
	
	PairData( double corr, double corr2, double beta, int size, double avgChange, double vol, double vol5) {
		m_corr = corr;
		m_corr2 = corr2;
		m_beta = beta;
		m_size = size;
		m_avg = avgChange;
		m_vol = vol;
		m_vol5 = vol5;
	}

	public PairData() {
	}

	public static void showHeader( OStream os) {
		os.report( "Cor1", "Cor2", "Avg Mv", "Vol 1D", "Vol 5D", "");
	}

	public void show(OStream os) {
		os.report( Main.fmt( m_corr), Main.fmt( m_corr2), Main.fmt( m_avg), Main.fmt( m_vol), Main.fmt( m_vol5), "" );
	}
}
