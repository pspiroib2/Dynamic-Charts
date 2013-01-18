package arb;


import java.io.FileNotFoundException;

import lib.OStream;
import lib.S;


import com.ib.client.Contract;
import com.ib.client.TickType;

public class Data implements Cloneable {
	Contract m_contract;
	private int m_id;
	private double m_bid = S.SMALL;
	private double m_ask = Double.MAX_VALUE;
	private double m_last = Double.MAX_VALUE;
	private double m_close = Double.MAX_VALUE;
	private int m_bidSize;
	private int m_askSize;
	private int m_lastSize;
	private boolean m_requested; // mkt data
	private double m_delta;
	private double m_model;
	private OStream m_log;
	
	// get
	Contract contract()				{ return m_contract; }
	public double ask() 			{ return m_ask; }
	public double bid()				{ return m_bid; }
	public double last()			{ return m_last != Double.MAX_VALUE ? m_last : m_close; }
	public int id()					{ return m_id; }
	public int conid()				{ return m_contract.m_conId; }
	public String symbol()			{ return m_contract.m_symbol; }
	public String exchange() 		{ return m_contract.m_exchange; }
	public boolean requested() 		{ return m_requested; }
	public double mark()			{ return Math.max( m_bid, Math.min( m_last, m_ask) ); }
	public double model() 			{ return m_model; }
	public double delta() 			{ return m_delta; }
	public double mult()			{ return 1; }

	// set
	public void requested(boolean v) { m_requested = v; }

	// helpers
	public boolean hasBid() 		{ return bid() > 0; }
	public boolean hasAsk() 		{ return ask() > 0; }
	String fmt( double v) 			{ return S.fmt2a( v); } 
	public double mid() 			{ return (m_bid + m_ask) / 2; }

	protected Data( Contract c, int id) {
		m_contract = c;
		m_id = id;
	}
	
	void log( String name) {
		try {
			m_log = new OStream( name, true);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
		
	/** Overridden in subclass. */
	public void updateFrom(int field, double price) {
		if( price == -1) {
			price = -1;
		}
		
		switch( field) {
			case TickType.BID:
				m_bid = price == -1 ? 0 : price; // -1 means no
				break;
			case TickType.ASK:
				m_ask = price == -1 ? Double.MAX_VALUE : price; // -1 means no price;
				break;
			case TickType.LAST:
				m_last = price;
				if( m_log != null) {
					m_log.log( S.fmt2a( m_last) );
				}
				break;
			case TickType.CLOSE:
				m_close = price;
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
	
	public void updateOptionFrom(int field, double impliedVol, double delta, double modelPrice, double pvDividend) {
		m_delta = delta;
		m_model = modelPrice;
	}
	
	static class StockData extends Data {
		private Under m_under;

		public StockData(Contract c, int id, Under under) {
			super(c, id);
			m_under = under;
		}
		
		public void updateFrom(int field, double price) {
			super.updateFrom( field, price);
			m_under.onStockUpdated();
		}
	}
	
	static class FutureData extends Data {
		public double mult() { return 100; }

		private int m_position;
		int position() 					{ return m_position; }
		public void position(int v) 	{ m_position = v; }

		public FutureData(Contract c, int id) {
			super(c, id);
		}
	}
	
	static class OptionData extends Data {
		private Strike m_strike;
		private int m_position;
		int position() 					{ return m_position; }
		public void position(int v) 	{ m_position = v; }

		public double mult() { return 100; }

		public OptionData( Strike pair, Contract c, int id) {
			super( c, id);
			m_strike = pair;			
		}
		
		/** Return how much you will pay to liquidate the position. */
		public double liq() {
			return m_position > 0
				? m_position * bid() * mult()
				: m_position * ask() * mult();
		}
		
		@Override public OptionData clone() {
			return (OptionData)super.clone();
		}
		
		@Override public void updateFrom(int field, double price) {
			super.updateFrom(field, price);
			m_strike.onOptionTicked();
		}
	}
	
	static class EfpData extends Data {
		public EfpData( Contract c, int id) {
			super( c, id);
		}
		public void updateFrom(int field, double price) {
			super.updateFrom( field, price);
		}
	}
	
	static class IndexData extends Data {
		private Under m_under;

		public IndexData( Contract c, int id, Under under) {
			super( c, id);
			m_under = under;
		}
		public double bid() { return last(); }
		public double ask() { return last(); }
		public double mid() { return last(); }
		
		public void updateFrom(int field, double price) {
			super.updateFrom( field, price);
			
			if( field == TickType.LAST) {
				m_under.onStockUpdated();
			}
		}
	}
	
	@Override public Data clone() {
		try {
			return (Data)super.clone();
		}
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}

