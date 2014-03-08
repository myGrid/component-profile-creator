package org.taverna.component.profile_creator;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.taverna.component.profile_creator.utils.GridPanel;

import uk.org.taverna.ns._2012.component.profile.Port;

/**
 * Dialog to allow users to define constraints on a port.
 * 
 * @see Port
 * @author Donal Fellows
 */
@SuppressWarnings("serial")
public class EditPortDialog extends JDialog {
	private final JFrame parent;
	private Port port;
	private JTextField name;
	private JSpinner minDepth, maxDepth, minOccurs, maxOccurs;

	public boolean validateModel(String name, int mindepth, Integer maxdepth,
			int minoccurs, Integer maxoccurs) {
		if (!name.isEmpty() && !name.matches("^[a-zA-Z]\\w*$")) {
			showMessageDialog(parent,
					"Name must be a simple word, if specified.", "Bad Format",
					ERROR_MESSAGE);
			return false;
		}
		if (maxdepth != null && mindepth > maxdepth) {
			showMessageDialog(parent, "Depth range must be sane.",
					"Bad Port Depth", ERROR_MESSAGE);
			return false;
		}
		if (maxoccurs != null && minoccurs > maxoccurs) {
			showMessageDialog(parent, "Occurrence range must be sane.",
					"Bad Occurence Count", ERROR_MESSAGE);
			return false;
		}
		try {
			new URI("address");// FIXME
			// Don't need the value!
			// TODO Check if the URL actually refers to an ontology?
		} catch (URISyntaxException ex) {
			showMessageDialog(parent, ex.getMessage(), "Bad Address",
					ERROR_MESSAGE);
			return false;
		}
		return true;
	}

	Port doUpdate(String name, int mindepth, Integer maxdepth, int minoccurs,
			Integer maxoccurs) {
		if (name.isEmpty())
			port.setName(null);
		else {
			port.setName(name);
			if (maxoccurs == null || maxoccurs > 1)
				maxoccurs = 1;
		}
		// FIXME
		// port.setValue(address);
		return port;
	}

	public EditPortDialog(final ProfileCreator parent, String title,
			final EditPort callback) {
		super(parent, title, true);
		this.parent = parent;
		Action okAction = new AbstractAction("OK") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = EditPortDialog.this.name.getText().trim();
				Integer minD = (Integer) ((SpinnerNumberModel) minDepth
						.getModel()).getNumber();
				Object maxD = ((SpinnerNumberModel) maxDepth.getModel())
						.getValue();
				Integer minO = (Integer) ((SpinnerNumberModel) minOccurs
						.getModel()).getNumber();
				Object maxO = ((SpinnerNumberModel) maxOccurs.getModel())
						.getValue();
				// String address =
				// EditPortDialog.this.address.getText().trim();
				if (validateModel(name, minD,
						(maxD instanceof Integer ? (Integer) maxD : null),
						minO, (maxO instanceof Integer ? (Integer) maxO : null))) {
					callback.edited(doUpdate(name, minD,
							(maxD instanceof Integer ? (Integer) maxD : null),
							minO, (maxO instanceof Integer ? (Integer) maxO
									: null)));
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
		port = parent.factory.createPort();
		GridPanel container;
		setContentPane(container = new GridPanel(-1));
		setLayout(new GridBagLayout());
		name = container.add("Name:", new JTextField(30), 0);
		container.add(new JSeparator(), 0, 1, 2);
		PositiveUnboundedModel min, max;
		minDepth = container.add("Min. Depth:", new JSpinner(
				min = new PositiveUnboundedModel(false)), 2);
		maxDepth = container.add("Max. Depth:", new JSpinner(
				max = new PositiveUnboundedModel(true)), 3);
		couple(min, max);
		container.add(new JSeparator(), 0, 4, 2);
		minOccurs = container.add("Min. Occurs:", new JSpinner(
				min = new PositiveUnboundedModel(false)), 5);
		maxOccurs = container.add("Max. Occurs:", new JSpinner(
				max = new PositiveUnboundedModel(true)), 6);
		couple(min, max);
		container.add(new JSeparator(), 0, 7, 2);
		JComponent jc = container.add(new JPanel(), 0, 8, 2);
		jc.add(new JButton(cancelAction));
		JButton ok;
		jc.add(ok = new JButton(okAction));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getRootPane().setDefaultButton(ok);
		getRootPane().registerKeyboardAction(cancelAction,
				getKeyStroke(VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
		pack();
	}

	private void couple(final PositiveUnboundedModel min,
			final PositiveUnboundedModel max) {
		min.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (min.compareTo(max) > 0)
					max.setValue(min.getValue());
			}
		});
		max.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (max.compareTo(min) < 0)
					min.setValue(max.getValue());
			}
		});
	}

	public interface EditPort {
		void edited(Port ont);
	}

	private class PositiveUnboundedModel extends SpinnerListModel implements
			Comparable<PositiveUnboundedModel> {
		private boolean unbounded, unboundable;
		private SpinnerNumberModel in;

		PositiveUnboundedModel(boolean mayBeUnbounded) {
			in = new SpinnerNumberModel(0, 0, null, 1);
			unbounded = unboundable = mayBeUnbounded;
		}

		@Override
		public Object getNextValue() {
			if (unbounded)
				return 0;
			return in.getNextValue();
		}

		@Override
		public Object getPreviousValue() {
			if (unbounded)
				return null;
			Object prev = in.getPreviousValue();
			if (prev == null && unboundable)
				prev = "unbounded";
			return prev;
		}

		@Override
		public Object getValue() {
			if (unbounded)
				return "unbounded";
			return in.getValue();
		}

		public Integer getBoundValue() {
			if (unbounded)
				return null;
			return (Integer) in.getNumber();
		}

		@Override
		public void setValue(Object value) {
			boolean old = unbounded;
			unbounded = value.equals("unbounded") && !unboundable;
			if (!unbounded) {
				in.setValue(value);
				fireStateChanged();
			} else if (old != unbounded)
				fireStateChanged();
		}

		@Override
		public int compareTo(PositiveUnboundedModel other) {
			Integer me = getBoundValue(), them = other.getBoundValue();
			if (me == null)
				return (them == null ? 0 : 1);
			if (them == null)
				return -1;
			return me.compareTo(them);
		}
	}
}
