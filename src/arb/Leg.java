/**
 * 
 */
package arb;

import com.ib.client.ComboLeg;
import com.ib.client.Contract;

class Leg {
	private Data m_data;
	private ComboLeg m_comboLeg;
	
	Data data() 			{ return m_data; }
	ComboLeg comboLeg() 	{ return m_comboLeg; }
	Contract contract() 	{ return m_data.contract(); }
	
	Leg( Data data, ComboLeg leg) {
		m_data = data;
		m_comboLeg = leg;
	}
	
}
