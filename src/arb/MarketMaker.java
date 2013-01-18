package arb;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import lib.S;


public class MarketMaker extends JPanel {
	FlowLayout LEFT = new FlowLayout(FlowLayout.LEFT);

	class BidAskPanel extends JPanel {
		JTextField m_legOffset = new JTextField( ".01", 5);
		JTextField m_strikeOffset = new JTextField(5);
		JTextField m_lowStrike = new JTextField(5);
		JTextField m_highStrike = new JTextField(5);
		JTextField m_moveAway = new JTextField(5);
		JTextField m_moveOther = new JTextField(5);

		JButton m_create = new JButton("Create");
		JButton m_cancel = new JButton("Cancel");
		JButton m_update = new JButton("Update");

		double legOffset()              { return dbl( m_legOffset, 0); } 
		double strikeOffset()           { return dbl( m_strikeOffset, 0); } 
		double lowStrike()              { return dbl( m_lowStrike, 0); } 
		double highStrike()     		{ return dbl( m_highStrike, Double.MAX_VALUE); } 
		double moveAway()               { return dbl( m_moveAway, 0); } 
		double moveOther()              { return dbl( m_moveOther, 0); } 
		

		BidAskPanel() {
			JPanel p2 = new JPanel(LEFT);
			p2.add(new JLabel("Create orders for strikes from"));
			p2.add(m_lowStrike);
			p2.add(new JLabel("to"));
			p2.add(m_highStrike);

			JPanel p = new JPanel(LEFT);
			p.add(new JLabel("Bid leg price +/-"));
			p.add(m_legOffset);
			p.add(new JLabel("with min/max offset of"));
			p.add(m_strikeOffset);
			p.add(m_create);
			p.add(m_update);

			JPanel p3 = new JPanel(LEFT);
			p3.add(new JLabel("When filled, move away"));
			p3.add(m_moveAway);
			p3.add(new JLabel("and move other side by "));
			p3.add(m_moveOther);

			JPanel p4 = new JPanel(LEFT);
			p4.add(m_cancel);

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(p2);
			add(p);
			add(p3);
			add(p4);

			m_create.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onCreate(BidAskPanel.this);
				}
			});
			m_update.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onUpdate(BidAskPanel.this);
				}
			});
			m_cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					onCancel(BidAskPanel.this);
				}
			});
		}
	}

	BidAskPanel m_bidPanel = new BidAskPanel();

	BidAskPanel m_askPanel = new BidAskPanel();

	private Exp m_exp;

	MarketMaker( Exp exp) {
		m_exp = exp;
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		m_bidPanel.setBorder(new TitledBorder("Bid Panel"));
		m_askPanel.setBorder(new TitledBorder("Ask Panel"));

		add(m_bidPanel);
		add(m_askPanel);
	}

	protected void onCreate(BidAskPanel panel) {
		if (panel == m_bidPanel) {
			m_exp.buyConversions(panel.lowStrike(), panel.highStrike(), panel.legOffset(), panel.strikeOffset());
		} 
		else {
			m_exp.sellConversions(panel.lowStrike(), panel.highStrike(), panel.legOffset(), panel.strikeOffset());
		}
	}

	protected void onUpdate(BidAskPanel panel) {
		if (panel == m_bidPanel) {
			m_exp.updateBuyConversions(panel.lowStrike(), panel.highStrike(), panel.legOffset(), panel.strikeOffset());
		} 
		else {
			m_exp.updateSellConversions(panel.lowStrike(), panel.highStrike(), panel.legOffset(), panel.strikeOffset());
		}
	}

	protected void onCancel(BidAskPanel panel) {
		if (panel == m_bidPanel) {
			m_exp.cancelBuys();
		}
		else {
			m_exp.cancelSells();
		}
	}

	private static double dbl(JTextField field, double def) {
		String text = field.getText();
		return S.isNull( text) ? def : Double.parseDouble( text);
	}
}
