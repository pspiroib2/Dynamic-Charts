/**
 * 
 */
package db;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import db.LegendPanel.Col;

abstract class ModelBase extends AbstractTableModel {
	protected void setWidth(JTable table, Col col, int width) {
		setWidth( table, col.ordinal(), width);
	}
	protected void setWidth(JTable table, int col, int width) {
		table.getColumnModel().getColumn( col).setPreferredWidth( width);
	}
}