package arb;

import javax.swing.JTextField;

public class DoubleField extends JTextField {

	public DoubleField(int columns) {
		super( columns);
	}
	
	public DoubleField( String text, int columns) {
		super( text, columns);
	}
	
	double getDouble() throws Exception  {
		try {
			return Double.parseDouble( getText() );
		}
		catch( Exception e) {
			throw e;
		}
	}
}
