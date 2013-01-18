package scale.profit;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

import lib.S;

import scale.close.MainBase;


/** Calc profit offset, not considering price increment. */
public class CompareToPut extends MainBase {
	private static final char T = '\t';

	// scale order params
	private int m_orderSize;
	private int m_compSize = 100;
	private double m_topPrice;
	private double m_priceIncrement;

	// simulation params
	private int m_numDays;
	private int m_barSizeInSecs;
	private double m_commission;
	
	// levels
	private int m_numLevels;
	private double[] m_levels;
	private double m_bottomPrice;
	private double m_midpoint;
	
	// debug output
	private boolean m_showRuns = true;
	private boolean m_showOutOfRange = false;
	private boolean m_showBars = false;
	private boolean m_showTrades = false;
	
	// what to show
	private boolean m_showProfits = true;
	private boolean m_showLevels = false;

	private double m_spent;
	private int m_position;

	// helpers
	static String fmt( double v) { return S.fmt2( v); }
	static String pct( double v) { return S.fmt2( v * 100) + "%"; }
	
	public static void main(String[] args) {
		new CompareToPut().run();
	}
	
	private void run() {
		int numRuns = 3000;
		
		out( "num runs\t" + numRuns);
		out( "component size\t" + m_compSize);

		int orderSize 			= 2000; 
		double topPrice 		= 46.09; 
		double priceIncrement 	= .1147; 
		double commission 		= .000; 
		int numDays 			= 9; 
		int barSize 			= 20; 
		double dailyVol 		= .01491; 
		double profitTaker 		= .3; 
		double curPrice 		= 44.92;
				  
		// set vars needed to create levels		
		m_orderSize = orderSize;
		m_topPrice = topPrice;
		m_priceIncrement = priceIncrement;
		m_numLevels = m_orderSize / m_compSize;
		m_levels = new double[m_numLevels];

		// create levels
		double levelPrice = topPrice;
		for( int i = 0; i < m_numLevels; i++) {
			m_levels[i] = levelPrice;
			levelPrice -= m_priceIncrement;
		}
		
		if( m_showLevels) {
			out( "");
			out( "Levels");
			for( int i = 0; i < m_numLevels; i++) {
				out( fmt( m_levels[i]) );
			}
			out( "");
		}
		
		// calc bottom and midpoint of range
		m_bottomPrice = m_levels[m_levels.length - 1];
		m_midpoint = (topPrice + m_bottomPrice) / 2;
		out( "midpoint\t" + fmt( m_midpoint) );
		
		//Bar first = new Bar( "20090101", "09:30:00", m_midpoint, m_midpoint, m_midpoint, m_midpoint, 0, 0, 0);

		// set vars that can be varried after levels are created (minus vol)
		m_commission = commission;
		m_numDays = numDays;
		m_barSizeInSecs = barSize;

		// map PT to list of profits generated with that PT
		TreeMap<Integer,List<Double>> map = new TreeMap<Integer,List<Double>>();

		// run through different sets of data
		for( int run = 0; run < numRuns; run++) {			
			if( run % 100 == 0 && m_showRuns) {
				out( "run " + run);
			}

			// create a set of bars
			Bars bars = new Bars();
			bars.create( curPrice, dailyVol, m_numDays, m_barSizeInSecs);
			
			// calculate the profit for each PT and add to map
			m_spent = 0;
			m_position = 0;
			performRun( bars, dailyVol, profitTaker);
			
			// map close to pnl
			Bar last = bars.get( bars.size() - 1);
			List<Double> pnls = getPnls( map, last.m_close);
			pnls.add( m_spent);
		}
		
		double strike = 45;
		double prem = .77;
		
		// fill in values between low and high
		out( "close\tscale\tcount\tput");
		double last = 0;           // WHY IS THERE ONE EXTRA ENTRY???????????
		int high = getHigh( map);
		for( int key = getLow( map); key <= high; key++) {
			List<Double> pnls = getPnls( map, key);
			double avg = pnls.size() == 0 ? last : S.average( pnls);
			
			double close = key / 100.0;
			double putPerShare = close < strike ? prem + close - strike : prem;
			double put = putPerShare * orderSize;
				
			out( fmt( key / 100.0) + "\t" + fmt( -avg) + "\t" + pnls.size() + "\t" + fmt( put) );
			last = avg;
		}
	}

	void performRun( Bars bars, double dailyVol, double profitTaker) {
		Bar first = bars.get( 0);
		
		// set iFilled to lowest filled level
		int iFilled = -1;
		for( int i = 0; i < m_numLevels; i++) {
			if( m_levels[i] >= first.open()) {
				iFilled = i;
				displayTrade( 0, true, first.open());
			}
		}
		
		int sharesSold = 0;
		for( Bar bar : bars) {
			boolean tookProfit = false;
			
			if( m_showBars) {
				out( "" + bar);
			}

			// we hit the profit-taker?
			double lowestFilled = iFilled >= 0 ? m_levels[iFilled] : 0;
			double salePrice = lowestFilled + profitTaker - .0001;
			while( lowestFilled != 0 && bar.m_high >= salePrice) {
				displayTrade( bar.longTime(), false, salePrice);
				sharesSold += m_compSize;
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
							displayTrade( bar.longTime(), true, highestOpen);
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
			
			if( bar.m_low < m_bottomPrice || bar.m_high > m_topPrice) {
				if( m_showOutOfRange) {
					out( "bar " + bar + " is out of range");
					System.exit( 0);
				}
			}
		}
		
		// sell remaining position at closing price
		displayTrade( Long.MAX_VALUE, false, m_position, bars.last().m_close);

		double comm = sharesSold * m_commission * 2; // consider commission for buy side as well. ???
		double total = sharesSold * profitTaker - comm;
	}
	
	private int getLow(TreeMap<Integer, List<Double>> map) {
		int low = Integer.MAX_VALUE;
		for( int v : map.keySet() ) {
			if( v < low) {
				low = v;
			}
		}
		return low;
	}
	
	private int getHigh(TreeMap<Integer, List<Double>> map) {
		int high = 0;
		for( int v : map.keySet() ) {
			if( v > high) {
				high = v;
			}
		}
		return high;
	}
	
	private List<Double> getPnls(TreeMap<Integer, List<Double>> map, double close) {
		int key = (int)((close + .005) * 100);
		return getPnls( map, key);
	}
	
	private List<Double> getPnls(TreeMap<Integer, List<Double>> map, int key) {
		
		List<Double> pnls = map.get( key);
		if( pnls == null) {
			pnls = new ArrayList<Double>();
			map.put( key, pnls);
		}
		return pnls;
	}

	void displayTrade( long longTime, boolean buy, double price) {
		displayTrade( longTime, buy, m_compSize, price);
	}
	
	void displayTrade( long longTime, boolean buy, int size, double price) {
		if( m_showTrades) {
			String side = buy ? "Buy" : "Sell";
			String str = "  " + S.timeAsStr( longTime) + " " + side + " " + size + " at " + S.fmt2( price);
			out( str);
		}
		
		if( buy) {
			m_spent += price * size + m_commission;
			m_position += size;
		}
		else {
			m_spent -= price * size + m_commission;
			m_position -= size;
		}
	}
}
