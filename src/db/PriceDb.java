package db;

import java.util.Collection;
import java.util.HashMap;


/** Prices for all symbols. */
public class PriceDb {
	public static PriceDb INSTANCE = new PriceDb();
	private HashMap<Key,SymbolDb> m_map = new HashMap<Key,SymbolDb>(); // maps symbol+Time(d/w/m) to SymbolDb
	
	/** Get all prices for symbol from date onward, from either file (if available) or download.
	 *  @param startDate is YYYYMMDD */ 
	public Collection<Price> getOrQueryAllPrices( String symbol, String startDate, BarType barType) throws BadSymbol {
		return getOrQueryAllPrices( symbol, startDate, SymbolDb.YESTERDAY, barType);
	}
	
	/** @param startDate is YYYYMMDD */
	public Collection<Price> getOrQueryAllPrices( String symbol, String startDate, String endDate, BarType barType) throws BadSymbol {
		Key key = new Key( symbol, barType);
		
		SymbolDb db = m_map.get( key);
		if( db == null) {
			db = new SymbolDb( symbol, barType);
			m_map.put( key, db);
		}
		return db.getPrices( startDate, endDate);
	}
	
	private void write() {
		for( SymbolDb db : m_map.values() ) {
			db.writeToFile();
		}
	}

	static void out(String string) {
		System.out.println( string);
	}


	public enum BarType { 
		DAILY( "d", 250), 
		WEEKLY( "w", 52),  // 50? ps 
		MONTHLY( "m", 12);
		
		private String m_letter;
		private int m_barsInYear;

		public String getChar() 		{ return m_letter; }
		public double barsInYear() 		{ return m_barsInYear; }

		BarType( String letter, int barsInYear) {
			m_letter = letter;
			m_barsInYear = barsInYear;
		}
	};

	private static class Key {
		private String m_symbol;
		private BarType m_time;

		public Key(String symbol, BarType time) {
			m_symbol = symbol;
			m_time = time;
		}
		
		@Override public boolean equals(Object obj) {
			Key other = (Key)obj;
			return m_symbol.equals( other.m_symbol) &&
			       m_time == other.m_time;
		}
		
		@Override public int hashCode() {
			return m_symbol.hashCode();
		}
	}

	public static void main(String[] args) {
		try {
			PriceDb db = new PriceDb();
			Collection<Price> prices = db.getOrQueryAllPrices( "IBM", "20110606", BarType.DAILY);
			for( Price price : prices) {
				out( "" + price);
			}
		} 
		catch (BadSymbol e) {
			e.printStackTrace();
		}
	}

	public static class BadSymbol extends Exception {}
}
