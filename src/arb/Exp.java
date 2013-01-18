package arb;

//import static scale.lib.OStream.fmt2a;

import static arb.Strike.eq;
import static arb.Strike.gt;
import static arb.Strike.lt;
import static arb.Strike.ltEq;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import lib.OStream;
import lib.S;
import arb.Data.EfpData;
import arb.Data.FutureData;
import arb.Data.OptionData;

import com.ib.client.Contract;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;


class Exp implements Comparable<Exp> {
	static int m_orderRef = (int)System.currentTimeMillis() % 100;
	public static final Format FMT1 = new DecimalFormat( "#,##0.0");
	static String C = ",";

	private Under m_under;
	private String m_expiry;
	private ArrayList<Strike> m_strikes = new ArrayList();   // sorted high to low
	private HashMap<Integer,Order> m_orders = new HashMap();
	private long m_daysTilExpiry;
	private double m_interest = Double.MAX_VALUE;
	private int m_synthPos;  // reversals
	private int m_negSynthPos; // conversions
	private OStream m_log;
	
	// fake positions
	int m_fakeSynthPos;
	int m_fakeShortSynthPos;
	int m_fakeBoxPos;
	int m_fakeConversionPos;
	int m_fakeEFP;
	int m_fakeFuturePos;

	// EFP
	private FutureData m_future; // make never-null
	private EfpData m_efp;
	private double m_highEfpBid = S.SMALL;		// of all time
	private double m_lowEfpAsk = Double.MAX_VALUE;		// of all time
	
	// conversion
	private Strike m_highConvBid;
	private Strike m_lowConvAsk;
	private Strike m_curHighConvBid;
	private Strike m_curLowConvAsk;
	
	// synth
	private Strike m_highSynthBid;
	private Strike m_lowSynthAsk;

	// box
	private Box m_box, m_bestBox;		// no min tv of bid for options being sold (will be neg.) best mean it might not be available anymore 
	private Box m_box2, m_bestBox2;		// min tv of bid for options being sold is zero

	// for put/call strategies, not related to conv/rev
	Legs m_bestCallStrikeLegs;
	Legs m_bestPutStrikeLegs;
	
	// table models
	private TopModel m_bidAskModel = new TopModel();
	private StrikeModel m_strikeModel = new StrikeModel();
	private BoxModel m_boxModel = new BoxModel();
	private DeltaModel m_deltaModel = new DeltaModel();

	// tables
	private Table m_topTable = new Table( m_bidAskModel);
	private Table m_deltaTable = new Table( m_deltaModel);
	private Table m_boxTable = new Table( m_boxModel);
	private Table m_strikeTable = new Table( m_strikeModel);

	// GUI elements
	private Lab m_tfDays = new Lab();
	private Lab m_tfInterest = new Lab();
	private Lab m_tfBox = new Lab(60);
	private JTextField m_tfBuyOffset = new JTextField(4);
	private JTextField m_tfSellOffset = new JTextField(4);
	private JTextField m_tfConv = new JTextField(4);
	private JTextField m_tfConvStrike = new JTextField(4);
	private JCheckBox m_cbShowPct = new JCheckBox( "Show percent");
	private JCheckBox m_cbAllowOrder = new JCheckBox( "Allow");
	private JCheckBox m_cbXmit = new JCheckBox( "Transmit");
	
	// box/synth elements
	private DoubleField m_tfBuyCallStrike = new DoubleField(4);
	private DoubleField m_tfSellCallStrike = new DoubleField(4);
	private DoubleField m_tfBuyBoxOffset = new DoubleField(4);
	private DoubleField m_tfSellBoxOffset = new DoubleField(4);
	private JButton m_bBuyBox = new JButton( "Buy Box");

	// buttons
	private JButton m_bMM = new JButton( "MM");
	private JButton m_bSubscribe = new JButton( "Subscribe");
	private JButton m_bCancelAll = new JButton( "Cancel All");

	private Arb m_arb;
	private boolean m_subscribed;
	private boolean m_buyActive, m_sellActive;      // synth orders, buy and sell
	private Order m_buy, m_sell;
	
	// get
	String expiry() 			{ return m_expiry; }
	String expiry2() 			{ return m_expiry.substring( 0, 6); }
	double interest() 			{ return m_interest; }
	Data future() 				{ return m_future; }
	double daysTilExpiry() 		{ return m_daysTilExpiry; }
	boolean hasFuture() 		{ return m_future != null; }
	private double i() 			{ return m_under.i(); }
	Data stock()				{ return m_under.stock(); }
	int synthPos()				{ return m_synthPos; }
	int shortSynthPos()			{ return m_negSynthPos; }
	Under under()				{ return m_under; }
	int fakeBoxPos() 			{ return m_fakeBoxPos; }
	int fakeShortSynthPos() 	{ return m_fakeShortSynthPos; }
	int fakeSynthPos() 			{ return m_fakeSynthPos; }
	int fakeConversionPos() 	{ return m_fakeConversionPos; }
	int fakeEFP() 				{ return m_fakeEFP; }
	int fakeFuturePos()			{ return m_fakeFuturePos; }

	// helpers
	boolean isValid( double val) 	{ return S.isValid( val); }
	String symbol() 				{ return m_under.symbol(); }
	double dividend()				{ return m_under.dividend(); } // move to Exp. ???
	public int futurePos() 			{ return m_future != null ? m_future.position() : 0; }
	double underMarkPrice()			{ return m_under != null ? m_under.markPrice() : Double.MAX_VALUE; }
	static void out( String str) 	{ Arb.out( str); }
	static String fmt( double v) 	{ return S.fmt2a( v); }
	static String fmtSt( double v) 	{ return S.isValid( v) ? FMT1.format( v) : null; }
	void msg( String str)			{ m_under.msg( str); } 
	private String id() 			{ return m_under.symbol() + " " + m_expiry; }

	public Exp(Arb arb, Under under, String expiry) {
		m_arb = arb;
		m_under = under;
		m_expiry = expiry;
		m_daysTilExpiry = calcDaysTilExpiry();
		
		m_tfDays.setText( "" + m_daysTilExpiry);
		
		if( symbol().equals( "XEO") ) {
			try {
				m_log = new OStream( "c:\\options\\bidask." + m_expiry + "." + S.TODAY + ".csv", true);
			} 
			catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
		}

		JScrollPane convScroll = new JScrollPane( m_topTable);
		convScroll.setPreferredSize( new Dimension( 300, 150) );
		
		JPanel convPanel = new JPanel( new BorderLayout() );
		convPanel.setBorder( new TitledBorder( "Conversion/EFP") );
		convPanel.add( convScroll);
		
		JScrollPane deltaScroll = new JScrollPane( m_deltaTable);
		deltaScroll.setPreferredSize( new Dimension( 300, 220) );
		
		JPanel deltaPanel = new JPanel( new BorderLayout() );
		deltaPanel.setBorder( new TitledBorder( "Delta") );
		deltaPanel.add( deltaScroll);

		JScrollPane boxScroll = new JScrollPane( m_boxTable);
		boxScroll.setBorder( new TitledBorder( "Box") );
		boxScroll.setPreferredSize( new Dimension( 300, 200) ); 
		
		JPanel boxPanel = new JPanel( new BorderLayout() );
		boxPanel.add( boxScroll);
		
		JScrollPane strikeScroll = new JScrollPane( m_strikeTable);
		strikeScroll.setBorder( new TitledBorder( "Strikes") );

		JPanel indexBoxPanel = new JPanel();
		indexBoxPanel.add( m_tfBox);

		JPanel actionPanel = new JPanel();
		actionPanel.add( m_bCancelAll);
		
		JPanel topPanel = new JPanel();
		topPanel.add( m_bSubscribe);
		topPanel.add( m_bMM);
		topPanel.add( new JLabel( "Days til exipry"));
		topPanel.add( m_tfDays);
		topPanel.add( new JLabel( "Interest"));
		topPanel.add( m_tfInterest);
		topPanel.add( m_cbShowPct);
		topPanel.add( m_cbXmit);
		topPanel.add( m_cbAllowOrder);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS) );
		//centerPanel.add( convPanel);
		centerPanel.add( deltaPanel);
		centerPanel.add( boxPanel);
		centerPanel.add( strikeScroll);
		
		JPanel botPanel = new JPanel();
		botPanel.setLayout( new BoxLayout( botPanel, BoxLayout.Y_AXIS) );
		botPanel.add( indexBoxPanel);
		botPanel.add( actionPanel);
		
		JPanel allPanels = new JPanel( new BorderLayout() );
		allPanels.add( topPanel, BorderLayout.NORTH);
		allPanels.add( centerPanel);
		allPanels.add( botPanel, BorderLayout.SOUTH);
		
		m_under.addTab( fmtExp( expiry), allPanels);

		m_bSubscribe.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onSubscribe();
			}
		});
		m_bMM.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onMM();
			}
		});
		m_bCancelAll.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCancelAll();
			}
		});
		m_bBuyBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onBuyBox();
			}
		});
	}

	protected void onMM() {
		double offset = .80;
		double low = 0;
		double high = 0;
		
		for( double s = low; s <= high + .01; s += .05) {
			Strike strike = getStrike( s);
			strike.mm( offset);
		}
	}
	
	protected void onBuyBox() {
		double highStrikeVal = Double.parseDouble( m_tfBuyCallStrike.getText() );
		double offset = Double.parseDouble( m_tfBuyBoxOffset.getText() );
		
		for( int i = 0; i < 4; i++, highStrikeVal -= 4) {			
			double lowStrikeVal = Double.parseDouble( m_tfSellCallStrike.getText() );
			
			for( int j = 0; j < 4; j++, lowStrikeVal += 4) {		
				Strike highStrike = getStrike( highStrikeVal);
				Strike lowStrike = getStrike( lowStrikeVal);				
				if( highStrike != null && lowStrike != null) {
					Box box = new Box( highStrike, lowStrike);
					double price = -offset - box.costAtExpiration();
					placeOrder( "BUY", 3, box.getLegs(), price, "box");
				}
			}
		}
	}
		
	private void buyOrSellSynth( String side, double strikeVal) throws Exception {
		String offsetStr = m_tfBuyBoxOffset.getText();
		if( S.isNull( offsetStr) ) {
			msg( "Enter offset");
			return;
		}
		
		double offset = Double.parseDouble( offsetStr);
		Strike strike = getStrike( strikeVal);
		
		double price = calcSynthPrice( side, strike.strike(), strike.synthLegs() );
		
		Order order = placeOrder( side, 1, strike.synthLegs(), price, "synth");
		if( order != null) {
			order.m_autoUpdate = true;
			order.m_hedge = true;
			order.m_offset = offset;
		}
	}
	
	/** Calcs for known strike. */
	private double calcSynthPrice(String side, double strike, Legs legs) throws Exception {
		double C = .2; // buy at least .20 below ask or .20 above bid; maybe .30 ???

		double fairPrice = 0;// fix. ps hedgeIndexPrice() - strike;
		
		double price;
		
		if( side.equals( "BUY") ) {
			double offset = m_tfBuyBoxOffset.getDouble();
			price = Math.min( fairPrice - offset, legs.ask() - C);
		}
		else {
			double offset = m_tfSellBoxOffset.getDouble();
			price = Math.max( fairPrice + offset, legs.bid() + C);
		}
		
		price = (int)((price + .005) * 100) / 100.0;
		
		return price;
	}
	
	private void updateOrder( Order order) {
		out( "Updating order " + order + " " + extra( order, order.m_lmtPrice) + "  elapsed=" + order.elapsed() );
		m_arb.placeOrder( order);
	}

	private void onCancelAll() {
		cancelBuys();
		cancelSells();
		m_orders.clear();
	}

	void cancelBuys() {
		for( Order order : m_orders.values() ) {
			if( order.isBuy() ) {
				out( "Canceling " + order.m_orderId);
				m_arb.cancelOrder( order.m_orderId);
			}
		}
	}

	void cancelSells() {
		for( Order order : m_orders.values() ) {
			if( order.isSell() ) {
				out( "Canceling " + order.m_orderId);
				m_arb.cancelOrder( order.m_orderId);
			}
		}
	}

	Order placeOrder( String side, int size, Strike strike, double lmtPrice, String orderRef) {
		return placeOrder(side, size, strike, strike.synthLegs(), lmtPrice, orderRef, true, true);
	}
	
	Order placeOrder( String side, int size, Legs legs, double lmtPrice, String orderRef) {
		return placeOrder(side, size, null, legs, lmtPrice, orderRef, m_cbXmit.isSelected(), false);
	}
	
	Order placeOrder( String side, int size, Strike strike, Legs legs, double lmtPrice, String orderRef, boolean transmit, boolean hedge) {
		// round
		lmtPrice = Math.round( lmtPrice * 100) / 100.0;

		Contract contract = new Contract();
		contract.m_symbol = symbol();
		contract.m_secType = "BAG";
		contract.m_currency = "USD";
		contract.m_exchange = "SMART";
		contract.legs( legs);

		Order order = new Order( contract, m_arb.nextId() );
		order.m_size = size;
		order.m_side = side;
		order.m_orderType = "LMT";
		order.m_lmtPrice = lmtPrice;
		order.m_transmit = transmit;
		order.m_orderRef = orderRef + m_orderRef++;
		order.m_tif = "DAY";
		order.m_strike = strike;
		order.m_hedge = hedge;
			
		// place order
		placeOrder(order);
		
		return order;
	}
	
	private void placeOrder( Order order) {
		if( !m_cbAllowOrder.isSelected() ) {
			msg( "Orders not enabled for this expiry");
			return;
		}
		
		if( !m_subscribed) {
			msg( "Not subscribed to mkt data");
			return;
		}
		
		out( "Placing order " + order + " " + extra( order, order.m_lmtPrice) );
		m_orders.put( order.m_orderId, order);
		m_arb.placeOrder( order);
	}

	private String extra( Order order, double price) {
		try {
			return extra_( order, price);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String extra_( Order order, double price) throws Exception {
		Contract c = order.contract();
		
		StringBuilder b = new StringBuilder();
		b.append( " realBid=" + fmt( c.legs().bid() ) );
		b.append( " realPrice=" + fmt( price) );
		b.append( " realAsk=" + fmt( c.legs().ask() ) );
		if( order.contract().hasLegs() ) {
			double strike = c.getFirstStrike();
			b.append( " bid=" + fmt( c.legs().bid() + strike) );
			b.append( " price=" + fmt( price + strike));
			b.append( " ask=" + fmt( c.legs().ask() + strike) );
		}
		b.append( " index=" + fmt( underMarkPrice() ) );
		return b.toString();
	}
	
	public void check() {
		updateInterest();
		updateSynthPositions();

		for( Strike strike : m_strikes) {
			strike.calcValues();
		}

		m_curHighConvBid = getHighConvBid();
		m_curLowConvAsk = getLowConvAsk();
		m_highSynthBid = m_curLowConvAsk;
		m_lowSynthAsk = m_curHighConvBid;

		checkInvestmentBox();	// best borrow/loan rates
		checkRiskyBox();		// highest profit but still "valid"
		
		if( m_efp != null) {
			m_highEfpBid = Math.max( m_highEfpBid, m_efp.bid() );
			m_lowEfpAsk = Math.min( m_lowEfpAsk, m_efp.ask() );
		}
		
		m_topTable.fireTableDataChanged();
		m_strikeTable.fireTableDataChanged();
		m_boxTable.fireTableDataChanged();
		m_deltaTable.fireTableDataChanged();
		
		try {
			if( m_log != null && m_subscribed) {
				m_log.log2( fmt( m_highSynthBid.synthBid() ) + C + 
						   fmt( m_lowSynthAsk.synthAsk() ) + C +
						   S.average( m_highSynthBid.synthBid(), m_lowSynthAsk.synthAsk() ) + C + 
						   fmt( underMarkPrice() ) + C +
						   fmt( m_under.hedgeMarkPrice() ) + ",," + 
						   fmtSt( m_highSynthBid.strike() ) + C +
						   fmtSt( m_lowSynthAsk.strike() ) );
					        
			}
		}
		catch( Exception e) {
		}	
	}

	void updateInterest() {
		if( stock() != null) {
			m_interest = i() * stock().last() / 365 * m_daysTilExpiry;
			m_tfInterest.setText( fmt( m_interest) );
		}
	}

	class Box implements Cloneable {
		private Strike m_buyCallStrike;
		private Strike m_sellCallStrike;
		
		Strike buyCallStrike() { return m_buyCallStrike; }
		Strike sellCallStrike() { return m_sellCallStrike; }
		
		public Box(Strike buyCallStrike, Strike sellCallStrike) {
			m_buyCallStrike = buyCallStrike;
			m_sellCallStrike = sellCallStrike;
			
			if( buyCallStrike == null || sellCallStrike == null) {
				out( "error");
			}
		}
		
		Legs getLegs() {
			Legs legs = new Legs( "Box");
			legs.add( m_buyCallStrike.synthLegs() );
			legs.flip( m_sellCallStrike.synthLegs() );
			return legs;
		}

		String getStrikes() {
			return m_buyCallStrike + "/" + m_sellCallStrike;
		}
		
		double getAsk() {
			return m_buyCallStrike.synthAsk() - m_sellCallStrike.synthBid();
		}
		
		double getBid() {
			return m_buyCallStrike.synthBid() - m_sellCallStrike.synthAsk();
		}
		
		double getModel() {
			return m_buyCallStrike.synthModel() - m_sellCallStrike.synthModel();
		}
		
		@Override public Box clone() {
			try {
				Box other = (Box)super.clone();
				
				if( m_buyCallStrike != null) {
					other.m_buyCallStrike = m_buyCallStrike.clone();
				}
				
				if( m_sellCallStrike != null) {
					other.m_sellCallStrike = m_sellCallStrike.clone();
				}
				
				return other;
			} 
			catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return null;
			}
		}

		double costToReplacePut() {
			return m_buyCallStrike.costToReplacePut();
		}
		
		double costToReplaceCall() {
			return m_sellCallStrike.costToReplaceCall();
		}
		
		// cost should be above and beyond what would have transpired at expiration
		double costToLiqAfterCallAssign() {
			try {
				double leg1 = -m_buyCallStrike.liqCall();					// buy call leg
				double leg2 = m_buyCallStrike.put().ask();					// sell put leg
				double leg3 = -m_sellCallStrike.strike() + stock().ask();	// sell call leg, got assigned
				double leg4 = -m_sellCallStrike.liqPut();					// buy put leg
				return leg1 + leg2 + leg3 + leg4 - costAtExpiration();
			}
			catch( Exception e) {
				return Double.MAX_VALUE;
			}
		}
			
		double costToLiqAfterPutAssign() {
			try {
				double leg1 = -m_buyCallStrike.liqCall();					// buy call leg
				double leg2 = m_buyCallStrike.strike() - stock().ask(); 	// sell put leg, got assigned
				double leg3 = m_sellCallStrike.call().ask();				// sell call leg
				double leg4 = -m_sellCallStrike.liqPut();					// buy put leg
				return leg1 + leg2 + leg3 + leg4 - costAtExpiration();
			}
			catch( Exception e) {
				return Double.MAX_VALUE;
			}
		}
		
		double costAfterCallAssign() {
			return Math.min( costToLiqAfterCallAssign(), costToReplaceCall() );
		}
		
		double costAfterPutAssign() {
			return Math.min( costToLiqAfterPutAssign(), costToReplacePut() );
		}
		
		double costAfterAnyAssign() {
			return Math.max( costAfterCallAssign(), costAfterPutAssign() );
		}
		
		double costAtExpiration() {
			return m_buyCallStrike.strike() - m_sellCallStrike.strike();
		}
		
		/** This should return true for European-style index options. */
		public boolean isValid() {
			if( m_sellCallStrike.callTimeValue() < 0) {
				return false;
			}
			
			if( m_under.isIndex() && m_buyCallStrike.putTimeValue() < 0) {
				return false;
			}
			
			return true;
		}

		public double totalTimeValue() {
			return m_buyCallStrike.putTimeValue() + m_sellCallStrike.callTimeValue();
		}
	}
	
	/** This is for best loan/borrow rates. */
	public void checkInvestmentBox() {
		double lowBorrowPct = Double.MAX_VALUE;
		double borrowLowStrike = 0;
		double borrowHighStrike = 0;
		double borrowAmt = 0;
		
		double highInvestPct = -Double.MAX_VALUE;
		double investLowStrike = 0;
		double investHighStrike = 0;
		double investAmt = 0;
		
		for( Strike highStrike : m_strikes) {
			for( Strike lowStrike : m_strikes) {
				if( highStrike.strike() > lowStrike.strike() && lowStrike.synthLegs() != null && highStrike.synthLegs() != null) {
					// borrow money: sell low strike, buy high strike
					if( highStrike.synthLegs().validBid() && lowStrike.synthLegs().validAsk() ) {
						double sell = lowStrike.synthLegs().bid();
						double buy = highStrike.synthLegs().ask();
						double borrowNow = sell - buy;
						if( borrowNow > 0 ) {
							double payLater = highStrike.strike() - lowStrike.strike();
							double cost = payLater - borrowNow;
							double pct = cost / borrowNow;
							if( pct < lowBorrowPct) {
								lowBorrowPct = pct;
								borrowLowStrike = lowStrike.strike();
								borrowHighStrike = highStrike.strike();
								borrowAmt = borrowNow;
							}
						}
					}
					
					// invest money
					if( lowStrike.synthLegs().validAsk() && highStrike.synthLegs().validBid() ) {
						double buy = lowStrike.synthLegs().ask();
						double sell = highStrike.synthLegs().bid();
						double investNow = buy - sell;
						double receiveLater = highStrike.strike() - lowStrike.strike();
						if( investNow > 0) {
							double profit = receiveLater - investNow;
							double pct = profit / investNow;
							if( pct > highInvestPct) {
								highInvestPct = pct;
								investLowStrike = lowStrike.strike();
								investHighStrike = highStrike.strike();
								investAmt = investNow;
							}
						}
					}
				}
			}
		}
		double borrowApr = lowBorrowPct / m_daysTilExpiry * 365;
		double investApr = highInvestPct / m_daysTilExpiry * 365;
		String borrowStr = "Borrow " + fmt( borrowAmt * 100) + " at " + S.fmtPct( borrowApr) + " with strikes " + borrowLowStrike + "/" + borrowHighStrike;
		String investStr = "Invest " + fmt( investAmt * 100) + " at " + S.fmtPct( investApr) + " with strikes " + investLowStrike + "/" + investHighStrike; 
		m_tfBox.setText( investStr + "  " + borrowStr);
	}
	
	/** This finds the Box with the lowest cost. */
	public void checkRiskyBox() {
		Strike buyCallStrike = getRiskyBuyCallStrike( -100);
		Strike sellCallStrike = getRiskySellCallStrike( -100);
		
		if( buyCallStrike != null && sellCallStrike != null) {
			Box box = new Box( buyCallStrike, sellCallStrike);
			m_box = box;
			
			if( m_bestBox == null || 
				lt( m_box.getAsk(), m_bestBox.getAsk() ) || 
				eq( m_box.getAsk(), m_bestBox.getAsk() ) &&
				m_box.totalTimeValue() > m_bestBox.totalTimeValue() ) {
				
				m_bestBox = m_box.clone();
			}
		}
		
		buyCallStrike = getRiskyBuyCallStrike( 0);
		sellCallStrike = getRiskySellCallStrike( 0);
		
		if( buyCallStrike != null && sellCallStrike != null) {
			Box box = new Box( buyCallStrike, sellCallStrike);
			m_box2 = box;
			
			if( m_bestBox2 == null || 
				lt( m_box2.getAsk(), m_bestBox2.getAsk() ) || 
				eq( m_box2.getAsk(), m_bestBox2.getAsk() ) &&
				m_box2.totalTimeValue() > m_bestBox2.totalTimeValue() ) {
				
				m_bestBox2 = m_box2.clone();
			}
		}		
	}
	
	/** Find strike with lowest synth ask, preferring lower strikes,
	 *  where bid of option you are selling has time value of at least minTimeVal. */
	private Strike getRiskyBuyCallStrike( double minTimeVal) {
		Strike best = null;
		
		for( Strike strike : m_strikes) {
			double val = strike.synthAsk();
			if( val != Double.MAX_VALUE && strike.putTimeValue() > minTimeVal) {
				if( best == null || ltEq( val, best.synthAsk() ) ) { 
					//eq( val, best.synthAsk() ) && strike.putTimeValue() > best.putTimeValue() ) {
					
					best = strike;
				}
			}
		}
		
		return best;
	}
	
	/** Find strike with highest synth bid, preferring higher strikes.
	 *  where bid of option you are selling has time value of at least minTimeVal. */
	private Strike getRiskySellCallStrike( double minTimeVal) {
		Strike best = null;
		
		for( Strike strike : m_strikes) {
			double val = strike.synthBid();
			
			if( val != Double.MAX_VALUE && strike.callTimeValue() > minTimeVal) {
				if( best == null || gt( val, best.synthBid() ) ) {
					best = strike;
				}
			}
		}
		
		return best;
	}
	
	/** Find highest bid, preferring lower strikes. */ // why???
	private Strike getHighConvBid() {
		Strike highBid = null;
		
		for( Strike strike : m_strikes) {
			if( strike.isValidReversal() && strike.isHigherBidThan( highBid) ) {
				highBid = strike;
			}
		}
		
		if( highBid != null && (m_highConvBid == null || highBid.isHigherBidThan( m_highConvBid) ) ) {
			m_highConvBid = highBid.clone();
		}
		
		return highBid;
	}

	/** Find lowest ask, preferring higher strikes. */
	private Strike getLowConvAsk() {
		// set current low ask
		Strike lowAsk = null;
		
		for( Strike strike : m_strikes) {
			if( strike.isValidConversion() && (lowAsk == null || strike.isBetterConversionThan( lowAsk) ) ) {
				lowAsk = strike;
			}
		}
		
		// set all-time low ask
		if( lowAsk != null && (m_lowConvAsk == null || lowAsk.isBetterConversionThan( m_lowConvAsk) ) ) {
			m_lowConvAsk = lowAsk.clone();
		}
		
		return lowAsk;
	}

	/*private Strike getHighSynthBid() {
		Strike highBid = null;
		
		for( Strike strike : m_strikes) {
			if( strike.synthLegs() != null) {
				if( highBid == null || strike.synthBid() > highBid.synthBid() ) {
					highBid = strike;
				}
			}
		}

		return highBid;
	}

	private Strike getLowSynthAsk() {
		Strike lowAsk = null;

		for( Strike strike : m_strikes) {
			if( strike.synthLegs() != null) {
				if( lowAsk == null || strike.synthAsk() < lowAsk.synthAsk() ) {
					lowAsk = strike;
				}
			}
		}
		
		return lowAsk;
	}*/

	private Strike getHighFutBid() {
		Strike bestStrike = null;
		double best = -Double.MAX_VALUE;
		for( Strike strike : m_strikes) {
			double val = strike.convFutBid();
			if( val != Double.MAX_VALUE && val > best) {
				bestStrike = strike;
				best = val;
			}
		}
		return bestStrike;
	}

	private Strike getLowFutAsk() {
		Strike bestStrike = null;
		double best = Double.MAX_VALUE;
		for( Strike strike : m_strikes) {
			double val = strike.convFutBid();
			if( val != Double.MAX_VALUE && val < best) {
				bestStrike = strike;
				best = val;
			}
		}
		return bestStrike;
	}

	Strike getOrCreateStrike( double strikeIn) {
		Strike strike = getStrike( strikeIn);
		if( strike == null) {
			strike = new Strike( m_arb, this, strikeIn);
			m_strikes.add( strike);
			Collections.sort( m_strikes);
		}
		return strike;
	}
	
	Strike getStrike( double strikeIn) {
		for( Strike strike : m_strikes) { // change to map. ???
			if( strike.isEq( strikeIn) ) {
				return strike;
			}
		}
		return null;
	}

	public void updateSynthPositions() {
		int synthPos = 0; 		// reversal
		int negSynthPos = 0; 	// conversion

		for( Strike strike : m_strikes) {
			synthPos += strike.synthPos();
			negSynthPos += strike.negSynthPos();
		}

		m_synthPos = synthPos;
		m_negSynthPos = negSynthPos;
	}

	/** Won't work across years. */
	long calcDaysTilExpiry() {
		long expiryMs = S.getTimeInMillis( expiry() );
		int expiry = S.dayOfYear( expiryMs);
		int today = S.dayOfYear( System.currentTimeMillis() );
		return expiry - today;
	}

	/** Called when an order is canceled or filled. */
	public void onDone(int orderId) {
		Order order = m_orders.remove( orderId);
		
		if( order != null) {
			if( order == m_buy) {
				m_buy = null;
				out( "buy order " + orderId + " removed from " + id() );
			}
			else if( order == m_sell) {
				m_sell = null;
				out( "sell order " + orderId + " removed from " + id() );
			}
			else {
				out( "order " + orderId + " removed from " + id() );
			}
		}
	}

	String getApr( double val) {
		double pct = val / stock().mid();
		double apr = pct / m_daysTilExpiry * 365;
		return S.fmtPct( apr);
	}

	/** Return the extra amount you will pay if call in conversion is assigned
	 *  and you end up with a short position. */
	double extra( double putBid) {
		return m_under.dividend() + m_interest - putBid; // pro-rate bid to the time dividend is paid. ???

	}
	
	class StrikeModel extends ArbModel {
		static final int STRIKE 	= 0;
		static final int PUTBID 	= 1;
		static final int PUTASK 	= 2;
		static final int CALLBID 	= 3;
		static final int CALLASK 	= 4;
		static final int SYNTHBID2 	= 5;
		static final int SYNTHASK2	= 6;
		static final int SYNTHBID 	= 7;
		static final int SYNTHASK	= 8;
		static final int SYNTHMID	= 9;
		static final int BIDDIF     = 10;
		static final int ASKDIF    	= 11;
		static final int BIDDIF2    = 12;
		static final int ASKDIF2   	= 13;
		static final int PUTTIME 	= 14;
		static final int CALLTIME 	= 15;
		static final int COUNT      = 16;
		
		public String getColumnName(int col) {
			switch( col) {
				case STRIKE:		return "Strike";
				case PUTBID: 		return "Put Bid";
				case PUTASK: 		return "Put Ask";
				case CALLBID: 		return "Call Bid";
				case CALLASK: 		return "Call Ask";
				case SYNTHBID2:		return "Real Bid";
				case SYNTHASK2:		return "Real Ask";
				case SYNTHBID:		return "Bid";
				case SYNTHASK:		return "Ask";
				case SYNTHMID:		return "Mid";
				case BIDDIF:		return "Bid Dif";
				case ASKDIF:		return "Ask Dif";
				case BIDDIF2:		return "Bid Dif2";
				case ASKDIF2:		return "Ask Dif2";
				case PUTTIME:		return "Put Time Vl";
				case CALLTIME:		return "Call Time Vl";
				default: 			return null;
			}
		}
		
		public int getColumnCount() {
			return COUNT;
		}

		public int getRowCount() {
			return m_strikes.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			try {
				return getValueAt_( rowIndex, columnIndex);
			}
			catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		public Object getValueAt_(int rowIndex, int columnIndex) throws Exception {
			Strike strike = m_strikes.get( rowIndex);
			
			// hid rows where put bid is < price net of dividends
			double price = strike.convAsk() - dividend();
			if( strike.put() == null || strike.put().bid() < price) {
				//return null;
			}
			
			switch( columnIndex) {
				case STRIKE:	return fmtSt( strike.strike() );

				case PUTBID: 	return strike.put() != null ? fmt( strike.put().bid() ) : null;
				case PUTASK: 	return strike.put() != null ? fmt( strike.put().ask() ) : null;
				//case PUTDELTA: 	return strike.put() != null ? fmt( strike.put().delta() ) : null;
				
				case CALLBID: 	return strike.call() != null ? fmt( strike.call().bid() ) : null;
				case CALLASK: 	return strike.call() != null ? fmt( strike.call().ask() ) : null;
				//case CALLDELTA:	return strike.call() != null ? fmt( strike.call().delta() ) : null;
				
				case SYNTHBID2:	return fmt( strike.realSynthBid() );
				case SYNTHASK2:	return fmt( strike.realSynthAsk() );
				case SYNTHBID:	return fmt( strike.synthBid() );
				case SYNTHASK: 	return fmt( strike.synthAsk() );
				case SYNTHMID: 	return fmt( strike.synthMid() );
				case BIDDIF:	return fmt( strike.synthBid() - underMarkPrice() );
				case ASKDIF:	return fmt( strike.synthAsk() - underMarkPrice() );
				
//				case CONVBID:	return getAprMaybe( strike.convBid() );
//				case CONVASK:	return getAprMaybe( strike.convAsk() );
				
				case PUTTIME:	return fmt( strike.putTimeValue() );
				case CALLTIME:	return fmt( strike.callTimeValue() );
				
				default: 		return null;
			}
		}
		
		@Override
		protected void onLeftClick(int row, int col) {
		}
	}
	
	void onSubscribe() {
		reqOptMktData();
		reqFutMktData();
		m_subscribed = true;
	}
	
	public void reqOptMktData() {
		for( Strike strike : m_strikes) {
			strike.reqMktData();
		}
	}
	
	void reqFutMktData() {
		if( m_future != null) {
			m_arb.reqMktData( m_future);
		}
		
		if( m_efp != null) {
			m_arb.reqMktData( m_efp);
		}
	}

	static String fmtExp(String expiry) {
		return expiry.substring( 0, 4) + " " + expiry.substring( 4, 6) + " " + expiry.substring( 6);
	}

	/** Called when interest rate or dividend changes. */
	public void clearAll() {
//		m_lowAsk = Double.MAX_VALUE;
//		m_lowBoxCost = Double.MAX_VALUE;
//		m_lowRevFutCost = Double.MAX_VALUE;
//		
//		for( Strike strike : m_strikes) {
//			strike.clearAll();
//		}
	}
	
	class TopModel extends AbstractTableModel {
		static final int STRATEGY   = 0;
		static final int STRIKE1	= 1;
		static final int BID 		= 2;
		static final int ASK 		= 3;
		static final int STRIKE2	= 4;
		static final int HIGHBID 	= 5;
		static final int LOWASK 	= 6;
		static final int COLUMNS	= 7;
		
		public String getColumnName(int col) {
			switch( col) {
				case STRIKE1:	return "Strike";
				case BID:		return "Bid";
				case HIGHBID: 	return "High Bid";
				case STRIKE2:	return "Strike";
				case LOWASK: 	return "Low Ask";
				case ASK: 		return "Ask";
				default: 		return null;
			}
		}
		
		public int getColumnCount() {
			return COLUMNS;
		}

		public int getRowCount() {
			return 2;
		}

		public Object getValueAt(int row, int col) {
			switch( row) {
				case 0: return getEFPValue( col);
				case 1: return getConvValue( col);
				default: return null;
			}
		}
		
		Object getEFPValue( int col) {
			if( col == STRATEGY) {
				return "EFP";
			}
			
			if( m_efp == null) {
				return null;
			}
			
			switch( col) {
				case BID: 		return getAprMaybe( -m_efp.ask() );
				case HIGHBID: 	return isValid( m_lowEfpAsk) ? getAprMaybe( -m_lowEfpAsk) : null;
				case LOWASK: 	return isValid( m_highEfpBid) ? getAprMaybe( -m_highEfpBid) : null;
				case ASK:		return getAprMaybe( -m_efp.bid() );
				default: 		return null;
			}
		}
		
		Object getConvValue( int col) {
			switch( col) {
				case STRATEGY:		return "Conversion"; 
				case STRIKE1:		return strike1();
				case BID: 			return m_curHighConvBid != null ? getAprMaybe( m_curHighConvBid.convBid() ) : null;
				case ASK: 			return m_curLowConvAsk != null ? getAprMaybe( m_curLowConvAsk.convAsk() ) : null;
				case STRIKE2:		return m_highConvBid + "/" + m_lowConvAsk;
				case HIGHBID:		return m_highConvBid != null ? getAprMaybe( m_highConvBid.convBid() ) : null;
				case LOWASK: 		return lowAsk();
				default: 			return null;
			}
		}

		private String strike1() {
			return (m_curHighConvBid != null ? m_curHighConvBid : null) + "/" + 
			       (m_curLowConvAsk != null ? m_curLowConvAsk : null);
		}

		private Object lowAsk() {
			return m_lowConvAsk != null ? getAprMaybe( m_lowConvAsk.convAsk() ) : null;
			/*if( m_lowConvAsk == null) {
				return null;
			}
			
			if( m_cbShowPct.isSelected() ) {
				double payNow = m_lowConvAsk.rawConvAsk();
				double profit = m_lowConvAsk.strike() - payNow;
				double pct = profit / payNow;
				double apr = pct / daysTilExpiry() * 360;
				return OStream.fmtPcta( apr);
			}
			return fmt( m_lowConvAsk.convAsk() );*/
		}
	}
	
	class BoxModel extends ArbModel {
		static final int STRATEGY   	= 0;
		static final int STRIKES		= 1;
		static final int BID 			= 2;
		static final int ASK 			= 3;
		static final int MODEL			= 4;
		static final int PUT_TV			= 5;
		static final int CALL_TV		= 6;
		static final int COLUMNS		= 7;
		
		public String getColumnName(int col) {
			switch( col) {
				case STRATEGY:  	return "Description";
				case STRIKES:		return "Strikes";
				case BID:			return "Bid";
				case ASK:			return "Ask";
				case PUT_TV:		return "Put TV";
				case CALL_TV:		return "Call TV";
				case MODEL:			return "Model";
				default: 			return null;
			}
		}
		
		public int getColumnCount() {
			return COLUMNS;
		}

		public int getRowCount() {
			return 4;
		}

		public Object getValueAt(int row, int col) {
			switch( row) {
				case 0: return getAnyValue( col, "Box (any)", m_box);
				case 1: return getAnyValue( col, "Box (w/ pos put tv)", m_box2); 
				case 2: return getAnyValue( col, "Best (saved)", m_bestBox); 
				case 3: return getAnyValue( col, "Best2 (saved)", m_bestBox2); 
				default: return null;
			}
		}
		
		Object getAnyValue( int col, String title, Box box) {
			if( col == STRATEGY) {
				return title;
			}
			
			if( box == null) {
				return null;
			}
			
			switch( col) {
				case STRIKES: 		return box.getStrikes();
				case BID:			return fmt( box.getBid() );
				case ASK:			return fmt( box.getAsk() );
				case MODEL:			return fmt( box.getModel() );
				case PUT_TV:		return fmt( box.buyCallStrike().putTimeValue() );
				case CALL_TV:		return fmt( box.sellCallStrike().callTimeValue() );
				default: 			return null;
			}
		}
		
		/** Never transmit since order is marketable. */
		@Override protected void onLeftClick(int row, int col) {
			if( col == ASK) {
				Box box = row == 0 ? m_box : m_bestBox;
				Legs legs = box.getLegs();
				placeOrder( "BUY", 1, null, legs, legs.ask(), "box", false, false);
			}
		}
	}

	public void handlePosition(Contract contract, int position) {
		if( contract.m_secType.equals( "OPT") ) {
			// fix API bug
			contract.repairExchange();
			
			OptionData data = handleOptionContract( contract, false);
			data.position( position);
		}
		else if( contract.m_secType.equals( "FUT") ) {
			handleFutureContract( contract, false);
			m_future.position( position);
		}
	}

	/** Called when Under gets stock contract. */
	void handleStockContract() {
		createEfp();
		for( Strike strike : m_strikes) {
			strike.handleStock();
		}
	}
	
	public OptionData handleOptionContract(Contract contract, boolean reqMktData) {
		Strike strike = getOrCreateStrike( contract.m_strike);
		OptionData data = strike.handleOption( contract);
		
		if( reqMktData) {
			m_arb.reqMktData( data);
		}
		
		return data;
	}

	public void handleFutureContract(Contract contract, boolean reqMktData) {
		if( m_future == null) {
			m_future = new FutureData( contract, m_arb.nextId() );
			
			createEfp();
			
			if( reqMktData) {
				reqFutMktData();
			}
		}
	}

	private void createEfp() {
		if( m_efp == null && m_future != null && stock() != null) {
			Legs legs = new Legs( "EFP");
			legs.add( true, 1, m_future);
			legs.add( false, 100, stock() );
			
			Contract contract = new Contract();
			contract.m_symbol = symbol();
			contract.m_secType = "BAG";
			contract.m_exchange = "SMART";
			contract.legs( legs);
			
			m_efp = new EfpData( contract, m_arb.nextId() ); // do when they click subscribe
		}
	}

	String getAprMaybe( double val) {
		return m_cbShowPct.isSelected()
			? getApr( val)
			: fmt( val);
	}

	public void fillPositions(ArrayList<Strike> list) {
		for( Strike strike : m_strikes) {
			strike.fillPositions( list);
		}
	}
	
	public void subscribePos() {
		for( Strike strike : m_strikes) {
			strike.subscribePos();
		}
	}
	
	public boolean hasDerivative() {
		return m_future != null && m_future.position() != 0 || m_synthPos != 0 || m_negSynthPos != 0; 
	}
	
	class DeltaModel extends AbstractTableModel {
		// columns
		static final int CONTRACT	= 0;
		static final int STRIKE		= 1;
		static final int BID		= 2;
		static final int ASK		= 3;
		static final int STRIKE2	= 4;
		static final int COLUMNS	= 5;
		
		// rows
		static final int STOCK 		= 0;
		static final int FUTURE 	= 1;
		static final int SYNTH 		= 2;
		static final int SYNTH2 	= 3;
		static final int ROWS		= 4;
		
		public int getColumnCount() {
			return COLUMNS;
		}

		public int getRowCount() {
			return ROWS;
		}
		
		public String getColumnName(int column) {
			switch( column) {
				case CONTRACT:	return "Type";
				case STRIKE:	return "Strike";
				case BID:		return "Bid";
				case ASK:		return "Ask";
				case STRIKE2:	return "Strike";
				default:		return null;
			}
		}

		public Object getValueAt(int row, int column) {
			switch( row) {
				case STOCK:		return getStock( column);
				case FUTURE: 	return getFuture( column);
				case SYNTH: 	return getSynth( column);
				case SYNTH2: 	return getSynth2( column);
				default:		return null;
			}
		}
		
		public Object getStock(int column) {
			switch( column) {
				case CONTRACT:	return "Stock"; 
				case STRIKE:	return null;
				case BID:		return stock() != null ? fmt( stock().bid() ) : null;
				case ASK:		return stock() != null ? fmt( stock().ask() ) : null;
				case STRIKE2:	return null;
				default: 		return null;
			}
		}

		public Object getFuture(int column) {
			switch( column) {
				case CONTRACT:	return "Future";
				case STRIKE:	return null;
				case BID:		return m_future != null ? fmt( m_future.bid() ) : null;
				case ASK:		return m_future != null ? fmt( m_future.ask() ) : null;
				case STRIKE2:	return null;
				default: 		return null;
			}
		}
		
		public Object getSynth(int column) {
			switch( column) {
				case CONTRACT:	return "Synthetic (w/ pos put tv)";
				case STRIKE:	return m_highSynthBid != null ? m_highSynthBid : null;
				case BID:		return m_highSynthBid != null ? fmt( m_highSynthBid.synthBid() ) : null;
				case ASK:		return m_lowSynthAsk  != null ? fmt( m_lowSynthAsk.synthAsk() ) : null;
				case STRIKE2:	return m_lowSynthAsk != null ? m_lowSynthAsk.strike() : null;
				default: 		return null;
			}
		}

		public Object getSynth2(int column) {
			switch( column) {
				case CONTRACT:	return "Synthetic (w/ high bid";
				case STRIKE:	return null;
				case BID:		return null; // fix this. ps
				case ASK:		return m_lowSynthAsk  != null ? fmt( m_lowSynthAsk.synthAsk() ) : null;
				case STRIKE2:	return m_lowSynthAsk != null ? m_lowSynthAsk.strike() : null;
				default: 		return null;
			}
		}
	}
	
	public void calcFakePositions() {
		m_fakeSynthPos = synthPos();
		m_fakeShortSynthPos = shortSynthPos();
		m_fakeFuturePos = futurePos();
		m_fakeConversionPos = 0;		
		m_fakeEFP = 0;
		m_fakeBoxPos = 0;
		
		// BOX
		if( m_fakeSynthPos > 0 && m_fakeShortSynthPos > 0) {
			m_fakeBoxPos = Math.min( m_fakeSynthPos, m_fakeShortSynthPos);
			m_fakeSynthPos -= m_fakeBoxPos;
			m_fakeShortSynthPos -= m_fakeBoxPos;
		}
		
		// Reversal
		int lots = m_under.fakeStockPos() / 100;
		if( lots < 0 && m_fakeSynthPos > 0) {
			int reversals = Math.min( -lots, m_fakeSynthPos);
			//int reversals = m_fakeSynthPos;
			m_under.incFakeStockPos( reversals * 100);
			m_fakeSynthPos -= reversals;
			m_fakeConversionPos = -reversals;
		}
		
		// Conversion
		else if( lots > 0 && m_fakeShortSynthPos > 0) {
			m_fakeConversionPos = Math.min( lots, m_fakeShortSynthPos);
			//m_fakeConversionPos = m_fakeShortSynthPos;
			m_under.decFakeStockPos( m_fakeConversionPos * 100);
			m_fakeShortSynthPos -= m_fakeConversionPos;
		}
		
		// long EFP (my way)
		lots = m_under.fakeStockPos() / 100;
		if( lots > 0 && m_fakeFuturePos < 0) {
			m_fakeEFP = Math.min( lots, -m_fakeFuturePos);
			m_under.decFakeStockPos( m_fakeEFP * 100);
			m_fakeFuturePos += m_fakeEFP;
		}
		
		// short EFP (my way)
		else if( lots < 0 && m_fakeFuturePos > 0) {
			int shortEfps = Math.min( -lots, m_fakeFuturePos);
			m_under.incFakeStockPos( shortEfps * 100);
			m_fakeFuturePos -= shortEfps;
			m_fakeEFP = -shortEfps;
		}

	}
	
	public double payAtExpiration() {
		double val = 0;
		
		for( Strike strike : m_strikes) {
			val += strike.payAtExpiration();
		}
		
//		if( m_efp != null) {
//			val += m_efp.payAtExpiration();
//		}
		
		return val;
	}
	
	public int compareTo(Exp o) {
		int rc = m_under.symbol().compareTo( o.m_under.symbol() );
		if( rc == 0) {
			rc = m_expiry.compareTo( o.m_expiry);
		}
		return rc;
	}
	
	public void test() {
		double tot = 0;
		out( expiry() );
		
		for( Strike strike : m_strikes) {
			double amt = strike.callPos() * strike.strike();

			if( strike.callPos() > 0) {
				out( "1\t" + strike.callPos() + "\t" + strike.strike() );
				tot += amt;
			}
			else if( strike.callPos() < 0) {
				out( "-1\t" + strike.callPos() + "\t" + strike.strike() );
				tot -= amt;
			}
		}
		out( "Total\t" + tot);
		out( "");
		
	}
	
	public void scanReport() {
		String t = "\t";
		double highEfpBid = -m_lowEfpAsk;
		double lowEfpAsk = -m_highEfpBid;
		double highConvBid = m_highConvBid != null ? m_highConvBid.convBid() : Double.MAX_VALUE;
		double lowConvAsk = m_lowConvAsk != null ? m_lowConvAsk.convAsk() : Double.MAX_VALUE;
		
		double highBid = S.max( highConvBid, highEfpBid); 
		double lowAsk = S.min( lowConvAsk, lowEfpAsk);
		
		double profit = Double.MAX_VALUE;
		if( S.isValid( highBid) && S.isValid( lowAsk) && highBid > lowAsk) {
			profit = highBid - lowAsk;
		}
		
		String str = symbol() + t + expiry2() + t + fmt( m_highEfpBid) + t + fmt( lowEfpAsk) + t + 
			fmt( highConvBid) + t + fmt( lowConvAsk) + t + fmt( profit); 
		m_arb.report( str); 
	}
	
	/** Buy Conversion: pay now to buy stock and receive strike at expiration. */
	void buyConversions( double lowStrike, double highStrike, double legOffset, double strikeOffset) {
		boolean process = true;
				
		for( int i = m_strikes.size() - 1; i >= 0; i--) {
			Strike strike = m_strikes.get( i);
			if( Strike.gtEq( strike.strike(), lowStrike) && Strike.ltEq( strike.strike(), highStrike) ) {
				if( process) {
					double price = strike.getConversionBuyPrice( legOffset, strikeOffset);
					if( strike.isValidConversion() ) {
						placeOrder( "BUY", 1, strike.convLegs(), price, "conv");
					}
				}
				process = !process;
			}
		}
	}

	public void updateBuyConversions( double lowStrike, double highStrike, double legOffset, double strikeOffset) {
		for( Order order : m_orders.values() ) {
			if( order.isBuy() ) {
				Strike strike = getStrike( order.contract().getFirstStrike() );
				double price = strike.getConversionBuyPrice( legOffset, strikeOffset);
				if( !Strike.eq( order.m_lmtPrice, price) ) {
					placeOrder( order);
				}
			}
		}
	}

	/** Sell Conversion: rec $ to sell short stock, pay strike at expiration. */
	void sellConversions( double lowStrike, double highStrike, double legOffset, double strikeOffset) {
		boolean process = false;
		
		for( Strike strike : m_strikes) {
			if( Strike.gtEq( strike.strike(), lowStrike) && Strike.ltEq( strike.strike(), highStrike) ) {
				if( process) {
					double price = strike.getConversionSellPrice( legOffset, strikeOffset);
					placeOrder( "SELL", 1, strike.convLegs(), price, "conv");
				}
				process = !process;
			}
		}
	}

	public void updateSellConversions(double d, double e, double f, double g) {
		
	}
	
	public void onOpenOrder(Order order, OrderState orderState) {
//		if( !m_orders.containsKey( order.orderId() ) ) {
//			out( "Adding open order " + order.orderId() + " to " + id() );
//			m_orders.put( order.orderId(), order);
//		}
	}
	
	/** Return how much you will pay to liquidate all positions. */
	public double liq() {
		double amt = 0;
		
		for( Strike strike : m_strikes) {
			amt += strike.liq();
		}
		
		return amt;
	}
	
	public boolean hasOrders() {
		return !m_orders.isEmpty();
	}

	static double round( double price) {
		return price > 0
			? Math.round( price * 100 + .01) / 100.0
			: Math.round( price * 100 - .01) / 100.0;
	}
	
	protected void onBuySynth() {
		m_buyActive = true;
		try {
			double strikeVal = m_tfBuyCallStrike.getDouble();
			buyOrSellSynth( "BUY", strikeVal);
		} 
		catch (Exception e) {
			msg( e.getMessage() );
		}
	}

	protected void onSellSynth() {
		m_sellActive = true;
		try {
			double strikeVal = m_tfSellCallStrike.getDouble();
			buyOrSellSynth( "SELL", strikeVal);
		} 
		catch (Exception e) {
			msg( e.getMessage() );
		}
	}
	
	private void abort() {
		if( m_buy != null) {
			out( "Shutting down buy order");
			m_arb.cancelOrder( m_buy.m_orderId);
			m_buy = null;
		}
		if( m_sell != null) {
			out( "Shutting down sell order");
			m_arb.cancelOrder( m_sell.m_orderId);
			m_sell = null;
		}
		m_buyActive = false;
		m_sellActive = false;
	}
	
	private Strike getHighSynthBid() {
		Strike best = null;
		
		for( Strike strike : m_strikes) {
			if( best == null || strike.synthBid() > best.synthBid() ) {
				best = strike;
			}
		}
		
		return best;
	}

	private Strike getLowSynthAsk() {
		Strike best = null;
		
		for( Strike strike : m_strikes) {
			if( best == null || strike.synthAsk() < best.synthAsk() ) {
				best = strike;
			}
		}
		
		return best;
	}
	
	public void cancelAll() {
		for( Order order : m_orders.values() ) {
			m_arb.cancelOrder( order.m_orderId);
		}
	}
	
	public double midpoint() {
		return m_highSynthBid != null && m_lowSynthAsk  != null
			? S.average( m_lowSynthAsk.synthAsk(), m_highSynthBid.synthBid() )
			: 0;
	}
}

// create text field for multiplier
// for orders and trades, show real price, synth price, offset, under price, hedge price, and ideally hedge.iv price
// show for place and update
// try MTL order type
// determine how much option size you can have open at one time
// try native ise combo; might update faster?
// order ref not working
// positions window is not updating
// show dif columns based on hedge index price

// why do you have 20 unxmitted orders when reuse orders is off?
// why is there such a pause at tws between order being canceled and new ordre being placed?
