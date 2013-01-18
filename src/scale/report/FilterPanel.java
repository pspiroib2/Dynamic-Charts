package scale.report;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import lib.OStream;
import lib.S;
import lib.Util;

class FilterPanel extends JPanel implements ActionListener {
    private static final String ALL = "All";

    private Report m_report;
	private ReportFrame m_frame;
    private Filter m_filter;
	private ArrayList<Trade> m_trades = new ArrayList<Trade>();
	private ArrayList<Daily> m_dailys = new ArrayList<Daily>();
	private double m_totalReal;
	private double m_spent;
	private double m_totalComm;
	private Map<String,Underlying> m_underMap = new HashMap<String,Underlying>(); 		// map under symbol to Underlying
	private HashMap<Integer,Scale> m_scales = new HashMap<Integer,Scale>(); //tree map?
	private JTextField m_symbols = new JTextField( 40);
	private JTextField m_startReporting = new TextField( 12);
	private JTextField m_startTrading = new TextField( 12);
	private JTextField m_end = new TextField( 12);
	private JLabel m_totalCommLab = new JLabel();
	private JComboBox m_account = new JComboBox( Report.INSTANCE.getAccounts() );
	private JTextField m_name = new JTextField( 10);
	private JCheckBox m_stocks = new CheckBox( "Stocks");
	private JCheckBox m_options = new CheckBox( "Ops");
    private JCheckBox m_futures = new CheckBox( "Futs");
    private JCheckBox m_prices = new CheckBox( "Prices");
    private JCheckBox m_divs = new CheckBox( "Divs");
    private JButton m_run = new JButton( "Run");
    private JCheckBox m_open = new CheckBox( "Opening trades");
    private JButton m_detail = new JButton( "Detail");
    private TradesModel m_tradesModel = new TradesModel();
    private Table m_tradesTable = new Table( m_tradesModel, 0, 1, 2, 4);
    private DailyModel m_dailyModel = new DailyModel();
    private Table m_pnlTable = new Table( m_dailyModel, 0);
    private PnlChart m_pnlChart = new PnlChart();
    double m_oldPnl; // pnl from before the start reporting period
	//OStream os;

	Report report() 		{ return m_report; }
	ReportFrame frame() 	{ return m_frame; }
	Filter filter() 		{ return m_filter; }
    
	FilterPanel( Report report, ReportFrame frame, Filter filter) {
		m_report = report;
		m_frame = frame;
		m_filter = filter;
		
//		try {
//			//os = new OStream( "c:\\temp\\pnl_" + m_filter.name() + ".csv", false);
//			//os.writeln( Trade.getHeader() );
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		m_account.insertItemAt( ALL, 0);
		
		//m_divs.setSelected( true);
		
		JScrollPane tradesScroll = new JScrollPane( m_tradesTable);
    	JScrollPane pnlScroll = new JScrollPane( m_pnlTable);
    	JScrollPane chartScroll = new JScrollPane( m_pnlChart);
    	
    	m_pnlTable.setPreferredScrollableViewportSize(new Dimension(270, 400));
    	m_tradesTable.getColumnModel().getColumn( 0).setPreferredWidth( 150);
    	m_tradesTable.getColumnModel().getColumn( 4).setPreferredWidth( 150);
    	
        JPanel line1 = new LeftPanel();
        line1.add( new JLabel( "Symbols"));
        line1.add( m_symbols);
        line1.add( Box.createHorizontalStrut( 10));
        line1.add( m_stocks);
        line1.add( m_options);
        line1.add( m_futures);
        line1.add( m_prices);
        line1.add( m_divs);
        
        JPanel westPanel = new JPanel();
        westPanel.setLayout( new BoxLayout( westPanel, BoxLayout.Y_AXIS) );
        westPanel.add( new V( 10) );
        westPanel.add( new LeftPanel( "Start trading", m_startTrading) ); 
        westPanel.add( new V( 5) );
        westPanel.add( new LeftPanel( "Start reporting", m_startReporting) ); 
        westPanel.add( new V( 5) );
        westPanel.add( new LeftPanel( "End", m_end) ); 
        
        JPanel line2 = new LeftPanel();
        line2.add( Box.createHorizontalStrut( 10));
        line2.add( "Account", m_account);
        line2.add( Box.createHorizontalStrut( 10));
        line2.add( "Name", m_name);
        line2.add( Box.createHorizontalStrut( 10));
        line2.add( m_run);
        line2.add( Box.createHorizontalStrut( 10));
        line2.add( m_open);
        line2.add( Box.createHorizontalStrut( 10));
        line2.add( m_detail);
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS));
        boxPanel.add( line1);
        boxPanel.add( line2);
        
        JPanel commPanel = new LeftPanel();
        commPanel.add( new JLabel( "Total commission") );
        commPanel.add( m_totalCommLab);
        
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout( new BorderLayout() );
        leftPanel.add( pnlScroll);
        leftPanel.add( commPanel, BorderLayout.SOUTH);
        
        JPanel openPanel = new JPanel();
        
        JPanel pnlPanel = new JPanel();
        pnlPanel.setLayout( new BorderLayout() );
        pnlPanel.add( leftPanel, BorderLayout.WEST);
        pnlPanel.add( chartScroll);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.Y_AXIS) );
        mainPanel.add( tradesScroll);
        mainPanel.add( pnlPanel);
        
        // build this panel
        setLayout( new BorderLayout() );
        add( westPanel, BorderLayout.WEST);
        add( boxPanel, BorderLayout.NORTH);
        add( mainPanel);
        
        // update GUI from filter
        String account = S.isNull( filter.account() ) ? ALL : filter.account();
        m_symbols.setText( Filter.getCDText( m_filter.unders() ) );
        m_name.setText( filter.name() );
        m_startTrading.setText( filter.startTrading() );
        m_startReporting.setText( filter.startReporting() );
        m_end.setText( filter.end() );
        m_account.setSelectedItem( account);
        m_stocks.setSelected( filter.hasSecType( Filter.STK) );
        m_options.setSelected( filter.hasSecType( Filter.OPT) );
        m_futures.setSelected( filter.hasSecType( Filter.FUT) );
        
        // add listeners
        m_run.addActionListener( this);
        m_symbols.addActionListener( this);
        m_account.addActionListener( this);
        m_name.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String name = m_name.getText();
				m_filter.name( name);
				m_frame.setName( FilterPanel.this, name);
			}
        });
        m_detail.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new DetailFrame( m_scales.values() );
			}
        });
        m_tradesTable.addMouseListener( new MouseAdapter() {
        	public void mouseClicked(MouseEvent e) {
        		if( e.isPopupTrigger() ) {
        			onDel();
        		}
        		else if( e.getClickCount() == 2) {
        			onTradesDoubleClicked();
        		}
        		else if( e.getClickCount() == 1 && e.isShiftDown() ) {
        			onExclude();
        		}
        	}
        });
        
        // populate tables
        run();
    }
	
	void clearAll() {
		m_trades.clear();
		m_dailys.clear();
		m_underMap.clear();
		m_scales.clear();
		m_oldPnl = 0;
		m_totalReal = 0;
		m_spent = 0;
		m_totalComm = 0;
	}

	/** Called when Run is clicked. */
	public void actionPerformed(ActionEvent event) {
		run();
	}
	
	private void run() {
		// update filter from GUI
		String account = (String)m_account.getSelectedItem();
		m_filter.setUnders( m_symbols.getText().toUpperCase() );
		m_filter.startTrading( m_startTrading.getText() );
		m_filter.startReporting( m_startReporting.getText() );
		m_filter.end( m_end.getText() );
		m_filter.secType( m_stocks.isSelected(), m_options.isSelected(), m_futures.isSelected() );
		m_filter.account( account == ALL ? null : account);
		m_symbols.setText( Filter.getCDText( m_filter.unders()) );

		clearAll();
		
		// load all trades that pass filter into Scales
		for( Trade trade : m_report.trades() ) {
			if( m_filter.pass( trade) ) {
				Scale scale = getOrCreateScale( trade);
				scale.addTrade( trade);  // (updates position which is needed to get prices)
			}
		}

		// add the daily closing prices and dividends for the needed dates only
		if( m_prices.isSelected() || m_divs.isSelected() ) {
			for( Scale scale : m_scales.values() ) {
				scale.getPrices( m_prices.isSelected(), m_divs.isSelected() );
			}
		}
		
		// clear scale positions
		for( Scale scale : m_scales.values() ) {
			scale.reset();
		}

		// get list of all trades and prices for all symbols and sort
		Trades allTrades = new Trades();
		for( Scale scale : m_scales.values() ) {
			allTrades.addAll( scale.allTrades() );
		}
		allTrades.sort();
		
		// apply trades to Scales, calculating pnls
		for( int i = 0; i < allTrades.size(); i++) {
			Trade trade = allTrades.get( i);
			Trade next = allTrades.getNext( i);
			
			Scale scale = getOrCreateScale( trade);
			scale.applyTrade( this, trade); // causes reportSplitTrade() to be called which adds trade to m_trades
			
			if( next == null || !next.dateTime().isSameDay( trade.dateTime() ) ) {
				double totalPnl = calcTotalMktVal() - m_spent;
				if( m_filter.report( trade) ) {
					Daily daily = new Daily( trade.dateTime(), m_totalReal, totalPnl - m_oldPnl);
					m_dailys.add( daily);
				}
				else {
					m_oldPnl = totalPnl;
				}
			}
		}
		
		// add opening trades to m_trades
		if( m_open.isSelected() ) {
			for( Scale scale : m_scales.values() ) {
				m_trades.addAll( scale.openingTrades() );
			}
		}
		
		// refresh GUI
		m_totalCommLab.setText( Util.fmtNice( m_totalComm) );
		m_tradesModel.fireTableDataChanged();
		m_dailyModel.fireTableDataChanged();
		m_pnlChart.revalidate();
		m_pnlChart.repaint();
		
		// scroll pnl chart to the right
		Dimension d = m_pnlChart.getPreferredSize();
		m_pnlChart.scrollRectToVisible( new Rectangle( d.width, d.height, 1, 1) );
		m_tradesTable.scroll();
		m_pnlTable.scroll();
	}
	

//		if( trade.isSettlement() ) {
//			Underlying under = m_underMap.get( trade.underSymbol() );
//			under.applyCashSettlement( trade.settlementIncome() ); 
//		}

	
	/** Called for all trades including dividends and prices. */
	public void reportSplitTrade(Trade trade) {
		m_spent += trade.spent();
		
		if( m_filter.report( trade) ) {
			m_totalReal += trade.realPnl();
			
			if( !m_open.isSelected() ) {
				m_trades.add( trade);
			}
			
			m_totalComm += trade.comm();
			//os.writeln( trade.getStr() ); 
		}
	}
	
	private double calcTotalMktVal() {
		double total = 0;
		for( Scale scale : m_scales.values() ) {
			total += scale.mktVal();
		}
		return total;
	}

	public void refresh() {
		m_tradesModel.fireTableDataChanged();
		m_dailyModel.fireTableDataChanged();
	}
	
	protected void onTradesDoubleClicked() {
		int row = m_tradesTable.getSelectedRow();
		
		if( row != -1) {
			String time = m_trades.get( row).dateTimeStr();

			if( S.isNull( m_filter.startTrading() ) ) {
				m_filter.startTrading( time);
				m_startTrading.setText( time);
			}
			else {
				m_filter.end( time);
				m_end.setText( time);
			}
			run();
		}
	}
	
	protected void onExclude() {
		int[] rows = m_tradesTable.getSelectedRows();
		for( int row : rows) {
			String time = m_trades.get( row).dateTimeStr();
			m_filter.exclude( time);
		}
		run();
	}
	
	private void onDel() {
		m_frame.deletePanel( this);
	}

	Underlying getOrCreateUnderlying( String underSymbol) {
		if( underSymbol.equals( "QQQQ") ) {
			S.err( "lkj");
		}
		Underlying under = m_underMap.get( underSymbol);
		if( under == null) {
			under = new Underlying( underSymbol);
			m_underMap.put( underSymbol, under);
		}
		return under;
	}

	private Scale getOrCreateScale( Trade trade) {
		return getOrCreateScale( trade.conid(), trade.underSymbol(), trade.secType(), trade.mult() );
	}
		
	private Scale getOrCreateScale( int conid, String underSymbol, String secType, int mult) {
		Scale scale = m_scales.get( conid);
		if( scale == null) {
			Underlying underlying = getOrCreateUnderlying( underSymbol);
			scale = new Scale( underlying, conid, secType, mult);
			m_scales.put( conid, scale);
		}
		return scale;
	}
	
	class TradesModel extends AbstractTableModel {
		public int getRowCount() { return m_trades.size(); }
		public int getColumnCount() { return 10; }
		
		public String getColumnName(int col) {
			switch( col) {
				case 0: return "Date/time";
				case 1: return "Account";
				case 2: return "Side";
				case 3: return "Size";
				case 4: return "Symbol";
				case 5: return "Price";
				case 6: return "Comm";
				case 7: return "Real Pnl";
				case 8: return "Position";
				case 9: return "Mkt Val";
				default: return null;
			}
		}
		
		public Object getValueAt(int row, int col) {
			Trade trade = m_trades.get( row);
			
			if( trade == null) {
				S.err( "Error: row " + row + " has null trade");
				return null;
			}
			
			switch( col) {
				case 0: return trade.dateTimeStr();
				case 1: return trade.account();
				case 2: return trade.side();
				case 3:	return trade.getSizeForDisplay( m_open.isSelected() ); 
				case 4: return trade.symbol(); // change this to description(). ps
				case 5: return Util.fmtNice( trade.price() );
				case 6: return Util.fmtNice2( trade.comm() );
				case 7: return Util.fmtNice2( trade.realPnl() );
				case 8: return trade.position();
				case 9: return Util.fmtShort( trade.mktVal() );
				default: return null;
			}
		}
    }
    
    class DailyModel extends AbstractTableModel {
		public int getRowCount() { return m_dailys.size(); }
    	public int getColumnCount() { return 3; }
    	
		public String getColumnName(int col) {
			switch( col) {
				case 0: return "Date";
				case 1: return "Real Pnl";
				case 2: return "Total Pnl";
				default: return null;
			}
		}
		public Object getValueAt(int row, int col) {
			Daily daily = m_dailys.get( row);
			switch( col) {
				case 0: return daily.dateTime().getYYYYMMDD2();
				case 1: return Util.fmtShort( daily.totalReal() );
				case 2: return Util.fmtShort( daily.totalPnl() );
				default: return null;
			}
		}
    }

    class PnlChart extends JComponent {
		int leftBuf = 5;
		int barWidth = 2;
		int vBuf = 8;
		int totalBar = 4;
		int rightBuf = 55;
		
		@Override public Dimension getPreferredSize() {
			int width = leftBuf + m_dailys.size() * totalBar + rightBuf; 
			return new Dimension( width, 10);
		}
		
		protected void paintComponent(Graphics g) {
			Rectangle rect = g.getClipBounds();
			double height = rect.height / 2.0 - vBuf;
			int mid = rect.height / 2;

			if( m_dailys.size() == 0) {
				return;
			}
			
			double maxPnl = 0;
			for( Daily daily : m_dailys) {
				maxPnl = Math.max( maxPnl, Math.abs( daily.totalPnl() ) );
			}
			
			for( int i = 0; i < m_dailys.size(); i++) {
				Daily daily = m_dailys.get( i);
				int x = leftBuf + i * totalBar;
				int barHeight = (int)(daily.totalPnl() / maxPnl * height * -1 + .5);
				
				if( barHeight < 0) {
					g.fillRect( x, mid + barHeight, barWidth, -barHeight);
				}
				else {
					g.fillRect( x, mid, barWidth, barHeight);
				}
			}

			// draw pnl text
			if( m_dailys.size() > 0) {
				int last = m_dailys.size() - 1;
				Daily daily = m_dailys.get( last);
				double pnl = daily.totalPnl();
				int y = (int)(mid + pnl / maxPnl * height * -1 + 6);
				g.drawString( Util.fmtNice( pnl), 
						      leftBuf + m_dailys.size() * totalBar, 
						      y);
			}
		}
	}

    static class LeftPanel extends JPanel {
    	static Dimension DIM = new Dimension( Integer.MAX_VALUE, 0);
    	
		public LeftPanel() {
			 setLayout( new FlowLayout( FlowLayout.LEFT, 5, 2) ); 
		}
		public LeftPanel( String str, JComponent c) {
			 setLayout( new FlowLayout( FlowLayout.LEFT, 5, 2) );
			 add( str, c);
		}
		void add( String str, JComponent c) {
			add( new JLabel( str) );
			add( c);
		}
		@Override public Dimension getMaximumSize() {
			DIM.height = super.getPreferredSize().height;
			return DIM;
		}
	}

	static class Renderer extends DefaultTableCellRenderer {
		ArrayList<Integer> m_cols = new ArrayList<Integer>();
		
		Renderer( int[] cols) {
			for( int col : cols) {
				m_cols.add( col);
			}
		}
			
	    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
	    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
	    	setHorizontalAlignment( m_cols.contains( col) ? SwingConstants.LEFT : SwingConstants.RIGHT);
	    	return this;
	    }
	}

	static class Table extends JTable {
		private final Renderer m_renderer;
		
		public Table(AbstractTableModel model, int... cols) {
			super( model);
			m_renderer = new Renderer( cols);
			setAutoResizeMode( JTable.AUTO_RESIZE_OFF);
			getColumnModel().setColumnMargin( 6);
		}
		
		public TableCellRenderer getCellRenderer(int row, int column) {
			return m_renderer;
		}
		
		public void scroll() {
			Dimension d = getPreferredSize();
			scrollRectToVisible( new Rectangle( d.width, d.height, 1, 1) );
		}
	}

	class TextField extends JTextField {
		public TextField(int n) {
			super( n);
			addActionListener( FilterPanel.this);
		}
	}

	class CheckBox extends JCheckBox {
		public CheckBox(String string) {
			super( string);
			addActionListener( FilterPanel.this);
		}
	}
	
	static class V extends JComponent {
		private int m_height;

		V( int i) {
			m_height = i;
		}
		@Override public Dimension getPreferredSize() {
			return new Dimension( 1, m_height);
		}
		@Override public Dimension getMaximumSize() {
			return new Dimension( 1, m_height);
		}
	}

}
// parse option symbols a little nicer
// link to charts
// write the date at the end of the pnl chart, too, and maybe total market value or some signal if flat or not
// rgld show no pnl change for 10 days; must be wrong, same with nok
//chart the pair, and allow decimal component sizes
//give right-click/open chart or ctrl click or ctrl dbl-click and give same start date as scale?
//check all ps
//make sure comms are added when trades are aggregated
//make sure dividends are included in pair chart
//whenopening a new report, scroll both windows to the bottom
// report, you need to list company names
// report bug: picb report two dividends on the same day
//bug: coder All 11 has different real and total pnl even though there are no positions
