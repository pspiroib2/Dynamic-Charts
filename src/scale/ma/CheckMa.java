package scale.ma;

import lib.S;
import scale.close.MainBase;
import scale.profit.Bar;

public class CheckMa extends MainBase {
	private static final MainBase INSTANCE = new CheckMa();
	static final String SYMBOL = "IBM";
	
	public static void main(String[] args) {
		INSTANCE.connect( 4);
		out( "connected");
	}
	
	public void onConnected(int orderId) {
		m_id = orderId;
	
		out( "requesting historical");
		ScaleStock stock = getOrCreateStock(SYMBOL);
		//m_socket.reqHistoricalData( m_id++, stock.contract(), OStream.TODAY + "16:00:00", "1 Y", "1 day", "TRADES", 1, 1);
	}
	
	public void historicalData(int reqId, Bar bar) {
		out( "received historical");
		ScaleStock stock = m_mapTickIdToStock.get( reqId);
		//stock.addBar( bar);
	}
	
	public void onFinishedHistoricalData(int reqId) {
		ScaleStock stock = m_mapSymbolToStock.get( SYMBOL);
		//stock.process();
		
		out( "done");
		System.exit( 0);
	}
}
