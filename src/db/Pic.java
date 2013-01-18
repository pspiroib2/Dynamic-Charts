///**
// * 
// */
//package db;
//
//import java.awt.Color;
//import java.awt.Graphics;
//import java.awt.Rectangle;
//
//import javax.swing.JComponent;
//
//import lib.S;
//import lib.Util;
//import db.DayEntry.Entry;
//import db.DayEntry.PriceType;
//
//class Pic extends JComponent {
//	private static final int vbuf = 10;
//	static Color LINE_COLOR = new Color( 197, 197, 197);
//	static Color DATE_COLOR = Color.gray;
//	
//	private ChartFrame m_frame;
//	private double m_maxYValue; // absolute value of max value of any line
//	private ChartData m_chartData;
//	private int m_height;			// height of window or printed page
//	private int m_width;		 	// width of window or printed page
//	
//	Pic( ChartFrame frame) {
//		m_frame = frame;
//		m_chartData = m_frame.chartData();
//	}
//
//	@Override protected void paintComponent(Graphics g) {
//		Rectangle rect = g.getClipBounds();
//		m_height = rect.height ;
//		m_width = rect.width;
//		
//		// draw zero line
//		int midY = m_height / 2;
//		g.setColor( Color.black);
//		g.drawLine( 0, midY, rect.width, midY);
//		
//		drawTimeLines( g, m_chartData);
//		
//		// determine of any of the Attribs has a negative factor; if we we chart prices, otherwise we chart percentages
//		PriceType priceType = m_chartData.usePrices() ? PriceType.CLOSE : PriceType.CHANGE;
//
//		m_maxYValue = m_chartData.getMaxValue( priceType, MaxType.NULL);
//		
//		for( int i = 0; i < m_chartData.getSize(); i++) {
//			if( !m_chartData.getAttribs( i).agg() ) { // don't draw lines part of the agg
//				drawPriceLine( g, i, m_chartData, priceType);
//			}
//		}
//	}
//
//	private void drawTimeLines(Graphics g, ChartData chartData) {
//		g.setColor( Color.gray);
//
//		int x = 0;
//		String last = null;
//		for( DayEntry dayEntry : chartData.data().values() ) {
//			String date = m_frame.days() < 367 ? dayEntry.month() : dayEntry.year();
//			if( last != null && !last.equals( date) ) {
//				g.setColor( LINE_COLOR);
//				g.drawLine( x( x), 0, x( x), m_height); 
//				g.setColor( DATE_COLOR);
//				g.drawString( date, x( x+1), m_height / 2 + 12); 
//			}
//			last = date;
//			x++;
//		}
//	}
//
//	void drawPriceLine(Graphics g, int i, ChartData chartData, PriceType priceType) {
//		int lastx = 0;
//		double lasty = 0;
//
//		g.setColor( chartData.getColor( i) );
//
//		int x = 0;
//		for( DayEntry dayEntry : chartData.data().values() ) {
//			Entry entry = dayEntry.get( i);
//			
//			double y = entry.getVal( priceType);
//
//			g.drawLine( x( lastx), y( lasty), x( x), y( y) );
//			
//			// draw dividends
//			if( entry.div() != 0 && m_frame.divs() ) {
//				g.setColor( DATE_COLOR);
//				String str = String.format( "%s (%s)", S.fmt( entry.div() ), S.fmtPct( entry.divPct() ) );
//				g.drawString( str, x( x), y( y) + 30);
//				g.setColor( chartData.getColor( i) );
//			}
//
//			lastx = x;
//			lasty = y;
//
//			x++;
//		}
//
//		// draw last price
//		String val = priceType == PriceType.CLOSE ? Util.fmt( lasty) : Util.fmtPct( lasty);
//		g.drawString( val, x( lastx) + 2, y( lasty) + 4);
//		
//		// draw trend line
//		if( m_frame.trend() ) {
//			double[] ys = m_chartData.calcSlope( i);
//			g.drawLine( x( 0), y( ys[0]), x( lastx), y( ys[1]) );
//		}
//	}
//
//	/** @param y is in the range 0 to m_max, gets scaled to m_height / 2 */
//	private int y(double y) {
//		return yy( (int)(y / m_maxYValue * (m_height-vbuf) / 2) );
//	}
//
//	/** @param y 0 is middle of chart, pos goes up, get converted to chart coordinates */
//	private int yy(int y) {
//		return m_height / 2 - y;
//	}
//
//	/** x is entry index */
//	private int x( int x) {
//		return x * (m_width - 50) / m_chartData.data().size();
//	}
//}
