package scale.report;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class Test {
	static class C extends JComponent {
		@Override protected void paintComponent(Graphics g) {
			g.drawRect(50, 50, 50, 40);
		}
	}
	
	public static void main(String[] args) {
		
		JFrame frame = new JFrame();
		frame.add( new C() );
		frame.setSize( 200, 200);
		frame.setVisible( true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}
