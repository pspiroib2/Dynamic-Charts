package scale.profit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import lib.S;

import scale.close.MainBase;

public class CalcChart extends MainBase {
	private static final char T = '\t';

	// scale order params
	private int m_orderSize;
	private int m_compSize = 100;
	private double m_topPrice;
	private double m_priceIncrement;

	// simulation params
	private int m_numRuns = 5000;
	private int m_numDays;
	private int m_barSizeInSecs;
	private double m_commission;
	
	// levels
	private int m_numLevels;
	private double[] m_levels;
	private double m_bottomPrice;
	private double m_midpoint;
	
	// debug output
	private boolean m_showRuns = false;
	private boolean m_showOutOfRange = false;
	private boolean m_showBars = false;
	private boolean m_showTrades = false;
	
	// what to show
	private boolean m_showProfits = true;

	HashMap<Integer,ArrayList<Double>> m_map = new HashMap<Integer,ArrayList<Double>>(); 
	private double m_spent;
	private int m_position;

	// helpers
	static String fmt( double v) { return S.fmt2( v); }
	static String pct( double v) { return S.fmt2( v * 100) + "%"; }
	
	public static void main(String[] args) {
		new CalcChart().run();
	}
	
	private void run() {
		calcForTopPrice( 1600, 45.40, .09, .005, 4, 10, .01491, .4, 44.34); // QQQQ
	}

	public void calcForTopPrice( int orderSize, double topPrice, double priceIncrement, double commission, int numDays, int barSize, double dailyVol, double pt, double curPrice) {
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
		
		// calc bottom and midpoint of range
		m_bottomPrice = m_levels[m_levels.length - 1];
		m_midpoint = (topPrice + m_bottomPrice) / 2;
		
		// set vars that can be varried after levels are created (minus vol)
		m_commission = commission;
		m_numDays = numDays;
		m_barSizeInSecs = barSize;

		// map PT to list of profits generated with that PT
		TreeMap<Double,ArrayList<Double>> mapPTToProfits = new TreeMap<Double,ArrayList<Double>>();

		// run through different sets of data
		for( int run = 0; run < m_numRuns; run++) {			
			if( run % 100 == 0 && m_showRuns) {
				out( "run " + run);
			}

			// create a set of bars
			Bars bars = new Bars();
			bars.create( curPrice, dailyVol, m_numDays, m_barSizeInSecs);
			
			// calculate the profit for each PT and add to map
			calcProfit( bars, mapPTToProfits, dailyVol, pt);
		}
		
		showResults( mapPTToProfits, dailyVol);
	}

	/** Calculate profit for given profit-taker.
	 *  Map profit-taker to list of profits for that profit-taker. 
	 * @param mapPTToProfits 
	 * @param bars 
	 * @param profitTaker2 */
	void calcProfit( Bars bars, TreeMap<Double, ArrayList<Double>> mapPTToProfits, double dailyVol, double profitTaker) {
		//out( "  profit-taker " + profitTaker);
		
		Bar first = bars.get( 0);
		
		m_spent = 0;
		m_position = 0;
		
		// set iFilled to lowest filled level
		int iFilled = -1;
		for( int i = 0; i < m_numLevels; i++) {
			if( m_levels[i] >= first.open()) {
				iFilled = i;
				trade( 0, true, first.open(), m_compSize);
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
			double realSalePrice = lowestFilled + profitTaker;
			double salePrice = lowestFilled + profitTaker - .0001;// use method for comp instead. ???
			while( lowestFilled != 0 && bar.m_high >= salePrice) {
				trade( bar.longTime(), false, realSalePrice, m_compSize);
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
							trade( bar.longTime(), true, highestOpen, m_compSize);
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
		
		double comm = sharesSold * m_commission * 2; // consider commission for buy side as well
		double total = sharesSold * profitTaker - comm; //real pnl
		double totalPerDay = total / m_numDays;
		
		// map PT to list of real pnl
		ArrayList<Double> profits = mapPTToProfits.get( profitTaker);
		if( profits == null) {
			profits = new ArrayList<Double>();
			mapPTToProfits.put( profitTaker, profits);
		}
		profits.add( totalPerDay);
		
		// sell remaining shares at close
		double close = bars.get( bars.size() - 1).m_close;
		trade( 2000000000, false, close, m_position);
		
		// map ending price to list of pnl
		int key = (int)Math.round( close * 100);
		ArrayList<Double> pnls = m_map.get( key);
		if( pnls == null) {
			pnls = new ArrayList<Double>();
			m_map.put( key, pnls);
		}
		pnls.add( -m_spent);
	}

	/** Show the average profit for each PT and find the PT with the highest average profit. */  
	void showResults( TreeMap<Double, ArrayList<Double>> mapPTToProfits, double dailyVol) {
		double bestProfit = 0;
		double bestProfitTaker = 0;
		double bestSd = 0;
		
		// show profits header
		if( m_showProfits) {
			out( "\t" + "PT" + "\t" + "Avg profit/day" + "\t" + "stddev");
		}
		
		// show profits and calcuate best PT
		for( Entry<Double, ArrayList<Double>> entry : mapPTToProfits.entrySet() ) {
			double profitTaker = entry.getKey();
			ArrayList<Double> profits = entry.getValue();
			Result result = S.both(profits);
			
			if( m_showProfits) {
				out( "\t" + S.fmt2( profitTaker) + "\t" + S.fmt2( result.avg()) + "\t" + S.fmt2( result.stdDev()) );
			}
			
			if( result.avg() > bestProfit) {
				bestProfit = result.avg();
				bestProfitTaker = profitTaker;
				bestSd = result.stdDev();
			}
		}
		
		out( "top" + T + "mid" + T + "bot" + T + "increment" + T + "commission" + T + "num days" + T + "bar size" + T + "daily vol" + T + "ideal PT" + T + "real pnl" + T + "SD");
		out( "" + fmt( m_topPrice) + T + fmt( m_midpoint) + T + fmt( m_bottomPrice) + T + m_priceIncrement + T + m_commission + T + m_numDays + T + m_barSizeInSecs + T + pct( dailyVol) + T + fmt( bestProfitTaker) + T + fmt( bestProfit)  + T + fmt( bestSd) );
		
		// show total pnl per closing price
		out( "");
		int min = minKey( m_map);
		int max = maxKey( m_map);
		
		double last = 0;
		double total = 0;
		int count = 0;
		for( int close = min; close <= max; close++) {
			ArrayList<Double> pnls = m_map.get( close);
			double pnl = pnls != null ? S.average( pnls) : last;
			int size = pnls != null ? pnls.size() : 0;
			out( S.fmt2( close / 100.0) + T + S.fmt2( pnl) + T + size);
			last = pnl;
			
			if( pnls != null) {
				total += pnl;
				count++;
			}
		}
		out( "average" + T + S.fmt2( total / count) );
//		for( Entry<Integer,ArrayList<Double>> entry : m_map.entrySet() ) {
//			double close = entry.getKey() / 100.0;
//			double pnl = OStream.average( entry.getValue() );
//			out( OStream.fmt2( close) + "\t" + OStream.fmt2( pnl) );
//		}
	}

	private int minKey(HashMap<Integer, ArrayList<Double>> map) {
		int min = Integer.MAX_VALUE;
		for( Integer key : map.keySet() ) {
			if( key < min) {
				min = key;
			}
		}
		return min;
	}
	
	private int maxKey(HashMap<Integer, ArrayList<Double>> map) {
		int max = 0;
		for( Integer key : map.keySet() ) {
			if( key > max) {
				max = key;
			}
		}
		return max;
	}
	
	/** Returns the amount spent. 
	 * @param size */
	void trade( long longTime, boolean buy, double price, int size) {
		double amount = price * size;
		if( buy) {
			m_spent += amount; 
			m_position += size;
		}
		else {
			m_spent -= amount;
			m_position -= size;
		}
		if( m_showTrades) {
			String side = buy ? "Buy" : "Sell";
			String str = "  " + S.timeAsStr( longTime) + " " + side + " " + size + " at " + fmt( price) + "  " + m_position + " " + fmt( m_spent);
			out( str);
		}
	}
}

// change bars to have real open/close