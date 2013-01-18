package arb;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class Lab extends JLabel {
	Lab() {
		this( 7);
	}
	
	Lab( int chars) {
		setBackground( Color.WHITE);
		setOpaque(true);
		setPreferredSize( new Dimension( chars * 10, 17) );  
		setHorizontalAlignment(SwingConstants.RIGHT);
		
		setBorder( new EmptyBorder( 0, 0, 0, 10) );
	}
}
