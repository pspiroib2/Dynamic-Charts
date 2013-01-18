package scale.close;

import java.util.HashSet;

import lib.S;


import com.ib.client.Contract;
import com.ib.client.Order;

/** API program to close all open positions. */
public class ClosePositions extends MainBase {
	static final ClosePositions INSTANCE = new ClosePositions();
	
	HashSet<Integer> m_set = new HashSet<Integer>();

	public static void main(String[] args) {
		INSTANCE.connect(1);
		S.sleep( 60*1000);
		System.exit( 0);
	}

	public void onConnected(int nextId) {
		m_id = nextId;

		out( "sending acct req");
		m_socket.reqAccountUpdates( true, null);
	}
	
	public void onPositionUpdate(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
		int conid = contract.m_conId;
		
		if( position != 0 && !m_set.contains( conid) ) {
			out( "received position " + contract.m_symbol + " " + position);
			close( contract, position);
			m_set.add( conid);
		}
	}

	public void close(Contract contract, int position) {
		contract.m_exchange = "SMART";
		contract.m_primaryExch = "ISLAND";
		
		Order order = new Order( contract, m_id++);
		order.m_side = position > 0 ? "SELL" : "BUY";
		order.m_size = Math.abs( position);
		order.m_orderType = "REL";
		order.m_lmtPrice = 0;
		order.m_auxPrice = .01;
		order.m_outsideRth = true;
		m_socket.placeOrder( contract, order);
	}
}
