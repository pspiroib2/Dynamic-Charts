package scale.report;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import lib.Util;
import scale.report.FilterPanel.Table;

public class DetailFrame extends JFrame {
	private Collection<Scale> m_scales;
	private ArrayList<Rec> m_recs = new ArrayList<Rec>();
	private JCheckBox m_open = new JCheckBox( "Show open positions");
	private JCheckBox m_closed = new JCheckBox( "Show closed positions");
	private Model m_model = new Model();
	ArrayList<Underlying> m_unders = new ArrayList<Underlying>();

	DetailFrame( Collection<Scale> scales) {
		m_scales = scales;

		// build list of underlyings and sort them
		for( Scale scale : m_scales) {
			if( !m_unders.contains( scale.underlying() ) ) {
				m_unders.add( scale.underlying() );
			}
		}
		Collections.sort( m_unders);

		// show open and close positions by default
		m_open.setSelected( true);
		m_closed.setSelected( true);

		// build list of records
		refresh();

		// build checkbox panel
		JPanel checkboxPanel = new JPanel();
		checkboxPanel.add( m_open);
		checkboxPanel.add( m_closed);
		
		// create table
		JTable table = new Table( m_model, 0);
		JScrollPane scroll = new JScrollPane( table);
		
		// build this frame
		add( checkboxPanel, BorderLayout.NORTH);
		add( scroll);
		setSize( 700, 300);
		setVisible( true);
		
		m_open.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
		m_closed.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refresh();
			}
		});
	}

	protected void refresh() {
		m_recs.clear();
		
		// build list of records 
		double[] totals = new double[3];
		for( Underlying under : m_unders) {
			double[] pnls = new double[3];
			pnls[0] = under.oldUnreal(); 
			pnls[1] = under.totalReal(); 
			pnls[2] = under.unreal();
			
			Rec rec = new Rec( under.symbol(), under.stockPos(), under.optPos(), pnls[0], pnls[1], pnls[2]);
			if( rec.valid() ) {
				boolean hasPosition = rec.hasPosition();
				if( hasPosition && m_open.isSelected() || !hasPosition && m_closed.isSelected() ) {
					m_recs.add( rec);
					totals[0] += pnls[0];
					totals[1] += pnls[1];
					totals[2] += pnls[2];
				}
			}
		}

		// add total row
		m_recs.add( new Rec( "Total", 0, 0, totals[0], totals[1], totals[2]) ); 

		m_model.fireTableDataChanged();
	}



	static class Rec {
		private String m_symbol;
		private int m_stockPos;
		private int m_optPos;
		private double m_totalReal;
		private double m_unrealStart;
		private double m_unrealEnd;
		
		public double unrealChange()	{ return m_unrealEnd - m_unrealStart; }
		public double totalChange() 	{ return m_totalReal + unrealChange(); } // change in total
		public boolean hasPosition() 	{ return m_stockPos != 0 || m_optPos != 0; }

		public Rec(String symbol, int stockPos, int optPos, double unrealStart, double totalReal, double unrealEnd) {
			m_symbol = symbol;
			m_stockPos = stockPos;
			m_optPos = optPos;
			m_unrealStart = unrealStart;
			m_totalReal = totalReal;
			m_unrealEnd = unrealEnd;
		}

		public boolean valid() {
			return m_stockPos != 0 || m_optPos != 0 || m_totalReal != 0;
		}
	}
	
	class Model extends AbstractTableModel {
		@Override public int getColumnCount() {
			return 8;
		}
		
		@Override public String getColumnName(int col) {
			switch( col) {
				case 0: return "Symbol";
				case 1: return "Stock Pos";
				case 2: return "Opt Pos";
				case 3: return "Unreal Start";
				case 4: return "Real";
				case 5: return "Unreal End";
				case 6: return "Unreal Chg";
				case 7: return "Total Chg";
				default: return null;
			}
		}

		@Override public int getRowCount() {
			return m_recs.size();
		}

		@Override public Object getValueAt(int row, int col) {
			Rec rec = m_recs.get( row);
			switch( col) {
				case 0: return rec.m_symbol;
				case 1: return rec.m_stockPos;
				case 2: return rec.m_optPos;
				case 3: return Util.fmtShort( rec.m_unrealStart);
				case 4: return Util.fmtShort( rec.m_totalReal);
				case 5: return Util.fmtShort( rec.m_unrealEnd);
				case 6: return Util.fmtShort( rec.unrealChange() );
				case 7: return Util.fmtShort( rec.totalChange() );
				default: return null;
			}
		}
	}
	
	
}
