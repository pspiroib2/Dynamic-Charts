package lib;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class OStream extends FileOutputStream {
	public static OStream create( String name, boolean append) {
		try {
			return new OStream( name, append);
		} 
		catch (FileNotFoundException e) {
			System.err.println( e.toString() );
			return null;
		}
	}
	
	public OStream(String name, boolean append) throws FileNotFoundException {
		super(name, append);
	}

	public void write( String line) {
		try {
			write( line.getBytes() );
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeln() {
		writeln( "");
	}

	public void writeln( String line) {
		try {
			write( (line + "\r\n").getBytes() );
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void log( String str) {
		writeln( Thread.currentThread().getName() + "\t" + S.now() + "\t" + str);
	}

	public void log2( String str) {
		writeln( S.now() + "," + str);
	}

	public void reportln( Object... fields) {
		report( fields);
		writeln(); 
	}

	public void report( Object... fields) {
		write( S.concat( fields) ); 
	}

	@Override public void close() {
		try {
			super.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
