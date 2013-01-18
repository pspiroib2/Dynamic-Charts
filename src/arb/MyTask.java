/**
 * 
 */
package arb;

import java.util.TimerTask;

import com.ib.client.EReader;

abstract class MyTask extends TimerTask {
	public abstract void process();
	
	public void run() {
		synchronized( EReader.LOCK) {
			try {
				process();
			}
			catch( Exception e) {
				Arb.log( e.toString() );
				e.printStackTrace();
			}
		}
	}
}