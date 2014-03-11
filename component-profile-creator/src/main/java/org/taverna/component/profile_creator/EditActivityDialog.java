package org.taverna.component.profile_creator;

import static org.taverna.component.profile_creator.utils.Cardinality.OPTIONAL;
import static org.taverna.component.profile_creator.utils.PositiveUnboundedModel.couple;
import static org.taverna.component.profile_creator.utils.TableUtils.configureColumn;
import static org.taverna.component.profile_creator.utils.TableUtils.installDelegatingColumn;
import static uk.org.taverna.ns._2012.component.profile.ActivityAnnotations.DESCRIPTION;

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

import uk.org.taverna.ns._2012.component.profile.Activity;
import uk.org.taverna.ns._2012.component.profile.ActivityAnnotation;
import uk.org.taverna.ns._2012.component.profile.ObjectFactory;
import uk.org.taverna.ns._2012.component.profile.SemanticAnnotation;

/**
 * Dialog to allow users to define constraints on contained activities.
 * 
 * @see Activity
 * @author Donal Fellows
 */
@SuppressWarnings("serial")
public class EditActivityDialog extends GridDialog {
	private final ObjectFactory factory;
	private Activity activity;
	private final JTextField type;
	private final PositiveUnboundedModel minOccurs, maxOccurs;
	private final JCheckBox mandateDescription;
	private final DefaultTableModel annotations;
	private final OntologyCollection ontosource;
	private Action deleteRowAction;
	private final EditActivity callback;

	public interface EditActivity {
		void edited(Activity doUpdate);
	}

	protected boolean validateModel(String type, Integer minoccurs,
			Integer maxoccurs, List<SemanticAnnotation> semanticAnnotations) {
		if (!type.isEmpty() && !type.matches("^[a-zA-Z]\\w*$")) {
			errorDialog("Type must be a simple word, if specified.",
					"Bad Format");
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

	private Activity doUpdate(String type, Integer minoccurs,
			Integer maxoccurs, boolean description,
			List<SemanticAnnotation> semanticAnnotations) {
		if (type.isEmpty())
			activity.setType(null);
		else
			activity.setType(type);
		activity.setMaxOccurs(maxoccurs == null ? "unbounded" : maxoccurs
				.toString());
		activity.setMinOccurs(BigInteger.valueOf(minoccurs));
		List<ActivityAnnotation> aa = new ArrayList<>();
		if (description) {
			ActivityAnnotation ann = factory.createActivityAnnotation();
			ann.setValue(DESCRIPTION);
			aa.add(ann);
		}
		activity.getAnnotation().clear();
		activity.getAnnotation().addAll(aa);

		activity.getSemanticAnnotation().clear();
		activity.getSemanticAnnotation().addAll(semanticAnnotations);
		return activity;
	}

	private static Activity makeDefaultActivity(ObjectFactory factory) {
		return factory.createActivity();
	}

	public EditActivityDialog(ProfileCreator parent, String title,
			final EditActivity callback) {
		this(parent, title, callback, makeDefaultActivity(parent.factory));
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void userAccepted() {
		String type = this.type.getText().trim();
		Integer minO = minOccurs.getBoundValue();
		Integer maxO = maxOccurs.getBoundValue();
		boolean description = mandateDescription.isSelected();
		List<SemanticAnnotation> semanticAnnotations = new ArrayList<>();
		for (Vector<?> row : (Vector<Vector<?>>) annotations.getDataVector())
			semanticAnnotations.add(((PossibleStatement) row.get(0))
					.getAnnotation((Cardinality) row.get(1)));
		if (validateModel(type, minO, maxO, semanticAnnotations)) {
			callback.edited(doUpdate(type, minO, maxO, description,
					semanticAnnotations));
			dispose();
		}
	}

	protected EditActivityDialog(ProfileCreator parent, String title,
			EditActivity callback, Activity activity) {
		super(parent, title, true);
		this.callback = callback;
		this.factory = parent.factory;
		this.ontosource = parent.ontologies;
		this.activity = activity;
		type = container.add("Type:", new JTextField(30), 0);
		container.add(new JSeparator(), 0, 1, 2);
		container.add("Min. Occurs:", new JSpinner(
				minOccurs = new PositiveUnboundedModel(false)), 2);
		container.add("Max. Occurs:", new JSpinner(
				maxOccurs = new PositiveUnboundedModel(true)), 3);
		couple(minOccurs, maxOccurs);
		container.add(new JSeparator(), 0, 4, 2);
		JComponent jc = container.add("Std. Annotations:", new JPanel(), 5);
		mandateDescription = new JCheckBox("Require Description");
		jc.add(mandateDescription);
		container.add(new JSeparator(), 0, 6, 2);
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
				annotations), 7);
		deleteRowAction = new RowDeletionAction(ann);
		ann.setPreferredScrollableViewportSize(new Dimension(24, 48));
		configureColumn(ann, 0, null, ontosource.tableRenderer(),
				ontosource.tableEditor());
		configureColumn(ann, 1, 64, Cardinality.tableRenderer(),
				Cardinality.tableEditor());
		installDelegatingColumn(ann.getColumnModel().getColumn(2), "Del");
		container.add(new JSeparator(), 0, 8, 2);
		container.add(okCancelPanel(), 0, 9, 2);
		install();
		pack();
	}

	private void install() {
		if (activity.getType() != null)
			type.setText(activity.getType());

		if (activity.getMaxOccurs() == null
				|| activity.getMaxOccurs().equals("unbounded"))
			maxOccurs.setValue("unbounded");
		else
			maxOccurs.setValue(Integer.parseInt(activity.getMaxOccurs()));
		if (activity.getMinOccurs() == null)
			minOccurs.setValue(0);
		else
			minOccurs.setValue(activity.getMinOccurs().intValue());

		for (ActivityAnnotation aa : activity.getAnnotation())
			switch (aa.getValue()) {
			case DESCRIPTION:
				mandateDescription
						.setSelected(aa.getMinOccurs().intValue() > 0);
				break;
			}

		for (SemanticAnnotation sa : activity.getSemanticAnnotation())
			annotations.addRow(new Object[] { ontosource.getStatementFor(sa),
					Cardinality.get(sa.getMinOccurs(), sa.getMaxOccurs()),
					new JButton(deleteRowAction) });
	}
}
