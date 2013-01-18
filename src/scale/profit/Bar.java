package scale.profit;

import java.util.StringTokenizer;

import lib.S;


public class Bar implements Comparable<Bar> {
	private long m_longTime; // in ms
	public double m_high;
	public double m_low;
	private double m_open;
	public double m_close;
	public long m_volume;
	public double m_wap;
	public String m_saveDate; // for toString only;
	public String m_saveTime; // for toString only? could be removed. ???
	
	public String timeStr() { return S.timeAsStr( m_longTime); }
	public double close() { return m_close; }
	public double open() { return m_open; }
	public long longTime() { return m_longTime; }
	
	public void time( String date, String time) { m_longTime = S.getLongTime( date + " " + time); }

	private Bar() {
		
	}
	
	public Bar(String date, String time, double high, double low, double open, double close, long volume, double wap, int count) {
		// if time is null, assume end-of-day
		if( time == null) {
			time = "16:00:00";
		}
	
		m_saveDate = date;
		m_saveTime = time;
		
		m_longTime = S.getLongTime( date + " " + time);
		m_high = high;
		m_low = low;
		m_open = open;
		m_close = close;
		m_volume = volume;
		m_wap = wap;
		//m_count = count;
	}
	
	public Bar(long timeInMs, double high, double low, double open, double close, long volume, double wap, int count) {
		m_longTime = timeInMs;
		m_high = high;
		m_low = low;
		m_open = open;
		m_close = close;
		m_volume = volume;
		m_wap = wap;
		//m_count = count;
	}

	public String toString() {
		return m_saveDate + "\t" + m_close;
	}

	public int compareTo(Bar o) {
		if( m_longTime < o.m_longTime) {
			return -1;
		}
		if( m_longTime > o.m_longTime) {
			return 1;
		}
		return 0;
	}

	public String timeAsStr() {
		return S.timeAsStr( m_longTime);
	}

	public String getWriteString() {
		char C = ',';
		
		StringBuffer buf = new StringBuffer();
		buf.append( m_longTime);	
		buf.append( C);
		buf.append( m_high);	
		buf.append( C);
		buf.append( m_low);	
		buf.append( C);
		buf.append( m_open);	
		buf.append( C);
		buf.append( m_close);	
		buf.append( C);
		buf.append( m_volume);	
		buf.append( C);
		buf.append( m_wap);	
		buf.append( C);
		buf.append( m_saveDate);	
		buf.append( C);
		buf.append( m_saveTime);
		
		return buf.toString();
	}

	public static Bar createFromWriteString(String line) {
		StringTokenizer st = new StringTokenizer( line, ",");
		
		Bar bar = new Bar();
		bar.m_longTime = Long.parseLong( st.nextToken() );
		bar.m_high = Double.parseDouble( st.nextToken() );
		bar.m_low = Double.parseDouble( st.nextToken() );
		bar.m_open = Double.parseDouble( st.nextToken() );
		bar.m_close = Double.parseDouble( st.nextToken() );
		bar.m_volume = Long.parseLong( st.nextToken() );
		bar.m_wap = Double.parseDouble( st.nextToken() );
		bar.m_saveDate = st.nextToken();
		bar.m_saveTime = st.nextToken();
		
		return bar;
	}
}
