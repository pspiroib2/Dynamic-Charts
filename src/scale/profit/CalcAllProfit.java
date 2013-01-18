package scale.profit;

import java.util.HashMap;

import lib.S;


import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.TickType;

// possibly feed in current price as starting point for first bar

/** Calculate ideal PT for all active scale orders. */
public class CalcAllProfit extends WrapperAdapter {
	private class Val {
		Contract m_contract;
		Order m_buy;
		Order m_sell;
		boolean m_req;
		double m_vol = -1;
		public boolean m_rep;
		
		public Val(Contract contract) {
			m_contract = contract;
		}

		public boolean valid() {
			return m_buy != null && m_sell != null && m_vol != -1 && 
				m_buy.m_filled != -1 && m_sell.m_filled != -1;
		}
	}
	
	HashMap<String,Val> m_mapSymbolToVal = new HashMap(); // order id
	HashMap<Integer,Order> m_mapPermIdToOrder = new HashMap(); // order id	
	HashMap<Integer,Val> m_mapTickerIdToVal = new HashMap(); // ticker id
	EClientSocket m_socket;
	int m_id = 1;

	public static void main(String[] args) {
		new CalcAllProfit().run();
	}

	private void run() {
		m_socket = new EClientSocket( this);
		m_socket.eConnect( "localhost", 7496, 0);
	}

	@Override
	public void onConnected(int orderId) {
		m_socket.reqAllOpenOrders();
	}

	@Override
	public void onOpenOrder(int orderId, Contract contract, Order order, OrderState orderState) {
		//out( "open " + order.m_permId);

		m_mapPermIdToOrder.put( order.m_permId, order);

		Val val = m_mapSymbolToVal.get( contract.m_symbol);
		if( val == null) {
			val = new Val( contract);
			m_mapSymbolToVal.put( contract.m_symbol, val);
		}

		if( order.m_side.equals( "BUY") ) {
			val.m_buy = order;
		}
		else {
			val.m_sell = order;
		}
		
		if( !val.m_req) {
			int id = m_id++;
			m_mapTickerIdToVal.put( id, val);
			m_socket.reqMktData( id, contract, "104", false);
			val.m_req = true;
		}
	}
	
	@Override
	public void onOrderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
	//	out( "status " + permId);
		Order order = m_mapPermIdToOrder.get( permId);
		order.m_filled = filled;
	}
	
	public void tickGeneric(int id, int tickType, double value) {
		if( tickType == TickType.OPTION_HISTORICAL_VOL) {
			//out( "vol " + id + " " + tickType + " " + value);
			Val val = m_mapTickerIdToVal.get( id);
			val.m_vol = value;
			check( val);
		}
	}

	private void check(Val val) {
		if( val.valid() ) {
			if( !val.m_rep) {
				String s = val.m_contract.m_symbol;
				
				int totalSize = val.m_buy.m_size - val.m_sell.m_filled;
				
				// must add in initialFilledPosition
				
				int compSize = val.m_buy.m_scaleInitLevelSize;
	
				double start = val.m_buy.m_lmtPrice;
				double inc = val.m_buy.m_scalePriceIncrement;
				double vol = val.m_vol / Math.sqrt(250);
				
				out( "" + s + " " + totalSize + " " + compSize + " " + start + " " + inc + " " + S.fmtPctb( vol) );
				val.m_rep = true;
				
				CalcSimProfit p = new CalcSimProfit();
				//p.calcForTopPrice( totalSize, start, inc, .005, 5, 30, vol);
			}
		}
		else {
			out( "invalid val");
		}
	}
}
