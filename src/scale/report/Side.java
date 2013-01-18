package scale.report;

import lib.S;

public enum Side { 
	BUY, SELL, SETTLE, PRICE, DIVIDEND;
	
	static Side create( String str, int size) {
		if( str.substring( 0, 3).equals( "BUY") ) {
			assert size > 0;
			return BUY;
		}
		if( str.substring( 0, 4).equals( "SELL") ) {
			assert size < 0;
			return SELL;
		}
		if( str.equals( "ALLOC") ) {
			return size > 0 ? BUY : SELL;
		}
		S.err( "Invalid side " + str);
		System.exit( 0);
		return BUY;
	}
}
