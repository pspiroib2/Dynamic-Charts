//package arb;
//
//import java.awt.Component;
//import java.awt.FlowLayout;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//
//import javax.swing.BoxLayout;
//import javax.swing.JButton;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.border.TitledBorder;
//
//import lib.S;
//
//
//public class TraderPanel extends JPanel {
//	private Exp m_exp;
//	DoubleField m_ratio = new DoubleField( 5);
//	Lab m_index = new Lab();
//	Lab m_hedgeIndex = new Lab();
//	Lab m_midpoint = new Lab();
//	DoubleField m_buyHedgeOffset = new DoubleField( ".10", 5);
//	DoubleField m_buyAskOffset = new DoubleField( ".10", 5);
//	DoubleField m_sellHedgeOffset = new DoubleField( ".10", 5);
//	DoubleField m_sellBidOffset = new DoubleField( ".10", 5);
//	private JButton m_bBuySynth = new JButton( "Buy Synth");
//	private JButton m_bSellSynth = new JButton( "Sell Synth");
//	private Lab m_hedgeIndexOffset = new Lab();
//	private Lab m_midpointOffset = new Lab();
//
//	double index() 								{ return m_exp.underMarkPrice(); }
//	double midpoint()							{ return m_exp.midpoint(); }
//	Under under() 								{ return m_exp.under(); }
//	double ratio() throws Exception 			{ return m_ratio.getDouble(); }
//	double hedgeIndex() throws Exception 		{ return under().hedgeMarkPrice() * ratio(); }
//	double hedgeIndexBid() throws Exception 	{ return under().hedgeBid() * ratio(); }
//	double hedgeIndexAsk() throws Exception 	{ return under().hedgeAsk() * ratio(); }
//	int hedgeSize() throws Exception 			{ return (int)(ratio() * 100 + .5); } // round up to 1000???
//	static String fmt( double v) 				{ return S.fmt2a( v); }
//	double buyHedgeOffset() throws Exception	{ return m_buyHedgeOffset.getDouble(); }
//	double sellHedgeOffset() throws Exception	{ return m_sellHedgeOffset.getDouble(); }
//	double buyAskOffset() throws Exception		{ return m_buyAskOffset.getDouble(); }
//	double sellBidOffset() throws Exception		{ return m_sellBidOffset.getDouble(); }
//	
//	TraderPanel( Exp exp) {
//		m_exp = exp;
//		
//		JPanel p1 = new JPanel( new FlowLayout( FlowLayout.LEFT));
//		p1.add( new JLabel( "Ratio") );
//		p1.add( m_ratio);
//		p1.add( new JLabel( "Index") );
//		p1.add( m_index);
//		p1.add( new JLabel( "Hedge Index") );
//		p1.add( m_hedgeIndex);
//		p1.add( m_hedgeIndexOffset);
//		p1.add( new JLabel( "Midpoint") );
//		p1.add( m_midpoint);
//		p1.add( m_midpointOffset);
//		
//		JPanel p2 = new JPanel( new FlowLayout( FlowLayout.LEFT));
//		p2.add( new JLabel( "Buy at hedge index -") );
//		p2.add( m_buyHedgeOffset);
//		p2.add( new JLabel( "but not higher than ask -") );
//		p2.add( m_buyAskOffset);
//		p2.add( m_bBuySynth);
//		
//		JPanel p3 = new JPanel( new FlowLayout( FlowLayout.LEFT));
//		p3.add( new JLabel( "Sell at hedge index +") );
//		p3.add( m_sellHedgeOffset);
//		p3.add( new JLabel( "but not lower than bid +") );
//		p3.add( m_sellBidOffset);
//		p3.add( m_bSellSynth);
//
//		setBorder( new TitledBorder( "Trader") );
//		setLayout( new BoxLayout( this, BoxLayout.Y_AXIS));
//		add( p1);
//		add( p2);
//		add( p3);
//	
//		m_bBuySynth.addActionListener( new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				onBuySynth();
//			}
//		});
//		
//		m_bSellSynth.addActionListener( new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				onSellSynth();
//			}
//		});
//	}
//	
//	void update() {
//		try {
//			m_index.setText( fmt( index() ) );
//			m_hedgeIndex.setText( fmt( hedgeIndex() ) );
//			m_hedgeIndexOffset.setText( fmt( hedgeIndex() - index() ) );
//			m_midpoint.setText( fmt( midpoint() ) );
//			m_midpointOffset.setText( fmt( midpoint() - index() ) );
//		}
//		catch( Exception e) {
//			e.printStackTrace();
//		}
//	}
//	
//	protected void onBuySynth() {
//		m_exp.onBuySynth();
//	}
//
//
//	protected void onSellSynth() {
//		m_exp.onSellSynth();
//	}
//}
//// display current buy/sell price, set it from Exp
//// don't forget to add don't change strike so frequently
//// display strikes with best bid/ask
