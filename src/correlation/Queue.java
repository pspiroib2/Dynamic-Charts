package correlation;

import java.util.concurrent.LinkedBlockingQueue;

public class Queue extends Thread {
	LinkedBlockingQueue<Runnable>m_queue = new LinkedBlockingQueue<Runnable>();
	
	Queue() {
		start();
	}
	
	public void add( Runnable runnable) {
		try {
			m_queue.put( runnable);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
			
	public void run() {
		while( true) {
			try {
				Runnable runnable = m_queue.take();
				runnable.run();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
