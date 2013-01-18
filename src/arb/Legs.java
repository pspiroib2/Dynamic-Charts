/**
 *
 */
package arb;

import java.util.ArrayList;
import java.util.Vector;

import lib.S;


import com.ib.client.ComboLeg;

public class Legs extends ArrayList<Leg> {
	private String m_name;

	// helpers
	public double bid() 		{ return bidOrAsk( false); }
	public double ask() 		{ return bidOrAsk( true); }
	public boolean validAsk() 	{ return bid() != Double.MAX_VALUE; }
	public boolean validBid() 	{ return ask() != Double.MAX_VALUE; }
	
	public Legs() {
	}
	
	public Legs(String name) {
		m_name = name;
	}

	@Override public String toString() {
		if( !S.isNull( m_name) ) {
			return m_name;
		}
		
		return super.toString();
	}
	
	private String strike( int legIndex) {
		return Exp.fmtSt( get( legIndex).data().contract().m_strike);
	}
	
	void add( boolean buy, int ratio, Data data) {
		if( ratio != 1 && ratio != 100) {
			Arb.err( "Only ratios of 1 and 100 accepted");
			System.exit( 0);
		}

		ComboLeg leg = new ComboLeg();
		leg.m_conId = data.conid();
		leg.m_exchange = data.exchange();
		leg.m_ratio = ratio;
		leg.m_action = buy ? "BUY" : "SELL";
		
		add( new Leg( data, leg) );
	}
	
	public double model() {
		double price = 0;
		
		for( Leg leg : this) {
			double model = leg.data().model();
			
			if( model == Double.MAX_VALUE) {
				return Double.MAX_VALUE;
			}
			
			price += leg.comboLeg().isBuy() ? model : -model;
		}
		
		return price;
	}

	public double bidOrAsk( boolean buyingCombo) {
		double total = 0;

		for( Leg leg : this) {
			Data data = leg.data();
			
			double price = buyingCombo
				? leg.comboLeg().isBuy() ? data.ask() : -data.bid()
				: leg.comboLeg().isBuy() ? data.bid() : -data.ask();
				
			if( !S.isValid( price) ) {
				return Double.MAX_VALUE;
			}

			total += price;
		}

		return total;
	}

	public double getFirstStrike() {
		for( Leg leg : this) {
			if( leg.contract().m_secType.equals( "OPT") ) {
				return leg.contract().m_strike;
			}
		}
		Arb.err( "Error: no strike");
		return 0;
	}
	
	public String expiry() {
		for( Leg leg : this) {
			if( leg.data().m_contract.hasExpiry() ) {
				return leg.data().m_contract.expiry();
			}
		}
		return null;
	}

	public void add(Legs legs) {
		for( Leg leg : legs) {
			add( leg);
		}
	}
	
	public void flip(Legs legs) {
		for( Leg leg : legs) {
			ComboLeg comboLeg = leg.comboLeg().clone();
			comboLeg.flip();
			add( new Leg( leg.data(), comboLeg) );
		}
	}
	
	public String getFirstSymbol() { 
		return get( 0).contract().m_symbol; 
	}
	
	public Vector<ComboLeg> comboLegs() {
		Vector<ComboLeg> v = new Vector<ComboLeg>();
		
		for( Leg leg : this) {
			v.add( leg.comboLeg() );
		}
		
		return v;
	}
}
