package arb;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import lib.OStream;
import lib.S;

import arb.Data.OptionData;
import arb.Exp.Box;

import com.ib.client.Contract;
import com.ib.client.Order;

public class Strike implements Comparable<Strike>, Cloneable {
	private static final double SMALL = .00001;
	static long m_time;

	private Exp m_exp;
	private double m_strike;
	private OptionData m_put;
	private OptionData m_call;
	private double m_convBid = Double.MAX_VALUE; // bid and ask with strike removed, i.e. true cost
	private double m_convAsk = Double.MAX_VALUE; // if you stop storing these, you will have to clone Data in clone()
	private double m_convFutBid = Double.MAX_VALUE;
	private double m_convFutAsk = Double.MAX_VALUE;
	private double m_highConvBid = S.SMALL;
	private double m_lowConvAsk = Double.MAX_VALUE;
	private double m_revFutCost = Double.MAX_VALUE;
	private OStream m_log;

	// combos
	private Legs m_convLegs;  		// conversion legs (buy stock)
	private Legs m_convFutLegs;
	private Legs m_synthLegs;		// synthetic (buy call, sell put)
	private Box m_box;				// current best Box with this strike as the low strike
	private Box m_bestBox;			// all-time best box
	private Arb m_arb;

	Legs convLegs()				{ return m_convLegs; }
	double convBid() 			{ return m_convBid; }
	double convAsk() 			{ return m_convAsk; }
	double convFutBid() 		{ return m_convFutBid; }
	double convFutAsk() 		{ return m_convFutAsk; }
	double highConvBid()		{ return m_highConvBid; }
	double lowConvAsk() 		{ return m_lowConvAsk; }
	Data put() 					{ return m_put; }
	Data call() 				{ return m_call; }
	double strike()				{ return m_strike; }
	Exp exp() 					{ return m_exp; }
	Legs synthLegs()			{ return m_synthLegs; }
	public Box box()			{ return m_box; }
	public Box bestBox() 		{ return m_bestBox; }
	
	// helpers
	Data stock()				{ return m_exp.stock(); }
	double realSynthBid() 		{ return m_synthLegs != null ? m_synthLegs.bid() : -Double.MAX_VALUE; }
	double realSynthAsk() 		{ return m_synthLegs != null ? m_synthLegs.ask() : Double.MAX_VALUE; }
	double synthBid()			{ return m_synthLegs != null ? m_synthLegs.bid() + m_strike : -Double.MAX_VALUE; }
	double synthAsk()			{ return m_synthLegs != null ? m_synthLegs.ask() + m_strike : Double.MAX_VALUE; }
	double synthModel()			{ return m_synthLegs != null ? m_synthLegs.model() + m_strike : Double.MAX_VALUE; }
	double synthMid()			{ return (synthBid() + synthAsk() ) / 2; }
	String expiry() 			{ return either().m_contract.expiry(); }
	String expiry2() 			{ return either().m_contract.expiry().substring( 0, 6); }
	Data either() 				{ return m_put != null ? m_put : m_call; }
	String fmt( double v) 		{ return S.fmt2a( v); }
	public int callPos() 		{ return m_call != null ? m_call.position() : 0; }
	public int putPos() 		{ return m_put != null ? m_put.position() : 0; }
	public int synthPos() 		{ return callPos() > 0 && putPos() < 0 ? Math.min( callPos(), -putPos() ) : 0; }
	public int negSynthPos() 	{ return putPos() > 0 && callPos() < 0 ? Math.min( putPos(), -callPos() ) : 0; }
	String symbol()				{ return m_exp.under().symbol(); }
	public boolean done() 		{ return m_call != null && m_put != null; }
	public boolean isEq(double strike) 	{ return eq( m_strike, strike); }
	public boolean isLt( Strike strike) { return lt( m_strike, strike.strike() ); }
	public boolean isGt( Strike strike) { return gt( m_strike, strike.strike() ); }
	public double fairSynthPrice() 		{ return m_exp.underMarkPrice() - m_strike; }

	Strike( Arb arb, Exp exp, double strike) {
		m_arb = arb;
		m_exp = exp;
		m_strike = strike;
		
		if( symbol().equals( "XEO") && m_strike == 120.0) {
			try {
				m_log = new OStream( "c:\\options\\midpoint.ts." + S.TODAY + ".t", true);
			}
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void create() {
		if( m_put != null && m_call != null) {
			if( m_synthLegs == null) {
				m_synthLegs = new Legs( "Synth" + m_strike);
				m_synthLegs.add( true, 1, m_call);
				m_synthLegs.add( false, 1, m_put);
			}
			
			if( stock() != null && m_convLegs == null) {
				m_convLegs = new Legs( "Conv" + m_strike);
				m_convLegs.add( true, 100, stock());
				m_convLegs.add( false, 1, m_call);
				m_convLegs.add( true, 1, m_put);
			}

			if( m_exp.future() != null && m_convFutLegs == null) {
				m_convFutLegs = new Legs( "ConvFut" + m_strike);
				m_convFutLegs.add( true, 1, m_exp.future() );
				m_convFutLegs.add( false, 1, m_call);
				m_convFutLegs.add( true, 1, m_put);
			}
		}
	}

	public void calcValues() {
		if( m_put != null && m_call != null && m_exp.interest() != Double.MAX_VALUE) {
			// bid
			double bid = m_convLegs.bid();
			if( bid != Double.MAX_VALUE) {
				m_convBid = bid - strike();
				m_highConvBid = Math.max( m_convBid, m_highConvBid);
			}
			
			// ask
			double ask = m_convLegs.ask();
			if( ask != Double.MAX_VALUE) {
				m_convAsk = ask - strike();
				m_lowConvAsk = Math.min( m_convAsk, m_lowConvAsk);
			}
			
			// fut bid
			if( m_convFutLegs != null) {
				double convFutBid = m_convFutLegs.bid();
				if( convFutBid != Double.MAX_VALUE) {
					m_convFutBid = convFutBid - m_strike;
				}
				
				// fut ask
				double convFutAsk = m_convFutLegs.ask();
				if( convFutAsk != Double.MAX_VALUE) {
					m_convFutAsk = convFutAsk - m_strike;
				}
			}
		}
	}

	@Override
	public String toString() {
		return Exp.fmtSt( m_strike);
	}

	/** Called when Exp gets stock contract. */
	public void handleStock() {
		create();		
	}
	
	public OptionData handleOption( Contract contract) {
		if( contract.m_right.charAt( 0) == 'P') {
			if( m_put == null) {
				m_put = new OptionData( this, contract, m_arb.nextId() );
				m_arb.put( m_put);
				create();
			}
			return m_put;
		}

		if( m_call == null) {
			m_call = new OptionData( this, contract, m_arb.nextId() );
			m_arb.put( m_call);
			create();
		}
		return m_call;
	}

	public void reqMktData() {
		if( m_put != null) {
			m_arb.reqMktData( m_put);
		}
		if( m_call != null) {
			m_arb.reqMktData( m_call);
		}
	}
	
	/** Sort high to low. */
	public int compareTo(Strike o) {
		if( m_strike < o.m_strike) {
			return 1;
		}
		if( m_strike > o.m_strike) {
			return -1;
		}
		return 0;
	}
	
	public void clearAll() {
		m_highConvBid = S.SMALL;
		m_lowConvAsk = Double.MAX_VALUE;
	}
	
	@Override
	protected Strike clone() {
		try {
			Strike other = (Strike)super.clone();
			if( other.m_put != null) {
				other.m_put = m_put.clone();
			}
			if( other.m_call != null) {
				other.m_call = m_call.clone();
			}
			return other;
		} 
		catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/** For finding best reversal. */
	public boolean isHigherBidThan(Strike other) {
		if( m_convBid == Double.MAX_VALUE) {
			return false;
		}
		
		return other == null ||
			   gt( m_convBid, other.m_convBid) ||
		       eq( m_convBid, other.m_convBid) && m_strike < other.m_strike;
	}
	
	/** For finding best conversion. */
	public boolean isBetterConversionThan(Strike other) {
		if( m_convAsk == Double.MAX_VALUE) {
			return false;
		}
		
		return lt( m_convAsk, other.m_convAsk ) ||
		       eq( m_convAsk, other.m_convAsk) && gt( m_strike, other.m_strike);
	}
	
	static boolean ltEq(double v1, double v2) {
		return lt( v1, v2) || eq( v1, v2);
	}
	
	static boolean gtEq(double v1, double v2) {
		return gt( v1, v2) || eq( v1, v2);
	}
	
	static boolean gt(double v1, double v2) {
		return v1 - v2 > SMALL;
	}
	
	static boolean lt(double v1, double v2) {
		return v2 - v1 > SMALL;
	}
	
	static boolean eq(double v1, double v2) {
		return Math.abs( v1 - v2) <= SMALL;
	}
	
	/** Sell stock at strike, buy at mkt, sell call. */
	public double costToReplaceCall() {
		return m_call != null && S.isValid( m_call.bid() ) && stock() != null 
			? -m_strike + stock().ask() - m_call.bid()  
			: Double.MAX_VALUE;
	}
	
	/** Buy stock at strike, sell at mkt, sell put. */
	public double costToReplacePut() {
		return m_put != null && S.isValid( m_put.bid() ) && stock() != null 
			? m_strike - stock().bid() - m_put.bid() 
			: Double.MAX_VALUE;
	}
	
	/** Valid if put bid can cover interest and cost of call assignment is < 0. */
	public boolean isValidConversion() {
		return m_put != null && S.isValid( m_put.bid() ) && m_put.bid() > m_exp.interest() &&
			   costToReplaceCall() < 0; 
	}

	/** Valid if cost of put assignment is < 0. */
	public boolean isValidReversal() { //rename to isValidSynthetic
		// for stocks, we don't care if we get assigned since holding a long stock position costs us nothing
		if( exp().under().secType().equals( "STK") ) {
			return true;
		}
		
		double cost = costToReplacePut();		
		return S.isValid( cost) && cost < 0;
	}

	public void fillPositions(ArrayList<Strike> list) {
		if( putPos() != 0 || callPos() != 0) {
			list.add( this);
		}
	}
	
	/** Return put bid or zero. */
	public double putBid() { 
		return m_put != null && S.isValid( m_put.bid() ) ? m_put.bid() : 0; 
	}
	
	/** Return call bid or zero. */
	public double callBid() {
		return m_call != null && S.isValid( m_call.bid() ) ? m_call.bid() : 0; 
	}
	
	/** Return the max of selling the call or exercising it. */
	public double liqCall() {
		double v1 = m_call.bid();
		double v2 = -m_strike + stock().bid();
		return Math.max( v1, v2);
	}

	/** Return the max of selling the put or exercising it. */
	public double liqPut() {
		double v1 = m_put.bid();
		double v2 = m_strike - stock().ask();
		return Math.max( v1, v2);
	}
	
	public void subscribePos() {
		if( putPos() != 0) {
			m_arb.reqMktData( m_put);
		}
		if( callPos() != 0) {
			m_arb.reqMktData( m_call);
		}
	}
	
	double intrinsicPutValue() {
		return stock() != null ? Math.max( m_strike - stock().mid(), 0) : 0;
	}

	double intrinsicCallValue() {
		return stock() != null ? Math.max( stock().last() - m_strike, 0) : 0;
	}

	/** Return put midpoint minus intrinsic value. */
	public double putTimeValue() {
		return m_put != null ? m_put.bid() - intrinsicPutValue() : Double.MAX_VALUE;
	}
	
	/** Return call midpoint minus intrinsic value. */
	public double callTimeValue() {
		return m_call != null ? m_call.bid() - intrinsicCallValue() : Double.MAX_VALUE;
	}

	public double rawConvAsk() { 
		return m_convLegs != null ? m_convLegs.ask() : Double.MAX_VALUE; 
	}
	
	public boolean hasMismatch() { 
		return callPos() != -putPos(); 
	}
	
	public double payAtExpiration() {
		return callPos() * m_strike * 100;
	}
	
	/** Return liq cost above and beyond what would happen at expiration. */
	public double liqCost() {
		double val = Double.MAX_VALUE;
		
		if( m_put != null && m_call != null) {
			if( callPos() > 0 && S.isValid( m_put.ask() ) && S.isValid( m_call.bid() ) ) {
				val = m_put.ask() - m_call.bid() - (m_strike - stock().mark() );
			}
			else if( putPos() > 0 && S.isValid( m_call.ask() ) && S.isValid( m_put.bid() ) ) {
				val = m_call.ask() - m_put.bid() - (stock().mark() - m_strike);
			}
		}
			
		return val;
	}
	
	public void closePut() {
		Contract contract = createContract( "P");

		Order order = new Order( contract, m_arb.nextId() );
		order.m_size = putPos() + callPos();
		order.m_side = "SELL";
		order.m_orderType = "LMT";
		order.m_lmtPrice = m_put.bid();
		order.m_transmit = false;
		order.m_orderRef = "closeput" + Exp.m_orderRef++;

		Arb.out( "closing put " + order.m_orderId);
		m_arb.placeOrder(order);
	}
	
	public double getConversionBuyPrice(double legOffset, double strikeOffset) {
		double legPrice = convLegs().bid() + legOffset;
		double maxPrice = m_strike + strikeOffset;
		return Math.min( legPrice, maxPrice);
	}
	
	public double getConversionSellPrice(double legOffset, double strikeOffset) {
		double legPrice = convLegs().bid() - legOffset;
		double minPrice = m_strike + strikeOffset;
		return Math.max( legPrice, minPrice);
	}
	
	private Contract createContract( String right) {
		Contract contract = new Contract();
		contract.m_symbol = m_exp.symbol();
		contract.m_secType = "OPT";
		contract.expiry( m_exp.expiry() );
		contract.m_strike = m_strike;
		contract.m_right = right;
		contract.m_currency = "USD";
		contract.m_exchange = "SMART";
		return contract;
	}
	
	public void replaceCall() {
		Contract contract = createContract( "C");
		
		double price = round( stock().last() - m_strike - .07);
		price = Math.max( price, m_call.bid() + Arb.REPLACE_OFFSET);

		Order order = new Order( contract, m_arb.nextId() );
		order.m_size = putPos() - -callPos();
		order.m_side = "SELL";
		order.m_orderType = "LMT";
		order.m_lmtPrice = price;
		order.m_transmit = false;
		order.m_orderRef = "replacecall" + Exp.m_orderRef++;

		Arb.out( "replacing call" + order.m_orderId);
		m_arb.placeOrder(order);
	}
	
	public void replacePut() {
		Contract contract = createContract( "P");
		
		double price = round( m_strike - stock().last() - .07);
		price = Math.max( price, m_put.bid() + Arb.REPLACE_OFFSET);

		Order order = new Order( contract, m_arb.nextId() );
		order.m_size = callPos() - -putPos();
		order.m_side = "SELL";
		order.m_orderType = "LMT";
		order.m_lmtPrice = price;
		order.m_transmit = false;
		order.m_orderRef = "replaceput" + Exp.m_orderRef++;

		Arb.out( "replacing call" + order.m_orderId);
		m_arb.placeOrder(order);
	}
	
	/** Round down to nearest dime. */
	private double round(double d) {
		return (int)(d * 10) / 10.0;
	}
	
	public void mm( double offset) {
		// sell
		if( m_strike < stock().last() ) {
			double price = stock().last() + offset - m_strike;
			m_exp.placeOrder( "SELL", 2, m_synthLegs, price, "MMSynth");
		}
		
		// buy
		else {
			double price = stock().last() - offset - m_strike;
			m_exp.placeOrder( "BUY", 2, m_synthLegs, price, "MMSynth");
		}
	}
	
	/** Return how much you will pay to liquidate the position. */
	public double liq() {
		double amt = 0;
		
		// sell put
		if( putPos() != 0) {
			amt += m_put.liq();
		}
		if( callPos() != 0) {
			amt += m_call.liq();
		}
		
		return amt; 
	}
	
	public void onOptionTicked() {
		if( m_log != null) {
			m_log.log( S.fmt2a( synthMid() ) );
		}
	}
	
}
