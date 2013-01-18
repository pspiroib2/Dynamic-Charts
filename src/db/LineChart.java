/**
 * 
 */
package db;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import lib.S;
import lib.Util;
import db.DayEntry.Entry;
import db.DayEntry.PriceType;

class LineChart extends AnyChart {
	private static final int LBUF = 50;
	private static final int RBUF = 50;
	private static final int HBUF = LBUF + RBUF;
	
	static Color LINE_COLOR = new Color( 180, 180, 180);
	static Color DIV_COLOR = new Color(118, 118, 118);
	
	private ChartFrame m_frame;
	private ChartData m_chartData;
	
	int tbuf() { return 10; }
	int bbuf() { return 35; }
	
	LineChart( ChartFrame frame) {
		m_frame = frame;
		m_chartData = m_frame.chartData();
		
		addMouseListener( new MouseAdapter() {
		    public void mouseReleased(MouseEvent e) {
		    	double pct = (e.getX() - LBUF) / (double)(m_width-HBUF);
		    	String date = m_chartData.getDate( pct);
		    	if( date != null) {
		    		m_frame.setStartDate( date);
		    	}
		    }
		});
	}

	@Override protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		// determine of any of the Attribs has a negative factor; if we we chart prices, otherwise we chart percentages
		PriceType priceType = m_frame.usePrices() ? PriceType.MAX_CLOSE : PriceType.MAX_CHANGE;
		
		if( m_frame.usePrices() ) {
			m_minY = m_chartData.getMinMaxValue( PriceType.MIN_CLOSE);
			m_maxY = m_chartData.getMinMaxValue( PriceType.MAX_CLOSE);
		}
		else {
			m_minY = m_chartData.getMinMaxValue( PriceType.MIN_CHANGE);
			m_maxY = m_chartData.getMinMaxValue( PriceType.MAX_CHANGE);
		}
		m_range = m_maxY - m_minY;
		
		// draw horizontal line at starting point of first chart
		if( m_chartData.getSize() > 0) {
			double val = m_chartData.data().firstEntry().getValue().get( 0).getValue( priceType);
			int y = y( val);
			g.setColor( LINE_COLOR);
			g.drawLine( 0, y, m_width, y);
		}

		// draw vertical time lines
		drawTimeLines( g, m_chartData);
		
		// draw price lines
		for( int i = 0; i < m_chartData.getSize(); i++) {
			if( !m_chartData.getAttribs( i).agg() ) { // don't draw lines part of the agg
				drawPriceLine( g, i, m_chartData, priceType);
			}
		}
	}

	private void drawTimeLines(Graphics g, ChartData chartData) {
		g.setColor( Color.gray);

		int x = 0;
		String last = null;
		for( DayEntry dayEntry : chartData.data().values() ) {
			String date = m_frame.days() < 367 ? dayEntry.month() : dayEntry.year();
			if( last != null && !last.equals( date) ) {
				g.setColor( LINE_COLOR);
				g.drawLine( x( x), 0, x( x), m_height); 
				g.drawString( date, x( x+1), m_height / 2 + 12); 
			}
			last = date;
			x++;
		}
	}

	void drawPriceLine(Graphics g, int i, ChartData chartData, PriceType priceType) {
		int lastx = 0;
		int lasty = 0;
		double lastVal = 0;
		int divOffset = 0;
		
		int count = 0;
		for( DayEntry dayEntry : chartData.data().values() ) {
			Entry entry = dayEntry.get( i);
			
			double price = entry.getVal( priceType);
			
			if( price < m_minY) {
				S.err( "yes");
			}

			int x = x( count);
			int y = y( price);
			
			if( count == 0) {
				lastx = x;
				lasty = y;
			}
			
			g.setColor( chartData.getColor( i) );
			g.drawLine( lastx, lasty, x, y);
			
			// draw dividends for first chart only
			if( entry.div() != 0 && m_frame.divs() && i == 0) {
				g.setColor( DIV_COLOR);
				int yy = m_bottom - 5 - divOffset;
				g.drawString( entry.price().getDividendForDisplay(), x - 10, yy);
				g.drawLine( x, y + 15, x, yy - 12);
				
				divOffset = divOffset == 0 ? 15 : 0;
			}

			lastx = x;
			lasty = y;
			lastVal = price;

			count++;
		}

		// draw last price
		String val = priceType == PriceType.MAX_CLOSE ? Util.fmt( lastVal) : Util.fmtPct( lastVal);
		g.drawString( val, lastx + 2, lasty + 4);
		
		// draw trend line
		if( m_frame.trend() ) {
			double[] ys = m_chartData.calcSlope( i);
			g.drawLine( x( 0), y( ys[0]), lastx, y( ys[1]) );
		}
	}

	/** x is entry index */
	private int x( int x) {
		return LBUF + x * (m_width - HBUF) / m_chartData.data().size();
	}
}
//move dividend down