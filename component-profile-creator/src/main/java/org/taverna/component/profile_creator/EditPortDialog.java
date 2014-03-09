package org.taverna.component.profile_creator;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import uk.org.taverna.ns._2012.component.profile.ObjectFactory;
import uk.org.taverna.ns._2012.component.profile.Port;
import uk.org.taverna.ns._2012.component.profile.PortAnnotation;
import uk.org.taverna.ns._2012.component.profile.PortAnnotations;
import uk.org.taverna.ns._2012.component.profile.SemanticAnnotation;

/**
 * Dialog to allow users to define constraints on a port.
 * 
 * @see Port
 * @author Donal Fellows
 */
@SuppressWarnings("serial")
public class EditPortDialog extends JDialog {
	private final JFrame parent;
	private final ObjectFactory factory;
	private Port port;
	private final JTextField name;
	private final PositiveUnboundedModel minDepth, maxDepth, minOccurs,
			maxOccurs;
	private final JCheckBox mandateDescription, mandateExample;

	/*
	 * <semanticAnnotation class="http://purl.org/DP/components#PortType"
	 * predicate="http://purl.org/DP/components#portType" ontology="components">
	 * http://purl.org/DP/components#ParameterPort
	 * </semanticAnnotation>
	 */

	public boolean validateModel(String name, int mindepth, Integer maxdepth,
			int minoccurs, Integer maxoccurs, boolean b, boolean c) {
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
		// FIXME semantic annotations
		try {
			new URI("address");
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
			Integer maxoccurs, boolean description, boolean example) {
		if (name.isEmpty())
			port.setName(null);
		else {
			port.setName(name);
			if (maxoccurs == null || maxoccurs > 1)
				maxoccurs = 1;
		}
		port.setMaxDepth(maxdepth == null ? "unbounded" : maxdepth.toString());
		port.setMinDepth(BigInteger.valueOf(mindepth));
		port.setMaxOccurs(maxoccurs == null ? "unbounded" : maxoccurs
				.toString());
		port.setMinOccurs(BigInteger.valueOf(minoccurs));
		List<PortAnnotation> pa = new ArrayList<>();
		if (description) {
			PortAnnotation ann = factory.createPortAnnotation();
			ann.setValue(PortAnnotations.DESCRIPTION);
			ann.setMinOccurs(BigInteger.ONE);
			ann.setMaxOccurs("1");
			pa.add(ann);
		}
		if (example) {
			PortAnnotation ann = factory.createPortAnnotation();
			ann.setValue(PortAnnotations.EXAMPLE);
			ann.setMinOccurs(BigInteger.ONE);
			ann.setMaxOccurs("1");
			pa.add(ann);
		}
		port.getAnnotation().clear();
		port.getAnnotation().addAll(pa);

		// FIXME semantic annotations
		return port;
	}

	public EditPortDialog(ProfileCreator parent, String title,
			final EditPort callback) {
		this(parent, title, callback, parent.factory.createPort());
	}

	protected EditPortDialog(final ProfileCreator parent, String title,
			final EditPort callback, Port port) {
		super(parent, title, true);
		this.parent = parent;
		this.factory = parent.factory;
		Action okAction = new AbstractAction("OK") {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = EditPortDialog.this.name.getText().trim();
				Integer minD = minDepth.getBoundValue();
				Integer maxD = maxDepth.getBoundValue();
				Integer minO = minOccurs.getBoundValue();
				Integer maxO = maxOccurs.getBoundValue();
				boolean description = mandateDescription.isSelected();
				boolean example = mandateExample.isSelected();
				// String address =
				// EditPortDialog.this.address.getText().trim();
				if (validateModel(name, minD, maxD, minO, maxO, description,
						example)) {
					callback.edited(doUpdate(name, minD, maxD, minO, maxO,
							description, example));
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
		this.port = port;
		GridPanel container;
		setContentPane(container = new GridPanel(-1));
		setLayout(new GridBagLayout());
		name = container.add("Name:", new JTextField(30), 0);
		container.add(new JSeparator(), 0, 1, 2);
		container.add("Min. Depth:", new JSpinner(
				minDepth = new PositiveUnboundedModel(false)), 2);
		container.add("Max. Depth:", new JSpinner(
				maxDepth = new PositiveUnboundedModel(true)), 3);
		couple(minDepth, maxDepth);
		container.add(new JSeparator(), 0, 4, 2);
		container.add("Min. Occurs:", new JSpinner(
				minOccurs = new PositiveUnboundedModel(false)), 5);
		container.add("Max. Occurs:", new JSpinner(
				maxOccurs = new PositiveUnboundedModel(true)), 6);
		couple(minOccurs, maxOccurs);
		container.add(new JSeparator(), 0, 7, 2);

		JComponent jc = container.add("Std. Annotations:", new JPanel(), 8);
		mandateDescription = new JCheckBox("Require Description");
		mandateExample = new JCheckBox("Require Example");
		jc.add(mandateDescription);
		jc.add(mandateExample);
		container.add(new JSeparator(), 0, 9, 2);

		// FIXME semantic annotations

		jc = container.add(new JPanel(), 0, 10, 2);
		jc.add(new JButton(cancelAction));
		JButton ok;
		jc.add(ok = new JButton(okAction));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getRootPane().setDefaultButton(ok);
		getRootPane().registerKeyboardAction(cancelAction,
				getKeyStroke(VK_ESCAPE, 0), WHEN_IN_FOCUSED_WINDOW);
		install();
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

	private void install() {
		Object absentOccurs = "unbounded";
		if (port.getName() != null) {
			name.setText(port.getName());
			absentOccurs = 1;
		}

		if (port.getMaxDepth() == null
				|| port.getMaxDepth().equals("unbounded"))
			maxDepth.setValue("unbounded");
		else
			maxDepth.setValue(Integer.parseInt(port.getMaxDepth()));
		if (port.getMinDepth() == null)
			minDepth.setValue(0);
		else
			minDepth.setValue(port.getMinDepth().intValue());

		if (port.getMaxOccurs() == null)
			maxOccurs.setValue(absentOccurs);
		else if (port.getMaxOccurs().equals("unbounded"))
			maxOccurs.setValue("unbounded");
		else
			maxOccurs.setValue(Integer.parseInt(port.getMaxOccurs()));
		if (port.getMinOccurs() == null)
			minOccurs.setValue(0);
		else
			minOccurs.setValue(port.getMinOccurs().intValue());

		for (PortAnnotation pa : port.getAnnotation())
			switch (pa.getValue()) {
			case DESCRIPTION:
				mandateDescription
						.setSelected(pa.getMinOccurs().intValue() > 0);
				break;
			case EXAMPLE:
				mandateExample.setSelected(pa.getMinOccurs().intValue() > 0);
				break;
			}

		for (SemanticAnnotation sa : port.getSemanticAnnotation())
			sa.getValue();// URI
		// FIXME
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
			unbounded = value.equals("unbounded") && unboundable;
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
