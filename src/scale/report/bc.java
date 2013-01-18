package scale.report;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.Timer;

public class bc {
	public static void main(String[] args) {
		new bc().run();
	}
	
	enum Direction { UP, DOWN, LEFT, RIGHT };

	static int MAX_LENGTH = 1000000;
	static double GROW = 1;
	static int SIZE = 600;
	static int DELAY = 6;

	Line c = new Line();
	Player p1 = new Player(Color.red, 'a', 'z', 's', 'd', 10, 10);
	Player p2 = new Player(Color.blue, 'l', '.', ';', '\'', SIZE - 50, 10);
	double lineLen = 40;
	int realLength = (int)lineLen;
	
	void run() {
		JFrame window = new JFrame();
		window.add( c);
		window.setVisible( true);
		window.setSize( SIZE, SIZE);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		KeyListener kl = new KeyAdapter() {
			@Override public void keyPressed(KeyEvent e) {
				p1.onKey( e);
				p2.onKey( e);
			}
		};
		window.addKeyListener(kl);

		Timer t = new Timer( DELAY, new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				p1.onMove();
				p2.onMove();
				c.repaint();
			}
		});
		t.start();
	}
	
	static class Position {
		final int x;
		final int y;
		
		Position( int xx, int yy) {
			x = xx;
			y = yy;
		}
	}

	class Line extends JComponent {
		@Override protected void paintComponent(Graphics g) {
			p1.paint(g);
			p2.paint(g);
		}
	}
	
	class Player {
		int x = 10;
		int y = 10;
		Position[] positions = new Position[MAX_LENGTH];
		Direction direction = Direction.DOWN; 
		char up, down, left, right;
		private Color color;
		
		Player( Color c, char u, char d, char l, char r, int xx, int yy) {
			color = c;
			up = u;
			down = d;
			left = l;
			right = r;
			x = xx;
			y = yy;
		}
		
		public void paint(Graphics g) {
			g.setColor( color);
			for( int i = 0; i < realLength; i++) {
				Position pos = positions[i];
				if( pos != null) {
					g.drawRect(pos.x, pos.y, 2, 2);
				}
			}
		}

		protected void onMove() {
			
			if( lineLen < MAX_LENGTH) {
				lineLen += GROW;
				realLength = (int)lineLen;
			}
			
			switch( direction) {
				case UP:
					y--;
					break;
				case DOWN:
					y++;
					break;
				case LEFT:
					x--;
					break;
				case RIGHT:
					x++;
					break;
			}
			
			shiftPositions();
			positions[realLength-1] = new Position( x, y);
		}

		protected void onKey(KeyEvent e) {
			char ch = e.getKeyChar();
			if( ch == up) {
				direction = Direction.UP;
			}
			if( ch == down) {
				direction = Direction.DOWN;
			}
			if( ch == left) {
				direction = Direction.LEFT;
			}
			if( ch == right) {
				direction = Direction.RIGHT;
			}
		}
		
		private void shiftPositions() {
			System.arraycopy( positions, 1, positions, 0, realLength - 1);
		}

	}
}
