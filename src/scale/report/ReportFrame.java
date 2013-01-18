package scale.report;
 

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import lib.S;
import arb.WindowAdapter;

 
public class ReportFrame extends JFrame {
    private Report m_report;
    private JButton m_button = new JButton( "Go");
    private JTabbedPane m_tabbedPane = new JTabbedPane();
	private ArrayList<String> m_unders = new ArrayList<String>();
	private ArrayListModel m_model = new ArrayListModel( m_unders);
    private JList m_list = new JList( m_model) {
    	public Dimension getPreferredScrollableViewportSize() {
    		return new Dimension( 100, 10); 
    	}
    };
	JScrollPane m_scroll = new JScrollPane( m_list);
	private Filters m_filters = new Filters();
	private String m_filename;

	ReportFrame( Report report, String filename) {
    	m_report = report;
    	m_filename = filename;
    	
    	JPanel leftPanel = new JPanel( new BorderLayout() );
    	leftPanel.add( m_button, BorderLayout.NORTH);
    	leftPanel.add( m_scroll);
    	
        add( leftPanel, BorderLayout.WEST);
        add( m_tabbedPane);
        setSize( 1300, 600);
        setTitle( "Reports");
        setVisible( true);
        setLocationRelativeTo( null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        m_list.addMouseListener( new MouseAdapter() {
        	public void mouseClicked(MouseEvent e) {
        		if( e.getClickCount() == 2) {
        			onCreateFilter();
        		}
        	}
        });
        m_tabbedPane.addMouseListener( new MouseAdapter() {
        	public void mouseClicked(MouseEvent e) {
        		if( e.getClickCount() == 2) {
        			onDeleteTab();
        		}
        	}
        });
        m_button.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onCreateFilter();
			}
        });
        addWindowListener( new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onExit();
			}
        });
    }
	
	protected void onExit() {
		writeFile();
		System.exit( 0);
	}

	/** This is called when the trades have been read in. */
	void refresh(Collection<String> unders) {
		// refresh list of symbols on left
		ArrayList list = new ArrayList( unders);
		Collections.sort( list);
		m_model.update( list);

		readFile();		
	}
	
	protected void onCreateFilter() {
		ArrayList<String> unders = new ArrayList<String>(); 
		int[] is = m_list.getSelectedIndices();
		for( int i : is) {
			unders.add( m_unders.get( i) );
		}
		
		String title = is.length == 0 ? "All" : unders.toString().replaceAll( "\\[|\\]", "");
		
		Filter filter = new Filter( title, unders);
		m_filters.add( filter);
		
		FilterPanel panel = new FilterPanel( m_report, this, filter);
		m_tabbedPane.addTab( title, panel);
		m_tabbedPane.setSelectedIndex( m_tabbedPane.getTabCount() - 1);
	}

	public void setName(FilterPanel filterPanel, String name) {
		int i = getTabIndex( filterPanel);
		m_tabbedPane.setTitleAt( i, name);
	}

	public void deletePanel(FilterPanel filterPanel) {
		int i = getTabIndex( filterPanel);
		m_tabbedPane.removeTabAt( i);
		m_filters.remove( i);
	}
	
	protected void onDeleteTab() {
		int i = m_tabbedPane.getSelectedIndex();
		m_tabbedPane.removeTabAt( i);
		m_filters.remove( i);
	}

	int getTabIndex( FilterPanel filterPanel) {
		for( int i = 0; i < m_tabbedPane.getTabCount(); i++) {
			if( m_tabbedPane.getComponentAt( i) == filterPanel) {
				return i;
			}
		}
		return -1;
	}

	private void readFile() {
        try {
        	FileInputStream is = new FileInputStream( m_filename);
        	ObjectInputStream ois = new ObjectInputStream( is);
        	m_filters = (Filters)ois.readObject();
        }
        catch( Exception e) {
			S.err( "Error " + e);
        	m_filters = new Filters();
        }

        for( Filter filter : m_filters) {
        	FilterPanel panel = new FilterPanel( m_report, this, filter);
        	m_tabbedPane.addTab( filter.name(), panel);
        }
	}

	private void writeFile() {
		try {
			FileOutputStream os = new FileOutputStream( m_filename);
			ObjectOutputStream oos = new ObjectOutputStream( os);
			oos.writeObject( m_filters);
			oos.close();
		}
		catch( Exception e) {
			S.err( "Error " + e);
		}
	}
	
	static class Filters extends ArrayList<Filter> implements Serializable {
		private static final long serialVersionUID = 1L;
	}
}
