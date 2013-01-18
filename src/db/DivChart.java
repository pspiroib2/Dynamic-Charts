package db;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;

import db.DayEntry.PriceType;

public class DivChart extends BarBase {

	public DivChart(ChartFrame frame) {
		super(frame);
	}
	
	int bbuf() { return 16; }	

	protected void paintComponent(Graphics g) {
		super.paintComponent( g);

		Rectangle rect = g.getClipBounds();
		int height = rect.height ;

		m_minY = m_chartData.getMinMaxValue( PriceType.MIN_DIV) *.9;
		m_maxY = m_chartData.getMinMaxValue( PriceType.MAX_DIV);
		m_range = m_maxY - m_minY;

		drawTimeLines( g);
		
		int barIndex = 0;
		for( DayEntry dayEntry : m_chartData.data().values() ) {
			Price price = dayEntry.get( 0).price();
			if( price.dividend() > 0) {

				int start = leftBuf + barIndex * totalBarWidth();
				int top = y( price.dividend() );
				int bot = y( m_minY);//0);
			
				g.setColor( Color.blue);
				g.fillRect( start, top, barWidth, bot - top);
				g.drawString( price.getDividendForDisplay(), start - 10, height - 2);
			}
			
			barIndex++;
		}
	}
	
	private void drawTimeLines(Graphics g) {
		g.setColor( Color.gray);

		int barIndex = 0;
		String last = null;
		for( DayEntry dayEntry : m_chartData.data().values() ) {
			String date = m_frame.days() < 367 ? dayEntry.month() : dayEntry.year();
			if( last != null && !last.equals( date) ) {
				int start = leftBuf + barIndex * totalBarWidth() - 1;
				g.setColor( LineChart.LINE_COLOR);
				g.drawLine( start, 0, start, m_height); 
			}
			last = date;
			barIndex++;
		}
	}

	public Dimension getPreferredSize() {
		return new Dimension( getPrefWidth(), 60);
	}
}
