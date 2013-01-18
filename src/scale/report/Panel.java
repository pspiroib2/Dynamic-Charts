//package scale.report;
//
//import java.awt.BorderLayout;
//import java.awt.Component;
//import java.awt.GridLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.ArrayList;
//import java.util.HashMap;
//
//import javax.swing.Box;
//import javax.swing.BoxLayout;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JScrollPane;
//import javax.swing.JTable;
//import javax.swing.JTextField;
//import javax.swing.table.AbstractTableModel;
//
//import lib.JtsCalendar;
//import lib.S;
//import lib.Util;
//
//class Panel extends JPanel {
//	private final ReportFrame m_frame;
//
//	class TradesModel extends AbstractTableModel {
//		public int getRowCount() { return m_trades.size(); }
//		public int getColumnCount() { return 7; }
//		
//		public Object getValueAt(int row, int col) {
//			Trade trade = m_trades.get( row);
//			double real = m_allReals.get( row);
//			
//			switch( col) {
//				case 0: return trade.dateTime().getYYYYMMDDHHMMSS();
//				case 1: return trade.side();
//				case 2: return trade.size();
//				case 3: return trade.symbol(); // change this to description(). ps
//				case 4: return Util.fmt( trade.price() );
//				case 5: return Util.fmt( trade.comm() );
//				case 6: return Util.fmt( real);
//				default: return null;
//			}
//		}
//    }
//    
//    class DailyModel extends AbstractTableModel {
//		public int getRowCount() { return m_dailys.size(); }
//    	public int getColumnCount() { return 3; }
//		public Object getValueAt(int row, int col) {
//			Daily daily = m_dailys.get( row);
//			switch( col) {
//				case 0: return daily.dateTime().getYYYYMMDD2();
//				case 1: return Util.fmt( daily.totalReal() );
//				case 2: return Util.fmt( daily.totalPnl() );
//				default: return null;
//			}
//		}
//    }
//    
//    private JTextField m_symbolsField = new JTextField( 50);
//	private JTextField m_start = new JTextField( 10);
//	private JTextField m_end = new JTextField( 10);
//	private JTextField m_orderRef = new JTextField( 10);
//	private JTextField m_name = new JTextField( 10);
//	private JCheckBox m_stocks = new JCheckBox( "Stocks");
//	private JCheckBox m_options = new JCheckBox( "Ops");
//    private JCheckBox m_futures = new JCheckBox( "Futs");
//    private JButton m_run = new JButton( "Run");
//    private TradesModel m_tradesModel = new TradesModel();
//    private JTable m_tradesTable = new JTable( m_tradesModel);
//    private Component m_realChart = new JPanel();
//    private DailyModel m_dailyModel = new DailyModel();
//    private JTable m_pnlTable = new JTable( m_dailyModel);
//    private Component m_pnlChart = new JPanel();
//
//    ArrayList<Trade> m_trades = new ArrayList<Trade>();
//    ArrayList<Double> m_allReals = new ArrayList<Double>();
//	ArrayList<Daily> m_dailys = new ArrayList<Daily>();
//	double m_totalReal;
//	double m_spent;
//	private HashMap<Integer,Double> m_map = new HashMap<Integer,Double>();
//	private Report m_report;
//	private String m_symbols;
//    
//    Panel(Report report, ReportFrame reportFrame) {
//    	m_report = report;
//    	m_frame = reportFrame;
//		JScrollPane tradesScroll = new JScrollPane( m_tradesTable);
//    	JScrollPane pnlScroll = new JScrollPane( m_pnlTable);
//    	
//        JPanel line1 = new JPanel();
//        line1.add( new JLabel( "Symbols"));
//        line1.add( m_symbolsField);
//        line1.add( Box.createHorizontalStrut( 10));
//        line1.add( m_stocks);
//        line1.add( m_options);
//        line1.add( m_futures);
//        
//        JPanel line2 = new JPanel();
//        line2.add( new JLabel( "Start"));
//        line2.add( m_start);
//        line2.add( Box.createHorizontalStrut( 10));
//        line2.add( new JLabel( "End"));
//        line2.add( m_end);
//        line2.add( Box.createHorizontalStrut( 10));
//        line2.add( new JLabel( "Order ref"));
//        line2.add( m_orderRef);
//        line2.add( Box.createHorizontalStrut( 10));
//        line2.add( new JLabel( "Name"));
//        line2.add( m_name);
//        line2.add( Box.createHorizontalStrut( 10));
//        line2.add( m_run);
//        
//        JPanel boxPanel = new JPanel();
//        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS));
//        boxPanel.add( line1);
//        boxPanel.add( line2);
//        
//        JPanel gridPanel = new JPanel();
//        gridPanel.setLayout( new GridLayout( 2, 2) );
//        gridPanel.add( tradesScroll);
//        gridPanel.add( m_realChart);
//        gridPanel.add( pnlScroll);
//        gridPanel.add( m_pnlChart);
//        
//        setLayout( new BorderLayout() );
//        add( boxPanel, BorderLayout.NORTH);
//        add( gridPanel);
//        
//        m_run.addActionListener( new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				onRun();
//			}
//        });
//    }
//
//	private void onRun() {
//		m_symbols = m_symbolsField.getText().toUpperCase().trim();
//		m_symbolsField.setText( m_symbols);
//
//		m_trades.clear();
//		m_allReals.clear();
//		m_dailys.clear();
//		m_totalReal = 0;
//		m_spent = 0;
//		m_report.processTrades( this);
//	}
//
//	public boolean pass(Trade trade) {
//		if( S.isNotNull( m_symbols) && m_symbols.indexOf( trade.underSymbol() ) == -1) {
//			return false;
//		}
//		
//		return true;
//	}
//	
//	public void trade(Trade trade, Trade next, String str) {
//		m_totalReal += trade.realPnl();
//		m_spent += trade.spent();
//		m_map.put( trade.conid(), trade.mktVal() );
//
//		m_trades.add( trade);
//		m_allReals.add( m_totalReal);
//		
//
//		if( next == null || !next.dateTime().isSameDay( trade.dateTime() ) ) {
//			Daily daily = new Daily();
//			daily.m_dateTime = trade.dateTime();
//			daily.m_totalReal = m_totalReal;
//			daily.m_totalPnl = calcTotalMktVal() - m_spent;
//			m_dailys.add( daily);
//		}
//	}
//
//	private double calcTotalMktVal() {
//		double total = 0;
//		for( double v : m_map.values() ) {
//			total += v;
//		}
//		return total;
//	}
//
//	public void refresh() {
//		m_tradesModel.fireTableDataChanged();
//		m_dailyModel.fireTableDataChanged();
//	}
//
//	static class Daily {
//		JtsCalendar m_dateTime;
//		double m_real;
//		double m_pnl;
//		double m_totalReal;
//		double m_totalPnl;
//		
//		public JtsCalendar dateTime() { return m_dateTime; }
//		double real() 			{ return m_real; }
//		double pnl() 			{ return m_pnl; }
//		double totalReal() 		{ return m_totalReal; }
//		double totalPnl() 		{ return m_totalPnl; }
//	}
//}