package scale.close;

import java.util.HashMap;

import lib.S;

import scale.ma.ScaleStock;
import scale.profit.WrapperAdapter;

import com.ib.client.EClientSocket;

/** Base class for other programs. */
public class MainBase extends WrapperAdapter {
	protected EClientSocket m_socket;
	protected int m_id;
	protected HashMap<Integer,ScaleStock> m_mapTickIdToStock = new HashMap<Integer,ScaleStock>();
	protected HashMap<String,ScaleStock> m_mapSymbolToStock = new HashMap<String,ScaleStock>();

	public EClientSocket socket() { return m_socket; }

	public void connect(int clientId) {
		m_socket = new EClientSocket( this);
		m_socket.eConnect( "localhost", 7496, clientId);
	}
	
	public void connectionClosed() {
		out( "Connection was closed");
	}
	
	public void tickGeneric(int tickerId, int tickType, double value) {
	}
	
	public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		ScaleStock stock = m_mapTickIdToStock.get( tickerId);
		stock.updateFrom( field, price);
		onTicked( stock);
	}
	
	protected void onTicked(ScaleStock stock) {}
	
	protected ScaleStock getOrCreateStock( String symbol) {
		ScaleStock stock = m_mapSymbolToStock.get( symbol);
		if( stock == null) {
			stock = new ScaleStock( symbol, m_id++);
			m_mapSymbolToStock.put( symbol, stock);
			m_mapTickIdToStock.put( stock.tickerId(), stock);
			onCreated( stock);
		}
		return stock;
	}
	

	protected void onCreated(ScaleStock stock) {}

	public void tickSize(int tickerId, int field, int size) {
		ScaleStock stock = m_mapTickIdToStock.get( tickerId);
		stock.updateSizeFrom( field, size);
	}
}
