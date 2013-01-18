/**
 * 
 */
package db;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import lib.Util;

class HeaderRenderer extends DefaultTableCellRenderer {
    @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
    	super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
    	if( value != null) {
    		setText( Util.underline( value.toString() ) );
	    	setHorizontalAlignment( col == 0 ? SwingConstants.LEFT: SwingConstants.RIGHT);
    	}
    	return this;
    }
}