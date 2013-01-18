/*
 * ExecutionFilter.java
 *
 */
package com.ib.client;

public class ExecutionFilter{
	public static final ExecutionFilter ALL = new ExecutionFilter();
	
    public int 		m_clientId;
    public String 	m_acctCode;
    public String 	m_time;
    public String 	m_symbol;
    public String 	m_secType;
    public String 	m_exchange;
    public String 	m_side;

    public ExecutionFilter() {
        m_clientId = 0;
    }

    public ExecutionFilter( String symbol) {
    	m_symbol = symbol;
    }

    public ExecutionFilter( int clientId, String acctCode, String time,
    		String symbol, String secType, String exchange, String side) {
        m_clientId = clientId;
        m_acctCode = acctCode;
        m_time = time;
        m_symbol = symbol;
        m_secType = secType;
        m_exchange = exchange;
        m_side = side;
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
            ExecutionFilter l_theOther = (ExecutionFilter)other;
            l_bRetVal = (m_clientId == l_theOther.m_clientId &&
                    m_acctCode.equalsIgnoreCase( l_theOther.m_acctCode) &&
                    m_time.equalsIgnoreCase( l_theOther.m_time) &&
                    m_symbol.equalsIgnoreCase( l_theOther.m_symbol) &&
                    m_secType.equalsIgnoreCase( l_theOther.m_secType) &&
                    m_exchange.equalsIgnoreCase( l_theOther.m_exchange) &&
                    m_side.equalsIgnoreCase( l_theOther.m_side) );
        }
        return l_bRetVal;
    }
}