package scale.report;

import java.util.ArrayList;

public class Marks extends ArrayList<Mark> {
	private double m_maxMktVal;

	public double maxMktVal() {
		if( m_maxMktVal == 0) {
			calcMaxMktVal();
		}
		return m_maxMktVal;
	}

	private void calcMaxMktVal() {
		for( Mark mark : this) {
//			m_maxMktVal = Math.max( m_maxMktVal, mark.mktVal() );
		}
	}
}
