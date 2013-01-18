package scale.ma;

import java.util.ArrayList;
import java.util.List;

public class MovAvg {
	static final int MAX = 3;
	
	private List<Double> m_vals = new ArrayList<Double>();

	public void add( double val) {
		m_vals.add( val);
		if( m_vals.size() > MAX) {
			m_vals.remove( 0);
		}
	}
	
	public boolean isIncreasing() {
		for( int i = 1; i < m_vals.size(); i++) {
			double val = m_vals.get( i);
			if( val <= m_vals.get( i - 1) ) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isDecreasing() {
		for( int i = 1; i < m_vals.size(); i++) {
			double val = m_vals.get( i);
			if( val >= m_vals.get( i - 1) ) {
				return false;
			}
		}
		return true;
	}	
}
