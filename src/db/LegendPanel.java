package db;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import lib.S;
import lib.Util;

public class LegendPanel extends JPanel {
	private final LegendModel m_model = new LegendModel();
	private final ChartFrame m_frame;

	// getters
	public LegendModel model()			{ return m_model; }

	// helpers
	private ChartData chartData() 		{ return m_frame.chartData(); }

	LegendPanel(ChartFrame frame) {
		super( new BorderLayout() );
		m_frame = frame;

		LegendTable legendTable = new LegendTable( m_model);
		legendTable.setBackground( getBackground() );
		
		add( legendTable.getTableHeader(), BorderLayout.NORTH);
		add( legendTable);
	}

	enum Col { 
		SYM, TOT, ANN, VOL, ALF, BET, RSQ, SHP, AGG, MLT, CLS;

		static Col getCol( int i) {
			for( Col col : Col.values() ) {
				if( col.ordinal() == i) {
					return col;
				}
			}
			return Col.SYM;
		}
	};
	
	
	class LegendModel extends ModelBase {
		public int getColumnCount() { return Col.values().length; }
		public int getRowCount() { return chartData().getSize(); }

		@Override public String getColumnName(int colIn) {
			Col col = Col.getCol( colIn);
			
			switch( col) {
				case SYM: return "Symbol";
				case TOT: return "Total";
				case ANN: return "Annual";
				case VOL: return "Vol";
				case ALF: return "Alpha";
				case BET: return "Beta";
				case SHP: return "Sharpe";
				case RSQ: return "R-Sqrd";
				case AGG: return "Agg";
				case MLT: return "Mult"; 
				default:  return null;
			}
		}

		public Object getValueAt(int row, int colIn) {
			Col col = Col.getCol( colIn);
			Attribs attribs = chartData().getAttribs( row);
			
			switch( col) {
				case SYM: return attribs.symbolForDisplay();
				case ANN: return Util.fmtPct( chartData().getAnnual( row) );
				case TOT: return Util.fmtPct( chartData().getTotal( row) );
				case VOL: return Util.fmtPct( chartData().getDailyVol( row) );
				case ALF: return Util.fmtPct( attribs.alpha() );
				case BET: return Util.fmt( attribs.beta() );
				case SHP: return Util.fmt( attribs.sharpe() );
				case RSQ: return Util.fmtPct( attribs.rSquared() );
				case AGG: return attribs.agg() ? "Y" : null;
				case MLT: return attribs.agg() ? attribs.aggShares() : null;
				case CLS: return "X";
				default:  return null;
			}
		}
		
		public boolean isCellEditable(int row, int colIn) {
			Col col = Col.getCol( colIn);
			return col == Col.MLT;
		}
		
		public void setValueAt(Object val, int row, int colIn) {
			Col col = Col.getCol( colIn);
			if( col == Col.MLT) {
				Attribs attribs = chartData().getAttribs( row);
				double v = S.isNull( (String)val) ? 0 : Double.parseDouble( (String)val); 
				attribs.mult( v);
				m_frame.reset();
			}
		}
		
		public String getCellToolTipText(int row, int col) {
			Attribs attribs = chartData().getAttribs( row);
			return attribs.name();
		}

		public void onClicked(int row, int colIn) {
			Attribs attribs = chartData().getAttribs( row);
			
			Col col = Col.getCol( colIn);
			switch( col) {
				case AGG:
					if( !attribs.isAgg() ) {
						attribs.toggleAgg();
					}
					m_frame.reset();
					break;
					
				case CLS:
					if( attribs.isAgg() ) {
						// can't remove
					}
					else {
						chartData().removeAttribs( row);
					}
					m_frame.reset();
					break;
			}
		}
		
		void onHeaderClicked( int colIn) {
			Col col = Col.getCol( colIn);
			switch( col) {
				case AGG: 
					chartData().onAggHeaderClicked();
					m_frame.reset();
					break;
			}
		}

		public void setWidths(LegendTable table) {
			setWidth( table, Col.SYM, 45);
			setWidth( table, Col.TOT, 60);
			setWidth( table, Col.ANN, 55);
			setWidth( table, Col.VOL, 50);
			setWidth( table, Col.ALF, 55);
			setWidth( table, Col.BET, 45);
			setWidth( table, Col.SHP, 55);
			setWidth( table, Col.RSQ, 55);
			setWidth( table, Col.AGG, 30);
			setWidth( table, Col.CLS, 20);
			
		}
	}

	private static class LegendRenderer extends DefaultTableCellRenderer {
	    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
	    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
	    	setForeground( ((LegendTable)table).chartData().getColor( row) );
	    	setHorizontalAlignment( col == 0 ? SwingConstants.LEFT : SwingConstants.RIGHT);
	    	return this;
	    }
	}
	
	private class LegendTable extends TableBase {
		private LegendRenderer m_renderer = new LegendRenderer();

		public ChartData chartData() 	{ return LegendPanel.this.chartData(); }
		public LegendModel getModel() 	{ return (LegendModel)super.getModel(); }

		public LegendTable(LegendModel model) {
			super( model);

			model.setWidths( this);

		}
		
		public TableCellRenderer getCellRenderer(int row, int column) { 
			return m_renderer; 
		}
		
		@Override public String getToolTipText(MouseEvent e) {
			int row = rowAtPoint( e.getPoint() );
			int col = columnAtPoint( e.getPoint() );
			col = convertColumnIndexToModel( col);
			return getModel().getCellToolTipText( row, col);
		}
		
		@Override protected void onClicked(int row, int col) {
			getModel().onClicked(row, col);
		}
		
		@Override protected void onHeaderClicked(int col) {
			getModel().onHeaderClicked(col);
		}
	}
}
