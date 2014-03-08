package org.taverna.component.profile_creator;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.UUID.randomUUID;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.taverna.component.profile_creator.utils.GridPanel;

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
	private final ProfileCreator owner;

	public boolean validateModel(String name, String address) {
		if (name.isEmpty() || address.isEmpty()) {
			showMessageDialog(owner, "Both fields must not be empty.",
					"Empty Field", ERROR_MESSAGE);
			return false;
		}
		if (!name.matches("^[a-zA-Z]\\w*$")) {
			showMessageDialog(owner, "Name must be a simple word.",
					"Bad Format", ERROR_MESSAGE);
			return false;
		}
		try {
			new URI(address);
			// Don't need the value!
			// TODO Check if the URL actually refers to an ontology?
		} catch (URISyntaxException ex) {
			showMessageDialog(owner, ex.getMessage(), "Bad Format",
					ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	protected Ontology makeNew() {
		Ontology o = owner.factory.createOntology();
		o.setId(randomUUID().toString());
		o.setValue("");
		return o;
	}

	public EditOntologyDialog(ProfileCreator parent, String title,
			final EditOntology callback) {
		super(parent, title, true);
		owner = parent;
		Action okAction = new AbstractAction("OK") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = EditOntologyDialog.this.name.getText().trim();
				String address = EditOntologyDialog.this.address.getText()
						.trim();
				if (validateModel(name, address)) {
					ont.setId(name);
					ont.setValue(address);
					callback.edited(ont);
					dispose();
				}
			}
		};
		Action cancelAction = new AbstractAction("Cancel") {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};
		ont = makeNew();
		GridPanel container;
		setContentPane(container = new GridPanel(-1));
		name = container.add("Name:", new JTextField(30), 0);
		address = container.add("Address:", new JTextField(30), 1);
		container.add(new JSeparator(), 0, 2, 2);
		JComponent jc = container.add(new JPanel(), 0, 3, 2);
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
