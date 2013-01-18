/**
 * 
 */
package scale.report;

import java.util.Collection;
import java.util.List;

import javax.swing.AbstractListModel;

class ArrayListModel extends AbstractListModel {
	private List m_list;
	
	ArrayListModel( List list) {
		m_list = list;
	}
	public Object getElementAt(int index) {
		return m_list.get( index);
	}
	public int getSize() {
		return m_list.size();
	}
	void update( Collection list) {
		m_list.clear();
		m_list.addAll( list);
		fireContentsChanged(this, 0, m_list.size() );
	}
}