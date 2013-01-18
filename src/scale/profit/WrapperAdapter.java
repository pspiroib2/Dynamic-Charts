package scale.profit;

import java.io.FileNotFoundException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import lib.OStream;
import lib.S;


import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EWrapper;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;


public class WrapperAdapter implements EWrapper {

	private int m_reqId;
	
	public int nextId() { return m_reqId++; }

	public void bondContractDetails(int reqId, ContractDetails contractDetails) {
		
	}

	public void contractDetails(int reqId, ContractDetails contractDetails) {
		
	}

	public void contractDetailsEnd(int reqId) {
		
	}

	public void currentTime(long time) {
		
	}

	public void onTrade(int orderId, Contract contract, Execution execution) {
		
	}

	public void fundamentalData(int reqId, String data) {
		
	}

	public void onHistoricalBar(int reqId, Bar bar) {
		
	}

	public void managedAccounts(String accountsList) {
		
	}

	public void onConnected(int reqId) {
		out( "Received id " + reqId);
		m_reqId = reqId;
	}

	public void onOpenOrder(int orderId, Contract contract, Order order,
			OrderState orderState) {
		
	}

	public void onOrderStatus(int orderId, String status, int filled,
			int remaining, double avgFillPrice, int permId, int parentId,
			double lastFillPrice, int clientId, String whyHeld) {
		
	}

	public void onRealtimeBar(int reqId, Bar bar) {
		
	}

	public void receiveFA(int faDataType, String xml) {
		
	}

	public void scannerData(int reqId, int rank,
			ContractDetails contractDetails, String distance, String benchmark,
			String projection, String legsStr) {
		
	}

	public void scannerDataEnd(int reqId) {
		
	}

	public void scannerParameters(String xml) {
		
	}

	public void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry) {
		
	}

	public void tickGeneric(int tickerId, int tickType, double value) {
		
	}

	public void tickOptionComputation(int tickerId, int field,
			double impliedVol, double delta, double modelPrice,
			double pvDividend) {
		
	}

	public void tickPrice(int tickerId, int field, double price,
			int canAutoExecute) {
		
	}

	public void tickSize(int tickerId, int field, int size) {
		
	}

	public void tickString(int tickerId, int tickType, String value) {
		
	}

	public void updateAccountTime(String timeStamp) {
		
	}

	public void updateAccountValue(String key, String value, String currency,
			String accountName) {
		
	}

	public void updateMktDepth(int tickerId, int position, int operation,
			int side, double price, int size) {
		
	}

	public void updateMktDepthL2(int tickerId, int position,
			String marketMaker, int operation, int side, double price, int size) {
		
	}

	public void updateNewsBulletin(int msgId, int msgType, String message,
			String origExchange) {
		
	}

	public void onPositionUpdate(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		
	}

	public void onFinishedHistoricalData( int reqId) {
	}
	
	public void connectionClosed() {
		out( "Connection closed");
	}

	public void error(Exception e) {
		out( e.toString() );
	}

	public void error(String str) {
		out( str);
	}

	public void error(int id, int errorCode, String errorMsg) {
		// ignore farm status  202=order canceled
		if( errorCode == 2104 || errorCode == 2106 || errorCode == 202) {
			return;
		}
		
		out( "" + id + " " + errorCode + " " + errorMsg);
	}
	
	static OStream m_os;
	
	public static void err( String str) {
		out( "Error: " + str);
	}
	
	public static void out( String str) {
		S.err( str);
		log( str);
	}

	public static void log( String str) {
		if( m_os == null) {
			try {
				m_os = new OStream( "c:\\options\\log." + S.TODAY + ".t", true);
				m_os.writeln( "--------------------------------");
			} 
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		m_os.log( str);
	}
	
	public static void msg( final JFrame frame, final String msg) {
		new Thread() {
			public void run() {
				JOptionPane.showMessageDialog( frame, msg);
			}
		}.start();
	}
}
