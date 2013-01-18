package db;

import java.awt.Color;

class Attribs {
	private String m_symbol;
	private String m_name;
	private Color m_color;
	private double m_vol;
	private double m_beta;
	private double m_alpha;
	private double m_sharpe;
	private double m_rSquared;
	private boolean m_agg; 		// if true, included in agg line 
	private boolean m_missing; 	// if true, means that some data is missing
	private String m_filename;
	private double m_aggShares; // set by program
	private double m_mult;      // set by user

	public boolean isAgg() 			{ return m_symbol == ChartData.AGG; } // returns true for the aggregate row
	public boolean agg()			{ return m_agg; } // returns true for normal row if it is part of the agg
	public Color color() 			{ return m_color; }
	public String name() 			{ return m_name; }
	public double beta()			{ return m_beta; }
	public double sharpe()			{ return m_sharpe; }
	public double alpha()			{ return m_alpha; }
	public double rSquared()		{ return m_rSquared; }
	public boolean missing()		{ return m_missing; }
	public String filename() 		{ return m_filename; }
	public double aggShares()		{ return m_mult != 0 ? m_mult : m_aggShares; }
	public double vol() 			{ return m_vol; }
	public String symbol() 			{ return m_symbol; }

	public void missing(boolean v) 	{ m_missing = v; }
	public void agg( boolean v) 	{ m_agg = v; }
	public void toggleAgg() 		{ m_agg = !m_agg; }
	public void filename(String v) 	{ m_filename = v; }
	public void mult( double v)		{ m_mult = v; }
	public void beta(double v) 		{ m_beta = v; }
	public void rSquared(double v) 	{ m_rSquared = v; }
	public void aggShares(double v) { m_aggShares = v; }
	public void alpha(double v) 	{ m_alpha = v; }
	public void sharpe(double v) 	{ m_sharpe = v; }
	public void vol(double v) 		{ m_vol = v; }

	public Attribs(String symbol, String name, Color color) {
		m_symbol = symbol;
		m_name = name;
		m_color = color;
	}

	public String symbolForDisplay() {
		return m_missing ? m_symbol + '*' : m_symbol; 
	}
}
