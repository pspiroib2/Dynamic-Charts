/**
 * 
 */
package scale.report;

import java.util.ArrayList;

import lib.S;


class Pair { // could be replaced with group
	private String m_sym1;
	private String m_sym2;
	private ArrayList<Scale> m_scales = new ArrayList<Scale>();
	
	public String filename() { return "c:\\fin\\sub\\pair_" + both() + ".csv"; }
	public String both() { return m_sym1 + "_" + m_sym2; }

	Pair( String sym1, String sym2) {
		m_sym1 = sym1;
		m_sym2 = sym2;

		S.out( filename(), Trade.getHeader() );
	}
	
	public void add( Scale scale) {
		m_scales.add( scale);
	}

	public void trade(Trade trade) {
		S.out( filename(), trade.getStr() );
	}
}
