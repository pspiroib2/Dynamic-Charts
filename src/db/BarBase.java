package db;

import java.awt.Color;

import db.PriceDb.BarType;

public class BarBase extends AnyChart {
	protected static final int leftBuf = 3;
	protected static final int rightBuf = 3;
	protected static final int barWidth = 3;
	protected static final int half = (barWidth - 1) / 2;
	protected static final Color GREEN = new Color( 0, 200, 0);
	protected static final Color RED = new Color( 200, 0, 0);

	protected ChartFrame m_frame;
	protected ChartData m_chartData;

	int tbuf() { return 0; }
	int bbuf() { return 0; }

	public BarBase(ChartFrame frame) {
		m_frame = frame;
		m_chartData = m_frame.chartData();
	}

	protected int getPrefWidth() {
		return leftBuf + totalBarWidth() * m_chartData.data().entrySet().size() + rightBuf;
	}

	protected final int totalBarWidth() { 
		int space = m_chartData.barType() == BarType.DAILY ? 1 : 3;
		return barWidth + space + ChartFrame.m_spaceBetweenBars;  
	}
	
	protected int barSpace() {
		return totalBarWidth() - barWidth;
	}
}
