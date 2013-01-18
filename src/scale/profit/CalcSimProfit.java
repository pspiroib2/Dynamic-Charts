package scale.profit;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map.Entry;

import lib.S;

import scale.close.MainBase;

/*
 *             TO DO
 *             
 *             Calculate bar size based on vol and profit-taker; what is smallest time period
 *             where a trade could occur?
 *             
 *             PROBLEM: why does profit go up when bar size goes down?
 *             
 *             HOW to incorporate higher volatility at lower profit-takers?
 *             
 *             MAKE constant for COMM
 *             
 *             I predict a much bigger range should make it so pt doesn't matter, because you won't have
 *             prices going out of range off the top
 */

/** Calc profit offset, not considering price increment. */
public class CalcSimProfit extends MainBase {
	private static final char T = '\t';

	// scale order params
	private int m_orderSize;
	private int m_compSize = 100;
	private double m_topPrice;
	private double m_priceIncrement;

	// simulation params
	private int m_numRuns = 2000;
	private double[] m_profitTakersRangeAndStep;
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
	private boolean m_showLevels = true;

	// helpers
	static String fmt( double v) { return S.fmt2( v); }
	static String pct( double v) { return S.fmt2( v * 100) + "%"; }
	
	public static void main(String[] args) {
		new CalcSimProfit().run();
	}
	
	private void run() {
		m_profitTakersRangeAndStep = new double[] { .2, .2, .03 };

		out( "num runs\t" + m_numRuns);
		out( "component size\t" + m_compSize);
		out( "PT range is " + m_profitTakersRangeAndStep[0] + " to " + m_profitTakersRangeAndStep[1] + " by " + m_profitTakersRangeAndStep[2]);
		
		// calcForTopPrice( 2500, 42.16, .0767, .005, 3, 10, .01103); // qqqq
		// calcForTopPrice( 2900, 36.23, .052, .005, 3, 10, .01027);
		// calcForTopPrice( 13200, 6.36, .01, .005, 3, 10, .03873);
		//calcForTopPrice( 2100, 41.97, .41, .005, 3, 10, .05170);
		calcForTopPrice( 2000, 46.09, .1147, .000, 9, 20, .01491, 44.92); // QQQQ best if .48
		//calcForTopPrice( 8000, 9.54, .013, .005, 3, 10, .02661);
	}

	public void calcForTopPrice( int orderSize, double topPrice, double priceIncrement, double commission, int numDays, int barSize, double dailyVol, double curPrice) {
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
		
		//Bar first = new Bar( "20090101", "09:30:00", m_midpoint, m_midpoint, m_midpoint, m_midpoint, 0, 0, 0);

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
			for( double profitTaker = m_profitTakersRangeAndStep[0]; profitTaker <= m_profitTakersRangeAndStep[1] + .001; profitTaker += m_profitTakersRangeAndStep[2]) { 
				calcForProfitTaker( bars, mapPTToProfits, dailyVol, profitTaker);
			}
		}
		
		showResults( mapPTToProfits, dailyVol);
	}

	/** Calculate profit for given profit-taker.
	 *  Map profit-taker to list of profits for that profit-taker. 
	 * @param mapPTToProfits 
	 * @param bars 
	 * @param profitTaker2 */
	void calcForProfitTaker( Bars bars, TreeMap<Double, ArrayList<Double>> mapPTToProfits, double dailyVol, double profitTaker) {
		//out( "  profit-taker " + profitTaker);
		
		Bar first = bars.get( 0);
		
		// set iFilled to lowest filled level
		int iFilled = -1;
		for( int i = 0; i < m_numLevels; i++) {
			if( m_levels[i] >= first.open()) {
				iFilled = i;
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
		
		double comm = sharesSold * m_commission * 2; // consider commission for buy side as well
		double total = sharesSold * profitTaker - comm;
		double totalPerDay = total / m_numDays;
		
		ArrayList<Double> profits = mapPTToProfits.get( profitTaker);
		if( profits == null) {
			profits = new ArrayList<Double>();
			mapPTToProfits.put( profitTaker, profits);
		}
		profits.add( totalPerDay);
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
		
		// show best PT
		out( "top" + T + "mid" + T + "bot" + T + "increment" + T + "commission" + T + "num days" + T + "bar size" + T + "daily vol" + T + "ideal PT" + T + "real pnl" + T + "SD");
		out( "" + fmt( m_topPrice) + T + fmt( m_midpoint) + T + fmt( m_bottomPrice) + T + m_priceIncrement + T + m_commission + T + m_numDays + T + m_barSizeInSecs + T + pct( dailyVol) + T + fmt( bestProfitTaker) + T + fmt( bestProfit)  + T + fmt( bestSd) );
	}
	
	void displayTrade( long longTime, boolean m_buy, double m_price) {
		if( m_showTrades) {
			String side = m_buy ? "Buy" : "Sell";
			String str = "  " + S.timeAsStr( longTime) + " " + side + " " + m_compSize + " at " + S.fmt2( m_price);
			out( str);
		}
	}
}
