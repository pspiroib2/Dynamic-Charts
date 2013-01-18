package correlation;

import java.util.HashMap;

import scale.profit.Bar;
import scale.profit.Bars;

import com.ib.client.Contract;

public class Proxy {
	private Main m_main;
	
	HashMap<Integer,Bars> m_map = new HashMap<Integer,Bars>();

	private void out(String string) { Main.out( string); }

	Proxy( Main main) {
		m_main = main;
	}

	public void req(int reqId, Contract c, String date) {
		String filename = "d:\\temp\\corr\\" + c.m_symbol + ".csv";
		Bars bars = Bars.read( filename, c.m_symbol);
		if( bars != null) {
			m_main.onFinished( reqId, bars, false);
		}
		else {
//			m_main.onFinished( reqId, new Bars(), false);
			out("requesting bars " + c.m_symbol);
			bars = new Bars( c.m_symbol);
			m_map.put( reqId, bars);
			m_main.socket().reqHistoricalData(reqId, c, date, "1 Y", "1 day", "TRADES", 0, 1);
		}
	}

	public void onHistoricalBar(int reqId, Bar bar) {
		Bars bars = m_map.get( reqId);
		bars.add( bar);
	}

	public void onFinishedHistoricalData(int reqId) {
		Bars bars = m_map.get( reqId);

		String filename = "d:\\temp\\corr\\" + bars.symbol() + ".csv";
		bars.write( filename);
		
		m_main.onFinished( reqId, bars, true);
	}

}
