package db;

import java.util.ArrayList;
import java.util.Collection;

import lib.JtsCalendar;
import lib.S;
import lib.Util;
import db.PriceDb.BarType;

/** This program looks to see how much return you make off your original investment as the dividend changes.
 *  Preferred stocks don't work. */
public class CheckNly {
	public static void main(String[] args) {
		try {
			main_( args);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	static int DIVS_PER_YEAR = 12;
	static String SYM = "HYG";
	
	public static void main_(String[] args) throws Exception {
		
		// read SPY prices
		Collection<Price> temp = PriceDb.INSTANCE.getOrQueryAllPrices( SYM, "20000101", BarType.DAILY); //NLY, TWO, WHX, ARR (pays a monthly dividend)
		ArrayList<Price> list = new ArrayList<Price>( temp);
		
		String first = list.get( 0).date();
		String last = list.get( list.size() - 1).date();
		String time = JtsCalendar.getYears( first, last);
		
		S.err( SYM + " for " + time + " years");
		
		S.err( String.format( "%s\t%s\t%s\t%s\t%s\t%s", 
				"Date    ", "Strt Pr", "Strt %", "Low %", "Avg %", "High %") );
		
		for( int i = 0; i < list.size(); i++) {
			Price price = list.get( i);
			if( price.dividend() > 0) {
				Price prev = list.get( i - 1);
				double startPrice = prev.close();
				double highDiv = getHighDiv( list, i);
				double lowDiv = getLowDiv( list, i);
				double avgDiv = getAvgDiv( list, i);
				
				double startPct = price.dividend() / startPrice * DIVS_PER_YEAR; 
				double lowPct = lowDiv / startPrice * DIVS_PER_YEAR;
				double highPct = highDiv / startPrice * DIVS_PER_YEAR;
				double avgPct = avgDiv / startPrice * DIVS_PER_YEAR;
				S.err( String.format( "%s\t%s\t%s\t%s\t%s\t%s", price.date(), startPrice, Util.fmtPct( startPct), Util.fmtPct( lowPct), Util.fmtPct( avgPct), Util.fmtPct( highPct) ) );
			}
		}
	}

	private static double getLowDiv(ArrayList<Price> list, int start) {
		double min = Double.MAX_VALUE;
		for( int i = start; i < list.size(); i++) {
			Price price = list.get( i);
			if( price.dividend() > 0) {
				min = Math.min( min, price.dividend() );
			}
		}
		return min;
	}

	private static double getHighDiv(ArrayList<Price> list, int start) {
		double max = 0;
		for( int i = start; i < list.size(); i++) {
			Price price = list.get( i);
			if( price.dividend() > 0) {
				max = Math.max( max, price.dividend() );
			}
		}
		return max;
	}

	private static double getAvgDiv(ArrayList<Price> list, int start) {
		ArrayList<Double> divs = new ArrayList<Double>();
		for( int i = start; i < list.size(); i++) {
			Price price = list.get( i);
			if( price.dividend() > 0) {
				divs.add( price.dividend() );
			}
		}
		return Util.average(divs);
	}
}
