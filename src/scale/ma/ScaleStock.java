package scale.ma;

import static lib.S.fmt4;
import static scale.ma.Main.ORDER_SIZE;
import static scale.ma.Main.TOLERANCE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import lib.S;

import scale.profit.Bar;

import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.TickType;



public class ScaleStock {
	private static final String COM = ",";
	
	private int m_tickerId;
	private String m_symbol;
	private int m_position;
	private TreeSet<Bar> m_bars = new TreeSet<Bar>();
	private MovAvg m_long = new MovAvg();
	private MovAvg m_short = new MovAvg();
	private boolean m_active; // set to true after historical data is processed
	private HashSet<Order> m_orders = new HashSet<Order>();
	private List<Bar> m_historicalBars = new ArrayList<Bar>();
	
	// prices
	private double m_bid;
	private double m_ask;
	private double m_last;
	private int m_bidSize;
	private int m_askSize;
	private int m_lastSize;
	private double m_high;
	private double m_low = Double.MAX_VALUE;
	
	
	// get
	public String symbol() 			{ return m_symbol; }
	public double ask() 			{ return m_ask; }
	public double bid()				{ return m_bid; }
	public double last()			{ return m_last; }
	public int tickerId()			{ return m_tickerId; }
	public int position()			{ return m_position; }

	// set
	public void ask( double v) 		{ m_ask = v; }
	public void bid( double v)		{ m_bid = v; }
	public void incPos( int v) 		{ m_position += v; }
	public void decPos( int v) 		{ m_position -= v; }
	public void position( int v)	{ m_position = v; }

	// helpers
	void out( String str) 			{ S.out( file(), S.now() + " " + str); }
	String file() 					{ return "c:\\files\\" + m_symbol + ".t"; }
	String file2() 					{ return "c:\\files\\" + m_symbol + ".csv"; }
	
	public ScaleStock(String symbol, int id) {
		m_symbol = symbol;
		m_tickerId = id;

		S.deleteFile( file() );
		S.deleteFile( file2() );
		S.out( file2(), "Time,LongAvg,ShortAvg,WAP,Bid,Ask");
	}
	
	public Contract createContract() {
		Contract c = new Contract();
		c.m_symbol = m_symbol;
		c.m_secType = "STK";
		c.m_exchange = "SMART";
		c.m_primaryExch = "ISLAND";
		return c;
	}
	
	public void updateFrom(int field, double price) {
		switch( field) {
			case TickType.BID:
				m_bid = price; 
				break;
			case TickType.ASK:
				m_ask = price;
				break;
			case TickType.LAST:
				m_last = price;
				break;
		}
	}
	
	public void updateSizeFrom(int field, int size) {
		switch( field) {
			case TickType.BID_SIZE:
				m_bidSize = size;
				break;
			case TickType.ASK_SIZE:
				m_askSize = size;
				break;
			case TickType.LAST_SIZE:
				m_lastSize = size;
				break;
		}
	}
	
	public int numBars() {
		return m_bars.size();
	}
	
	public String prices() {
		return m_bid + " " + m_ask + " " + m_last; 
	}
	
	public void addHistoricalBar(Bar bar) {
		m_historicalBars.add( bar);
	}
	
	public void processHistoricalBars() {
		for( Bar bar : m_historicalBars) {
			processRealtimeBar( bar);
		}
		
		m_active = true;
	}
	
	public void processRealtimeBar(Bar bar) {
		try {
			out( "processing bar " + bar.toString() );
			m_bars.add( bar);
			
			double longMa = calcMA( Main.LONG_MA);
			m_long.add( longMa);
			
			double shortMa = calcMA( Main.SHORT_MA);
			m_short.add( shortMa);
			
			if( bar.m_high > m_high) {
				m_high = bar.m_high;
			}
			
			if( bar.m_low < m_low) {
				m_low = bar.m_low;
			}
			
			out( "long=" + fmt4(longMa) + "  short=" + fmt4(shortMa) + "  wap=" + fmt4(bar.m_wap) + "  bid=" + m_bid + "  ask=" + m_ask);
			S.out( file2(), bar.timeAsStr() + "," + longMa + "," + shortMa + "," + bar.m_wap + "," + m_bid + "," + m_ask);
			
			if( m_active) {
				checkMovAvg( longMa, shortMa, bar);
			}
		} 
		catch (BarsException e) {
			if( m_active) {
				Main.out( e.toString() );
			}
		}
	}

	public double calcMA(int bars) throws BarsException {
		if( m_bars.size() < bars) {
			throw new BarsException( "can't calculate MA " + bars + " for " + m_symbol);
		}
		
		Iterator<Bar> iter = m_bars.descendingIterator();
		
		double total = 0;
		
		for( int i = 0; i < bars; i++) {
			Bar bar = iter.next();
			total += bar.m_wap;
		}
		
		return total / bars;
	}
	
	private void checkMovAvg(double longMa, double shortMa, Bar bar) {
		if ( m_orders.size() > 0) {
			return;
		}
		
		double wap = bar.m_wap;
		
		if( m_short.isIncreasing() && bar.m_high == m_high && position() <= 0) {
			if( shortMa - longMa >= TOLERANCE || wap - longMa > longMa - shortMa) {						
				int size = ORDER_SIZE + Math.abs( position() );
				open( "BUY", size);
			}
		}
		else if( m_short.isDecreasing() && bar.m_low == m_low && position() >= 0) {
			if( longMa - shortMa >= TOLERANCE || longMa - wap > shortMa - longMa) {
				int size = ORDER_SIZE + position();
				open( "SELL", size);
			}
		}
	}
	
	/** Place opening order and queue up closing order. */ 
	private void open( String side, int size) {
		final Contract c = createContract();
		out( side + "ING " + size + " " + symbol() + "  position=" + position() );
		
		Order order = new Order(c, Main.INSTANCE.nextId() );
		order.m_side = side;
		order.m_size = size;
		order.m_orderType = "MKT";
		
		m_orders.add( order);
		
		Main.INSTANCE.placeOrder(c, order);
	}

	public void onExecution(Execution execution) {
		if( execution.m_side.equals( "BOT")) {
			incPos( execution.m_shares);
			out( "BOT " + m_symbol + " at " + execution.m_price + "  pos=" + m_position);
		}
		else {
			decPos( execution.m_shares);
			out( "SLD " + m_symbol + " at " + execution.m_price + "  pos=" + m_position);
		}
	}
	
	public void onFilledOrCanceled(Order order, String status) {
		out( m_symbol + " was " + status);
		m_orders.remove( order);
	}
}
