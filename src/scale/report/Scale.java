/**
 * 
 */
package scale.report;

import java.util.Collection;

import lib.S;
import db.Price;
import db.PriceDb;
import db.PriceDb.BadSymbol;

class Scale {
	static final String C = ",";

	private Trades m_allTrades = new Trades(); // these are un-split trades
	private Trades m_openingTrades = new Trades();  // these are split trades, needed for fifo and lifo real pnl
	
	private Underlying m_underlying;
	private int m_conid;
	private String m_secType;
	private int m_mult;

	private int m_position;
	private double m_price;
	private double m_maxMktVal;
	private double m_totalReal;
	private boolean m_reportedTrade;
	private double m_oldUnreal; // unreal pnl from before we started reporting trades so we can calculate the difference
	
	Underlying underlying()	{ return m_underlying; }
	Trades allTrades() 		{ return m_allTrades; }
	Trades openingTrades() 	{ return m_openingTrades; }
	int position()			{ return m_position; }
	double mktVal() 		{ return m_position * m_price * m_mult; }
	double totalReal() 		{ return m_totalReal; }
	double unreal()			{ return mktVal() - costBasis(); }
	String symbol()			{ return m_underlying.symbol(); }
	String secType()		{ return m_secType; }
	int conid()				{ return m_conid; }
	
	Scale( Underlying underlying, int conid, String secType, int mult) {
		m_underlying = underlying;
		m_conid = conid;
		m_secType = secType.intern();
		m_mult = mult;
		
		underlying.add( this);
	}

	public void applyTrade( FilterPanel filterPanel, Trade trade) {
		// price or dividend?
		if( trade.isPrice() || trade.isDividend() ) {
			if( m_position != 0) {  // ignore prices and dividends if position is zero
				// update price or real pnl 
				if( trade.isPrice() ) {
					m_price = trade.price();
				}
				else {
					double realPnl = trade.price() * m_position; 
					trade.realPnl( realPnl);
					m_totalReal += realPnl;
				}
				trade.position( m_position);
				trade.mktVal( mktVal() );
				filterPanel.reportSplitTrade( trade);
			}
			return;
		}
		
		int absPosition = Math.abs( m_position);

		// trade flips the sign of the position?
		if( m_position < 0 && trade.isBuy()  && trade.size() > absPosition ||
		    m_position > 0 && trade.isSell() && trade.size() > absPosition) { 

			// do closing trade
			int closingSize = absPosition;
			Trade closingTrade = trade.splitTrade( closingSize);
			trade_( filterPanel, closingTrade); // pass closingTrade so it won't get marked
			
			// do opening trade
			int openingSize = trade.size() - closingSize;
			Trade openingTrade = trade.splitTrade( openingSize);
			trade_( filterPanel, openingTrade);
		}
		
		// trade is opening or closing, not both
		else {
			trade_( filterPanel, trade.clone() ); // must use clone so real pnl can be tracked properly if trade is in multiple filters
		}
		
		m_allTrades.add( trade);
	}
	
	/** Trade is either opening or closing but not both. */
	public void trade_(FilterPanel filterPanel, Trade trade) {
		boolean isOpening = m_position >= 0 && trade.isBuy() || m_position <= 0 && trade.isSell();
		
		// if this is the first reported trade, save unreal pnl
		boolean reportTrade = m_reportedTrade ? true : filterPanel.filter().report( trade);
		if( reportTrade && !m_reportedTrade) {
			m_oldUnreal = unreal();
			m_reportedTrade = true;
		}

		m_position += trade.signedSize();
		
		// don't update the price for stock trades that were done as part of option exercise
		// because the stock is traded at the strike price has nothing to do with the market
		// price unless this is the first trade in which case we have nothing better to use
		if( !trade.stockExercise() || m_price == 0) {
			m_price = trade.price(); // probably never do this and only take prices from yahoo
		}
		
		// record mkt val and position as of this trade
		trade.mktVal( mktVal() );
		trade.position( m_position);
		
		double realPnl = 0;
		double pt = 0;

		// opening trade?
		if( isOpening) {
			m_openingTrades.add( trade);
			m_maxMktVal = Math.max( m_maxMktVal, mktVal() );
		}
		
		// closing trade?
		else {
			Trade closingTrade = trade;
			while( closingTrade.remainingSize() > 0) {
				Trade openingTrade = m_openingTrades.findBestMatch(closingTrade);
				if( openingTrade == null) {
					S.err( "Error: no opening trade for " + closingTrade);
					return; // happens if there is no associated opening trade
				}
				
				int size = Math.min( openingTrade.remainingSize(), closingTrade.remainingSize() );
				realPnl += closingTrade.income( size) + openingTrade.income( size);
				
				openingTrade.reduceBy( size);
				closingTrade.reduceBy( size);
				
				pt = closingTrade.price() - openingTrade.price(); //report this. ps
			}
			
			closingTrade.realPnl( realPnl);
			
			if( reportTrade) {
				m_totalReal += realPnl;
			}
		}
		
		// write to symbol file, e.g. IBM.csv. amd GUI
		String str = trade.getStr() + C + m_position + C + mktVal() + C + S.fmt2( pt);
		m_underlying.os().writeln( str);

		filterPanel.reportSplitTrade( trade);
	}
	
	/** Add daily closing prices and/or dividends to m_allTrades and re-sort. */
	public void getPrices( boolean doPrices, boolean divs) {
		if( m_secType != "STK" || m_allTrades.size() < 2) {
			return;
		}
		
		try {
			Trade first = m_allTrades.get( 0);
			Trade last = m_allTrades.lastTrade();
			String symbol = m_underlying.symbol();
			
			String lastDate = m_position != 0 ? db.SymbolDb.YESTERDAY : last.dateStr();
			
			Collection<Price> prices = PriceDb.INSTANCE.getOrQueryAllPrices( symbol, first.dateStr(), lastDate, PriceDb.BarType.DAILY);
			
			for( Price price : prices) {
				if( price.dividend() > 0) {
					if( divs) {
						Trade trade = new Trade( price.dateTime(), Side.DIVIDEND, 0, m_conid, m_conid, 1, symbol, symbol, m_secType, price.dividend(), 0, null, null);
						m_allTrades.add( trade);
					}
				}
				else {
					if( doPrices) {
						Trade trade = new Trade( price.dateTime(), Side.PRICE, 0, m_conid, m_conid, 1, symbol, symbol, m_secType, price.close(), 0, null, null);
						m_allTrades.add( trade);
					}
				}
			}
		} 
		catch (BadSymbol e) {
			S.err( "Error: Not adding prices or dividends for " + m_underlying); 
			//e.printStackTrace();
		}
	}

	void addTrade( Trade trade) { 
		m_allTrades.add( trade); 
		m_position += trade.signedSize();
	}
	
	void reset() {
		m_position = 0;
	}

	private double costBasis() {
		double costBasis = 0;
		int pos = 0;
		
		for( Trade trade : m_openingTrades) {
			pos += trade.signedRemainingSize();
			costBasis += trade.spent2();
		}
		
		// sanity check; opening trade positions should match open position
		if( pos != m_position) {
			S.err( "Error: positions don't match for " + symbol() + " " + m_position + " " + pos);
		}
		
		return costBasis;
	}
	public double oldUnreal() { return m_oldUnreal; }
}
