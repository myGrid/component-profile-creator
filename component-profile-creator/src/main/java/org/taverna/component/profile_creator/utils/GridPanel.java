package org.taverna.component.profile_creator.utils;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NORTH;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

@SuppressWarnings("serial")
public class GridPanel extends JPanel {
	int expand;

	public GridPanel(int expandingRow) {
		setLayout(new GridBagLayout());
		expand = expandingRow;
	}

	public <T extends JComponent> T add(String label, T component, int y) {
		add(new JLabel(label), 0, y, 1);
		return add(component, 1, y, 1);
	}

	public <T extends JComponent> T add(Action action, T component, int y) {
		add(new JButton(action), 0, y, 1);
		return add(component, 1, y, 1);
	}

	public <T extends JComponent> T add(T component, int x, int y) {
		return add(component, x, y, 1);
	}

	public <T extends JComponent> T add(T component, int x, int y, int width) {
		GridBagConstraints constr = new GridBagConstraints();
		constr.gridx = x;
		constr.gridy = y;
		constr.weightx = (x == 0 ? 0 : 1);
		constr.weighty = (y != expand ? 0 : 1);
		constr.gridwidth = width;
		constr.fill = (x == 0 ? HORIZONTAL : BOTH);
		constr.anchor = NORTH;
		if (component instanceof Scrollable)
			add(new JScrollPane(component), constr);
		else
			add(component, constr);
		return component;
	}
}