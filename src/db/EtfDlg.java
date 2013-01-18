package db;

import static db.Etfs.etfs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import lib.S;

public class EtfDlg extends JDialog implements DocumentListener {
	private final String SPACE = "   ";
	private final JTextField m_filter = new JTextField( 15);
	private final JList m_list;
	private final ChartFrame m_chart;
	Vector<String> v = new Vector<String>();

	EtfDlg(ChartFrame chartFrame) {
		super( chartFrame);
		m_chart = chartFrame;
		
		JPanel topPanel = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0) );
		topPanel.add( m_filter);
		
		m_list = new JList();
		JScrollPane scroll = new JScrollPane( m_list);
		
		add( topPanel, BorderLayout.NORTH);
		add( scroll);
		
		filter();
		
		setVisible( true);
		setSize( 400, 600);
		
		m_list.addMouseListener( new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				onClicked();
			}
		});
		
		m_filter.getDocument().addDocumentListener( this);
	}

	public void changedUpdate(DocumentEvent e) {
	}
	
	public void insertUpdate(DocumentEvent e) {
		filter();
	}
	
	public void removeUpdate(DocumentEvent e) {
		filter();
	}

	void filter() {
		String filter = m_filter.getText().toUpperCase().trim();
		
		v.clear();
		for( int i = 0; i < etfs.length; i += 2) {
			String symbol = etfs[i];
			String name = etfs[i+1];
			if( S.isNull( filter) || symbol.toUpperCase().indexOf( filter) != -1 || name.toUpperCase().indexOf( filter) != -1) {
				v.add( symbol + SPACE + name);
			}
		}
		
		m_list.setListData(v);
	}
	
	protected void onClicked() {
		int i = m_list.getSelectedIndex();
		if( i != -1) {
			String item = v.get( i);
			StringTokenizer st = new StringTokenizer( item);
			String symbol = st.nextToken();
			String name = item.substring( symbol.length() + SPACE.length() ); 
			m_chart.addSymbol( symbol, name);
		}
	}
}
