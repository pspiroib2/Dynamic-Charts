/**
 * 
 */
package scale.profit;

public class Result {
	private double m_avg;
	private double m_stdDev;
	
	public double avg() 	{ return m_avg; }
	public double stdDev() 	{ return m_stdDev; }

	public Result(double avg, double stdDev) {
		m_avg = avg;
		m_stdDev = stdDev;
	}
	
	public String toString() {
		return "avg: " + m_avg + "  stddev: " + m_stdDev;
	}
}
