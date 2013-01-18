/*
 * EWrapper.java
 *
 */
package com.ib.client;

import scale.profit.Bar;

public interface EWrapper extends AnyWrapper {

    ///////////////////////////////////////////////////////////////////////
    // Interface methods
    ///////////////////////////////////////////////////////////////////////
    void tickPrice( int tickerId, int field, double price, int canAutoExecute);
    void tickSize( int tickerId, int field, int size);
    void tickOptionComputation( int tickerId, int field, double impliedVol,
    		double delta, double modelPrice, double pvDividend);
    void tickGeneric(int tickerId, int tickType, double value);
	void tickString(int tickerId, int tickType, String value);
	void tickEFP(int tickerId, int tickType, double basisPoints,
			String formattedBasisPoints, double impliedFuture, int holdDays,
			String futureExpiry, double dividendImpact, double dividendsToExpiry);
    void onOrderStatus( int orderId, String status, int filled, int remaining,
            double avgFillPrice, int permId, int parentId, double lastFillPrice,
            int clientId, String whyHeld);
    void onOpenOrder( int orderId, Contract contract, Order order, OrderState orderState);
    void updateAccountValue(String key, String value, String currency, String accountName);
    void onPositionUpdate(Contract contract, int position, double marketPrice, double marketValue,
            double averageCost, double unrealizedPNL, double realizedPNL, String accountName);
    void updateAccountTime(String timeStamp);
    void onConnected( int orderId);
    void contractDetails(int reqId, ContractDetails contractDetails);
    void bondContractDetails(int reqId, ContractDetails contractDetails);
    void contractDetailsEnd(int reqId);
    void onTrade( int orderId, Contract contract, Execution execution);
    void updateMktDepth( int tickerId, int position, int operation, int side, double price, int size);
    void updateMktDepthL2( int tickerId, int position, String marketMaker, int operation,
    		int side, double price, int size);
    void updateNewsBulletin( int msgId, int msgType, String message, String origExchange);
    void managedAccounts( String accountsList);
    void receiveFA(int faDataType, String xml);
    void onHistoricalBar(int reqId, Bar bar);
    void onFinishedHistoricalData(int reqId);
    void scannerParameters(String xml);
    void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance,
    		String benchmark, String projection, String legsStr);
    void scannerDataEnd(int reqId);
    void onRealtimeBar(int reqId, Bar bar);
    void currentTime(long time);
    void fundamentalData(int reqId, String data);
}