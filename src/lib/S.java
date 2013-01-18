package lib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


import scale.profit.Result;


public class S {
	static final Format CORR = new DecimalFormat( ".00");
	static final char C = ':';
	static final char COMMA = ',';
	public static final double SMALL = -Double.MAX_VALUE;
	public static final Random RND = new Random( System.currentTimeMillis() );
	private static final char Q = '"';
	public static long DAY = 1000*60*60*24;
	public static String TODAY = dateAsStr( System.currentTimeMillis() );
	public static String YESTERDAY = dateAsStr( System.currentTimeMillis() - DAY);
	public static Format FMT4 = new DecimalFormat( "0.0000");
	public static Format FMT2 = new DecimalFormat( "#,##0.00");
	private static Timer m_timer;
	private static String m_tempDir;

	/** Return a random number. When called repeatedly, numbers in 
	 *  the list will have a mean of zero and a stddev as specified. */
	public static double next( double stddev) {
		return Math.sqrt( -2 * Math.log( RND.nextDouble() ) ) * Math.cos( 2 * Math.PI * RND.nextDouble() ) * stddev;
	}

	/** Return YYYYMMDD. */
	public static String dateAsStr( long time) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( time);
		return cal.get( Calendar.YEAR) + pad( cal.get( Calendar.MONTH) + 1) + pad( cal.get( Calendar.DAY_OF_MONTH) );
	}
	
	/** @param date YYYYMMDD
	 *  @return MM/DD/YY */
	public static String userDate( String date) {
		return isNull( date) ? "" : String.format( "%s/%s/%s", date.substring( 0, 4), date.substring( 4, 6), date.substring( 6) );
	}
	
	/** Return YYYYMMDD. */
	public static String excelDateAsStr( long time) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( time);
		return cal.get( Calendar.YEAR) + "-" + pad( cal.get( Calendar.MONTH) + 1) + "-" + pad( cal.get( Calendar.DAY_OF_MONTH) );
	}
	
	/** @param date is YYYYMMDD */
	public static long getTimeInMillis( String date) {
		String year = date.substring( 0, 4);
		String month = date.substring( 4, 6);
		String day = date.substring( 6, 8);
		
		Calendar cal = GregorianCalendar.getInstance();
		cal.set( Integer.parseInt( year), Integer.parseInt( month) - 1, Integer.parseInt( day) );
		return cal.getTimeInMillis();
	}		
	
	public static int dayOfYear( long time) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( time);
		return cal.get( Calendar.DAY_OF_YEAR);
	}		
	
	public static String fmt4( double val) {
		return FMT4.format( val);
	}
	
	public static String fmt2( double val) {
		StringBuffer buf = new StringBuffer();
		buf.append( Q);
		buf.append( FMT2.format( val) );
		buf.append( Q);
		return buf.toString();
	}
	
	public static String fmt2a( double val) {
		return isValid( val) ? FMT2.format( val) : "";
	}
	
	public static boolean isValid( double val) {
		return val != Double.MAX_VALUE && val != SMALL;
	}
	
	public static String now() {
		return timeAsStr( System.currentTimeMillis() );
	}

	/** return hh:mm:ss military time */
	public static String timeAsStr( long timeInMillis) {
		Calendar cal = GregorianCalendar.getInstance();
		cal.setTimeInMillis( timeInMillis);
		int hour = cal.get( Calendar.HOUR_OF_DAY);
		hour = hour == 0 ? 12 : hour;
		return pad( hour) + C + pad( cal.get( Calendar.MINUTE) ) + C + pad( cal.get( Calendar.SECOND) );
	}

	private static String pad(int hours) {
		return hours < 10 ? "0" + String.valueOf( hours) : String.valueOf( hours);
	}

	private static String pad3(int hours) {
		return hours < 10 
			? "00" + hours 
			: hours < 100
				? "0" + hours
				: String.valueOf( hours);
	}

	public static void err( String str, Object... params) {
		err( String.format( str, params) );
	}
	
	public static void err( String str) {
		System.out.println( now() + "\t" + Thread.currentThread().getName() + "\t" + str);
	}
	
	public static void deleteFile(String pnlFilename) {
		File file = new File( pnlFilename);
		file.delete();
	}
	
	public static void sleep(int ms) {
		try {
			Thread.sleep( ms);
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void out(String filename, String string) {
		try {
			OStream os = new OStream( filename, true);
			os.writeln( string);
			os.close();
		}
		catch( Exception e) {
			err( e.toString() );
		}
	}

	public static void outNoln(String filename, String string) {
		try {
			OStream os = new OStream( filename, true);
			os.write( string);
			os.close();
		}
		catch( Exception e) {
			err( e.toString() );
		}
	}

	public static Result both(List<Double> vals) {
		double avg = average( vals);
		double stddev = stddev( vals, avg);
		return new Result( avg, stddev);
	}

	public static double stddev(List<Double> vals) {
		double avg = average( vals);
		return stddev( vals, avg);
	}
	
	public static double stddev(List<Double> vals, double avg) {
		List<Double> squares = new ArrayList<Double>();
		
		for( double high : vals) {
			double dif = high - avg;
			double sqr = Math.pow( dif, 2);
			squares.add( sqr);
		}
		
		double avgSquare = average( squares);
		double root = Math.sqrt( avgSquare);
		return root;
	}

	public static double average(List<Double> vals) {
		double total = 0;
		for( double val : vals) {
			total += val;
		}
		return total / vals.size();
	}

	public static double average(double... vals) {
		double total = 0;
		for( double high : vals) {
			total += high;
		}
		return total / vals.length;
	}

	/** @param list must be sorted */
	public static double mean(List<Double> list) {
		int i = list.size();
		if( i % 2 == 0) {
			int half = i / 2;
			double v1 = list.get( half - 1);
			double v2 = list.get( half);
			return (v1 + v2) / 2;
		}
		int half = i / 2;
		return list.get( half + 1);
	}
	
	/** Enter time in format: YYYYMMDD HH:MM:SS
	 *  Return time in millis */
    public static long getLongTime( String str) { 
        // parse date/time passed in 
        int year   = Integer.parseInt( str.substring( 0, 4) ); 
        int month  = Integer.parseInt( str.substring( 4, 6) ) - 1; // month is 0 based 
        int day    = Integer.parseInt( str.substring( 6, 8) ); 
        int hour   = Integer.parseInt( str.substring( 9, 11) ); 
        int minute = Integer.parseInt( str.substring( 12, 14) ); 
        int second = Integer.parseInt( str.substring( 15, 17) ); 

        // create calendar with date/time passed in 
        GregorianCalendar cal = new GregorianCalendar(); 
        cal.set( year, month, day, hour, minute, second); 
        cal.set( GregorianCalendar.MILLISECOND, 0); 
        
        return cal.getTimeInMillis();
    }

	public static String fmtPctb(double val) {
		return fmt2( val * 100) + "%";
	} 

	public static String fmtPct(double val) {
		return fmt2a( val * 100) + "%";
	}
	
	public static String fmtPct4(double val) {
		return fmt4( val * 100) + "%";
	}
	
	public static boolean isNull( String str) {
		return str == null || str.equals( "");
	}
	
	public static boolean isNotNull( String str) {
		return !isNull( str);
	}
	
	public static double parseDouble(String text) {
		return isNull( text) ? 0.0 : Double.parseDouble( text);
	}
	
	public static double min(double v1, double v2) {
		if( !isValid( v1) && !isValid( v2) ) {
			return Double.MAX_VALUE;
		}
		if( !isValid( v1) ) {
			return v2;
		}
		if( !isValid( v2) ) {
			return v1;
		}
		return Math.min( v1, v2);
	}
	
	public static double max(double v1, double v2) {
		if( !isValid( v1) && !isValid( v2) ) {
			return Double.MAX_VALUE;
		}
		if( !isValid( v1) ) {
			return v2;
		}
		if( !isValid( v2) ) {
			return v1;
		}
		return Math.max( v1, v2);
	}
	
	public static void exec( final int ms, final Runnable runnable) {
		if( m_timer == null) {
			m_timer = new Timer();
		}
		m_timer.schedule( new TimerTask() {
			public void run() {
				runnable.run();
			}
		}, ms);
	}

	public static String dateTimeAsStr(long date) {
		return excelDateAsStr( date) + " " + timeAsStr( date);
	}
	
	public static void clearDir( String dirname) {
		File dir = new File( dirname);
		File[] files = dir.listFiles();
		if( files != null) {
			for( File file : files) {
				file.delete();
			}
		}
	}

	public static void show(ArrayList<Double> list) {
		for( double val : list) {
			err( "" + fmt2( val) );
		}
	}

	public static String concat( Object... objs) {
		StringBuffer buf = new StringBuffer();
		for( Object obj : objs) {
			if( obj instanceof Double) {
				buf.append( fmt( (Double)obj) );
			}
			else {
				buf.append( obj);
			}
			buf.append( COMMA);
		}
		return buf.toString();
	}
	
	public static void excel( String param) {
		try {
			Runtime.getRuntime().exec( "C:\\Program Files (x86)\\Microsoft Office\\Office12\\EXCEL.EXE " + param);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static String fmt( Double s) { 
		return CORR.format( s); 
	}
	
	public static String fmt( double s) { 
		return CORR.format( s); 
	}

	/** Round to nearest penny. */
	public static double round(double d) {
		double v = d * 100 + .5;
		return Math.floor( v) / 100;
	}

	public static String getTempDir() {
		if( m_tempDir == null) {
			m_tempDir = System.getProperty( "java.io.tmpdir");
		}
		return m_tempDir;
	}
}
