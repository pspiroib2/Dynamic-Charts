package db;

import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;

public abstract class AnyChart extends JComponent {
	protected double m_minY;
	protected double m_maxY;
	protected double m_range;
	protected int m_height; // height of chart area
	protected int m_bottom; // very bottom
	protected int m_width;
	private int m_tbuf;

	protected void paintComponent(Graphics g) {
		Rectangle rect = g.getClipBounds();
		m_tbuf = tbuf();		
		m_height = rect.height - vbuf();
		m_width = rect.width;
		m_bottom = rect.height;
	}
	
	public int y(double price) {
		double yy = (price - m_minY) / m_range * m_height;
		return (int)(m_height - yy + m_tbuf + .5);
	}
	
	abstract int tbuf();
	abstract int bbuf();
	int vbuf() { return tbuf() + bbuf(); }
}
