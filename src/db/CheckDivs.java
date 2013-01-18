package db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import lib.S;
import lib.Util;
import db.PriceDb.BadSymbol;
import db.PriceDb.BarType;

/** Facinating--this program shows that you can make, on average, about seven
 *  cents by buy at the close, collecting the dividend, and selling at the open. */
// Yahoo: -PA
public class CheckDivs {
	static String[] dow = { "MMM", "AA", "AXP", "T", "BAC", "BA", "CAT", "CVX", "CSCO", "DD", "XOM", "GE", /*"HPQ",*/ "HD", "INTC", "IBM", "JNJ", "JPM", "KFT", "MCD", "MRK", "MSFT", "PFE", "PG", "KO", "TRV", "UTX", "VZ", "WMT", "DIS" };
	static String[] lows = { "GGN", "FGP", "MFA", "STD", "CMO", "ANH", "HTS", "OZM", "VOC", "DX", "FSC", "PT", "ERF", "PSEC", "KCAP", "DOM", "TICC", "PNNT", "BKCC", "CPLP", "YPF", "OXF", "CEL", "VLCCF", "AGNC", "AINV", "CIM", "ESEA", "RSO", "IVR", "BFR", "NRGY", "CYS", "FTE", "LINC", "LPHI", "BMA", "TEF", "NLY", "CBK" };
	static String[] syms = { "AMID", "BBD", "BBEP", "BCBP", "BHI", "C", "CLP", "CNL", "CWT", "DCOM", "DNKN", "EEP", "ETN", "FE", "HCP", "HVB", "IDA", "INTC", "JNS", "KKR", "LSBK", "MCEP", "MCI", "MPV", "MWE", "NATI", "NBL", "NE", "NOR", "NPD", "NSEC", "NYB", "PBNY", "PEBO", "RGP", "RRMS", "SIFI", "SJW", "SO", "SRCE", "SXI", "TMP", "TX", "WABC", "WBCO", "WBS", "WLT", "WMK", "WSTG", "WVFC" };	
	static HashMap<String,Price> spyMap = new HashMap<String,Price>();
	static TreeMap<Double,Double> m_map = new TreeMap<Double,Double>();
	
	public static void main(String[] args) {
		try {
			main_( args);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main_(String[] args) throws BadSymbol {
		// read SPY prices
		Collection<Price> spys = PriceDb.INSTANCE.getOrQueryAllPrices( "DIA", "20000101", BarType.DAILY);
		for( Price price : spys) {
			spyMap.put( price.date(), price);
		}
		
		Stats.showHeader();
		
		stat( false);
		//stat( true);
	}
	
	private static void stat(boolean useSpy) throws BadSymbol {
		String date = "20000101";
		Stats all = new Stats( "ALL", useSpy);
		
		for( String sym : dow) {
			Stats single = new Stats( sym, useSpy);
			single.stat( sym, date);
			single.show();
			
			all.stat( sym, date);
		}
		
		for( String sym : lows) {
			Stats single = new Stats( sym, useSpy);
			single.stat( sym, date);
			single.show();
			
			all.stat( sym, date);
		}
		
		for( String sym : syms) {
			Stats single = new Stats( sym, useSpy);
			single.stat( sym, date);
			single.show();
			
			all.stat( sym, date);
		}
		
		all.show();
		
		for( Entry<Double,Double> entry : m_map.entrySet() ) {
			S.err( "%s\t%s", Util.fmt( entry.getKey() ), Util.fmt( entry.getValue() ) );
		}
	}
	
	static class Stats {
		double totalGain = 0;
		double biggestWin = 0;
		double biggestLoss = 0;
		int winners = 0;
		int losers = 0;
		ArrayList<Double> gains = new ArrayList<Double>();
		private String m_symbol;
		private boolean m_adj;
		double m_totalDivs;
	
		public Stats(String sym, boolean adj) {
			m_symbol = sym;
			m_adj = adj;
		}

		void stat(String symbol, String start) throws BadSymbol {
			Collection<Price> prices = PriceDb.INSTANCE.getOrQueryAllPrices( symbol, start, BarType.DAILY);
			double last = 0;
			double spyLast = 0;
			for( Price price : prices) {
				Price spy = spyMap.get( price.date() );
				if( spy == null) {
					S.err( "Error: no SPY price for " + price.date() );
					continue;
				}
				
				if( price.dividend() > 0 && spy.dividend() == 0) {
					double spyMult = last / spyLast;
					
					double gain = m_adj
						? price.open() + price.dividend() - last + spyMult * (spyLast - spy.open() ) - .02
						: price.open() + price.dividend() - last - .01;
						
					if( gain > -2 && gain < 2) { 
						totalGain += gain;
						if( gain > 0) {
							winners++;
						}
						else {
							losers++;
						}
						
						biggestWin = Math.max( biggestWin, gain);
						biggestLoss = Math.min( biggestLoss, gain);
						gains.add( gain);
						m_totalDivs += price.dividend();
						m_map.put( price.dividend(), gain);
					}
				}
				last = price.close();
				spyLast = spy.close();
			}
		}

		static void showHeader() {
			S.err("Symbol\tAdj\tNumber\tTotal profit\tAverage profit\tStd dev\tWinners\tLosers\tBiggest\tSmallest\tAvg Div");
		}

		void show() {
			double total = winners + losers;
			double avgDiv = m_totalDivs / total;
			S.err( "%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s",
				m_symbol,
				m_adj,
				(int)total,
				Util.fmt( totalGain), 
				Util.fmt( totalGain / total),
				Util.fmt( Util.stddev( gains) ),
				Util.fmtPct0( winners / total), 
				Util.fmtPct0( losers / total),
				Util.fmt( biggestWin),
				Util.fmt( biggestLoss),
				Util.fmt( avgDiv)
			);
		}			
	}
}
