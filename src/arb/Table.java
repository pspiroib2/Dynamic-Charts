package arb;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/** Left-align letters, right-align all else.
 *  Maintains selection during refresh. */
public class Table extends JTable {

	public Table(AbstractTableModel model) {
		super( model);
		
		addMouseListener( new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				onMouseClicked( e);
			}
		});
	}

	protected void onMouseClicked(MouseEvent e) {
		int row = getSelectedRow();
		int col = getSelectedColumn();

		if( SwingUtilities.isLeftMouseButton( e) ) {
			onLeftClick( row, col);
		}
		else if( SwingUtilities.isRightMouseButton( e) ) {
			onRightClick( row, col);
		}
	}
	
	ArbModel arbModel() {
		if( dataModel instanceof ArbModel) {
			return (ArbModel)dataModel;
		}
		return null;
	}

	protected void onLeftClick(int row, int col) {
		ArbModel model = arbModel();
		if( model != null) {
			model.onLeftClick(row, col);
		}
	}

	protected void onRightClick(int row, int col) {
		ArbModel model = arbModel();
		if( model != null) {
			model.onRightClick(row, col);
		}
	}

	public AbstractTableModel getModel() {
		return (AbstractTableModel)super.getModel();
	}
	
	void fireTableDataChanged() {
		SwingUtilities.invokeLater( new Runnable() {
			public void run() {
				fireTableDataChanged_();
			}
		});
	}
	
	void fireTableDataChanged_() {
		int[] rows = getSelectedRows();
		int col = getSelectedColumn();
		getModel().fireTableDataChanged();
		
		for( int row : rows) {
			selectionModel.addSelectionInterval( row, row);
		}
		columnModel.getSelectionModel().setSelectionInterval( col, col);		
	}
	
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return Renderer.INSTANCE;
	}

	/** Left-align letters, right-align all else. */
	static class Renderer extends DefaultTableCellRenderer {
		static Renderer INSTANCE = new Renderer();
		
		public Component getTableCellRendererComponent(JTable table, Object val, boolean isSelected, boolean hasFocus, int row, int column) {
			super.getTableCellRendererComponent(table, val, isSelected, hasFocus, row, column); 
			
			int alignment = val != null && val.toString().length() > 0 && Character.isLetter( val.toString().charAt( 0) )
				? LEFT : RIGHT;
			setHorizontalAlignment( alignment);
			
			return this;
		}
	}
}
