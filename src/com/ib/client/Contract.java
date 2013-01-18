/*
 * Contract.java
 *
 */
package com.ib.client;

import java.util.Vector;

import lib.S;


import arb.Legs;

public class Contract implements Cloneable {

	public int    m_conId;
    public String m_symbol; // underlying symbol
    public String m_secType;
    private String m_expiry;
    public double m_strike;
    public String m_right;
    public String m_multiplier;
    public String m_exchange;
    public String m_currency;
    public String m_localSymbol;
    public String m_primaryExch;      // pick a non-aggregate (ie not the SMART exchange) exchange that the contract trades on.  DO NOT SET TO SMART.
    public boolean m_includeExpired;  // can not be set to true for orders.
    public String m_comboLegsDescrip; // received in open order version 14 and up for all combos
    public Legs m_legs = new Legs();
    public UnderComp m_underComp;

    public Vector comboLegs()  { return m_legs.comboLegs(); }
	public boolean hasLegs() { return m_legs.size() > 0; }
	public String getFirstExp() { return m_legs.expiry(); }
	public void legs(Legs legs) { m_legs = legs; }
	public Legs legs() { return m_legs; }
	public void expiry(String v) { m_expiry = v; }

    public Contract() {
    }

    public Contract(int conId, String symbol, String secType, String expiry,
                    double strike, String right, String multiplier,
                    String exchange, String currency, String localSymbol,
                    Legs comboLegs, String primaryExch, boolean includeExpired) {
    	m_conId = conId;
        m_symbol = symbol;
        m_secType = secType;
        m_expiry = expiry;
        m_strike = strike;
        m_right = right;
        m_multiplier = multiplier;
        m_exchange = exchange;
        m_currency = currency;
        m_includeExpired = includeExpired;
        m_localSymbol = localSymbol;
        m_legs = comboLegs;
        m_primaryExch = primaryExch;
    }

    @Override
    public String toString() {
    	return m_legs.size() > 0
    		? m_symbol + " " + m_legs.toString()
    		: m_symbol + " " + m_secType + " " + m_exchange + " " + m_expiry + " " + m_strike + " " + m_right;
    }

	public double getFirstStrike() { return m_legs.getFirstStrike(); }
	public String expiry() { return m_expiry; }
	
	public String getExpiry() {
		return m_secType.equals( "BAG")
			? m_legs.expiry()
			: m_expiry; 
	}

	public boolean hasExpiry() {
		return m_expiry != null && m_expiry.length() > 0;
	}

	public void repairExchange() {
		if( S.isNull( m_exchange) ) {
			m_exchange = m_primaryExch;
		}
	}

	/** This is wrong for combo. fix it. ??? */
    public double multiplier() { 
    	return !S.isNull( m_multiplier) ? Double.parseDouble( m_multiplier) : 1; 
    }

}
