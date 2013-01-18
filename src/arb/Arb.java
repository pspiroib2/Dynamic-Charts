package arb;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

import lib.OStream;
import lib.S;

import scale.profit.WrapperAdapter;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EReader;
import com.ib.client.Execution;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.OrderState;

public class Arb extends WrapperAdapter {
	static final long MS_IN_DAY = 24 * 60 * 60 * 1000;
	static final double REPLACE_OFFSET = .2; // when replacing put/call, sell at least this much above current bid

	private static final int PAUSE = 20;

	interface Req {
		void process(ContractDetails cd);
	}

	private EClientSocket m_socket;
	private AbstractMap<Integer,Data> m_mktDataMap = new ConcurrentHashMap();
	private AbstractMap<String,Under> m_underMap = new ConcurrentHashMap();
	private ArrayList<Under> m_unders = new ArrayList();
	private HashMap<Integer,Req> m_reqs = new HashMap();
	private OStream m_rpt;
	private boolean m_connected;

	// GUI elements
	private Lab m_tfMktData = new Lab();
	private Lab m_labStatus = new Lab();
	private JTextField m_tfUnder = new JTextField( "XEO", 6);
	private PosFrame m_posFrame;
	private JFrame m_frame = new JFrame();
	private AbstractMap<Integer, Data> m_cons = new ConcurrentHashMap<Integer, Data>();

	public static void main(String[] args) {
		new Arb().run();
	}

	private void run() {
		// connect
		m_socket = new EClientSocket( this);
		m_socket.eConnect( "localhost", 7496, 0);

		JMenuItem showPositions = new JMenuItem( "Show Positions");
		JMenuItem showTrades = new JMenuItem( "Show Trades");
		JMenuItem scan = new JMenuItem( "Scan");

		JMenuBar menubar = new JMenuBar();
		menubar.add( showPositions);
		menubar.add( showTrades);
		menubar.add( scan);

		JPanel p = new JPanel();
		p.add( m_tfUnder);

		JPanel p2 = new JPanel();
		p2.add( new JLabel( "Mkt data") );
		p2.add( m_tfMktData);
		p2.add( m_labStatus);

		m_frame.setJMenuBar( menubar);
		m_frame.add( p, BorderLayout.NORTH);
		m_frame.add( p2, BorderLayout.SOUTH);
		m_frame.setSize( 400, 200);
		m_frame.setTitle( "Arb");
		m_frame.setVisible( true);
		m_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		m_frame.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onExit();
			}
		});

		showPositions.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onShowPositions();
			}
		});
		showTrades.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onShowTrades();
			}
		});
		scan.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onScan();
			}
		});
		m_tfUnder.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCreate();
			}
		});

		MyTask task = new MyTask() {
			@Override public void process() {
				onCheck();
			}
		};


		Timer timer = new Timer( "Timer");
		timer.schedule( task, 4000, 1000);

		onCreate();
	}

	static String[] SYMBOLS = { "AAPL", "ADBE", "ADSK", "AKAM", "AMGN", "AMZN", "APOL", "ATVI", "BBBY", "BIDU", "BIIB", "BRCM", "CELG", "CEPH", "CERN", "CHKP", "CSCO", "CTSH", "CTXS", "DELL", "DISH", "DTV", "EBAY", "ERTS", "ESRX", "EXPE", "FISV", "FLEX", "FLIR", "FSLR", "FWLT", "GENZ", "GILD", "GOOG", "HANS", "HOLX", "HSIC", "IACI", "ILMN", "INTU", "ISRG", "LBTYA", "LIFE", "LINTA", "LOGI", "LRCX", "MICC", "MRVL", "NIHD", "NTAP", "NVDA", "ORLY", "PCLN", "PDCO", "RIMM", "RYAAY", "SBUX", "SHLD", "SRCL", "STX", "SYMC", "URBN", "VRSN", "VRTX", "WCRX", "WYNN", "YHOO" };

	protected void onScan() {
		new Thread() {
			public void run() {
				onScan_();
			}
		}.start();
	}

	void onScan_() {
		try {
			m_rpt = new OStream( "c:\\options\\report.t", false);

			for( String symbol : SYMBOLS) {
				if( !m_connected) {
					break;
				}
				scan( symbol);
			}

			m_rpt.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void scan(String symbol) {
		out( "Scanning " + symbol);

		int SCAN_TIME = 20000;

		Under under;

		synchronized( EReader.LOCK) {
			under = getOrCreateUnder( symbol);
			under.scan();
		}

		S.sleep( SCAN_TIME);

		synchronized( EReader.LOCK) {
			under.scanReport();
			desubscribe( symbol);
		}
	}

	protected void onShowPositions() {
		if( m_posFrame == null) {
			m_posFrame = new PosFrame( this);
			m_socket.reqAccountUpdates( true, null);
		}
		else {
			m_posFrame.setVisible( true);
			m_posFrame.setState( Frame.NORMAL);
			m_posFrame.toFront();
		}
	}

	protected void onShowTrades() {
		m_socket.reqExecutions( ExecutionFilter.ALL);
	}

	protected void onCreate() {
		String symbol = m_tfUnder.getText().toUpperCase();

		Under under = getOrCreateUnder( symbol);
		under.createFrame();

		m_tfUnder.setText( null);
	}

	Under getOrCreateUnder(final String symbol) {
		Under under = m_underMap.get( symbol);
		if( under == null) {
			under = new Under( this, symbol);
			m_underMap.put( symbol, under);
			m_unders.add( under);
			Collections.sort( m_unders);
			S.exec( 5000, new Runnable() {
				public void run() {
					out( "Requesting open orders - " + symbol);
					m_socket.reqAllOpenOrders();
				}
			});
		}
		return under;
	}

	@Override public void onConnected(int orderId) {
		super.onConnected( orderId);
		m_connected = true;

		m_socket.reqAutoOpenOrders(true);
	}

	protected void onCheck() {
		m_labStatus.setText( m_connected ? "Connected" : null);

		for( Under under : m_underMap.values() ) {
			try {
				under.check();
			}
			catch( Exception e) {
				out( e.toString() );
				e.printStackTrace();
			}
		}

		if( m_posFrame != null) {
			m_posFrame.check();
		}
	}

	@Override public void onPositionUpdate(Contract contract, int position, double marketPrice, double marketValue, double averageCost, double unrealizedPNL, double realizedPNL, String accountName) {
		// ignore cash types
		if( contract.m_secType.equals( "CASH") ) {
			return;
		}

		Under under = getOrCreateUnder( contract.m_symbol);
		under.onPositionUpdate(contract, position, marketPrice, marketValue, averageCost, unrealizedPNL, realizedPNL, accountName);
	}

	@Override public void tickPrice(int tickerId, int field, double price, int canAutoExecute) {
		Data data = m_mktDataMap.get( tickerId);
		if( data != null) { // could be null after desubscription
			data.updateFrom( field, price);
		}
	}

	@Override public void tickSize(int tickerId, int field, int size) {
		Data data = m_mktDataMap.get( tickerId);
		if( data != null) { // could be null after desubscription
			data.updateSizeFrom( field, size);
		}
	}

	@Override
	public void tickOptionComputation(int tickerId, int field, double impliedVol, double delta, double modelPrice, double pvDividend) {
		Data data = m_mktDataMap.get( tickerId);
		if( data != null) { // could be null after desubscription
			data.updateOptionFrom( field, impliedVol, delta, modelPrice, pvDividend);
		}
	}

	@Override
	public void onOrderStatus(int orderId, String status, int filled, int remaining, double avgFillPrice, int permId, int parentId, double lastFillPrice, int clientId, String whyHeld) {
		if( status.equals( "Filled") || status.equals( "Cancelled") ) {
			log( "order status " + orderId + " " + status);
			for( Under under : m_underMap.values() ) {
				under.onDone( orderId);
			}
		}
	}

	@Override
	public void onOpenOrder(int orderId, Contract contract, Order order, OrderState orderState) {
		if( contract.m_secType.equals( "BAG") ) {
			//out( "Received open bag order");

			Legs legs = new Legs();

			StringTokenizer st = new StringTokenizer( contract.m_comboLegsDescrip, ",");
			while( st.hasMoreTokens() ) {
				StringTokenizer st2 = new StringTokenizer( st.nextToken(), "|");
				int legConid = Integer.parseInt( st2.nextToken() );
				int legRatio = Integer.parseInt( st2.nextToken() ); // signed

				Data legData = m_cons.get( legConid);
				if( legData != null) {
					legs.add( legRatio > 0, Math.abs( legRatio), legData);
				}
				else {
					out( "Error: no leg for conid " + legConid);
				}
			}

			contract.legs( legs);

			if( legs.size() > 0) {
				Under under = m_underMap.get( legs.getFirstSymbol() );
				if( under != null) {
					under.onOpenOrder( order, orderState);
				}
			}
		}
	}

	@Override public void onTrade(int orderId, Contract contract, Execution exec) {
	}

	public void reqMktData( Data data) {
		if( !data.requested() ) {
			//log( "Req mkt data " + data.id() + " " + data.contract() );
			m_mktDataMap.put( data.id(), data);
			m_socket.reqMktData( data.id(), data.contract(), "", false);
			m_tfMktData.setText( "" + m_mktDataMap.size() );
			data.requested( true);
			S.sleep( PAUSE); // avoid exceeding msg limit
		}
	}

	public void desubscribe(String symbol) {
		for( Entry<Integer,Data> entry : m_mktDataMap.entrySet() ) {
			Integer id = entry.getKey();
			Data data = entry.getValue();
			if( data.m_contract.m_symbol.equals( symbol) ) {
				//log( "Desub mkt data " + data.id() + " " + data.contract() );
				m_socket.cancelMktData( id);
				m_mktDataMap.remove( id);
				data.requested( false);
				S.sleep( PAUSE); // avoid exceeding msg limit
			}
		}
		m_tfMktData.setText( "" + m_mktDataMap.size() );
	}

	void show() {
		m_frame.setVisible( true);
		m_frame.setState( Frame.NORMAL);
		m_frame.toFront();
	}

	public void fillPositions(ArrayList<Strike> list) {
		for( Under under : m_underMap.values() ) {
			under.fillPositions( list);
		}
	}

	/** Fill with any Under that has any option or futures position. */
	public void fillExps(ArrayList<Exp> exps) {
		for( Under under : m_underMap.values() ) {
			under.calcFakePositions();
			under.fillExps( exps);
		}
	}

	public void reqContractDetails( Contract contract) {
		reqContractDetails( contract, null);
	}

	void reqContractDetails( Contract contract, final Req req) {
		int reqId = nextId();

        if( req != null) {
        	m_reqs.put( reqId, req);
        }

		log( "Req contract details " + reqId + " " + contract);
		m_socket.reqContractDetails( reqId, contract);
		S.sleep( PAUSE); // avoid exceeding msg limit
	}


	@Override
	public void contractDetails(int reqId, ContractDetails cd) {
		Req req = m_reqs.get( reqId);
		if( req != null) {
			req.process( cd);
		}
		else {
			Under under = m_underMap.get( cd.m_contract.m_symbol);
			if( under != null) {
				under.onContractDetails( reqId, cd);
			}
			else {
				err( "Received contract description for " + cd.m_contract.m_symbol);
			}
		}
	}

	public void subscribePos() {
		for( Under under : m_underMap.values()) {
			under.subscribePos();
		}
	}

	@Override
	public void error(int reqId, int errorCode, String errorMsg) {
		// sec def not found
		if( errorCode == 200) {
			Req req = m_reqs.get( reqId);
			if( req != null) {
				req.process( null);
				return;
			}
		}

		super.error(reqId, errorCode, errorMsg);
	}

	public void test() {
		Under under = m_underMap.get( "AIG");
		under.test();

	}

	public void report(String str) {
		m_rpt.writeln( str);
	}

	public void placeOrder(Order order) {
		m_socket.placeOrder( order.contract(), order);
		order.m_lastUpdateTime = System.currentTimeMillis();
	}

	public void cancelOrder(int id) {
		out( "Canceling order " + id);
		m_socket.cancelOrder( id);
		S.sleep( PAUSE); // avoid exceeding msg limit
	}

	@Override
	public void connectionClosed() {
		out( "Connection closed");
		m_connected = false;
		msg( m_frame, "Connection lost");
	}

	public void put( Data data) {
		m_cons.put( data.conid(), data);
	}

	void onExit() {
		if( m_connected) {
			for( Under under : m_unders) {
				under.cancelAll();
//				if( under.hasOrders() ) {
//					msg( m_frame, "Can't exit with live orders");
//					return;
//				}
			}
		}

		out( "User terminated");
		System.exit( 0);
	}

}

/* Possible strategies
 *
 * Reversal by itself: Buy synth, sell stock (borrow money, lend shares).
 * You will pay interest and dividend because you are short.
 * You are happy if they exercise early because your short position is gone early and you can sell the call.
 * Your risk is that the borrow rates go up.
 * Works with hard-to-borrow stocks when the rate you can earn is more than the rate you pay to IB.
 *
 * Box: reversal plus conversion. Can make a couple of $ on OEX. Bid lower than offer to make an extra $. Check status in U44982.
 * Risk is early assignment. If you get assigned on the put, you don't care.
 * If you get assigned on the call, you either sell the put and hope the rates don't go up,
 * or replace the call.
 * For american style index, you would want to make sure that if you get assigned, you can close out the positions
 * you bought or replace the assigned options and still make money (put or call).
 *
 * Buy conversion, sell EFP (buy future, sell synth).
 * You get better rate buying efp and closing stock position than trading future directly.
 * Do conversion leg first since EFP doesn't change much.
 * Risk is early assignment. Either replace the call or sell the put and ride it out.
 * As whenever selling a call, make sure the put bid can cover the interest and the cost to replace call is < 0.
 *
 * Conversion by itself: lending money.
// next: wait for the fill and then place profit order
 *
 *
 *   incorporate dividends better into rev/con strats as they could stand-alone, like an EFP
 *   hook up buttons for buy/sell efp, with price and size
 *   for european style, you need to ignore the put bid
 *   calculating apr should consider actual cash flow, like box, and also dividends
 *
 *
 *
 * To Do:
 * ignore put bid for european exercise (give checkbox)
 * consider put bid for box of non-european exercise
 * review put bid and replacement cost for all existing options
 * BUG AssignCost must be wrong because it is pos yet put bid could buy the whole conversion
 * Try other index options such as oex. Try ndx options against ndx future.
 * Create order to buy Box when clicking on price.
 * --->>>Keep an eye on how the costToReplace changes for the OEX boxes. Still OK on 11/17.
 * Write a scanner to find hard-to-borrow by looking at efp's. Or maybe look at decliners.

 * Check daily high-5.
 *
 * For Box, prefer higher buy-call strike for stocks and lower buy-call strike for indexes
 * For indexes, you must consider how much you are spending (pct return).
 *
 * Instead of placing all boxes, wait for one to popup and then submit. Or, wait for one to popup, and then submit two sides one penny away.
 *
 * Replace low bid on top pos panel with bid/ask
 *
 * Look back at your trades and notice why you were able to sell high strikes at same price as low strikes.
 *
 * For index options, maybe box is only worthwhile if profit also > cost to liq, since you may have to do that after the puts and calls are worthless.
 *
 * Try MM for all valid conversions and/or synths that are just inside implied prices.
 * Start MM algo of synthetics or of conversion/reversals.
 *
 * Create a scanner for ING situation, and auto-create trades.
 *
 * How does cost to replace put and call change as the underlying changes?
 *
 * Get volatility. Calc 2sd change by midway to expiration. Calc new values for replace call and put.
 *
 * Create a Monte Carlo machine. For all the different ways the stock can move, predict change
 * in options prices using delta. Get assigned when time value of call bid goes below zero (some pct of the
 * I think this is the only way to know if it is really profitable in the long run.
 *
 * For calculating initial price for bid/ask spread, you could use interest rate or
 * you could use model price of the options if it is available through api
 *
 * New Strategies ------------------------------------------------------------------------
 * Buy stock, buy put, collect dividend, exercise put
 * Buy stock sell call if not assigned you collect dividend if assigned you should make money on the call
 * Reversal strategy is not good because rates do in fact go up.
 * Jelly roll
 * Try more OEX and other index option trades.
 *
 * NOTES ---------------------------------------------------------------------------------
 * When selling buying calls (buy synth, reversion, box), prefer the higher strikes because they bring in more cash.
 * So, you really always prefer the higher strikes.
 *
 * With Box, it seems you do not trade inside the bid/ask.
 *
 * Market making the synthetics looks reasonable, but you have to be quick, you can't do one side at a time or you have to switch back and forth very quickly.
 *
 * With NDX Box, you can borrow 200k at 1.25% with only $176 increase in margin. ELV and Available funds actually increased! IB is charging 1.61/1.11/.61/.36
 *
 * To avoid pin risk, close out positions at exactly correct amount for strikes that are very near the stock price.
 *
 * The main lesson is, you don't sell conversions just because the current ib rate is better.
 *
 * Strategies Tried ----------------------------------------------------------------------
 *   AIG Box, one half at a time. Would have worked out if not for the extra reversals you sold.
 *   LUK Buy conversion, sell EFP.  Not good because you shell out a lot of cash for very little return.
 *   OEX Box, which is strange because it is an index option. Worked out nicely.
 *
 * LOG -----------------------------------------------------------------------------------
 * 10/29 bot LUK 20 conv for .14 sld efp for .20. There is currently no dividend but they are priced like they are expecting one.
 * 11/2  bought some OEX BOXES in U44982; took in 123,700 cash, will pay back 123,000 on DEC09; executed these 1/2 at a time, was able to sell the reversal above the bid
 * 11/5  Bot two OEX boxes for 11/20, for -2 each. Bot well below the asking price, should have bid even lower.
 *       some ibm opts were exercised; review ibm trades and interest after nov exp
 * 11/13 did 10 aig conversions, paid .29, and wasn't able to do the reversals because shares were not available for shorting; paid .29 when bid/ask was .34/.34
 * 11/16 bought and sold a bunch of AIG synthetics then sold 700 stock to even things out. Got lucky and made 67.
 * 11/19 bot 10 aig conversions because rate has gone to 40%; bot .10 - .20 lower than the ask; still have a big short position which is killing me
 * 11/23 bot 5 ING synth, sold 5 futures, made .25. This was a major, totally risk-free imbalance that quickly corrected after my trade. Risk free because I didn't sell the stock or a call.
 * 11/23 bot 20 aig conversions to get out of this painful position
 * 12/28 bot 1 jan 15 570/400 oex box -171.65 -173.25 -171.40 .25 better than the ask
 *       repl p/c are around -.50 to -1.00; liq p/c are about 1.53-1.77
 * 12/28 bot 1 12/31 530/500 oex box -30.85 -32.30 -30.45    .40 better
 *       liq is 1.00-1.20, repl is -.22 to -.58, roughly
 *       then accidentally sold it at 32.35; oops, lost 157.80; rebought it again at 30.85
 * 12/28 bot 1 12/31 540/480 oex box -60.95 -62.50 -60.60    .35 better
 * 12/29 bot 1 12/31 545/485 oex box -60.40 -62.10 -60.15    .25 better
 *       bot 1 12/31 545/515 oex box -30.45 -31.70 -30.10    .35 better
 * 12/31 02:03:00:640	BOT 1 OEX 565/375 -191.65 -193.25 -191.20
 * 1/4   BOT 1 OEX box -61.00 -62.55 -60.55   .45
 * 1/4   BOT 1 OEX box -61.10 -62.55 -60.55   .55
 * 1/4   BOT 1 OEX box -61.20 -62.55 -60.55   .65 better
 * 1/4   BOT 1 OEX box -61.30 -62.55 -60.55   .75 better
 * 1/6   1:40:58:625	BOT 1 OEX BAG -40.85 -41.75 -39.80    1.05
 * 1/6   11:41:02:281	BOT 1 OEX BAG -65.85  no bid -64.90    .95
 * 1/6   made a mistake; bot 480/375 vertical spread for 105 then sold for 104.90
 * 1/7   day before expiration; you were not able to close out positions at .20; try .30 next time
 * 1/8   got assigned on 4/6 calls; took quite a while to replace them; incurred many cancel fees (check how much)
 *       index opened like 1.20 down from yesterday, so you took quite a hit to replace those calls;
 *       do act quicker, price it at -.10 rounded down to nearest dime
 *
 * 1/20  Made a huge mistake bot 38 boxes for the wrong expiry, at a shitty price!!!!!
 *
 * 2/3 Your feb options all have time value; you really should not be getting assigned.
 *     Check if you lost money on any of the assignments; the time value should eat up part of the loss.
 *     I did get assigned; should have worked to my benefit.
 *
 * 2/4 Picked up a bunch at all different strikes for .70 on Thursday.
 *
 *
*/
