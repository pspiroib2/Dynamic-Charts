/**
 * 
 */
package db;

import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.TableModel;

class TableBase extends JTable {
	protected void onHeaderClicked(int col) {}
	protected void onClicked( int row, int col) {}

	TableBase(TableModel model) {
		super( model);
		setShowGrid( false);
		getTableHeader().setDefaultRenderer( ChartFrame.HEADER);
		setAutoResizeMode( JTable.AUTO_RESIZE_OFF);
		setRowSelectionAllowed( false);
		setIntercellSpacing( new Dimension( 0, 1) );
		
		getTableHeader().addMouseListener( new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				onHeaderClicked( columnAtPoint( e.getPoint() ) );
			}
		});

		addMouseListener( new MouseAdapter() {
			@Override public void mouseReleased(MouseEvent e) {
				int row = rowAtPoint( e.getPoint() );
				int col = columnAtPoint( e.getPoint() );
				col = convertColumnIndexToModel( col);
				onClicked( row, col);
			}
		});
	}

}