package scale.ma;

import java.util.ArrayList;
import java.util.HashMap;

import lib.S;

import scale.close.MainBase;
import scale.profit.Bar;

import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;

// if it wants to buy, it should cancel the sell order
// use scanner to trade stocks that are up a lot for the day at 9:45
//you need high volume so you don't pay a big spread

/** Write the pnl for all trades to files. */
public class Main extends MainBase {
	// constants
	private static final int LONG = 39;
	private static final int SHORT = 2;
	
	private static final int SECONDS_PER_BAR = 5;
	private static final int BARS_PER_MIN = 60 / SECONDS_PER_BAR;
	public static final int LONG_MA = LONG * BARS_PER_MIN;
	public static final int SHORT_MA = SHORT * BARS_PER_MIN;
	public static final int ORDER_SIZE = 100;
	public static final double TOLERANCE = .005;
	private static final String[] STOCKS = { "AA", "AXP", "BA", "BAC", "C", "CAT", "CVX", "DD", "DIS", "GE", "GM", "HD", "HPQ", "IBM", "INTC", "JNJ", "JPM", "KFT", "KO", "MCD", "MMM", "MRK", "MSFT", "PFE", "PG", "T", "UTX", "VZ", "WMT", "XOM" };
//	private static final String[] STOCKS = { "BAC", "CAT", "GM", "JPM", "KFT", "PFE", "PG", "T", "VZ", };
//	private static final String[] STOCKS = { "AA", };
	
	// currently using close
	
	// static
	public static final Main INSTANCE = new Main();
	
	public int nextId() { return m_id++; }

	// member vars
	HashMap<Integer, Order> m_mapOrderIdToOrder = new HashMap<Integer, Order>();
	
	public static void main(String[] args) {
		INSTANCE.connect( 8);
	}

	public void onConnected(int nextId) {
		m_id = nextId;
		
		int NUM_SECS = (LONG + 1) * 60; // request 1 extra minute of historical data

		for (String symbol : STOCKS) {
			ScaleStock stock = getOrCreateStock(symbol);
			out( "requesting historical bars for " + stock.symbol() );
			String endTime = S.TODAY + " " + S.now() + " EST";
			m_socket.reqHistoricalData(stock.tickerId(), stock.createContract(), endTime, NUM_SECS + " S", "5 secs", "TRADES", 1, 1);

			out( "requesting market data for " + stock.symbol() );
			m_socket.reqMktData(stock);
		}
		
		m_socket.reqAccountUpdates( true, null);
	}
	
	public void onHistoricalBar(int reqId, Bar bar) {
		ScaleStock stock = m_mapTickIdToStock.get(reqId);
		stock.addHistoricalBar( bar);
	}
	
	public void onFinishedHistoricalData(int reqId) {
		ScaleStock stock = m_mapTickIdToStock.get(reqId);
		out( "processing historical bars for " + stock.symbol() );
		stock.processHistoricalBars();

		out( "requesting real-time bars for " + stock.symbol() );
		m_socket.reqRealTimeBars(stock.tickerId(), stock.createContract(), 5, "TRADES", false);
	}
	
	public void onPositionUpdate(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
		ScaleStock stock = m_mapSymbolToStock.get( contract.m_symbol);
		if( stock != null) {
			out( "Received position update  " + contract.m_symbol + " " + position);
			stock.position( position);
		}
		m_socket.reqAccountUpdates( false, null); // move to accountEnd() when released
	}

	public void onRealtimeBar(int reqId, Bar bar) {
		ScaleStock stock = m_mapTickIdToStock.get(reqId);
		stock.processRealtimeBar( bar);
	}
	
	/** Return orders for specified symbol. */
	private Order[] getOrders(String symbol) {
		ArrayList<Order> list = new ArrayList<Order>();
		for (Order order : m_mapOrderIdToOrder.values()) {
			if (order.contract().m_symbol.equals(symbol)) {
				list.add(order);
			}
		}
		return list.toArray(new Order[list.size()]);
	}

	/** Place order. */
	public void placeOrder(Contract contract, Order order) {
		m_socket.placeOrder(contract, order);
		m_mapOrderIdToOrder.put(order.m_orderId, order);
	}

	/** Order was filled or canceled? */
	public void onOrderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
		if( status.equals( "Filled") || status.equals( "Cancelled") ) {
			Order order = m_mapOrderIdToOrder.remove( orderId);
			if( order != null) {
				ScaleStock stock = m_mapSymbolToStock.get( order.contract().m_symbol);
				if( stock != null) {
					stock.onFilledOrCanceled( order, status);
				}
			}
		}
	}
	
	public void onTrade(int orderId, Contract contract, Execution execution) {
		ScaleStock stock = m_mapSymbolToStock.get( contract.m_symbol);
		if( stock != null) {
			stock.onExecution( execution);
		}
	}
}

/*
do:log all events including position events, acks, fills; let a separate object do this.
let orders be parent/child
*/
