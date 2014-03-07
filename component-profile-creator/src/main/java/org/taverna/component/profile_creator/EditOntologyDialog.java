package org.taverna.component.profile_creator;

import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import uk.org.taverna.ns._2012.component.profile.Ontology;

/**
 * Dialog to allow users to define an ontology to use.
 * 
 * @author Donal Fellows
 */
@SuppressWarnings("serial")
public class EditOntologyDialog extends JDialog {
	private Ontology ont;
	private JTextField name, address;

	public EditOntologyDialog(final ProfileCreator parent, String title,
			final EditOntology callback) {
		super(parent, title, true);
		Action okAction = new AbstractAction("OK") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = EditOntologyDialog.this.name.getText().trim();
				String address = EditOntologyDialog.this.address.getText()
						.trim();
				if (name.isEmpty() || address.isEmpty()) {
					showMessageDialog(parent, "Both fields must not be empty.",
							"Empty Field", ERROR_MESSAGE);
					return;
				}
				if (!name.matches("^[a-zA-Z]\\w*$")) {
					showMessageDialog(parent, "Name must be a simple word.",
							"Bad Format", ERROR_MESSAGE);
					return;
				}
				try {
					new URI(address);
					// Don't need the value!
					// TODO Check if the URL actually refers to an ontology?
				} catch (URISyntaxException ex) {
					showMessageDialog(parent, ex.getMessage(), "Bad Address",
							ERROR_MESSAGE);
					return;
				}
				ont.setId(name);
				ont.setValue(address);
				callback.edited(ont);
				dispose();
			}
		};
		Action cancelAction = new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		ont = parent.factory.createOntology();
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = gbc.gridy = 0;
		gbc.anchor = EAST;
		add(new JLabel("Name:"), gbc);
		gbc.gridy = 1;
		add(new JLabel("Address:"), gbc);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = HORIZONTAL;
		add(name = new JTextField(30), gbc);
		gbc.gridy = 1;
		add(address = new JTextField(30), gbc);
		gbc.gridwidth = 2;
		gbc.gridy = 2;
		gbc.gridx = 0;
		add(new JSeparator(), gbc);
		JComponent jc;
		gbc.gridy = 3;
		add(jc = new JPanel(), gbc);
		jc.add(new JButton(cancelAction));
		JButton ok;
		jc.add(ok = new JButton(okAction));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getRootPane().setDefaultButton(ok);
		getRootPane().registerKeyboardAction(cancelAction,
				getKeyStroke(VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
		pack();
	}

	public interface EditOntology {
		void edited(Ontology ont);
	}
}
