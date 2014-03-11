package org.taverna.component.profile_creator.utils;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public abstract class GridDialog extends JDialog {
	protected final GridPanel container;
	protected final Action okAction, cancelAction;

	public GridDialog(JFrame parent, String title, boolean modal) {
		super(parent, title, modal);
		setContentPane(container = new GridPanel(-1));
		setLayout(new GridBagLayout());
		okAction = new AbstractAction("OK") {
			@Override
			public void actionPerformed(ActionEvent e) {
				userAccepted();
			}
		};
		cancelAction = new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
	}

	protected void errorDialog(String message, String title) {
		showMessageDialog(getParent(), message, title, ERROR_MESSAGE);
	}

	protected JPanel okCancelPanel() {
		JPanel panel = new JPanel();
		panel.add(new JButton(cancelAction));
		JButton ok;
		panel.add(ok = new JButton(okAction));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getRootPane().setDefaultButton(ok);
		getRootPane().registerKeyboardAction(cancelAction,
				getKeyStroke(VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
		return panel;
	}

	protected abstract void userAccepted();
}
