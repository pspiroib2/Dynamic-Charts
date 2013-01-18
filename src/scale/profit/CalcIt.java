package scale.profit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

/** Calc profit offset, not considering price increment. */
public class CalcIt {
	static final Random RND = new Random( System.currentTimeMillis() );
	static final int SECS_PER_DAY = (int)(6.5 * 60 * 60);
	static final int NUM_RUNS = 100;
	static final int NUM_DAYS = 1;
	static final int barSizeInSecs = 10; // calculate this. ???
	
	public static void main(String[] args) {
		int orderSize = 2000;
		int compSize = 100;
		double startingPrice = 31.5;
		double priceIncrement = .15;
		double profitTaker = .11;
		double dailyVol = .021;
		
		Result result = calcSimProfit( orderSize, compSize, startingPrice, priceIncrement, profitTaker, dailyVol);
		out( "" + result);
	}
	
	public static Result calcSimProfit(int orderSize, int compSize, double startingPrice, double priceIncrement, double profitTaker, double dailyVol) {
		final TreeMap<Double,ArrayList<Double>> map = new TreeMap<Double,ArrayList<Double>>();
		final List<Bar> bars = new ArrayList<Bar>();
		
		// output
		boolean showBars = false;
		boolean showTrades = true;

		// levels
		int numLevels = orderSize / compSize;
		double[] levels = new double[numLevels];
		
		// create levels
		double levelPrice = startingPrice;
		for( int i = 0; i < numLevels; i++) {
			levels[i] = levelPrice;
			levelPrice -= priceIncrement;
		}		
		
		// time
		int barsPerDay = SECS_PER_DAY / barSizeInSecs;
		int timeSliceInMS = barSizeInSecs * 1000;
		
		int numSteps = numLevels - 1;
		double bottom = startingPrice - numSteps * priceIncrement;
		double midpoint = (startingPrice + bottom) / 2;
		double volPerTimeUnit = dailyVol / Math.sqrt( barsPerDay);  // 1 min bars   // get real vol of january
		int numBars = barsPerDay * NUM_DAYS;
		Bar first = new Bar( "20090101", "09:30:00", midpoint);

		// set iFilled to lowest filled level
		int iFilled = -1;
		for( int i = 0; i < levels.length; i++) {
			if( levels[i] >= midpoint) {
				iFilled = i;
			}
		}

		// debug
		out( "numBars: " + numBars);		
		out( "top: " + startingPrice);
		out( "mid: " + midpoint);
		out( "bot: " + bottom);
		out( "vol: " + volPerTimeUnit);
		out( "");
		
		ArrayList<Double> profits = new ArrayList<Double>();

		// run through different sets of data
		for( int run = 0; run < NUM_RUNS; run++) {
			out( "run " + run);

			bars.clear();
			bars.add( first);

			// create the bars
			double price = first.price();
			long time = first.m_longTime;
			for( int i = 1; i < numBars; i++) {
				boolean moveUp = RND.nextBoolean();
				double offset = price * volPerTimeUnit;
				price = moveUp ? price + offset : price - offset;

				time += timeSliceInMS;

				Bar bar = new Bar( time, price);
				bars.add( bar);
			}
			
			// calculate the profit for each PT and add to map
			process( levels, bars, profitTaker, showBars, map, iFilled, profits);
		}
		
		double avg = average(profits);
		double stddev = stddev(profits, avg);
		
		return new Result( avg, stddev);
	}

	/** Calculate profit for given profit-taker.
	 *  Map profit-taker to list of profits for that profit-taker. 
	 * @param levels 
	 * @param bars 
	 * @param map 
	 * @param iFilled */
	static void process( double[] levels, List<Bar> bars, double profitTaker, boolean showBars, TreeMap<Double, ArrayList<Double>> map, int iFilled, ArrayList<Double> profits) {
		int sharesSold = 0;
		
		for( Bar bar : bars) {
			boolean tookProfit = false;
			
			if( showBars) {
				out( "" + bar);
			}

			// we hit the profit-taker?
			double lowestFilled = iFilled >= 0 ? levels[iFilled] : 0;
			double salePrice = lowestFilled + profitTaker - .0001;
			while( lowestFilled != 0 && bar.price() >= salePrice) {
				sharesSold += 100;
				tookProfit = true;
				iFilled--;
				lowestFilled = iFilled >= 0 ? levels[iFilled] : 0;
				salePrice = lowestFilled + profitTaker - .0001;
			}
			
			if( !tookProfit) {
				while( true) {
					int iOpen = iFilled + 1;
					if( iOpen < levels.length) {
						double highestOpen = levels[iOpen] + .0001;
						if( bar.price() <= highestOpen) {
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
		
		double comm = sharesSold * .005 * 2; // consider commission for buy side as well
		double total = sharesSold * profitTaker - comm;
		double profitForDay = total / NUM_DAYS;
		
		profits.add( profitForDay);
	}
	
	static void out( String str) {
		//S.err( str);
		System.out.println( str);
	}
	
	
	
	
	
	private static String pad(int hours) {
		return hours < 10 ? "0" + String.valueOf( hours) : String.valueOf( hours);
	}

	public static String timeAsStr( long timeInMillis) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( timeInMillis);
		int hour = cal.get( Calendar.HOUR);
		hour = hour == 0 ? 12 : hour;
		return pad( hour) + ":" + pad( cal.get( Calendar.MINUTE) ) + ":" + pad( cal.get( Calendar.SECOND) );
	}

    public static long getLongTime( String str) { 
        // parse date/time passed in 
        int year   = Integer.parseInt( str.substring( 0, 4) ); 
        int month  = Integer.parseInt( str.substring( 4, 6) ) - 1; // month is 0 based 
        int day    = Integer.parseInt( str.substring( 6, 8) ); 
        int hour   = Integer.parseInt( str.substring( 9, 11) ); 
        int minute = Integer.parseInt( str.substring( 12, 14) ); 
        int second = Integer.parseInt( str.substring( 15, 17) ); 

        // create calendar with date/time passed in 
        GregorianCalendar cal = new GregorianCalendar(); 
        cal.set( year, month, day, hour, minute, second); 
        cal.set( GregorianCalendar.MILLISECOND, 0); 
        
        return cal.getTimeInMillis();
    } 
	
	public static double stddev(List<Double> highs, double avg) {
		List<Double> squares = new ArrayList<Double>();
		
		for( double high : highs) {
			double dif = high - avg;
			double sqr = Math.pow( dif, 2);
			squares.add( sqr);
		}
		
		double avgSquare = average( squares);
		double root = Math.sqrt( avgSquare);
		return root;
	}

	public static double average(List<Double> highs) {
		double total = 0;
		for( double high : highs) {
			total += high;
		}
		return total / highs.size();
	}

	private static class Bar implements Comparable<Bar> {
		private long m_longTime; // in ms
		private double m_price;
		
		double price() { return m_price; }

		public Bar(String date, String time, double price) {
			this( getLongTime( date + " " + time), price);
		}
		
		public Bar(long timeInMs, double price) {
			m_longTime = timeInMs;
			m_price = price;
		}

		public String toString() {
			return "Bar: " + timeAsStr( m_longTime) + " " + m_price;
		}

		public int compareTo(Bar o) {
			if( m_longTime < o.m_longTime) {
				return -1;
			}
			if( m_longTime > o.m_longTime) {
				return 1;
			}
			return 0;
		}
	}
	
}
