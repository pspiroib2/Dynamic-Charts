package db;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;

import javax.swing.JLabel;

import lib.Util;

public class HtmlButton extends JLabel {
	private String m_text;
	protected boolean m_selected;
	private ActionListener m_al;

	public boolean isSelected() { return m_selected; }

	public void addActionListener(ActionListener v) { m_al = v; }

	public HtmlButton( String text) {
		super( text);
		m_text = text;
		
		setForeground( Color.blue);

		addMouseListener( new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				onClicked();
			}
		});
		
		setFont( getFont().deriveFont( Font.PLAIN) );
	}
	
	public void setSelected( boolean v) {
		m_selected = v;
		setText( v ? Util.underline( m_text) : m_text);
	}
	
	protected void onClicked() {
		if( m_al != null) {
			m_al.actionPerformed( null);
		}
	}

	public static class HtmlCheckBox extends HtmlButton {
		public HtmlCheckBox( String str) {
			super( str);
		}
		
		@Override protected void onClicked() {
			setSelected( !m_selected);
			super.onClicked();
		}
	}
	
	public static class HtmlRadioButton extends HtmlButton {
		private HashSet<HtmlRadioButton> m_group;

		HtmlRadioButton( String text, HashSet<HtmlRadioButton> group) {
			super( text);
			m_group = group;
			group.add( this);
		}
		
		@Override protected void onClicked() {
			for( HtmlRadioButton but : m_group) {
				but.setSelected( false);
			}
			setSelected( true);
			super.onClicked();
		}
	}
}
