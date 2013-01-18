package scale.vol;

import java.text.DecimalFormat;
import java.util.ArrayList;

import lib.S;

import scale.close.MainBase;
import scale.profit.Bar;
import scale.profit.Bars;
import scale.profit.Result;

import com.ib.client.Contract;

// Question: why is weekly vol < daily vol * sqrt(5)?
// Is there really reversion to mean?
// What is the meaningfulness of the avg price change not really being zero?
// What is the significance of using continuous compounding?



public class CalcVol extends MainBase {
	static final CalcVol INSTANCE = new CalcVol();
	private Bars m_bars = new Bars();
	DecimalFormat m_format = new DecimalFormat( "0.000000");
	
	public static void main(String[] args) {
		INSTANCE.runReal();
		//INSTANCE.runSim();
	}
	
	private void runReal() {
		INSTANCE.connect( 4);
		S.sleep(5000);
		//System.exit( 0);
	}

	private void runSim() {
		double midpoint = 30;
		m_bars.create(midpoint, .02, 3000, 5);
		onFinishedHistoricalData( 0);
	}
	
// try with and without after-hours flag. ???
	
	public void onConnected(int orderId) {
		Contract c = new Contract();
		c.m_symbol = "OEF"; 
		c.m_secType = "STK";
		c.m_exchange = "SMART";
		c.m_primaryExch = "ARCA";
		
		//for( int d = 11; d <= 11; d++) {
		String date = "20100119" + " 20:00:00";
		
		// 1 year of 1 day bars 
		m_socket.reqHistoricalData(100, c, date, "1 Y", "1 day", "TRADES", 0, 1);
		
		// 1 day of 30 second bars
			//m_socket.reqHistoricalData( m_id++, contract, date, "3600 S", "1 hour", "TRADES", 1, 1);
			//OStream.sleep( 1000);
		//}
	}

	public void onHistoricalBar(int reqId, Bar bar) {
		out( "" + bar);
		m_bars.add( bar);
	}
	
	public void onFinishedHistoricalData(int reqId) {
//		out( "Received " + m_bars.size() + " bars");
//		out( "First: " + m_bars.get( 0).timeAsStr() );
//		out( "Last: " + m_bars.get( m_bars.size() - 1).timeAsStr() );
//		double singleBar = calcTimedVolatility( 1, 0);
//		calcTimedVolatility( 2, singleBar);
//		calcTimedVolatility( 4, singleBar);
//		calcTimedVolatility( 8, singleBar);
//		calcTimedVolatility( 16, singleBar);
//		calcTimedVolatility( 30, singleBar);
//		calcTimedVolatility( 64, singleBar);
//		calcTimedVolatility( 128, singleBar);
//		calcTimedVolatility( 256, singleBar);
//		calcTimedVolatility( 23400/30, singleBar);
	}

	double calcTimedVolatility( int numBars, double singleBar) {		
		Result result = m_bars.calcVol( numBars);
		double vol = result.stdDev();
		double projected = singleBar * Math.sqrt( numBars);
		double oneDay = vol / Math.sqrt( numBars);
		double oneDayDif = Math.abs( oneDay - singleBar);

		out( "---------------");
		out( "Time period: " + numBars + " bars");
		out( "Avg pct change is: " + fmt( result.avg() ) + "%");
		out( "Std dev is: " + fmt( result.stdDev() ) + "%");
		out( "Projected std dev is is: " + fmt( projected) + "%"); // what it should be based on 1-bar value
		out( "");
		out( "Projected back one day is " + fmt( oneDay) ); // what 1-bar should be based on this bar size
		out( "Dif per one day is " + fmt( oneDayDif) );
		
		return result.stdDev();
	}

	private String fmt(double val) {
		return m_format.format( val);
	}
}
