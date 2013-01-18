package db;

import java.awt.Dimension;
import java.awt.Graphics;

import db.DayEntry.PriceType;

public class VolChart extends BarBase {

	public VolChart(ChartFrame frame) {
		super( frame);
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent( g);
		
		m_minY = m_chartData.getMinMaxValue( PriceType.MIN_VOL);
		m_maxY = m_chartData.getMinMaxValue( PriceType.MAX_VOL);
		m_range = m_maxY - m_minY;

		int barIndex = 0;
		for( DayEntry dayEntry : m_chartData.data().values() ) {
			Price price = dayEntry.get( 0).price();
			
			int start = leftBuf + barIndex * totalBarWidth();
			
			int y1 = y( price.vol() );

			if( price.open() < price.close() ) {
				g.setColor( GREEN);
			}
			else {
				g.setColor( RED);
			}
			g.fillRect( start, y1, barWidth, m_height - y1);
			
			barIndex++;
		}
	}
	
	@Override public Dimension getPreferredSize() {
		return new Dimension( getPrefWidth(), 100);
	}

}
