package com.ib.client;

import java.util.StringTokenizer;

import lib.S;


public class Execution implements Comparable<Execution> {
    public int m_orderId;
    public int m_clientId;
    public String m_execId;
    private String m_date;
    private String m_time;
    public String m_acctNumber;
    public String m_exchange;
    public String m_side;
    public int m_shares;
    public double m_price;
    public int m_permId;
    public int m_liquidation;
    public int m_cumQty;
    public double m_avgPrice;
    
    // get
    public String date() { return m_date; }
    public String time() { return m_time; }
    
    // set
    public void date( String v) { m_date = v; }
    public void time( String v) { m_time = v; }
    
    public boolean isBuy() { return m_side.equals( "BOT"); }

    public void dateTime( String dateTime) {
	    StringTokenizer st = new StringTokenizer( dateTime, " ");
		m_date = st.nextToken();
	    m_time = st.nextToken();
    }

    public boolean equals(Object other) {
        boolean l_bRetVal = false;

        if ( other == null ) {
            l_bRetVal = false;
		}
        else if ( this == other ) {
            l_bRetVal = true;
        }
        else {
            Execution l_theOther = (Execution)other;
            l_bRetVal = m_execId.equals( l_theOther.m_execId);
        }
        return l_bRetVal;
    }

	public boolean today() {
		return m_date.equals( S.TODAY);
	}

	public int compareTo(Execution o) {
		int v = m_date.compareTo( o.m_date);
		if( v == 0) {
			v = m_time.compareTo( o.m_time);
		}
		return v;
	}
}
