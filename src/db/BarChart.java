package db;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import lib.S;
import db.DayEntry.PriceType;

public class BarChart extends BarBase {

	public BarChart(ChartFrame frame) {
		super( frame);
		
		
		addMouseMotionListener( new MouseMotionListener() {
			@Override public void mouseMoved(MouseEvent e) {
				int index = getBarIndex( e.getX() );
				if (index != -1) {
					DayEntry bar = m_chartData.getBar( index);
					if (bar != null) {
					}
				}
			}
			@Override public void mouseDragged(MouseEvent e) {
			}
		});
		addMouseListener( new MouseAdapter() {
		    public void mouseReleased(MouseEvent e) {
				int index = getBarIndex( e.getX() );
				if (index != -1) {
					DayEntry bar = m_chartData.getBar( index);
					if (bar != null) {
						m_frame.setStartDate( bar.date() );
					}
				}
		    }
		});
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent( g);

		m_minY = m_chartData.getMinMaxValue( PriceType.MIN_LOW);
		m_maxY = m_chartData.getMinMaxValue( PriceType.MAX_HIGH);
		m_range = m_maxY - m_minY;

		drawTimeLines( g);
		
		int barIndex = 0;
		for( DayEntry dayEntry : m_chartData.data().values() ) {
			Price price = dayEntry.get( 0).price();
			
			int start = leftBuf + barIndex * totalBarWidth();
			
			g.setColor( Color.black);
			g.drawLine( start + half, y( price.high() ), start + half, y( price.low() ) );
			
			int yOpen = y( price.open() );
			int yClose = y( price.close() );

			// draw bar
			if( yOpen < yClose) {
				g.setColor( price.dividend() == 0 ? RED : Color.red);
				g.fillRect( start, yOpen, barWidth, yClose - yOpen);
			}
			else {
				g.setColor( price.dividend() == 0 ? GREEN : Color.green);
				g.fillRect( start, yClose, barWidth, yOpen - yClose);
			}
			
			barIndex++;
		}
	}
	
	private int getBarIndex( int x) {
		int barIndex = 0;
		int extra = leftBuf + barWidth + barSpace() / 2;
		for( DayEntry dayEntry : m_chartData.data().values() ) {
			if (barIndex * totalBarWidth() + extra > x) {
				return barIndex;
			}
			barIndex++;
		}
		return -1;
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
				g.drawString( date, start + 1, m_height); 
			}
			last = date;
			barIndex++;
		}
	}

	public Dimension getPreferredSize() {
		return new Dimension( getPrefWidth() , 10);
	}
}
