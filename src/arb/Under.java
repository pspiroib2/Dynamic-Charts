package arb;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import lib.S;

import arb.Arb.Req;
import arb.Data.IndexData;
import arb.Data.StockData;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;

public class Under implements Comparable<Under> {
	private JFrame m_frame;
	private JTextField m_tfInterest = new JTextField( "0.0%", 4);
	private JTextField m_tfDividend = new JTextField( 4);
	private Lab m_labPrice = new Lab();
	private Lab m_labHedge = new Lab();
	private Lab m_labHedge2 = new Lab();
	private Lab m_labRatio = new Lab();
	private Lab m_labHI1 = new Lab();
	private Lab m_labHI2 = new Lab();
	private AbstractMap<String,Exp> m_exps = new ConcurrentHashMap();
	private JTabbedPane m_pane = new JTabbedPane();
	private String m_symbol;
	private Data m_data; // market data
	private Under m_parent;
	private Under m_hedge;
	private Under m_hedge2; // ETF.IV
	private double m_i; // rate
	private double m_dividend; // set this. ???
	private int m_position;
	private String m_secType = "";
	private Arb m_arb;
	private boolean m_contractsRequested;
	int m_fakeStockPos;
	boolean m_autoRequest; // auto-request mkt data when contract details are returned
	private String m_expFilter;
	private boolean m_log;

	// get
	public double i() 			{ return m_i; }
	public double dividend() 	{ return m_dividend; }
	public Data stock() 		{ return m_data; }
	public Under hedge() 		{ return m_hedge; }
	public String symbol() 		{ return m_symbol; }
	public String secType() 	{ return m_secType; }
	public int position() 		{ return m_position; }
	int fakeStockPos() 			{ return m_fakeStockPos; }

	// set
	void secType(String v) 			{ m_secType = v; }
	void incFakeStockPos( int amt)	{ m_fakeStockPos += amt; }
	void decFakeStockPos( int amt)	{ m_fakeStockPos -= amt; }

	// helpers
	void out( String str) 		{ Arb.out( str); }
	public boolean isStock() 	{ return m_secType.equals( "STK"); }
	public boolean isIndex() 	{ return m_secType.equals( "IND"); }
	void msg( String str)		{ Arb.msg( m_frame, str); } 
	

	public Under( Arb arb, String symbol) {
		m_arb = arb;
		m_symbol = symbol;
		
		if( symbol.equals( "OEX") || symbol.equals( "XEO") || symbol.equals( "OEF") || symbol.equals( "OEF.IV") ) {
			//m_log = true;
			out( "Logging");
		}

		reqStock();
		
		if( symbol.equals( "OEX") || symbol.equals( "XEO") ) {
			m_hedge = m_arb.getOrCreateUnder( "OEF");
			m_hedge2 = m_arb.getOrCreateUnder( "OEF.IV");
		}
		else {
			m_hedge = this;
		}
	}
	
	public void createFrame() {
		if( m_frame != null) {
			m_frame.setState( Frame.NORMAL);
			m_frame.toFront();
			return;
		}
		
		JMenuItem back = new JMenuItem( "Back");
		
		JMenuBar b = new JMenuBar();
		b.add( back);
		
		JPanel topPanel = new JPanel();
		topPanel.add( new JLabel( "Interest rate") );
		topPanel.add( m_tfInterest);
//		topPanel.add( new JLabel( "Dividend") );
//		topPanel.add( m_tfDividend);
		topPanel.add( new JLabel( "Price") );
		topPanel.add( m_labPrice);
		topPanel.add( new JLabel( "Ratio") );
		topPanel.add( m_labRatio);
		topPanel.add( new JLabel( "HI1") );
		topPanel.add( m_labHI1);
		topPanel.add( new JLabel( "Hedge") );
		topPanel.add( m_labHedge);
		
		m_frame = new JFrame();
		m_frame.setJMenuBar( b);
		m_frame.add( topPanel, BorderLayout.NORTH);
		m_frame.add( m_pane);
		m_frame.setTitle( m_symbol);
		m_frame.setSize( 900, 820);
		m_frame.setVisible( true);
		
		m_tfInterest.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onUpdateInterest();
			}
		});
		m_tfDividend.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onUpdateDividend();
			}
		});
		back.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_arb.show();
			}
		});
		m_frame.addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				m_arb.desubscribe( m_symbol);
				m_arb.show();
				m_frame.dispose();
				m_frame = null;
			}
		});

		requestContracts();
	}
	
	void reqStock() {
		// request contract details for stock to get conid
		Contract stock = new Contract();
		stock.m_symbol = m_symbol;
		stock.m_secType = "STK";
		stock.m_exchange = "SMART";
		stock.m_primaryExch = "ISLAND";
		stock.m_currency = "USD";
		
		m_arb.reqContractDetails( stock, new Req() {
			public void process(ContractDetails cd) {
				if( cd != null) {
					onStock( cd);
				}
				else {
					reqIndex();
				}
			}
		});
	}
	
	private void onStock(ContractDetails cd) {
		if( m_data == null) {
			m_data = new StockData( cd.m_contract, m_arb.nextId(), this);
			m_arb.put( m_data);
			onStockOrIndex();
		}
		else {
			Arb.err( "Already have stock " + m_symbol);
		}
	}
	
	protected void reqIndex() {
		// request contract details for stock to get conid
		Contract index = new Contract();
		index.m_symbol = m_symbol;
		index.m_secType = "IND";
		index.m_currency = "USD";
		
		m_arb.reqContractDetails( index, new Req() {
			public void process(ContractDetails cd) {
				if( cd != null) {
					onIndex( cd);
				}
				else {
					Arb.err( "Can't resolve underlying " + m_symbol);
				}
			}
		});
	}
	
	protected void onIndex(ContractDetails cd) {
		if( m_data == null) {
			m_data = new IndexData( cd.m_contract, m_arb.nextId(), this);
			onStockOrIndex();
		}
		else {
			Arb.err( "Already have index " + m_symbol);
		}
	}
	
	private void onStockOrIndex() {
		out( "stockorindex " + m_symbol);
		
		m_secType = m_data.m_contract.m_secType;
		for( Exp exp : m_exps.values() ) {
			exp.handleStockContract();
		}
		
		if( m_log) {
			m_data.log( "c:\\options\\" + m_symbol + ".ts." + S.TODAY + ".t");
		}
		
		m_arb.reqMktData( m_data);
	}
	
	private void requestContracts() {		
		if( !m_contractsRequested) {
			// request contract details for all futures
			Contract future = new Contract();
			future.m_symbol = m_symbol;
			future.m_secType = "FUT";
			future.m_exchange = "ONE";
			m_arb.reqContractDetails( future);
	
			// request contract details for all options
			Contract option = new Contract();
			option.m_symbol = m_symbol;
			option.m_secType = "OPT";
			option.m_exchange = "SMART";
			option.m_primaryExch = "ISE";
			m_arb.reqContractDetails( option);
			
			m_contractsRequested = true;
		}
	}
	
	protected void onUpdateInterest() {
		m_i = Double.parseDouble( m_tfInterest.getText() );
		m_tfInterest.setText( S.fmtPct( m_i) );
		
		for( Exp exp : m_exps.values() ) {
			exp.updateInterest();
		}
		clearAll();
	}
	
	protected void onUpdateDividend() {
		m_dividend = S.parseDouble( m_tfDividend.getText() );
		clearAll();
	}
	
	private void clearAll() {
		for( Exp exp : m_exps.values() ) {
			exp.clearAll();
		}
	}
	
	public void onPositionUpdate(Contract contract, int position,
			double marketPrice, double marketValue, double averageCost,
			double unrealizedPNL, double realizedPNL, String accountName) {
		
		if( contract.m_secType.equals( "STK") ) {
			m_position = position;
		}
		else if( !S.isNull( contract.expiry()) ) {
			Exp exp = getOrCreateExp( contract.getExpiry());
			exp.handlePosition( contract, position);
		}
	}
	
	Exp getOrCreateExp(String expiry) {
		Exp exp = m_exps.get( expiry);
		if( exp == null) {
			exp = new Exp( m_arb, this, expiry);
			m_exps.put( expiry, exp);
		}
		return exp;
	}


	public void onContractDetails(int reqId, ContractDetails cd) {
		Contract contract = cd.m_contract;
		
		// skip this contract?                      //???
		if( !S.isNull( m_expFilter) && !S.isNull( contract.getExpiry()) && contract.getExpiry().compareTo( m_expFilter) > 0) {
			return;
		}
		
		if( contract.m_secType.equals( "FUT") ) {
			Exp exp = getOrCreateExp( contract.getExpiry());
			exp.handleFutureContract( contract, m_autoRequest);
		}
		else if( contract.m_secType.equals( "OPT") ) {
			Exp exp = getOrCreateExp( contract.getExpiry());
			exp.handleOptionContract( contract, m_autoRequest);
		}
		else {
			Arb.err( "Unknown sec type " + contract.m_secType);
		}
	}
	
	public void check() {
		if( m_data != null) {
			m_labPrice.setText( S.fmt2a( m_data.mark() ) );
			
			if( m_hedge != null) {
				m_labHedge.setText( S.fmt2a( hedgeMarkPrice() ) );
				
				double ratio = markPrice() / hedgeMarkPrice();
				m_labRatio.setText( "" + ratio);
			}
			
			if( m_hedge2 != null) {
				m_labHedge2.setText( S.fmt2a( hedge2MarkPrice() ) );
				
				double ratio = markPrice() / hedge2MarkPrice();
				m_labRatio.setText( "" + ratio);
			}
			
			for( Exp exp : m_exps.values() ) {
				exp.check();
			}
		}
	}

	public void calcFakePositions() {
		m_fakeStockPos = m_position;
		for( Exp exp : m_exps.values() ) {
			exp.calcFakePositions();
		}
	}
	
	public void onDone(int orderId) {
		for( Exp exp : m_exps.values() ) {
			exp.onDone( orderId);
		}
	}
	
	public void fillPositions(ArrayList<Strike> list) {
		for( Exp exp : m_exps.values() ) {
			exp.fillPositions( list);
		}		
	}
	
	public void subscribePos() {
		for( Exp exp : m_exps.values() ) {
			exp.subscribePos();
		}	
	}
	
	public int compareTo(Under o) {
		return m_symbol.compareTo( o.m_symbol);
	}
	
	int futPos() {
		int pos = 0;
		for( Exp exp : m_exps.values() ) {
			pos += exp.futurePos();
		}
		return pos;
	}
	
	public int synthPos() {
		int pos = 0;
		for( Exp exp : m_exps.values() ) {
			pos += exp.synthPos();
		}
		return pos;
	}
	
	public int shortSynthPos() {
		int pos = 0;
		for( Exp exp : m_exps.values() ) {
			pos += exp.shortSynthPos();
		}
		return pos;
	}
	
	public int delta() {
		return position() + (futPos() + synthPos() - shortSynthPos() ) * 100 + hedgePos();
	}
	
	public int hedgePos() {
		return m_hedge != null
			? (int)(m_hedge.position() * hedgeMarkPrice() / m_data.mark() + .5)
			: 0;
	}
	
	public void fillExps(ArrayList<Exp> exps) {
		for( Exp exp : m_exps.values() ) {
			if( exp.hasDerivative() ) {
				exps.add( exp);
			}
		}
	}
	
	public void test() {
		for( Exp exp : m_exps.values() ) {
			exp.test();
		}
	}
	
	public void addTab(String titleIn, JPanel panelIn) {
		int pos = m_pane.getTabCount();
		
		for( int i = 0; i < m_pane.getTabCount(); i++) {
			String title = m_pane.getTitleAt( i);
			if( titleIn.compareTo( title) < 0) {
				pos = i;
				break;
			}
		}
		
		m_pane.insertTab( titleIn, null, panelIn, null, pos);
	}
	
	public void scan() {
		m_expFilter = "20100199";
		
		// req mkt data for Exp's that might have been created already
		for( Exp exp : m_exps.values() ) {
			exp.onSubscribe();
		}
			
		m_autoRequest = true;
		requestContracts();
	}

	public void scanReport() {
		String t = "\t";
		
		String str = "symbol" + t + "exp" + t + "EFP bid" + t + "EFP ask" + t + "Conv bid" + t + "Conv ask" + t + "Profit"; 
		m_arb.report( str); 
		
		for( Exp exp : m_exps.values() ) {
			exp.scanReport();
		}
	}
	
	public void onOpenOrder(Order order, OrderState orderState) {
		if( order.contract().hasLegs() ) {
			Exp exp = getOrCreateExp( order.contract().getFirstExp() );
			exp.onOpenOrder( order, orderState);
		}		
	}

	/** Not needed since we are on a 1-sec update. */
	public void onStockUpdated() {
//		for( Exp exp : m_exps.values() ) {
//			exp.updateOrders();
//		}
	}
	
	public double bid() {
		return m_data != null ? m_data.bid() : -Double.MAX_VALUE;
	}
	
	public double ask() {
		return m_data != null ? m_data.ask() : Double.MAX_VALUE;
	}
	
	public double markPrice() {
		return m_data != null ? m_data.mark() : Double.MAX_VALUE;
	}
	
	public double hedgeMarkPrice() {
		return m_hedge != null ? m_hedge.markPrice() : Double.MAX_VALUE;
	}
	
	public double hedge2MarkPrice() {
		return m_hedge2 != null ? m_hedge2.markPrice() : Double.MAX_VALUE;
	}
	
	public Contract hedgeContract() {
		return m_hedge != null && m_hedge.stock() != null ? m_hedge.stock().contract() : null;
	}
	
	public double hedgeBid() { 
		return m_hedge != null ? m_hedge.bid() : Double.MAX_VALUE;
	}
	
	public double hedgeAsk() { 
		return m_hedge != null ? m_hedge.ask() : Double.MAX_VALUE;
	}
	
	public boolean hasOrders() {
		for( Exp exp : m_exps.values() ) {
			if( exp.hasOrders() ) {
				return true;
			}
		}
		return false;
	}
	public void cancelAll() {
		for( Exp exp : m_exps.values() ) {
			exp.cancelAll();
		}
	}
}
