 package correlation;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import lib.OStream;
import lib.S;

import scale.close.MainBase;
import scale.profit.Bar;
import scale.profit.Bars;
import scale.profit.Result;

import com.ib.client.Contract;

public class Main extends MainBase {
	static final Main INSTANCE = new Main();
	static final int SPXID = 9999;
	static final String T = "\t";
	static final Format CORR = new DecimalFormat( ".00");
	static String fmt( double s) { return CORR.format( s); }
	static final String m_date = "20101201" + " 20:00:00"; // don't use today, symbols may not be available
	static final double MINCORR = .7;
	static final String filename = "c:\\fin\\corr2.csv";

	
	int m_id = 0; // also index into m_bars array
	Proxy m_proxy = new Proxy( this);
	Bars m_spx = new Bars("SPX");
	OStream m_os;

	// core is totally correlated
	// BETAS WERE ALL WRONG, YOU MUST RECHECK THEM
	
	// how can a pair such as this have such dif corr for actual vs. returns? ACI	ICO
	// NOW YOU NEED TO RE-CHECK THE CORRS OF ALL YOUR PAIRS

	String[] sp500 = { "A","AA","AAPL","ABC","ABT","ACE","ADBE","ADI","ADM","ADP","ADSK","AEE","AEP","AES","AET","AFL","AGN","AIG","AIV","AIZ","AKAM","AKS","ALL","ALTR","AMAT","AMD","AMGN","AMP","AMT","AMZN","AN","ANF","AON","APA","APC","APD","APH","APOL","ARG","ATI","AVB","AVP","AVY","AXP","AYE","AZO","BA","BAC","BAX","BBBY","BBT","BBY","BCR","BDX","BEN","BF B","BHI","BIG","BIIB","BK","BLL","BMC","BMS","BMY","BRCM","BRK B","BSX","BTU","BXP","C","CA","CAG","CAH","CAM","CAT","CB","CBG","CBS","CCE","CCL","CEG","CELG","CEPH","CERN","CF","CFN","CHK","CHRW","CI","CINF","CL","CLF","CLX","CMA","CMCSA","CME","CMI","CMS","CNP","CNX","COF","COG","COH","COL","COP","COST","CPB","CPWR","CRM","CSC","CSCO","CSX","CTAS","CTL","CTSH","CTXS","CVH","CVS","CVX","D","DD","DE","DELL","DF","DFS","DGX","DHI","DHR","DIS","DISCA","DNB","DNR","DO","DOV","DOW","DPS","DRI","DTE","DTV","DUK","DV","DVA","DVN","EBAY","ECL","ED","EFX","EIX","EK","EL","EMC","EMN","EMR","EOG","EP","EQR","EQT","ERTS","ESRX","ETFC","ETN","ETR","EXC","EXPD","EXPE","F","FAST","FCX","FDO","FDX","FE","FHN","FII","FIS","FISV","FITB","FLIR","FLR","FLS","FMC","FO","FRX","FSLR","FTI","FTR","GAS","GCI","GD","GE","GENZ","GILD","GIS","GLW","GME","GNW","GOOG","GPC","GPS","GR","GS","GT","GWW","HAL","HAR","HAS","HBAN","HCBK","HCN","HCP","HD","HES","HIG","HNZ","HOG","HON","HOT","HP","HPQ","HRB","HRL","HRS","HSP","HST","HSY","HUM","IBM","ICE","IFF","IGT","INTC","INTU","IP","IPG","IRM","ISRG","ITT","ITW","IVZ","JBL","JCI","JCP","JDSU","JEC","JNJ","JNPR","JNS","JPM","JWN","K","KEY","KFT","KG","KIM","KLAC","KMB","KMX","KO","KR","KSS","L","LEG","LEN","LH","LIFE","LLL","LLTC","LLY","LM","LMT","LNC","LO","LOW","LSI","LTD","LUK","LUV","LXK","M","MA","MAR","MAS","MAT","MCD","MCHP","MCK","MCO","MDP","MDT","MEE","MET","MFE","MHP","MHS","MI","MJN","MKC","MMC","MMM","MO","MOLX","MON","MOT","MRK","MRO","MS","MSFT","MTB","MU","MUR","MWV","MWW","MYL","NBL","NBR","NDAQ","NEE","NEM","NI","NKE","NOC","NOV","NOVL","NRG","NSC","NSM","NTAP","NTRS","NU","NUE","NVDA","NVLS","NWL","NWSA","NYT","NYX","ODP","OI","OKE","OMC","ORCL","ORLY","OXY","PAYX","PBCT","PBI","PCAR","PCG","PCL","PCLN","PCP","PCS","PDCO","PEG","PEP","PFE","PFG","PG","PGN","PGR","PH","PHM","PKI","PLD","PLL","PM","PNC","PNW","POM","PPG","PPL","PRU","PSA","PTV","PWR","PX","PXD","Q","QCOM","QEP","QLGC","R","RAI","RDC","RF","RHI","RHT","RL","ROK","ROP","ROST","RRC","RRD","RSG","RSH","RTN","S","SAI","SBUX","SCG","SCHW","SE","SEE","SHLD","SHW","SIAL","SJM","SLB","SLE","SLM","SNA","SNDK","SNI","SO","SPG","SPLS","SRCL","SRE","STI","STJ","STT","STZ","SUN","SVU","SWK","SWN","SWY","SYK","SYMC","SYY","T","TAP","TDC","TE","TEG","TER","TGT","THC","TIE","TIF","TJX","TLAB","TMK","TMO","TROW","TRV","TSN","TSO","TSS","TWC","TWX","TXN","TXT","TYC","UNH","UNM","UNP","UPS","URBN","USB","UTX","V","VAR","VFC","VIA B","VLO","VMC","VNO","VRSN","VTR","VZ","WAG","WAT","WDC","WEC","WFC","WFMI","WFR","WHR","WIN","WLP","WM","WMB","WMT","WPI","WPO","WU","WY","WYN","WYNN","X","XEL","XL","XLNX","XOM","XRAY","XRX","YHOO","YUM","ZION","ZMH" };
	String[] pair = { "AVB", "AYE" };
	String[] pairs = { "JLL", "CBG", "ALSK", "WIN", "WTS", "WGOV", "BTU", "PVR", "WLT", "ANR", };  
	String[] core =  { "IJS","IJT","IVE","IVW","ICF","IYR","WPS","DJP","EEM","EPP","EWA","EWJ","EWZ","EZA","FXI","IEV","ILF","RSX" };
	String[] tech = { "AAPL", "GOOG", "MSFT", "HPQ", "IBM" };
	String[] dow = { "AA","AXP","BA","BAC","CAT","CSCO","CVX","DD","DIS","GE","HD","HPQ","IBM","INTC","JNJ","JPM","KFT","KO","MCD","MMM","MRK","MSFT","PFE","PG","T","TRV","UTX","VZ","WMT","XOM" };
	String[] airlines = { "AMR", "UAL", "DAL", "LCC", "AAI", "LUV", "JBLU", "ALK", "HA", "GOL", "TAM", "RYAAY", "LFL", "RJET", "ALGT", "SKYW", "CPA", "PNCL", "ZNH", "AAR", "XJT", "GIA", "CEA" };
	String[] scales = { "CATY", "BANF", "JLL", "CBG" };
	String[] electronics = { "JST","SYPR","TASR","MEAS","OTIV","DAKT","SMTX","FARO","ESIO","ROG","CSR","SRI","MFLX","INVE","PLXS","TTMI","OSIS","VPG","AMOT","WGOV","KEM","TNL","VIDE","PKE","SRSL","BHE","VII","MVIS","CTS","SPA","DNEX","PKI","WTS","LB","MTD","ALOG","COHR","NTE","ZIGO","TMO","BTN","NEWP","ASEI","BMI","CKP","TRCI","UQM","IIVI","XRIT","MEMS" };
	String[] telecom = { "VZ","T","LVLT","Q","FTR","WIN","TNE","CTL","KT","TMX","CBB","FTE","TI","TEF","NTT","BTM","TDS","ATT","GNCMA","ALSK","CCOP","IDT","PT","TSP","TEO","TI A","TDS S","TDA","XOHO","BT","MTA","PHI","ATNI","SHEN","SURW","HTCO","TBH","TDI","CBB PRB","BTM C","NTL","WWVY","TFONY","IDT C","GNCMB" };
	String[] coal = { "MEE","PCX","ACI","BTU","CNX","ICO","ANR","WLT","JRCC","CLD","YZC","LLEN","PUDA","NRP","PVR","PVG","SCOK","MMEX","ARLP","NCOC","CHGY","AHGP","AWSRD","AENY","TGMP","RNO","HNRG","WLB","SGZH" };
	private String[] m_symbols;

	/*
	 * Things to check: price ratio, std dev ratio, correlations 
	 */
	
	Bars[] m_bars;
	Queue m_queue = new Queue();

	public static void main(String[] args) {
		INSTANCE.run();
	}

	private void run() {
		try {
			m_os = new OStream( filename, false);
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		m_symbols = readSymbols();
		
		connect(4);
		//onConnected( 0);
	}

	private String[] readSymbols() {
		try {
			return readSymbols_();
		}
		catch( Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String[] readSymbols_() throws Exception {
		FileReader fr = new FileReader( "c:\\jts\\export.csv");
		BufferedReader b2 = new BufferedReader( fr);
		
		ArrayList<String> ar = new ArrayList<String>();
		
		String line = b2.readLine();
		while( S.isNotNull( line) ) {
			StringTokenizer st = new StringTokenizer( line, ",");
			String des = st.nextToken();
			String symbol = st.nextToken();
			ar.add( symbol);
			
			line = b2.readLine();
		}
		
		return ar.toArray( new String[ar.size()]);
	}

	public void onConnected(int orderId) {
		m_bars = new Bars[m_symbols.length];
		for (int i = 0; i < m_bars.length; i++) {
			m_bars[i] = new Bars( m_symbols[i]);
		}
		
		Contract c = new Contract();
		c.m_symbol = "SPX";
		c.m_secType = "IND";
		c.m_exchange = "CBOE";
		m_proxy.req( SPXID, c, m_date);
	}

	void req() {
		Contract c = new Contract();
		c.m_symbol = m_symbols[m_id];
		c.m_secType = "STK";
		c.m_exchange = "SMART";
		c.m_primaryExch = "ISLAND";
		
		m_proxy.req( m_id++, c, m_date);
	}

	public void onHistoricalBar(int reqId, Bar bar) {
		m_proxy.onHistoricalBar( reqId, bar);
	}
	
	public void onFinishedHistoricalData(int reqId) {
		m_proxy.onFinishedHistoricalData(reqId);
	}

	public void onFinished( int reqId, Bars bars, boolean pause) {
		if( reqId == SPXID) {
			m_spx = bars;
		}
		else {
			m_bars[reqId] = bars;
		}
		
		if( reqId == m_symbols.length - 1) {
			process();
		}
		else {
			if( pause) {
				S.sleep( 60000);
			}
			m_queue.add( new Runnable() {
				public void run() {
					req();
				}
			});
		}
	}

	private void process() {	
		if( m_socket != null) {
			m_socket.eDisconnect();
		}

		int[] times = { 30, 60, 90, 180 };

		printHeaders( times);
		
		for (int i = 0; i < m_symbols.length; i++) {
			S.err( "Processing " + m_symbols[i]);
			for (int j = i + 1; j < m_symbols.length; j++) {
				correlatePairAllTimes(i, j, times);
			}
		}
		
		m_os.close();
		S.excel( filename);
		System.exit(0);
	}
	
	private void printHeaders(int[] times) {
		report( "Sym1", "Sym2");
		for( int i = 0; i < times.length; i++) {
			PairData.showHeader( m_os);
		}
		report( "Cor1 Avg", "Cor2 Avg", "");
		report( "Close 1", "Close 2", "Ratio", "");
		report( "Beta 1", "Beta 2", "Ratio", "");
		reportln( "Adj Ratio");
	}

	private void correlatePairAllTimes(int i, int j, int[] times) {
		Bars bars1In = m_bars[i];
		Bars bars2In = m_bars[j];
		
		if( bars1In.size() == 0 || bars2In.size() == 0) {
			return; 
		}
		
		TwoBars twoBars = syncBars(bars1In, bars2In); // this should be done once per pair, not per time
		
		Bars bars1 = twoBars.b1;
		Bars bars2 = twoBars.b2;

		// calculate correlations
		PairData[] pairs = new PairData[times.length];
		for( int k = 0; k < times.length; k++) {			
			pairs[k] = correlate(bars1, bars2, times[k]);
		}
		double avgCorr = avgCorr( pairs);		// correlation of closing prices
		double avgCorr2 = avgCorr2( pairs);		// correlation of daily returns
		
		// skip pairs with less than .8 correlation
		if( avgCorr < MINCORR || avgCorr2 < MINCORR) {
			return;
		}
		
		// show symbols
		report( m_symbols[i], m_symbols[j]);

		// show correlations, one for each time period
		for( PairData pair : pairs) {
			pair.show( m_os);
		}
		
		// show corr averages
		report( fmt( avgCorr), fmt( avgCorr2), "");
		
		// show closing prices and ratio
		double close1 = bars1.close();
		double close2 = bars2.close();
		double closeRatio = close1 / close2;
		report( fmt( close1), fmt( close2), fmt( closeRatio), "");

		// show beta ratios and avg
//		double avgRatio = showBetas( bars1, bars2, 180);
//
//		// show adjusted ratio
//		double adjRatio = closeRatio * avgRatio;
//		report( fmt( adjRatio) );
		
		m_os.writeln();
	}

	private PairData correlate(Bars bars1, Bars bars2, int max) {
		if( bars1.size() != bars2.size() ) {
			out( "ERROR: BARS DON'T MATCH");
			out( "" + bars1);
			out( "" + bars2);
			System.exit( 0);
		}
		
		int start = bars1.size() > max ? bars1.size() - max : 0;
		
		if( bars1.last().longTime() != bars2.last().longTime() ||
			bars1.first().longTime() != bars2.first().longTime() ) {
			out( "ERROR: BARS DON'T MATCH");
			out( "" + bars1);
			out( "" + bars2);
			System.exit( 0);
		}

		double sxy = 0;
		double sx = 0;
		double sy = 0;
		double sx2 = 0;
		double sy2 = 0;
		double sdifx = 0;
		double sdify = 0;
		double sdifxy = 0;
		double sdifx2 = 0;
		double sdify2 = 0;
		Row past = null;
		int n = 0;
		for (int k = start; k < bars1.size(); k++) {
			Bar bar1 = bars1.get(k);
			Bar bar2 = bars2.get(k);
			
			Row row = new Row(bar1.close(), bar2.close(), past);
			
			if( past != null) {
				sx += row.x;
				sy += row.y;
				sxy += row.xy;
				sx2 += row.x2;
				sy2 += row.y2;
				
				sdifx += row.difx;
				sdify += row.dify;
				sdifxy += row.difxy;
				sdifx2 += row.difx2;
				sdify2 += row.dify2;
				n++;
			}
			
			past = row;
		}

		// sanity check
		if( n != bars1.size() - start - 1) {
			out( "error");
		}
		
		double sxsq = sx * sx;
		double sysq = sy * sy;
		double corr = (n * sxy - sx * sy) / Math.sqrt( (n * sx2 - sxsq) * (n * sy2 - sysq) );
		
		double sdifxsq = sdifx * sdifx;
		double sdifysq = sdify * sdify;
		double corr2 = (n * sdifxy - sdifx * sdify) / Math.sqrt( (n* sdifx2 - sdifxsq) * (n * sdify2 - sdifysq) );
		
		double avgdifx = sdifx / n;
		double avgdify = sdify / n;
		double beta = (sdifxy - n * avgdifx * avgdify) / (sdifx2 - n * avgdifx * avgdifx);
		
		Result vol = calcVol( bars1, bars2, start, 1);
		Result vol5 = calcVol( bars1, bars2, start, 5);

		return new PairData( corr, corr2, beta, n, vol.avg(), vol.stdDev(), vol5.stdDev() );
	}
	
	private double showBetas(Bars bars1, Bars bars2, int time) { // could be uncorrelated. ???
		PairData index1 = correlate( m_spx, bars1, time); 
		PairData index2 = correlate( m_spx, bars2, time); 
		double ratio = index1.m_beta / index2.m_beta; 
		report( index1.m_beta, index2.m_beta, ratio, "");
		return ratio;
	}

	private Bars rev(Bars barsIn) {
		Bars bars = new Bars( barsIn.symbol() );
		for( int i = barsIn.size() - 1; i >= 0; i--) {
			bars.add( barsIn.get( i) );
		}
		return bars;
	}

	private Result calcVol(Bars bars1, Bars bars2, int start, int days) {
		ArrayList<Double> diffs = new ArrayList<Double>();
		
		double last = Double.MAX_VALUE;
		for( int i = start; i < bars1.size(); i += days) {
			double price =  bars1.get( i).close() - bars2.get( i).close();
			if( last != Double.MAX_VALUE) {
				double dif = price - last;
				diffs.add( dif);
			}
			last = price;
		}
		
		return S.both( diffs);
	}

	static class TwoBars {
		Bars b1;
		Bars b2;
	}
	
	private TwoBars syncBars(Bars bars1, Bars bars2) {
		Bars rev1 = rev( bars1); // could be done in place. ???
		Bars rev2 = rev( bars2);
		
		int i1 = 0;
		int i2 = 0;
		
		while( i1 < rev1.size() && i2 < rev2.size() ) {
			Bar bar1 = rev1.get( i1);
			Bar bar2 = rev2.get( i2);
			
			if( bar1.longTime() == bar2.longTime() ) {
				i1++;
				i2++;
			}
			else if( bar1.longTime() > bar2.longTime() ) {
				rev1.remove( i1);
			}
			else {
				rev2.remove( i2);
			}
		}
		
		while( i1 < rev1.size() ) {
			rev1.remove( i1);
		}

		while( i2 < rev2.size() ) {
			rev2.remove( i2);
		}
		
		if( rev1.last().longTime() != rev2.last().longTime() ||
			rev1.first().longTime() != rev2.first().longTime() ) {
			out( "not matching");
		}
		

		TwoBars twoBars = new TwoBars();
		twoBars.b1 = rev( rev1);
		twoBars.b2 = rev( rev2);
		return twoBars;
	}
	
	private double avgCorr(PairData[] pairs) {
		double tot = 0;
		for( PairData pair : pairs) {
			tot += pair.m_corr;
		}
		return tot / pairs.length;
	}

	private double avgCorr2(PairData[] pairs) {
		double tot = 0;
		for( PairData pair : pairs) {
			tot += pair.m_corr2;
		}
		return tot / pairs.length;
	}

	public void report( Object... ss) {
		m_os.report( ss);
	}

	public void reportln( Object... ss) {
		m_os.reportln( ss);
	}


}
