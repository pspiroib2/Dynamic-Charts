/**
 * 
 */
package arb;

class Key {
	String m_expiry;
	double m_strike;
	
	public Key(String expiry, double strike) {
		m_expiry = expiry;
		m_strike = strike;
	}

	@Override
	public boolean equals(Object obj) {
		Key other = (Key)obj;
		return m_expiry.equals( other.m_expiry) && m_strike == other.m_strike;
	}
	
	@Override
	public int hashCode() {
		return (int)(m_expiry.hashCode() * m_strike);
	}
}