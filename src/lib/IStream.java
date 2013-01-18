/**
 * 
 */
package lib;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class IStream {
	private BufferedReader m_br;
	
	public IStream( String file) throws FileNotFoundException {
		m_br = new BufferedReader( new FileReader( file) );
	}
	
	public String readln() {
		try {
			return m_br.readLine();
		}
		catch( Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}