package scale.mean;

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
public class DailyMean extends WrapperAdapter {
	private static final char C = ',';
	protected EClientSocket m_socket;
	private ArrayList<Bar> m_bars = new ArrayList();

	public static void main(String[] args) {
		new DailyMean().run();
	}

	private void run() {
		m_socket = new EClientSocket( this);
		m_socket.eConnect( "localhost", 7496, 0);
	}
	
	@Override
	public void onConnected(int orderId) {
		out( "connected");
		final Contract contract = new Contract();
		contract.m_symbol = "QQQQ";
		contract.m_secType = "STK";
		contract.m_exchange = "SMART";
				
		
		String endTime = "20090720 16:00:00 EST";
		m_socket.reqHistoricalData( 1, contract, endTime, "28800 S", "1 min", "TRADES", 1, 1);

		new Thread( new Runnable() {
			public void run() {
				try {Thread.sleep( 2000);} catch (InterruptedException e) {	e.printStackTrace();}
				String endTime = "20090721 16:00:00 EST";
				m_socket.reqHistoricalData( 1, contract, endTime, "28800 S", "1 min", "TRADES", 1, 1);
			}
		}).start();

		new Thread( new Runnable() {
			public void run() {
				try {Thread.sleep( 2000);} catch (InterruptedException e) {	e.printStackTrace();}
				String endTime = "20090722 16:00:00 EST";
				m_socket.reqHistoricalData( 1, contract, endTime, "28800 S", "1 min", "TRADES", 1, 1);
			}
		}).start();
	}
	
	@Override
	public void onHistoricalBar(int reqId, Bar bar) {
		m_bars.add( bar);
	}
	
	int MIN = 1;

	int i = 0;
	
	@Override
	public void onFinishedHistoricalData(int reqId) {
		out( "received all bars");
		
		if( ++i == 3) {
			processBars( 5 * MIN);
			processBars( 10 * MIN);
			processBars( 30 * MIN);
			processBars( 60 * MIN);
			processBars( 120 * MIN);
			processBars( 180 * MIN);
			out( "done");
			System.exit( 0);
		}
	}

	private void processBars( int numBars) {
		S.deleteFile( filename( numBars) );
		
		LinkedList<Bar> m_ma = new LinkedList<Bar>();
		
		int uu=0, ud=0, du=0, dd=0;
		
		for( int i = 0; i < m_bars.size(); i++) {
			Bar bar = m_bars.get( i);
			m_ma.add( bar);
			if( m_ma.size() > numBars) {
				m_ma.removeFirst();
			}
			if( m_ma.size() == numBars && i + 1 < m_bars.size() ) {
				double close = bar.m_close;
				
				double ma = avg( m_ma);
				double dif1 = close - ma;
				double pct1 = dif1 / ma;
				
				double forward = m_bars.get( i + 1).m_close;
				double dif2 = forward - close;
				double pct2 = dif2 / close;
				
				S.out( filename( numBars), "" + pct1 + C + pct2);
				
				if( dif1 > 0) {
					if( dif2 > 0) {
						uu++;
					}
					else {
						ud++;
					}
				}
				else {
					if( dif2 > 0) {
						du++;
					}
					else {
						dd++;
					}
				}					
			}
		}
		S.out( filename( numBars), "" + uu + C + ud + C + du + C + dd);
	}

	private String filename(int numBars) {
		return "c:\\fin\\ma_" + numBars + ".csv";
	}

	private double avg( LinkedList<Bar> bars) {
		double total = 0;
		
		for( Bar bar : bars) {
			total += bar.m_close;
		}
		
		return total / bars.size();
	}
	
	
}
