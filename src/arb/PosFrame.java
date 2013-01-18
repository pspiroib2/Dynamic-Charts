package arb;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

public class PosFrame extends JFrame {
	static final Comp COMP = new Comp();

	private PosModel m_posModel = new PosModel();
	private UnderModel m_underModel = new UnderModel();

	private Table m_posTable = new Table( m_posModel);
	private Table m_underTable = new Table( m_underModel);
	
	private Arb m_arb;
	private ArrayList<Strike> m_strikes = new ArrayList(); // strikes with positions
	private ArrayList<Exp> m_exps = new ArrayList();
	private JCheckBox m_showAll = new JCheckBox( "Show all");
	
	PosFrame( Arb arb) {
		m_arb = arb;
		
		JPanel topPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT) );
		topPanel.add( m_showAll);
		
		JScrollPane posScroll = new JScrollPane( m_posTable);
		JScrollPane underScroll = new JScrollPane( m_underTable);
		
		JMenuItem i1 = new JMenuItem( "Back");
		JMenuItem i2 = new JMenuItem( "Subscribe");
		JMenuItem i3 = new JMenuItem( "Test");
		
		JMenuBar menubar = new JMenuBar();
		menubar.add( i1);
		menubar.add( i2);
		menubar.add( i3);
		setJMenuBar( menubar);
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout( new BoxLayout( centerPanel, BoxLayout.Y_AXIS) );
		centerPanel.add( posScroll);
		centerPanel.add( underScroll);
		
		add( topPanel, BorderLayout.NORTH);
		add( centerPanel);
		
		setSize( 800, 600);
		setTitle( "Positions");
		setVisible( true);

		i1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_arb.show();
			}
		});
		i2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_arb.subscribePos();
			}
		});
		i3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onTest();
			}
		});
	}
	
	protected void onTest() {
		m_arb.test();
	}

	void check() {
		// refresh option positions
		m_strikes.clear();
		m_arb.fillPositions( m_strikes);
		if( !m_showAll.isSelected() ) {
			filter( m_strikes);
		}
		Collections.sort( m_strikes, COMP);
		m_posTable.fireTableDataChanged();

		// refresh combo positions
		m_exps.clear();
		m_arb.fillExps( m_exps);
		Collections.sort( m_exps);
		m_underTable.fireTableDataChanged();
	}
		
	private boolean hasMismatch(ArrayList<Strike> strikes) {
		for( Strike strike : strikes) {
			if( strike.hasMismatch() ) {
				return true;
			}
		}
		return false;
	}

	private void filter(ArrayList<Strike> strikes) {
		for( Iterator<Strike> iter = strikes.iterator(); iter.hasNext(); ) {
			Strike strike = iter.next();
			if( !strike.hasMismatch() ) {
				iter.remove();
			}
		}
	}

	class PosModel extends ArbModel {
		static final int UNDER 		= 0;
		static final int EXP 		= 1;
		static final int STRIKE 	= 2;
		static final int PUT 		= 3;
		static final int CALL 		= 4;
		static final int LOW_BID 	= 5;
		static final int CALL_TV	= 6;
		static final int PUT_TV	= 7;
		static final int COLUMNS 	= 8;
		
		public int getColumnCount() {
			return COLUMNS;
		}

		public int getRowCount() {
			return m_strikes.size();
		}
		
		@Override
		public String getColumnName(int column) {
			switch( column) {
				case UNDER:		return "Under";		
				case EXP:		return "Exp";
				case STRIKE:	return "Strike";
				case PUT:		return "Put";
				case CALL:		return "Call";
				case LOW_BID:	return "Low bid";
				case CALL_TV:	return "Call TV";
				case PUT_TV:	return "Put TV";
				default:		return null;
			}
		}

		public Object getValueAt(int row, int column) {
			Strike strike = m_strikes.get( row);
			
			switch( column) {
				case UNDER:		return strike.exp().under().symbol();
				case EXP:		return Exp.fmtExp( strike.exp().expiry() );
				case STRIKE:	return strike;
				case PUT:		return strike.putPos();
				case CALL:		return callPos( strike);
				
				// value of the long options
				case LOW_BID:
					if( strike.putPos() > 0) {
						return Exp.fmt( strike.put().bid() );
					}
					return strike.exp().under().isIndex()
						? Exp.fmt( strike.call().bid() )
						: null;
					
				// cost to replace the short option
				case CALL_TV:	
					return strike.callPos() < 0 
						? Exp.fmt( strike.callTimeValue() )
						: null;
						
				case PUT_TV:	
					return strike.putPos() < 0 
						? Exp.fmt( strike.putTimeValue() )
						: null;
					
				default:		
					return null;
			}
		}

		private Object callPos(Strike strike) {
			String str = "" + strike.callPos();
			if( strike.hasMismatch() ) {
				str = "*" + str + "*";
			}
			return str;
		}
		
		@Override
		protected void onLeftClick(int row, int col) {
			Strike strike = m_strikes.get( row);
			if( col == CALL) {
				strike.replaceCall();
			}
			else if( col == PUT) {
				strike.replacePut();
			}
		}
	}
	
	
	static class Comp implements Comparator<Strike> {
		public int compare(Strike s1, Strike s2) {
			int rc = s1.symbol().compareTo( s2.symbol() );
			
			if( rc == 0) {
				rc = s1.exp().expiry().compareTo( s2.exp().expiry() );
			}
			
			if( rc == 0) {
				rc = compStrike( s1.strike(), s2.strike() );
			}
			
			return rc;
		}

		private int compStrike(double d, double e) {
			if( Strike.lt( e, d) ) {
				return -1;
			}
			if( Strike.gt( e, d) ) {
				return -1;
			}
			return 0;
		}
	}
	
	/*class Und {
		int m_stock
		int m_fut;
		int m_synth;
		int m_shortSynth;
		
		int delta() {
			return m_stock + m_fut + m_synth - m_shortSynth;
		}
	}*/
	
	class UnderModel extends AbstractTableModel {
		static final int SYMBOL			= 0;
		static final int EXPIRY			= 1;
		static final int BOX 			= 2;
		static final int CONVERSION		= 3;
		static final int EFP			= 4;
		static final int SHORT_SYNTH 	= 5;
		static final int SYNTH 			= 6;
		static final int FUTURE 		= 7;
		static final int STOCK 			= 8;
		static final int HEDGE			= 9;
		static final int DELTA 			= 10;
		static final int PAY 			= 11;
		static final int LIQ 			= 12;
		static final int COLUMNS		= 13;

		public int getColumnCount() {
			return COLUMNS;
		}
		
		@Override
		public String getColumnName(int column) {
			switch( column) {
				case SYMBOL:		return "Symbol";
				case EXPIRY:		return "Expiry";
				case BOX:			return "Box";
				case CONVERSION:	return "Conversion";
				case EFP:			return "EFP";
				case STOCK:			return "Stock";
				case FUTURE:		return "Future";
				case SYNTH:			return "Synth";
				case SHORT_SYNTH:	return "Short Synth";
				case DELTA:			return "Delta";
				case HEDGE:			return "Hedge";
				case PAY:			return "Pay";
				case LIQ:			return "Liq";
				default:			return null;
			}
		}

		public int getRowCount() {
			return m_exps.size();
		}

		public Object getValueAt(int row, int column) {
			Exp exp = m_exps.get( row);
			switch( column) {
				case SYMBOL:		return exp.symbol();
				case EXPIRY:		return Exp.fmtExp( exp.expiry() );
				case BOX:			return exp.fakeBoxPos();
				case CONVERSION:	return exp.fakeConversionPos();
				case EFP:			return exp.fakeEFP();
				case SYNTH:			return exp.fakeSynthPos();
				case SHORT_SYNTH:	return exp.fakeShortSynthPos();
				case FUTURE:		return exp.fakeFuturePos();
				case STOCK:			return nz( exp.under().fakeStockPos() );
				case DELTA:			return nz( exp.under().delta() );
				case HEDGE:			return nz( exp.under().hedgePos() );
				case PAY:			return Exp.fmt( exp.payAtExpiration() );
				case LIQ:			return Exp.fmt( exp.liq() );
				default:			return null;
			}
		}

		private Object nz(int i) {
			return i != 0 ? i : null;
		}
	}
}
