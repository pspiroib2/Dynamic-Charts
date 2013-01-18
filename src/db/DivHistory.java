package db;

import java.util.ArrayList;
import java.util.Collection;

import lib.JtsCalendar;
import lib.S;
import db.PriceDb.BadSymbol;
import db.PriceDb.BarType;

/** This program looks to see how much return you make off your original investment as the dividend changes.
 *  Preferred stocks don't work. */
public class DivHistory {
	public static void main(String[] args) {
		try {
			new DivHistory().run( args);
		}
		catch( Exception e) {
			e.printStackTrace();
		}
	}
	static int DIVS_PER_YEAR = 12;
	static String[] SYMS = { 
		//"OTT", it just dropped off a cliff
		//"VLCCF", trending down for a long time
		"WHX",
		"OXF",
		"LPHI",
		"CEL",
		"CFP",
		"DCIX",
		"PT",
		"CLM",
		"MTLPR",
		"CRF",
		"WHZ",
		"CTEL",
		"DHT",
		"ARR",
		"EFC",
		"AI",
		"CIM",
		"AMTG",
		"AOD",
		"MTGE",
		"TEU",
		"EC",
		"OXLC",
		"AGNC",
		"NAI",
		"RNF",
		"TWO",
		"YPF",
		"RSO",
		"CYS",
		"MNDO",
		"BDCL",
		"BGCP",
		"EOD",
		"TNP",
		"NLR",
		"IVR",
		"VOC",
		"NLY",
		"HTS",
		"CWH",
		"RNO",
		"AGD",
		"IGD",
		"NMM",
		"FTE",
		"IRS",
		"REM",
		"TAN",
		"MITT",
		"CPLP",
		"NCZ",
		"EDUC",
		"MCGC",
		"KCAP",
		"IRR",
		"GGN",
		"BGY",
		"NCV",
		"BOE",
		"RNDY",
		"LRE",
		"NRF",
		"TICC",
		"CHKR",
		"MFA",
		"STK",
		"EWP",
		"CMO",
		"SAN",
		//"DNI", changed from 1/month to 4/year recently
		"DX",
		"SBLK",
		"NKA",
		"GLBS",
		"PSEC",
		"PER",
		"MCC",
		"ECT",
		"EXG",
		"PBI",
		"QRE",
		"ANH",
		"JE",
		"IGA",
		"HRZN",
		"GNT",
		"FSC",
		"TEF",
		"MEMP",
		"GGT",
		"IHD",
		"CXS",
		"BKCC",
		"TNK",
		"WIN",
		"CH",
		"IID",
		"GDL",
		"BQR",
		"GULF",
		"CII",
		"NCT",
		"SDT",
		"ETY",
		"ETW",
		"ETJ",
		"PHK",
		"KSW",
		"GPM",
		"HTY",
		"SLRC",
		"PNNT",
		"GRH",
		"AINV",
		"NFJ",
		"MLPL",
		"SFL",
		"GSJK",
		"ETV",
		"PBP",
		"GAB",
		"PGP",
		"NTLS",
		"NRP",
		"IAF",
		"INB",
		"CODI",
		"DEX",
		"RIMG",
		"MORT",
		"BCX",
		"GLAD",
		"IDE",
		"BBEP",
		"SRV",
		"PMT",
		"STON",
		"IAE",
		"SB",
		"EROC",
		"BCF",
		"DHY",
		"SDR",
		"FGP",
		"EOS",
		"FDUS",
		"UVE",
		"DHF",
		"FMY",
		"CNSL",
		"GLO",
		"TCPC",
		"HIX",
		"VGR",
		"EXD",
		"MSB",
		"NDRO",
		"EOI",
		"ARI",
		"FFBC",
		"NAT",
		"NMFC",
		"ALSK",
		"MXF",
		"CLMT",
		"LOGI",
		"GLQ",
		"TCRD",
		"KBWD",
		"UAN",
		"JLA",
		"NOK",
		"TAC",
		"JGT",
		"KFN",
		"SPH",
		"BDJ",
		"MSP",
		"DHS",
		"MFV",
		"MCEP",
		"JSN",
		"ETB",
		"FHY",
		"HNW",
		"CHI",
		"MCN",
		"FAV",
		"MMLP",
		"AWP",
		"XTEX",
		"HTR",
		"TIA",
		"GLV",
	};
	
	
	public void run(String[] args) throws Exception {
		field( "Symbol");
		field( "Per Yr");
		field( "Years");
		field( "Avg");
		field( "Cur");
		field( "");
		field( "");
		System.out.println();
		
		for (String sym : SYMS) {
			try {
				process( sym);
			}
			catch( Exception e) {
			}
		}
	}
	
	void process( String sym) throws BadSymbol {
		// read SPY prices
		Collection<Price> temp = PriceDb.INSTANCE.getOrQueryAllPrices( sym, "20000101", BarType.DAILY); //NLY, TWO, WHX, ARR (pays a monthly dividend)
		ArrayList<Price> list = new ArrayList<Price>( temp);
		
		Price first = list.get( 0);
		Price last = list.get( list.size() - 1);
		
		// get all divs
		ArrayList<Double> divs = new ArrayList<Double>();
		for( int i = 0; i < list.size(); i++) {
			Price price = list.get( i);
			if( price.dividend() > 0) {
				divs.add( price.dividend() );
			}
		}
		double lastDiv = divs.get( divs.size() - 1);
		
		if (divs.size() >= 25) {
			JtsCalendar firstDate = first.dateTime();
			JtsCalendar lastDate = last.dateTime();
			double days = firstDate.daysSpanned( lastDate);
			double years = days / 365;
			long perYear = Math.round( divs.size() / years);
			double lastPct = lastDiv / last.close() * perYear;
			double avgPct = S.average( divs) / last.close() * perYear; 
			
			//
			// HANDLE SPLITS!!!!!!!!!!!!!!
			//
			
			if (perYear >= 4 && years >= 5 && lastPct > .10 && avgPct > .08) {
				field( sym);
				field( "" + perYear);
				field( "" + Math.round( years) );
				field( S.fmtPct( avgPct) );
				field( S.fmtPct( lastPct) );
				
				for (double div : divs) {
					double pct = div / last.close() * perYear;
					field( S.fmtPct( pct) );
					//field( S.fmt( div) );
				}
				
	
				System.out.println();
			}
		}
	}
	
	static void field( String s) {
		System.out.print( s);
		System.out.print( "\t");
	}
		
}
