package org.taverna.component.profile_creator;

import static org.taverna.component.profile_creator.utils.Cardinality.OPTIONAL;
import static org.taverna.component.profile_creator.utils.PositiveUnboundedModel.couple;
import static org.taverna.component.profile_creator.utils.TableUtils.configureColumn;
import static org.taverna.component.profile_creator.utils.TableUtils.installDelegatingColumn;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.taverna.component.profile_creator.utils.Cardinality;
import org.taverna.component.profile_creator.utils.GridDialog;
import org.taverna.component.profile_creator.utils.OntologyCollection;
import org.taverna.component.profile_creator.utils.OntologyCollection.PossibleStatement;
import org.taverna.component.profile_creator.utils.PositiveUnboundedModel;
import org.taverna.component.profile_creator.utils.TableUtils.RowDeletionAction;

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
public class EditPortDialog extends GridDialog {
	private final ObjectFactory factory;
	private Port port;
	private final JTextField name;
	private final PositiveUnboundedModel minDepth, maxDepth, minOccurs,
			maxOccurs;
	private final JCheckBox mandateDescription, mandateExample;
	private final DefaultTableModel annotations;
	private final OntologyCollection ontosource;
	private Action deleteRowAction;
	private EditPort callback;

	/*
	 * <semanticAnnotation class="http://purl.org/DP/components#PortType"
	 * predicate="http://purl.org/DP/components#portType" ontology="components">
	 * http://purl.org/DP/components#ParameterPort </semanticAnnotation>
	 */

	public boolean validateModel(String name, int mindepth, Integer maxdepth,
			int minoccurs, Integer maxoccurs,
			List<SemanticAnnotation> semanticAnnotations) {
		if (!name.isEmpty() && !name.matches("^[a-zA-Z]\\w*$")) {
			errorDialog("Name must be a simple word, if specified.",
					"Bad Format");
			return false;
		}
		if (maxdepth != null && mindepth > maxdepth) {
			errorDialog("Depth range must be sane.", "Bad Port Depth");
			return false;
		}
		if (maxoccurs != null && minoccurs > maxoccurs) {
			errorDialog("Occurrence range must be sane.", "Bad Occurence Count");
			return false;
		}
		for (SemanticAnnotation sa : semanticAnnotations)
			if (ontosource.getStatementFor(sa) == null) {
				errorDialog("Could not double check the construction of the "
						+ "semantic annotation.", "Illegal Semantic Annotation");
				return false;
			}
		return true;
	}

	Port doUpdate(String name, int mindepth, Integer maxdepth, int minoccurs,
			Integer maxoccurs, boolean description, boolean example,
			List<SemanticAnnotation> semanticAnnotations) {
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
			pa.add(ann);
		}
		if (example) {
			PortAnnotation ann = factory.createPortAnnotation();
			ann.setValue(PortAnnotations.EXAMPLE);
			pa.add(ann);
		}
		port.getAnnotation().clear();
		port.getAnnotation().addAll(pa);

		port.getSemanticAnnotation().clear();
		port.getSemanticAnnotation().addAll(semanticAnnotations);
		return port;
	}

	public EditPortDialog(ProfileCreator parent, String title,
			final EditPort callback) {
		this(parent, title, callback, makeDefaultPort(parent.factory));
	}

	private static Port makeDefaultPort(ObjectFactory factory) {
		return factory.createPort();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void userAccepted() {
		String name = this.name.getText().trim();
		Integer minD = minDepth.getBoundValue();
		Integer maxD = maxDepth.getBoundValue();
		Integer minO = minOccurs.getBoundValue();
		Integer maxO = maxOccurs.getBoundValue();
		boolean description = mandateDescription.isSelected();
		boolean example = mandateExample.isSelected();
		List<SemanticAnnotation> semanticAnnotations = new ArrayList<>();
		for (Vector<?> row : (Vector<Vector<?>>) annotations.getDataVector())
			semanticAnnotations.add(((PossibleStatement) row.get(0))
					.getAnnotation((Cardinality) row.get(1)));
		if (validateModel(name, minD, maxD, minO, maxO, semanticAnnotations)) {
			callback.edited(doUpdate(name, minD, maxD, minO, maxO, description,
					example, semanticAnnotations));
			dispose();
		}
	}

	protected EditPortDialog(ProfileCreator parent, String title,
			EditPort callback, Port port) {
		super(parent, title, true);
		this.callback = callback;
		this.factory = parent.factory;
		this.ontosource = parent.ontologies;
		this.port = port;
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

		Action addSemanticAnnotation = new AbstractAction("Add Annotation") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (ontosource.getPossibleStatements().isEmpty())
					errorDialog("No ontologies imported, so no annotations "
							+ "are available.", "No Legal Annotations");
				else
					annotations.addRow(new Object[] {
							ontosource.getPossibleStatements().get(0),
							OPTIONAL, new JButton(deleteRowAction) });
			}
		};
		if (ontosource.getPossibleStatements().isEmpty())
			addSemanticAnnotation.setEnabled(false);
		annotations = new DefaultTableModel(new Object[0][], new Object[] {
				"Annotation", "Cardinality", "" });
		JTable ann = container.add(addSemanticAnnotation, new JTable(
				annotations), 10);
		deleteRowAction = new RowDeletionAction(ann);
		ann.setPreferredScrollableViewportSize(new Dimension(24, 48));
		configureColumn(ann, 0, null, ontosource.tableRenderer(),
				ontosource.tableEditor());
		configureColumn(ann, 1, 64, Cardinality.tableRenderer(),
				Cardinality.tableEditor());
		installDelegatingColumn(ann.getColumnModel().getColumn(2), "Del");
		container.add(new JSeparator(), 0, 11, 2);
		container.add(okCancelPanel(), 0, 12, 2);
		install();
		pack();
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
			annotations.addRow(new Object[] { ontosource.getStatementFor(sa),
					Cardinality.get(sa.getMinOccurs(), sa.getMaxOccurs()),
					new JButton(deleteRowAction) });
	}

	public interface EditPort {
		void edited(Port ont);
	}
}
