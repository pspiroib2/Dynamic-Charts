package scale.profit;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import lib.OStream;
import lib.S;


public class Bars extends ArrayList<Bar> {
	private static final double HOURS_PER_DAY = 6.5;  // assume some trading outside rth
	private static final int SECS_PER_DAY = (int)(HOURS_PER_DAY * 60 * 60);
	private final String m_symbol; 
	
	public Bar last() 			{ return get( size() -1); }
	public double close() 		{ return last().close(); }
	public String symbol() 		{ return m_symbol; }
	public Bar first() 			{ return get( 0); }

	public Bars() {
		this( "unknown");
	}
	
	public Bars(String symbol) {
		m_symbol = symbol;
	}
	
	public void write( String filename) {
		try {
			OStream os = new OStream( filename, false);
			
			for( Bar bar : this) {
				os.writeln( bar.getWriteString() );
			}
			
			os.close();
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Bars read( String filename, String symbol) {
		try {
			Bars bars = new Bars( symbol);
			
			BufferedReader reader = new BufferedReader( new FileReader( filename) );
			String line = reader.readLine();
			while( line != null) {
				Bar bar = Bar.createFromWriteString( line);
				bars.add( bar);
				
				line = reader.readLine();
			}
			reader.close();
			return bars;
		}
		catch( FileNotFoundException e1) {
			return null;
		}
		catch( Exception e2) {
			e2.printStackTrace();
			return null;
		}
	}
	
	public void create(double price, double dailyVol, int numDays, int barSizeInSecs) {
		int barsPerDay = SECS_PER_DAY / barSizeInSecs;
		double volPerTimeUnit = dailyVol / Math.sqrt( barsPerDay);
		int timeUnitInMS = barSizeInSecs * 1000;

		clear();

		// create the bars
		for( int day = 0; day < numDays; day++) {
			long dateTime = S.getLongTime( (20090101 + day) + " " + "09:30:00");
			
			for( int j = 0; j < barsPerDay; j++) {
				Bar bar = new Bar( dateTime, price, price, price, price, 0, 0, 0);
				add( bar);
				
				// get a pct change with mean 0 and stddev volPerTimeUnit
				//double pctChange = OStream.next( volPerTimeUnit);
				double pctChange = S.RND.nextBoolean() ? volPerTimeUnit : -volPerTimeUnit;

				// apply pct change to price
				price *= Math.exp( pctChange);
				
				dateTime += timeUnitInMS;
			}
		}
	}
	
	public Result calcVol( int period) {
		List<Double> vals = new ArrayList<Double>();
		
		Bar prev = get( 0);
		
		for( int i = 0; i < size(); i += period) {
			Bar bar = get( i);
			
			double periodReturn = Math.log( bar.m_close / prev.m_close) * 100; // continuously compounded return
			
			vals.add( periodReturn);
			prev = bar;
		}
		
		return S.both( vals);
	}

	/** Returns daily vol over last numDays days. */
	public double calcDailyVol( int numDays) {
		List<Double> vals = new ArrayList<Double>();
		
		int start = Math.max( size() - numDays, 0);
		
		Bar prev = get( 0);
		
		for( int i = start; i < size(); i++) {
			Bar bar = get( i);
			
			double periodReturn = Math.log( bar.m_close / prev.m_close) * 100; // continuously compounded return
			
			vals.add( periodReturn);
			prev = bar;
		}
		
		return S.both( vals).stdDev();
	}

	@Override public String toString() {
		return m_symbol + " " + super.toString();
	}
	
	public void show() {
		S.err( m_symbol);
		
		for( Bar bar : this) {
			S.err( "" + bar);
		}
	}
	public void show(int start, int n) {
		for( int i = start; i < size(); i+= n) {
			S.err( "" + get( i) );
		}
	}
}
