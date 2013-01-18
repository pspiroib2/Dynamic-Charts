package calcindexratio;

import java.util.ArrayList;
import java.util.TreeMap;

import lib.S;

import scale.profit.Bar;
import scale.profit.WrapperAdapter;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;

public class Main extends WrapperAdapter {
	protected EClientSocket m_socket = new EClientSocket( this);
	private TreeMap<Long,Snapshot> m_map = new TreeMap();
	private String m_queryLength;
	private String m_barSize = "30 mins";
	
	public static void main(String[] args) {
		out( "running");
		new Main().run();
		S.sleep( 60000);
	}
	
	private void run() {
		m_socket.eConnect( "127.0.0.1", 7496, 2);
	}

	String[] m_dates = {
//			"20100301",
//			"20100302",
//			"20100303",
//			"20100304",
//			"20100305",
//
//			"20100308",
//			"20100309",
//			"20100310",
//			"20100311",
//			"20100312",

			"20100331",
	};
	
	enum Part { ONE, TWO };
	
	int m_count = 0;
	Part m_part = Part.ONE;
	int m_reqId;
	
	@Override public void onConnected(int reqId) {
		super.onConnected(reqId);
		
		m_reqId = reqId;
		
		req();
	}
	
	private void req() {
		final Contract c = new Contract();
		
		String queryLength = "2 D";
		String barSize = "1 min";

//		String queryLength = "1 Y";
//		String barSize = "1 day";

		if( m_part == Part.ONE) {
			c.m_symbol = "OEF";
			c.m_secType = "STK";
			c.m_exchange = "SMART";
			c.m_primaryExch = "ARCA";
		}
		else {
			c.m_symbol = "XEO";
			c.m_secType = "IND";
			c.m_exchange = "CBOE";
		}
		
		S.sleep( 2000);
		String datetime = m_dates[m_count] + " 16:00:00";
		//out( "Requesting " + c.m_symbol + " " + datetime);
		m_socket.reqHistoricalData( m_reqId++, c, datetime, queryLength, barSize, "TRADES", 1, 1);
	}

	@Override public void onHistoricalBar(int reqId, Bar bar) {
		Snapshot a = m_map.get( bar.longTime());
		
		if( a == null) {
			a = new Snapshot( bar.longTime());
			m_map.put( bar.longTime(), a);
		}
		
		if( m_part == Part.ONE) {
			a.m_etf = bar.m_close;
		}
		else {
			a.m_index = bar.m_close;
		}
	}

	@Override public void onFinishedHistoricalData(int reqId) {
		if( m_part == Part.ONE) {
			m_part = Part.TWO;
			req();
		}
		else {
			showResults();
			
			if( m_count < m_dates.length - 1) {
				m_map.clear();
				m_part = Part.ONE;
				m_count++;
				req();
			}
			else {
				System.exit( 0);
			}
		}
	}

	private void showResults() {
		ArrayList<Double> list = new ArrayList();
		
		long longtime = 0;;
		
		for( Snapshot snapshot : m_map.values() ) {
			if( m_dates.length == 1) {
				S.err( snapshot.toString() );
			}
			else {
				list.add( snapshot.ratio() );
				longtime = snapshot.m_longtime;
			}
		}

		if( m_dates.length > 1) {
			double avg = S.average( list);
			S.err( S.excelDateAsStr( longtime) + "\t" + avg);
		}
	}

	static class Snapshot {
		long m_longtime;
		double m_etf;
		double m_index;
		
		Snapshot( long longtime) {
			m_longtime = longtime;
		}
		
		public double ratio() {
			return m_index / m_etf;
		}
		
		@Override
		public String toString() {
			return S.excelDateAsStr( m_longtime) + "\t" + ratio();
		}
	}
	
}
