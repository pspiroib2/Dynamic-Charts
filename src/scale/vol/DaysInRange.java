package scale.vol;

import java.util.Random;

import scale.close.MainBase;

public class DaysInRange extends MainBase {
	static final DaysInRange INSTANCE = new DaysInRange();
	static final int NUM_RUNS = 2000;
	static final int DAYS = 30;
	
	public static void main(String[] args) {
		INSTANCE.run();
	}
	
	static final double m_vol = .02173;
	double m_top = 31.5;
	double m_bot = 28.5;
	double m_mid = (m_top + m_bot) / 2;
	double m_step = .15; 
	int m_numLevels = (int)((m_top - m_bot) / .15 + 1.1);
	
	static final Random RND = new Random( System.currentTimeMillis() );
	
	void run() {
		out( "top of range is " + m_top);
		out( "starting point is " + m_mid);
		out( "bottom of range is " + m_top);
		out( "step is " + m_step);
		out( "numLevels is " + m_numLevels);
		
		int totalDaysInRange = 0;
		double totalLoss = 0;
		//int max = (int)Math.pow( 2, days + 1);
		for( int val = 0; val < NUM_RUNS; val++) {
			int run = RND.nextInt();
			
			out( "");
			out( "Run " + val + " with " + run);
			double price = m_mid;
			int daysInRange = 0;
			for( int bit = 0; bit < DAYS; bit++) {
				int test = 1 << bit;
				boolean set = (run & test) > 0;
				double offset = price * m_vol;
				price = set ? price + offset : price - offset;				
				if( price >= m_bot && price <= m_top) {
					daysInRange++;
				}
			}
			out( "Days in range: " + daysInRange + "  ending price: " + price);
			totalDaysInRange += daysInRange;
		}
		double avgDaysInRange = (double)totalDaysInRange / NUM_RUNS;
		double avgLoss = totalLoss / NUM_RUNS;
		eol();
		out( "Average days in range: " + avgDaysInRange);
		out( "Average position loss: " + avgLoss);
	}
	
	static void eol() {
		System.out.println( "");
	}
	
	static void out( boolean i) {
		System.out.print( i ? 1 : 0);
	}
}
