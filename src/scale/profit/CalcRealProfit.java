package scale.profit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.Map.Entry;

import lib.S;

import scale.close.MainBase;

import com.ib.client.Contract;

/** Calc profit offset, not considering price increment. */
public class CalcRealProfit extends MainBase {
	static final CalcSimProfit INSTANCE = new CalcSimProfit();
	static final Random RND = new Random( System.currentTimeMillis() );

	protected List<Bar> m_bars = new ArrayList<Bar>();
	protected int m_resps;
	protected int m_days;
	protected ArrayList<Trade> m_trades = new ArrayList<Trade>();
	protected TreeMap<Double,ArrayList<Double>> m_map = new TreeMap<Double,ArrayList<Double>>();

	// for real only
	String m_symbol = "RSX";
	String[] m_endDates = new String[] { "20090123", "20090130", "20090206", "20090213" };
	String m_queryLen = "5 D";

	// sim data for RSX
	int m_orderSize = 50000;
	int m_compSize = 100;
	double m_startingPrice = 30;
	double m_priceIncrement = .15;
	double m_lowPT = .11;
	double m_highPT = .11;
	double m_dailyVol = .02; // my calc; try with theirs
	double m_dailyAvgChange = -.0003342; // use this. ???

	private int m_numDays = 25;
	
	int m_numLevels = m_orderSize / m_compSize;
	double[] m_levels = new double[m_numLevels];
	
	public static void main(String[] args) {
		//INSTANCE.run( args);
	}
	
	private void run(String[] args) {
		// create levels
		double price = m_startingPrice;
		for( int i = 0; i < m_numLevels; i++) {
			m_levels[i] = price;
			price -= m_priceIncrement;
		}

		calcRealProfit( args);
	}

	public void calcRealProfit( String[] args) {
		super.connect(0);
		
		// set ScaleTrader params
//		m_symbol = "QQQQ";
//		int orderSize = 2000;
//		int compSize = 100;
//		double startingPrice = 31.5;
//		double stepDown = .15;
//		m_bot = .11;
//		m_top = .11;

		for( int i = 0; i < m_numLevels; i++) {
			m_levels[i] = m_startingPrice;
			m_startingPrice -= m_priceIncrement;
		}
	}
	
	public void onConnected(int orderId) {

		Timer timer = new Timer();
		
		for( String date : m_endDates) {
			final String tmp = date;
			timer.schedule( new TimerTask() {
				public void run() {
					req( tmp, m_queryLen);
				}
			}, 1000 * m_days++);
		}
	}
	
	void req( String date, String len) {
		Contract contract = new Contract();
		contract.m_symbol = m_symbol;
		contract.m_secType = "STK";
		contract.m_exchange = "SMART";
		contract.m_currency = "USD";
		contract.m_primaryExch = "ISLAND";

		out( "requesting bars for " + m_symbol + " ending " + date);
		m_socket.reqHistoricalData( m_id++, contract, date + " 16:00:00 EST", len, "1 min", "trades", 0, 1);
	}
	
	public void onHistoricalBar(int reqId, Bar bar) {
		m_bars.add( bar);
	}

	public void onFinishedHistoricalData(int reqId) {
		out( "Finished " + ++m_resps);
		if( m_resps == m_days) {
			processBars();
			showResults();
			System.exit( 0);
		}
	}
	
	void processBars() {
		for( double profitTaker = m_lowPT; profitTaker <= m_highPT; profitTaker += .01) { 
			process( profitTaker);
		}
	}
	
	/** Calculate profit for given profit-taker.
	 *  Map profit-taker to list of profits for that profit-taker. */
	void process( double profitTaker) {
		Bar first = m_bars.get( 0);
		
		int sharesSold = 0;
		
		// set iFilled to lowest filled level
		int iFilled = -1;
		for( int i = 0; i < m_numLevels; i++) {
			if( m_levels[i] >= first.open()) {
				iFilled = i;
			}
		}
		
		for( Bar bar : m_bars) {
			boolean tookProfit = false;

			// we hit the profit-taker?
			double lowestFilled = iFilled >= 0 ? m_levels[iFilled] : 0;
			double salePrice = lowestFilled + profitTaker - .0001;
			while( lowestFilled != 0 && bar.m_high >= salePrice) {
				displayTrade( bar, false, salePrice);
				sharesSold += 100;
				tookProfit = true;
				iFilled--;
				lowestFilled = iFilled >= 0 ? m_levels[iFilled] : 0;
				salePrice = lowestFilled + profitTaker - .0001;
			}
			
			if( !tookProfit) {
				while( true) {
					int iOpen = iFilled + 1;
					if( iOpen < m_numLevels) {
						double highestOpen = m_levels[iOpen] + .0001;
						if( bar.m_low <= highestOpen) {
							displayTrade( bar, true, highestOpen);
							iFilled++;
						}
						else {
							break;
						}
					}
					else {
						break;
					}
				}
			}
		}
		
		//displayTrades();
		double comm = sharesSold * .005 * 2; // consider commission for buy side as well
		double total = sharesSold * profitTaker - comm;
		double totalPerDay = total / m_numDays;
		//out( "Sold " + sharesSold + " shares @ " + OStream.fmt2( profitTaker) + " for total profit " + OStream.fmt2( total) );
		//out( "\t" + OStream.fmt2( profitTaker) + "\t" + OStream.fmt2( total) );
		
		ArrayList<Double> profits = m_map.get( profitTaker);
		if( profits == null) {
			profits = new ArrayList<Double>();
			m_map.put( profitTaker, profits);
		}
		profits.add( totalPerDay);
	}
	
	void showResults() {
		out( "\t" + "PT" + "\t" + "Avg profit/day" + "\t" + "Std De");
		for( Entry<Double, ArrayList<Double>> entry : m_map.entrySet() ) {
			double profitTaker = entry.getKey();
			ArrayList<Double> profits = entry.getValue();
			double avg = S.average(profits);
			double stddev = S.stddev(profits, avg);
			out( "\t" + S.fmt2( profitTaker) + "\t" + S.fmt2( avg) + "\t" + S.fmt2( stddev) );
		}
	}
	
	private void displayTrades() {
		for( Trade trade: m_trades) {
			out( "" + trade);
		}
	}
	
	void displayTrade( Bar bar, boolean m_buy, double m_price) {
		String side = m_buy ? "Buy" : "Sell";
		String str = bar + " " + side + " 100 at " + S.fmt2( m_price);
//		out( str);
	}

	static class Trade {
		String m_date;
		boolean m_buy;
		double m_price;
		private String m_time;
		
		Trade( String date, String time, boolean buy, double price) {
			m_date = date;
			m_time = time;
			m_buy = buy;
			m_price = price;
		}
		
		public String toString() {
			String side = m_buy ? "Buy" : "Sell";
			return m_date + " " + m_time + " " + side + " 100 at " + S.fmt2( m_price);
		}
	}
	
	
	
	
	
}
