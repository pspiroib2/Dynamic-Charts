package db;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import arb.WindowAdapter;

import lib.JtsCalendar;
import lib.S;
import lib.Settings;
import lib.Util;
import db.HtmlButton.HtmlCheckBox;
import db.HtmlButton.HtmlRadioButton;
import db.PriceDb.BadSymbol;

public class ChartFrame extends JFrame implements Printable {
	static HeaderRenderer HEADER = new HeaderRenderer();
	static int m_spaceBetweenBars = 0;

	private final Settings m_settings = new Settings();
	private final HtmlCheckBox m_adjusted = new HtmlCheckBox( "Adjusted");
	private final HtmlCheckBox m_divs = new HtmlCheckBox( "Divs");
	private final HtmlCheckBox m_trend = new HtmlCheckBox( "Trend");
	private final HtmlCheckBox m_bars = new HtmlCheckBox( "Bars");
	private final HtmlButton m_etf = new HtmlButton( "ETF");
	private final ChartData m_chartData = new ChartData();
	private final JTextField m_symbol = new JTextField( 7);
	private final DatesPanel m_datesPanel = new DatesPanel();
	private final LegendPanel m_legendPanel = new LegendPanel( this);
	private final CorrModel m_corrModel = new CorrModel();
	private final JTable m_corrTable = new CorrTable( m_corrModel);
	private final LineChart m_lineChart = new LineChart( this);
	private final JScrollPane m_barScroll;
	private String m_startDate = "20110101";
	private int m_dayss;//dont store, calc
	JPanel m_barPanel = new JPanel( new BorderLayout() ); // holds bar chart and volume
	static int count = 0;

	// getters
	ChartData chartData() 		{ return m_chartData; }
	int days() 					{ return m_dayss; }
	boolean divs()				{ return m_divs.isSelected(); }
	boolean trend()				{ return m_trend.isSelected(); }
	
	public static void main(String[] args) {
		if( S.isNotNull( System.getProperty("http.proxyHost") ) ) {
        	S.err( String.format( "proxy is %s:%s", 
        			System.getProperty("http.proxyHost"),
        			System.getProperty("http.proxyPort") ) );
		}
		
		new ChartFrame();
	}

	public ChartFrame() {
		count++;

		HtmlButton spaceButton = new HtmlButton( "<>") {
			protected void onClicked() {
				m_spaceBetweenBars++;
				ChartFrame.this.repaint();
			}
		};
		HtmlButton newFrame = new HtmlButton( "New") {
			protected void onClicked() {
				onNewChart();
			}
		};
		HtmlCheckBox print = new HtmlCheckBox( "Print") {
			protected void onClicked() {
				onPrint();
			}
		};
		HtmlCheckBox yahoo = new HtmlCheckBox( "Yahoo") {
			protected void onClicked() {
				onYahoo();
			}
		};

		JPanel topPanel = new JPanel();
		topPanel.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0));
		topPanel.add( m_symbol);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( m_etf);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( m_adjusted);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( m_divs);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( m_trend);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( m_bars);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( spaceButton);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( m_datesPanel);
		topPanel.add( print);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( yahoo);
		topPanel.add( Box.createHorizontalStrut( 8));
		topPanel.add( newFrame);
		
		JPanel corrPanel = new JPanel( new BorderLayout() );
		corrPanel.add( m_corrTable.getTableHeader(), BorderLayout.NORTH);
		corrPanel.add( m_corrTable);
		
		JPanel infoPanel = new JPanel( new BorderLayout() );
		infoPanel.add( m_legendPanel, BorderLayout.WEST);
		infoPanel.add( corrPanel, BorderLayout.EAST);
		
		JPanel northPanel = new JPanel( new BorderLayout() );
		northPanel.add( topPanel, BorderLayout.NORTH);
		northPanel.add( infoPanel, BorderLayout.SOUTH);
		
		m_datesPanel.m_YTD.setSelected( true);
		
		m_barPanel.add( new BarChart( this) );
		m_barPanel.add( new DivChart( this), BorderLayout.NORTH);
		m_barPanel.add( new VolChart( this), BorderLayout.SOUTH);
		m_barScroll = new JScrollPane( m_barPanel);

		add( northPanel, BorderLayout.NORTH);
		add( m_lineChart);
		
		setSize( 800, 400);
		setVisible( true);
		setLocationRelativeTo( null);
		//setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		m_corrTable.setBackground( getBackground() );


		reset();

		m_adjusted.setSelected( true);

		m_symbol.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onSymbolEntered();
			}
		});
		m_adjusted.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				reset();
			}
		});
		m_divs.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		});
		m_trend.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		});
		m_bars.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onTypeChanged();
			}
		});
		
		Font font = m_adjusted.getFont().deriveFont( Font.PLAIN);
		m_adjusted.setFont( font);
		
		m_settings.read();
		
		m_etf.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onAddEtf();
			}
		});
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if( --count == 0) {
					System.exit( 0);
				}
			}
		});
	}

	protected void onTypeChanged() {
		if( m_bars.isSelected() ) {
			remove( m_lineChart);
			add( m_barScroll);
			
			Dimension d = m_barPanel.getPreferredSize();
			m_barPanel.scrollRectToVisible( new Rectangle( d.width, d.height, 1, 1) );
		}
		else {
			remove( m_barScroll);
			add( m_lineChart);
		}
		repaint();
	}

	public void reset() {
		reset_();
		
		if( m_bars.isSelected() ) {
			m_barPanel.revalidate();

			Dimension d = m_barPanel.getPreferredSize();
			m_barPanel.scrollRectToVisible( new Rectangle( d.width, d.height, 1, 1) );
		}
		
		repaint();
		m_symbol.requestFocus();
	}
	
	private void reset_() {
		m_chartData.reset();
		
		int size = m_chartData.getSize();
		if( size == 0) {
			return;
		}
		
		try {
			m_chartData.getOrQueryPrices( m_startDate, m_adjusted.isSelected() );
		}
		catch (BadSymbol e) {
			return;
		}

		m_legendPanel.model().fireTableDataChanged();
		m_corrModel.fireTableDataChanged();
		m_corrModel.fireTableStructureChanged();
		m_corrModel.setWidths( m_corrTable);
		m_legendPanel.setVisible( size > 0);
		
		// set title to date range
		String realStart = m_chartData.firstDate();
		String realEnd = m_chartData.lastDate();
		String title = String.format( "Charts  %s - %s   %s years", S.userDate( realStart), S.userDate( realEnd), m_chartData.getYears() ); 
		setTitle( title);
	}

	protected void onAddEtf() {
		EtfDlg dlg = new EtfDlg( this);
	}
	
	protected void onNewChart() {
		Point p = getLocation();
		p.x += 20;
		p.y += 20;
		ChartFrame f = new ChartFrame();
		f.setLocation( p); 
	}

	private void onSymbolEntered() {
		String symbol = m_symbol.getText().toUpperCase();
		if( symbol.length() > 0) {
			m_chartData.removeAgg();
			
			if( symbol.indexOf( ':') != -1) {
				m_chartData.addFromIBFile( "IB", symbol);
			}
			else {
				StringTokenizer st = new StringTokenizer( symbol);
				while( st.hasMoreTokens() ) {
					m_chartData.addSymbol( st.nextToken(), null);
				}
			}
			
			reset();
			m_symbol.setText( null);
			
			if( m_chartData.getSize() > 1 && m_bars.isSelected() ) {
				m_bars.setSelected( false);
				onTypeChanged();
			}
		}
	}

	void addSymbol( String str, String name) {
		m_chartData.addSymbol( str, name);
		reset();
	}

	private void onPrint() {
		PageFormat format = new PageFormat();
		format.setOrientation( PageFormat.LANDSCAPE);

		PrinterJob job = PrinterJob.getPrinterJob();
		job.setPrintable( this, format);

		if( job.printDialog() ) {
			try {
				job.print();
			} 
			catch( PrinterException e) {
				S.err( "Error: " + e);
			}
		}
	}

	@Override public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
		if( pageIndex > 0) {
			return NO_SUCH_PAGE;
		}
		
		Graphics2D gr = (Graphics2D)graphics;
	    gr.translate( pageFormat.getImageableX(), pageFormat.getImageableY() );
	    printAll( gr);

	    // tell the caller that this page is part of the printed document
	    return PAGE_EXISTS;
	}

	private void onYahoo() {
		try {
			String url = String.format( "http://finance.yahoo.com/q?s=%s&ql=1", m_chartData.getSymbol( 0) );
			Desktop.getDesktop().browse( URI.create(url) );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void setStartDate( String date) {
		m_startDate = date;
		m_dayss = JtsCalendar.getDaysSince( date);
		reset();
	}		
	
	boolean usePrices() {
		return m_chartData.usePrices() && !m_trend.isSelected();
	}
	
	class DatesPanel extends JPanel {
		HashSet<HtmlRadioButton> m_set = new HashSet<HtmlRadioButton>();
		
		DateLabel m_1M = new DateLabel( "1M", 30, m_set);
		DateLabel m_3M = new DateLabel( "3M", 90, m_set);
		DateLabel m_6M = new DateLabel( "6M", 180, m_set);
		DateLabel m_YTD = new DateLabel( "YTD", doy(), m_set);
		DateLabel m_1Y = new DateLabel( "1Y", 365, m_set);
		DateLabel m_2Y = new DateLabel( "2Y", 365*2, m_set);
		DateLabel m_3Y = new DateLabel( "3Y", 365*3, m_set);
		DateLabel m_4Y = new DateLabel( "4Y", 365*4, m_set);
		DateLabel m_5Y = new DateLabel( "5Y", 365*5, m_set);
		DateLabel m_10Y = new DateLabel( "10Y", 365*10, m_set);
		DateLabel m_15Y = new DateLabel( "15Y", 365*15, m_set);
		DateLabel m_20Y = new DateLabel( "20Y", 365*20, m_set);

		DatesPanel() {
			setLayout( new FlowLayout( FlowLayout.CENTER, 10, 0) );
			add( m_1M);
			add( m_3M);
			add( m_6M);
			add( m_YTD);
			add( m_1Y);
			add( m_2Y);
			add( m_3Y);
			add( m_4Y);
			add( m_5Y);
			add( m_10Y);
			add( m_15Y);
			add( m_20Y);
		}

		private int doy() {
			JtsCalendar now = new JtsCalendar();
			String year = now.getYYYYMMDD().substring( 0, 4);
			return JtsCalendar.getDaysSince( year + "0101");
		}
	}

	private class DateLabel extends HtmlRadioButton {
		int m_days;
		
		DateLabel( String text, int days, HashSet<HtmlRadioButton> set) {
			super( text, set);
			m_days = days;
		}

		@Override protected void onClicked() {
			super.onClicked();
			m_dayss = m_days;
			long time = System.currentTimeMillis();
			time -= m_days * JtsCalendar.MILLIS_IN_DAY;
			JtsCalendar cal = new JtsCalendar( time);
			m_startDate = cal.getYYYYMMDD();
			reset();
		}
	}
	
	class CorrModel extends ModelBase {
		public String getColumnName(int col) 	{ return col == 0 ? "" : m_chartData.getSymbol( col); }
		public int getColumnCount() 			{ return m_chartData.getSize(); }
		public int getRowCount() 				{ return m_chartData.getSize() - 1; }

		public Object getValueAt(int row, int col) {
			if( col == 0) {
				return m_chartData.getSymbol( row);
			}
			double val = m_chartData.getCorr( row, col);
			return val == Double.MAX_VALUE ? null : Util.fmtPct( val);
		}
		
		public void setWidths(JTable table) {
			if( getColumnCount() > 0) {
				setWidth( table, 0, 45);
				for( int i = 1; i < getColumnCount(); i++) {
					setWidth( table, i, 55);
				}
			}
		}
	}

	private static class CorrRenderer extends DefaultTableCellRenderer {
	    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
	    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
	    	setHorizontalAlignment( col == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
	    	ChartData chartData = ((CorrTable)table).chartData();
	    	setForeground( chartData.getColor( row) );
	    	return this;
	    }
	}
	
	class CorrTable extends TableBase {
		private CorrRenderer m_renderer = new CorrRenderer();
		
		CorrTable(CorrModel model) {
			super( model);
			model.setWidths( this);
		}
		
		ChartData chartData() { return m_chartData; }
		
		@Override public TableCellRenderer getCellRenderer(int row, int column) {
			return m_renderer;
		}
	}
}

//^GSPC is s&p 500 index
//EURUSD=X
//EURCHF=X
//CHFUSD=X
//MXNUSD=X

//bug: vol not right for 20year ibm
//search for ps
//build in index support
//do you need to subtract out risk-free rate for any of these?
//allow sorting by column header
//let sharpe always be based on annual
//bug: still doesn't take focus right away. ps
//give price tooltips over bar chart
//give option to show/hide info panels. ps
//bug: set trend start is not working because it doesn't return x values. ps
//allow hiding panels
//let reports link to charts