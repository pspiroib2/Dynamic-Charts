package scale.overnight;


import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

import lib.S;

import scale.profit.Bar;
import scale.profit.WrapperAdapter;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;

/** This program compares the change in price going forward with
 *  the pct difference from the moving average. */
public class Main extends WrapperAdapter {
	private static final char C = ',';
	protected EClientSocket m_socket;
	private ArrayList<Bar> m_bars = new ArrayList();

	public static void main(String[] args) {
		new Main().run();
	}

	private void run() {
		m_socket = new EClientSocket( this);
		m_socket.eConnect( "localhost", 7496, 0);
	}
	
	@Override
	public void onConnected(int orderId) {
		out( "connected");
		final Contract contract = new Contract();
		contract.m_symbol = "OEF";
		contract.m_secType = "STK";
		contract.m_exchange = "ARCA";
						
		String endTime = "20100127 16:00:00 EST";
		m_socket.reqHistoricalData( 1, contract, endTime, "1 Y", "1 day", "TRADES", 0, 1);
	}
	
	@Override
	public void onHistoricalBar(int reqId, Bar bar) {
		m_bars.add( bar);
	}
	
	String FILE = "c:\\options\\overnight.csv";
	
	@Override
	public void onFinishedHistoricalData(int reqId) {
		out( "received all bars");

		S.deleteFile( FILE);
		
		double close = 0;
		
		for( Bar bar : m_bars) {
			if( close != 0) {
				double dif = bar.open() - close;
				double pct = dif / close;
				S.out( FILE, "" + dif + "," + pct);
			}
			close = bar.m_close;
		}
		
		System.exit( 0);
	}
}
