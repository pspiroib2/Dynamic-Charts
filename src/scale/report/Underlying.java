package scale.report;

import java.util.ArrayList;

import lib.JtsCalendar;
import lib.OStream;
import lib.S;


public class Underlying implements Comparable<Underlying> {
	static final String C = ",";

	private String m_symbol;
	private ArrayList<Scale> m_list = new ArrayList<Scale>();
	private OStream m_os;
	private double m_settlementIncome;
	
	public String symbol() { return m_symbol; }

	private String filename() { return "c:\\fin\\unders\\" + m_symbol + ".csv"; }
	OStream os() { return m_os; }

	Underlying( String symbol) {
		m_symbol = symbol;
		
		m_os = OStream.create( filename(), false);
		m_os.writeln( Trade.getHeader() + C + C + "Position,Mkt Val,PT,Tot Pnl,Tot Real");
	}
	
	public void add( Scale scale) {
		if( scale.secType().equals( "STK") && findStock() != null) {
			S.err( "Error: cannot have multiple stocks for conid " + scale.conid() );
			System.exit( 0);
		}
		m_list.add( scale);
	}
	
	public static void showSummaryHeader(String filename) {
		Report.report( filename, "", "Stk Pos", "Mkt Val", "Pnl", "Real Pnl", "", "Net Opt", "", "Avg Real", "Avg Real 2", "Cash Settlement");
	}
	
	public void applyCashSettlement(double settlementIncome) {
		m_settlementIncome += settlementIncome;
	}

	@Override public int compareTo(Underlying o) {
		return m_symbol.compareTo( o.m_symbol);
	}

	public double totalReal() {
		double real = 0;
		for( Scale scale : m_list) {
			real += scale.totalReal();
		}
		return real;
	}

	public double oldUnreal() {
		double real = 0;
		for( Scale scale : m_list) {
			real += scale.oldUnreal();
		}
		return real;
	}

	public double unreal() {
		double real = 0;
		for( Scale scale : m_list) {
			real += scale.unreal();
		}
		return real;
	}

	public int stockPos() {
		Scale scale = findStock();
		return scale != null ? scale.position() : 0;
	}
	
	public int optPos() {
		int pos = 0;
		for( Scale scale : m_list) {
			if( scale.secType().equals( "OPT")) {
				pos += scale.position();
			}
		}
		return pos;
	}

	private Scale findStock() {
		for( Scale scale : m_list) {
			if( scale.secType().equals( "STK")) {
				return scale;
			}
		}
		return null;
	}
}
