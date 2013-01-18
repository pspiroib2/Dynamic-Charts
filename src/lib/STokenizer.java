package lib;


enum State { QUOTED, NON };

public class STokenizer {
	static final char C = ',';
	
	String m_line;
	
	public STokenizer(String line) {
		m_line = line;
	}

	
	public String nextToken() {
		if( m_line.length() == 0) {
			return null;
		}
		
		State state = State.NON;

		StringBuffer buf = new StringBuffer();
		
		int i = 0;
		
		char c;
		
		
		while( i < m_line.length() ) {
			c = m_line.charAt( i++);
			
			if( c == '"') {
				state = state == State.QUOTED ? State.NON : State.QUOTED;
				continue;
			}

			if( state == State.NON && c == C) {
				break;
			}
			
			// ignore quoted commas
			if( c != C) {
				buf.append( c);
			}
		}
		
		m_line = m_line.substring( i);
		return buf.toString();
	}
	
	public static void main(String[] args) {
		STokenizer st = new STokenizer( "\"ab,cd\",efg,\"hij\"");
		while( st.hasMoreTokens() ) {
			String tok = st.nextToken();
			S.err( tok);
		}
	}

	private boolean hasMoreTokens() {
		return m_line.length() > 0;
	}
}
