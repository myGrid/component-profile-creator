package org.taverna.component.profile_creator;

import static java.awt.BorderLayout.CENTER;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_N;
import static java.awt.event.KeyEvent.VK_O;
import static java.awt.event.KeyEvent.VK_Q;
import static java.awt.event.KeyEvent.VK_S;
import static java.lang.System.getProperty;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showInputDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.slf4j.LoggerFactory.getLogger;
import static org.taverna.component.profile_creator.TavernaComponentProfileEditor.APP_NAME;
import static org.taverna.component.profile_creator.TavernaComponentProfileEditor.isOnMac;
import static org.taverna.component.profile_creator.utils.Cardinality.OPTIONAL;
import static org.taverna.component.profile_creator.utils.TableUtils.configureColumn;
import static org.taverna.component.profile_creator.utils.TableUtils.installDelegatingColumn;
import static org.taverna.component.profile_creator.utils.TableUtils.makeRowsDialogEditable;
import static org.taverna.component.profile_creator.utils.TableUtils.setRowLines;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.ws.Holder;

import org.simplericity.macify.eawt.ApplicationEvent;
import org.simplericity.macify.eawt.ApplicationListener;
import org.taverna.component.profile_creator.EditActivityDialog.EditActivity;
import org.taverna.component.profile_creator.EditOntologyDialog.EditOntology;
import org.taverna.component.profile_creator.EditPortDialog.EditPort;
import org.taverna.component.profile_creator.utils.Cardinality;
import org.taverna.component.profile_creator.utils.GridPanel;
import org.taverna.component.profile_creator.utils.OntologyCollection;
import org.taverna.component.profile_creator.utils.OntologyCollection.OntologyCollectionException;
import org.taverna.component.profile_creator.utils.OntologyCollection.PossibleStatement;
import org.taverna.component.profile_creator.utils.TableUtils.RowDeletionAction;
import org.taverna.component.profile_creator.utils.TableUtils.ShowDialog;

import uk.org.taverna.ns._2012.component.profile.Activity;
import uk.org.taverna.ns._2012.component.profile.ActivityAnnotation;
import uk.org.taverna.ns._2012.component.profile.BasicAnnotations;
import uk.org.taverna.ns._2012.component.profile.Component;
import uk.org.taverna.ns._2012.component.profile.ComponentAnnotation;
import uk.org.taverna.ns._2012.component.profile.ComponentAnnotations;
import uk.org.taverna.ns._2012.component.profile.ExceptionHandling;
import uk.org.taverna.ns._2012.component.profile.Extends;
import uk.org.taverna.ns._2012.component.profile.HandleException;
import uk.org.taverna.ns._2012.component.profile.ObjectFactory;
import uk.org.taverna.ns._2012.component.profile.Ontology;
import uk.org.taverna.ns._2012.component.profile.Port;
import uk.org.taverna.ns._2012.component.profile.PortAnnotation;
import uk.org.taverna.ns._2012.component.profile.Profile;
import uk.org.taverna.ns._2012.component.profile.Replacement;
import uk.org.taverna.ns._2012.component.profile.SemanticAnnotation;

/**
 * The main window of the profile editor.
 * 
 * @author Donal Fellows
 * @see TavernaComponentProfileEditor
 */
@SuppressWarnings("serial")
public class ProfileFrame extends JFrame {
	private final JAXBContext context;
	final OntologyCollection ontologies;
	final ObjectFactory factory;
	private final JLabel id;
	private final JTextField title, extend;
	private final JTextArea description;
	private final DefaultTableModel ontologyList, inputs, outputs, activities,
			componentAnnotations, exceptionHandling;
	private final JTable ontologyTable, inputTable, outputTable, activityTable;
	private final JCheckBox requireAuthor, requireDescription, requireTitle,
			failLists;
	private File file;
	private Profile profile;
	private boolean modified;
	private final Action deleteComponentAnnotationRow,
			deleteExceptionHandlingRow, quitAction;

	boolean isModified() {
		return modified;
	}

	void setModified(boolean modified) {
		boolean oldMod = this.modified;
		this.modified = modified;
		firePropertyChange("modified", oldMod, modified);
	}

	public File getFile() {
		return file;
	}

	void setFile(File file) {
		File oldFile = this.file;
		this.file = file;
		firePropertyChange("file", oldFile, file);
		if (file == null)
			setTitle(APP_NAME);
		else
			setTitle(APP_NAME + " \u2013 " + file);
	}

	abstract class WatchingAction extends AbstractAction implements
			PropertyChangeListener {
		public WatchingAction(String string, int key) {
			super(string);
			if (key != 0) {
				putValue(
						ACCELERATOR_KEY,
						getKeyStroke(key, getDefaultToolkit()
								.getMenuShortcutKeyMask()));
				putValue(MNEMONIC_KEY, key);
			}
			ProfileFrame.this.addPropertyChangeListener("modified", this);
		}

		protected void respondToModifiedChanged() {
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			respondToModifiedChanged();
		}
	}

	private void addOntologyRow(Ontology ont, boolean trapException)
			throws OntologyCollectionException {
		Throwable disable = null;
		try {
			ontologies.addOntology(ont);
		} catch (OntologyCollectionException e) {
			if (!trapException)
				throw e;
			disable = e;
		}
		final String id = ont.getId();
		JButton jb = new JButton(new AbstractAction("Del") {
			@Override
			public void actionPerformed(ActionEvent e) {
				int row = 0;
				for (Ontology o : profile.getOntology()) {
					if (o.getId().equals(id)) {
						profile.getOntology().remove(o);
						ontologyList.removeRow(row);
						setModified(true);
						break;
					}
					row++;
				}
			}
		});
		if (disable != null)
			ontologyList.addRow(new Object[] {
					new Disable(ont.getId(), disable),
					new Disable(ont.getValue(), disable), jb });
		else
			ontologyList
					.addRow(new Object[] { ont.getId(), ont.getValue(), jb });
	}

	static class Disable {
		private Object val;
		private Throwable exn;

		Disable(Object content, Throwable exception) {
			this.val = content;
			while (exception.getCause() != null)
				exception = exception.getCause();
			this.exn = exception;
		}

		String s() {
			return val.toString();
		}

		String t() {
			return exn.getMessage();
		}
	}

	static class DisabledRenderer extends DefaultTableCellRenderer {
		@Override
		public JComponent getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);
			if (value instanceof Disable) {
				Disable d = (Disable) value;
				setEnabled(false);
				setText(d.s());
				setToolTipText("<html>This ontology had a problem during loading.<p>"
						+ d.t());
			}
			return this;
		}
	}

	static class Range {
		int from;
		Integer to;

		Range(BigInteger from, String to) {
			if (from == null)
				this.from = 1;
			else
				this.from = from.intValue();
			if (to == null)
				this.to = 1;
			else if (to.equals("unbounded"))
				this.to = null;
			else
				this.to = Integer.parseInt(to);
		}

		@Override
		public String toString() {
			return (from == 0 ? "" : "" + from) + " - "
					+ (to == null ? "" : "" + to);
		}
	}

	private void addPort(JTable realTable, final DefaultTableModel table,
			final List<Port> portCollection, final Port port) {
		JButton jb = new JButton(new AbstractAction("Del") {
			@Override
			public void actionPerformed(ActionEvent e) {
				int row = 0;
				for (Port p : portCollection) {
					if (p == port) {
						portCollection.remove(p);
						table.removeRow(row);
						setModified(true);
						break;
					}
					row++;
				}
			}
		});
		portCollection.add(port);
		Holder<Integer> numLines = new Holder<>(0);
		String annDisplay = getAnnotationsForDisplay(port.getAnnotation(),
				port.getSemanticAnnotation(), numLines);
		table.addRow(new Object[] {
				new Range(port.getMinOccurs(), port.getMaxOccurs()),
				new Range(port.getMinDepth(), port.getMaxDepth()),
				port.getName() == null ? "" : port.getName(), annDisplay, jb });
		if (numLines.value > 1)
			realTable.setRowHeight(realTable.getRowCount() - 1, numLines.value
					* realTable.getRowHeight());
	}

	private void applyPortEdit(List<Port> ports, JTable table, int row,
			Port port, Object[] rowModel) {
		ports.set(row, port);
		rowModel[0] = new Range(port.getMinOccurs(), port.getMaxOccurs());
		rowModel[1] = new Range(port.getMinDepth(), port.getMaxDepth());
		rowModel[2] = port.getName() == null ? "" : port.getName();
		Holder<Integer> numLines = new Holder<>(0);
		rowModel[3] = getAnnotationsForDisplay(port.getAnnotation(),
				port.getSemanticAnnotation(), numLines);
		setRowLines(table, row, numLines.value);
	}

	private void addHandleException(HandleException he) {
		exceptionHandling.addRow(new Object[] {
				he.getPattern(),
				he.getPruneStack() != null,
				he.getReplacement() != null ? he.getReplacement()
						.getReplacementId() : "",
				he.getReplacement() != null ? he.getReplacement()
						.getReplacementMessage() : "",
				new JButton(deleteExceptionHandlingRow) });
	}

	private void addActivity(JTable realTable, final DefaultTableModel table,
			final List<Activity> activityCollection, final Activity activity) {
		JButton jb = new JButton(new AbstractAction("Del") {
			@Override
			public void actionPerformed(ActionEvent e) {
				int row = 0;
				for (Activity p : activityCollection) {
					if (p == activity) {
						activityCollection.remove(p);
						table.removeRow(row);
						setModified(true);
						break;
					}
					row++;
				}
			}
		});
		activityCollection.add(activity);
		Holder<Integer> numLines = new Holder<>(0);
		String annDisplay = getAnnotationsForDisplay(activity.getAnnotation(),
				activity.getSemanticAnnotation(), numLines);
		table.addRow(new Object[] {
				new Range(activity.getMinOccurs(), activity.getMaxOccurs()),
				activity.getType() == null ? "" : activity.getType(),
				annDisplay, jb });
		if (numLines.value > 1)
			realTable.setRowHeight(realTable.getRowCount() - 1, numLines.value
					* realTable.getRowHeight());
	}

	private void applyActivityEdit(List<Activity> activities, JTable table,
			int row, Activity activity, Object[] rowModel) {
		activities.set(row, activity);
		rowModel[0] = new Range(activity.getMinOccurs(),
				activity.getMaxOccurs());
		rowModel[1] = activity.getType() == null ? "" : activity.getType();
		Holder<Integer> numLines = new Holder<>(0);
		rowModel[2] = getAnnotationsForDisplay(activity.getAnnotation(),
				activity.getSemanticAnnotation(), numLines);
		setRowLines(table, row, numLines.value);
	}

	private String getAnnotationsForDisplay(List<?> annotations,
			List<SemanticAnnotation> semanticAnnotations,
			Holder<Integer> numLines) {
		StringBuilder sb = new StringBuilder(
				"<html><p style=\"white-space:nowrap\">");
		String sep = "";
		for (Object obj : annotations) {
			BasicAnnotations annType;
			if (obj instanceof PortAnnotation) {
				annType = ((PortAnnotation) obj).getValue().value();
			} else if (obj instanceof ComponentAnnotation) {
				annType = ((ComponentAnnotation) obj).getValue().value();
			} else if (obj instanceof ActivityAnnotation) {
				annType = ((ActivityAnnotation) obj).getValue().value();
			} else {
				getLogger(getClass()).warn("unhandled type " + obj.getClass());
				continue;
			}
			sb.append(sep).append("Needs ").append(annType.value());
			numLines.value++;
			sep = "<br>";
		}
		for (SemanticAnnotation sa : semanticAnnotations) {
			PossibleStatement ps;
			try {
				ps = ontologies.getStatementFor(sa);
			} catch (OntologyCollectionException e) {
				// e.printStackTrace();
				continue;
			}
			sb.append(sep).append(ps);
			numLines.value++;
			sep = "<br>";
		}
		return sb.toString();
	}

	private JTable defineTableList(final Class<?>[] classes, Object... columns) {
		final Object[] realColumns = new Object[columns.length + 1];
		System.arraycopy(columns, 0, realColumns, 0, columns.length);
		realColumns[columns.length] = "";
		JTable jt = new JTable(new DefaultTableModel(new Object[0][],
				realColumns) {
			@Override
			public boolean isCellEditable(int x, int y) {
				return y == (realColumns.length - 1) || true;
			}

			@Override
			public Class<?> getColumnClass(int col) {
				if (classes == null || col >= classes.length
						|| classes[col] == null)
					return Object.class;
				return classes[col];
			}
		});
		installDelegatingColumn(jt.getColumn(""), "Del");
		return jt;
	}

	private JTabbedPane setupTabbedPane() {
		JTabbedPane tabs = new JTabbedPane();
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(tabs, CENTER);
		return tabs;
	}

	public ProfileFrame() throws JAXBException {
		super(APP_NAME);
		setLocationRelativeTo(null);

		context = JAXBContext.newInstance(Profile.class);
		factory = new ObjectFactory();
		ontologies = new OntologyCollection(factory);
		profile = createDefault(factory);

		Action newAction = new WatchingAction("New", VK_N) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isModified()) {
					switch (confirmForSave("creating a new profile")) {
					case CANCEL_OPTION:
						return;
					case YES_OPTION:
						if (!save())
							return;
					}
				}
				try {
					installProfile(null, createDefault(factory));
				} catch (OntologyCollectionException e1) {
					// Should be unreachable
				}
			}

			@Override
			protected void respondToModifiedChanged() {
				setEnabled(isModified() || getFile() != null);
			}
		};
		Action saveAction = new WatchingAction("Save", VK_S) {
			@Override
			public void actionPerformed(ActionEvent e) {
				save();
			}

			@Override
			protected void respondToModifiedChanged() {
				setEnabled(isModified());
			}
		};
		Action saveAsAction = new WatchingAction("Save As...", 0) {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveas();
			}

			@Override
			protected void respondToModifiedChanged() {
				setEnabled(isModified());
			}
		};
		if (isOnMac())
			saveAsAction.putValue(
					ACCELERATOR_KEY,
					getKeyStroke(VK_S, SHIFT_DOWN_MASK
							+ getDefaultToolkit().getMenuShortcutKeyMask()));

		saveAsAction.putValue(Action.MNEMONIC_KEY, VK_A);
		Action openAction = new WatchingAction("Open...", VK_O) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isModified()) {
					switch (confirmForSave("opening another profile")) {
					case CANCEL_OPTION:
						return;
					case YES_OPTION:
						if (!save())
							return;
					}
				}
				open();
			}
		};
		Action openUrlAction = new WatchingAction("Open URL...", 0) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isModified()) {
					switch (confirmForSave("opening another profile")) {
					case CANCEL_OPTION:
						return;
					case YES_OPTION:
						if (!save())
							return;
					}
				}
				openUrl();
			}
		};
		quitAction = new WatchingAction("Quit", VK_Q) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (isModified()) {
					switch (confirmForSave("quitting")) {
					case CANCEL_OPTION:
						return;
					case YES_OPTION:
						if (!save())
							return;
					}
				}
				System.exit(0);
			}
		};
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				quitAction.actionPerformed(null);
			}
		});
		Action addOnt = new AbstractAction("Add Ontology") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new EditOntologyDialog(ProfileFrame.this, "Add an Ontology",
						new EditOntology() {
							@Override
							public boolean edited(Ontology ont) {
								return doEdit(ont);
							}
						}).setVisible(true);
			}

			private boolean doEdit(Ontology ont) {
				for (Ontology o : profile.getOntology())
					if (o.getId().equals(ont.getId())) {
						errorDialog("That ontology ID is already present!",
								"Duplicate Ontology");
						return false;
					}
				try {
					addOntologyRow(ont, false);
				} catch (OntologyCollectionException e) {
					errorDialog(
							"Problem when loading ontology: " + e.getMessage(),
							"Bad Ontology Location");
					return false;
				}
				profile.getOntology().add(ont);
				setModified(true);
				return true;
			}
		};
		Action addInput = new AbstractAction("Add Input Constraint") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new EditPortDialog(ProfileFrame.this,
						"Add an Input Port Constraint", new EditPort() {
							@Override
							public void edited(Port port) {
								doEdit(port);
							}
						}).setVisible(true);
			}

			private void doEdit(Port port) {
				if (port.getName() != null)
					for (Port p : profile.getComponent().getInputPort())
						if (p.getName() != null
								&& p.getName().equals(port.getName())) {
							errorDialog(

							"That input port already has a constraint!",
									"Duplicate Input Port Constraint");
							return;
						}
				addPort(inputTable, inputs, profile.getComponent()
						.getInputPort(), port);
				setModified(true);
			}
		};
		Action addOutput = new AbstractAction("Add Output Constraint") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new EditPortDialog(ProfileFrame.this,
						"Add an Output Port Constraint", new EditPort() {
							@Override
							public void edited(Port port) {
								doEdit(port);
							}
						}).setVisible(true);
			}

			private void doEdit(Port port) {
				if (port.getName() != null)
					for (Port p : profile.getComponent().getOutputPort())
						if (p.getName() != null
								&& p.getName().equals(port.getName())) {
							errorDialog(
									"That output port already has a constraint!",
									"Duplicate Output Port Constraint");
							return;
						}
				addPort(outputTable, outputs, profile.getComponent()
						.getOutputPort(), port);
				setModified(true);
			}
		};
		Action addActivity = new AbstractAction("Add Activity Constraint") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new EditActivityDialog(ProfileFrame.this,
						"Add an Activity Constraint", new EditActivity() {
							@Override
							public void edited(Activity port) {
								doEdit(port);
							}
						}).setVisible(true);
			}

			private void doEdit(Activity activity) {
				addActivity(activityTable, activities, profile.getComponent()
						.getActivity(), activity);
				setModified(true);
			}
		};
		final Action addSemanticAnnotation = new AbstractAction(
				"Add Component Annotation") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (ontologies.getPossibleStatements().isEmpty()) {
					errorDialog("No ontologies imported, so no annotations "
							+ "are available.", "No Legal Annotations");
					return;
				}
				componentAnnotations.addRow(new Object[] {
						ontologies.getPossibleStatements().get(0), OPTIONAL,
						new JButton(deleteComponentAnnotationRow) });
			}
		};
		Action addErrorHandler = new AbstractAction("Add Error Handler") {
			@Override
			public void actionPerformed(ActionEvent e) {
				addHandleException(defaultHandleException());
			}
		};

		JMenu fileMenu;
		setJMenuBar(new JMenuBar());
		getJMenuBar().add(fileMenu = new JMenu("File"));
		fileMenu.add(newAction);
		fileMenu.add(openAction);
		fileMenu.add(openUrlAction);
		fileMenu.add(new JSeparator());
		fileMenu.add(saveAction);
		fileMenu.add(saveAsAction);
		if (!isOnMac()) {
			fileMenu.add(new JSeparator());
			fileMenu.add(quitAction);
		}

		JTabbedPane tabs = setupTabbedPane();
		GridPanel panel;
		tabs.add("Global", panel = new GridPanel(5));
		id = panel.add("ID:", new JLabel(profile.getId()), 0);
		title = panel.add("Name:", new JTextField(), 1); // Keep short?
		description = panel.add("Description (plain text):", new JTextArea(3,
				40), 2);
		extend = panel.add("Extends Profile (URL):", new JTextField(), 3);
		JComponent jp = panel.add("Standard Annotations:", new JPanel(), 4);
		jp.add(requireAuthor = new JCheckBox("Author"));
		jp.add(requireDescription = new JCheckBox("Description"));
		jp.add(requireTitle = new JCheckBox("Title"));
		if (ontologies.getPossibleStatements().isEmpty())
			addSemanticAnnotation.setEnabled(false);
		componentAnnotations = new DefaultTableModel(new Object[0][],
				new Object[] { "Annotation", "Cardinality", "" });
		final JTable ann = panel.add(addSemanticAnnotation, new JTable(
				componentAnnotations), 5);
		deleteComponentAnnotationRow = new RowDeletionAction(ann);
		ontologies.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				addSemanticAnnotation.setEnabled(!ontologies
						.getPossibleStatements().isEmpty());
				configureColumn(ann, 0, null, ontologies.tableRenderer(),
						ontologies.tableEditor());
			}
		});
		ann.setPreferredScrollableViewportSize(new Dimension(24, 48));
		configureColumn(ann, 1, 64, Cardinality.tableRenderer(),
				Cardinality.tableEditor());
		installDelegatingColumn(ann.getColumnModel().getColumn(2), "Del");

		tabs.add("Ports", panel = new GridPanel());
		inputTable = panel.add(
				addInput,
				defineTableList(
						new Class<?>[] { null, null, null, String.class },
						"Cardinality", "Depth", "Name", "Annotations"), 0);
		inputs = (DefaultTableModel) inputTable.getModel();
		inputTable.setPreferredScrollableViewportSize(new Dimension(256, 48));
		makeRowsDialogEditable(inputTable, new ShowDialog() {
			@Override
			public Object[] edit(final Object[] rowData, final int rowNumber) {
				final List<Port> portList = profile.getComponent()
						.getInputPort();
				new EditPortDialog(ProfileFrame.this,
						"Edit an Input Port Constraint", new EditPort() {
							@Override
							public void edited(Port port) {
								applyPortEdit(portList, inputTable, rowNumber,
										port, rowData);
								setModified(true);
							}
						}, portList.get(rowNumber)).setVisible(true);
				return rowData;
			}
		});

		outputTable = panel.add(
				addOutput,
				defineTableList(
						new Class<?>[] { null, null, null, String.class },
						"Cardinality", "Depth", "Name", "Annotations"), 1);
		outputs = (DefaultTableModel) outputTable.getModel();
		outputTable.setPreferredScrollableViewportSize(new Dimension(256, 48));
		makeRowsDialogEditable(outputTable, new ShowDialog() {
			@Override
			public Object[] edit(final Object[] rowData, final int rowNumber) {
				final List<Port> portList = profile.getComponent()
						.getOutputPort();
				new EditPortDialog(ProfileFrame.this,
						"Edit an Output Port Constraint", new EditPort() {
							@Override
							public void edited(Port port) {
								applyPortEdit(portList, outputTable, rowNumber,
										port, rowData);
								setModified(true);
							}
						}, portList.get(rowNumber)).setVisible(true);
				return rowData;
			}
		});

		tabs.add("Annotation Ontologies", panel = new GridPanel());
		ontologyTable = panel.add(addOnt,
				defineTableList(null, "Name", "Location"), 0);
		ontologyList = (DefaultTableModel) ontologyTable.getModel();
		ontologyTable
				.setPreferredScrollableViewportSize(new Dimension(256, 48));
		ontologyTable.getColumn("Location").setMinWidth(192);
		ontologyTable.setDefaultRenderer(Object.class, new DisabledRenderer());

		tabs.add("Implementation", panel = new GridPanel());
		activityTable = panel.add(
				addActivity,
				defineTableList(new Class[] { null, null, String.class },
						"Cardinality", "Type", "Annotations"), 0);
		activities = (DefaultTableModel) activityTable.getModel();
		activityTable.setPreferredScrollableViewportSize(new Dimension(24, 48));
		makeRowsDialogEditable(activityTable, new ShowDialog() {
			@Override
			public Object[] edit(final Object[] rowData, final int rowNumber) {
				final List<Activity> actList = profile.getComponent()
						.getActivity();
				new EditActivityDialog(ProfileFrame.this,
						"Edit an Activity Constraint", new EditActivity() {
							@Override
							public void edited(Activity port) {
								applyActivityEdit(actList, activityTable,
										rowNumber, port, rowData);
								setModified(true);
							}
						}, actList.get(rowNumber)).setVisible(true);
				return rowData;
			}
		});
		failLists = panel.add(new JCheckBox("Fail lists"), 1, 1);
		if (ontologies.getPossibleStatements().isEmpty())
			addSemanticAnnotation.setEnabled(false);
		exceptionHandling = new DefaultTableModel(new Object[0][],
				new Object[] { "Pattern", "Prune", "Replacement ID",
						"Replacement Message", "" }) {
			@Override
			public Class<?> getColumnClass(int col) {
				if (col == 1)
					return Boolean.class;
				return super.getColumnClass(col);
			}
		};
		final JTable exn = panel.add(addErrorHandler, new JTable(
				exceptionHandling), 2);
		deleteExceptionHandlingRow = new RowDeletionAction(exn);
		exn.setPreferredScrollableViewportSize(new Dimension(24, 48));
		installDelegatingColumn(exn.getColumnModel().getColumn(4), "Del");

		abstract class ModifiedListener implements DocumentListener {
			public abstract void respond();

			@Override
			public void insertUpdate(DocumentEvent e) {
				setModified(true);
				respond();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setModified(true);
				respond();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		}
		title.getDocument().addDocumentListener(new ModifiedListener() {
			@Override
			public void respond() {
				profile.setName(title.getText().trim());
			}
		});
		description.getDocument().addDocumentListener(new ModifiedListener() {
			@Override
			public void respond() {
				profile.setDescription(description.getText().trim());
			}
		});
		extend.getDocument().addDocumentListener(new ModifiedListener() {
			@Override
			public void respond() {
				String ext = extend.getText().trim();
				if (ext.isEmpty())
					profile.setExtends(null);
				else {
					Extends e = factory.createExtends();
					e.setProfileId(ext);
					profile.setExtends(e);
				}
			}
		});
		class ComponentAnnotationChangeListener implements ChangeListener {
			private final JCheckBox listenTo;
			private final ComponentAnnotations subject;

			ComponentAnnotationChangeListener(JCheckBox checkbox,
					ComponentAnnotations what) {
				listenTo = checkbox;
				subject = what;
				listenTo.addChangeListener(this);
			}

			@Override
			public void stateChanged(ChangeEvent e) {
				List<ComponentAnnotation> l = profile.getComponent()
						.getAnnotation();
				ComponentAnnotation anno = null;
				for (ComponentAnnotation a : l)
					if (a.getValue().equals(subject)) {
						if (listenTo.isSelected())
							anno = a;
						else
							l.remove(a);
						break;
					}
				if (listenTo.isSelected() && anno == null) {
					anno = factory.createComponentAnnotation();
					anno.setValue(subject);
					l.add(anno);
				}
				setModified(true);
			}
		}
		new ComponentAnnotationChangeListener(requireAuthor,
				ComponentAnnotations.AUTHOR);
		new ComponentAnnotationChangeListener(requireDescription,
				ComponentAnnotations.DESCRIPTION);
		new ComponentAnnotationChangeListener(requireTitle,
				ComponentAnnotations.TITLE);
		failLists.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				getExceptionHandling().setFailLists(
						failLists.isSelected() ? factory.createFailLists()
								: null);
				setModified(true);
			}
		});
		componentAnnotations.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				rebuildSemanticAnnotationList();
				setModified(true);
			}
		});
		exceptionHandling.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				rebuildExceptionHandlingList();
				setModified(true);
			}
		});

		pack();
		setLocation((getDefaultToolkit().getScreenSize().width) / 2
				- getWidth() / 2, getDefaultToolkit().getScreenSize().height
				/ 2 - getHeight() / 2);
		validate();
		setModified(true);
		setModified(false);
	}

	private ExceptionHandling getExceptionHandling() {
		if (profile.getComponent().getExceptionHandling() == null)
			profile.getComponent().setExceptionHandling(
					factory.createExceptionHandling());
		return profile.getComponent().getExceptionHandling();
	}

	private static Profile createDefault(ObjectFactory factory) {
		Profile def = factory.createProfile();
		def.setId(UUID.randomUUID().toString());
		def.setName("");
		def.setDescription("");
		def.setComponent(factory.createComponent());
		return def;
	}

	private HandleException defaultHandleException() {
		HandleException he = factory.createHandleException();
		he.setPattern("");
		he.setReplacement(factory.createReplacement());
		he.getReplacement().setReplacementId(UUID.randomUUID().toString());
		he.getReplacement().setReplacementMessage("");
		return he;
	}

	public void loadFile(String filename) throws IOException, JAXBException,
			OntologyCollectionException {
		loadFile(new File(filename));
	}

	private void loadFile(File file) throws IOException, JAXBException,
			OntologyCollectionException {
		installProfile(file,
				(Profile) context.createUnmarshaller().unmarshal(file));
	}

	private JFileChooser fileDialog = new JFileChooser();

	public boolean open() {
		if (fileDialog.showOpenDialog(this) != APPROVE_OPTION)
			return false;
		try {
			loadFile(fileDialog.getSelectedFile());
			return true;
		} catch (Exception e) {
			errorDialog(e, "Error in Opening");
			return false;
		}
	}

	public boolean openUrl() {
		String url = showInputDialog(this,
				"Enter a (publicly-readable) URL to load from.", "Open URL",
				QUESTION_MESSAGE);
		if (url == null || url.trim().isEmpty())
			return false;
		try {
			installProfile(null, (Profile) context.createUnmarshaller()
					.unmarshal(new URL(url)));
			return true;
		} catch (MalformedURLException | JAXBException
				| OntologyCollectionException e) {
			errorDialog(e, "Error in Opening");
			return false;
		}
	}

	public boolean save() {
		try {
			saveFile();
			return true;
		} catch (Exception e) {
			errorDialog(e, "Error in Saving");
			return false;
		}
	}

	public boolean saveas() {
		if (fileDialog.showSaveDialog(this) != APPROVE_OPTION)
			return false;
		try {
			saveFile(fileDialog.getSelectedFile());
			return true;
		} catch (Exception e) {
			errorDialog(e, "Error in Saving");
			return false;
		}
	}

	private void saveFile() throws JAXBException {
		if (getFile() == null) {
			if (fileDialog.showSaveDialog(this) == APPROVE_OPTION)
				saveFile(fileDialog.getSelectedFile());
			return;
		}
		saveFile(getFile());
	}

	private void saveFile(File f) throws JAXBException {
		if (getExceptionHandling().getFailLists() == null
				&& getExceptionHandling().getHandleException().isEmpty())
			profile.getComponent().setExceptionHandling(null);
		context.createMarshaller().marshal(profile, f);
		setFile(f);
		setModified(false);
	}

	protected void installProfile(File f, Profile p)
			throws OntologyCollectionException {
		/*
		 * Important! Clone lists here to avoid
		 * ConcurrentModificationExceptions!
		 */

		id.setText(p.getId());
		title.setText(p.getName());
		description.setText(p.getDescription());
		Extends e = p.getExtends();
		extend.setText(e == null ? "" : e.getProfileId());
		ontologyList.setRowCount(0);
		for (Ontology o : p.getOntology())
			addOntologyRow(o, true);
		Component comp = p.getComponent();
		inputs.setRowCount(0);
		for (Port port : new ArrayList<>(comp.getInputPort()))
			addPort(inputTable, inputs, comp.getInputPort(), port);
		outputs.setRowCount(0);
		for (Port port : new ArrayList<>(comp.getOutputPort()))
			addPort(outputTable, outputs, comp.getOutputPort(), port);
		for (Activity activity : new ArrayList<>(comp.getActivity()))
			addActivity(activityTable, activities, comp.getActivity(), activity);

		for (ComponentAnnotation o : comp.getAnnotation())
			switch (o.getValue()) {
			case AUTHOR:
				requireAuthor.setSelected(true);
				break;
			case DESCRIPTION:
				requireDescription.setSelected(true);
				break;
			case TITLE:
				requireTitle.setSelected(true);
				break;
			}

		for (SemanticAnnotation sa : new ArrayList<>(
				comp.getSemanticAnnotation()))
			componentAnnotations.addRow(new Object[] {
					ontologies.getStatementFor(sa),
					Cardinality.get(sa.getMinOccurs(), sa.getMaxOccurs()),
					new JButton(deleteComponentAnnotationRow) });
		if (comp.getExceptionHandling() != null) {
			failLists
					.setSelected(comp.getExceptionHandling().getFailLists() != null);
			for (HandleException he : new ArrayList<>(comp
					.getExceptionHandling().getHandleException()))
				addHandleException(he);
		}

		setFile(f);
		profile = p;
		setModified(false);
	}

	int confirmForSave(String msg) {
		return showConfirmDialog(this,
				"Do you want to save the current profile before " + msg + "?",
				"Save First?", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE);
	}

	private void errorDialog(Throwable t, String title) {
		errorDialog(t.getMessage(), title);
		getLogger(getClass()).error(title, t);
	}

	private void errorDialog(String message, String title) {
		showMessageDialog(this, message, title, ERROR_MESSAGE);
	}

	private void rebuildSemanticAnnotationList() {
		profile.getComponent().getSemanticAnnotation().clear();
		for (int r = 0; r < componentAnnotations.getRowCount(); r++) {
			PossibleStatement ps = (PossibleStatement) componentAnnotations
					.getValueAt(r, 0);
			Cardinality c = (Cardinality) componentAnnotations.getValueAt(r, 1);
			profile.getComponent().getSemanticAnnotation()
					.add(ps.getAnnotation(c));
		}
	}

	@SuppressWarnings("unchecked")
	private void rebuildExceptionHandlingList() {
		getExceptionHandling().getHandleException().clear();
		for (Vector<?> row : (Vector<Vector<?>>) exceptionHandling
				.getDataVector()) {
			HandleException he = factory.createHandleException();
			he.setPattern((String) row.get(0));
			if (row.get(1) != null && ((Boolean) row.get(1)))
				he.setPruneStack(factory.createPruneStack());
			if (!((String) row.get(2)).isEmpty()) {
				Replacement repl = factory.createReplacement();
				repl.setReplacementId((String) row.get(2));
				repl.setReplacementMessage((String) row.get(3));
				he.setReplacement(repl);
			}
			getExceptionHandling().getHandleException().add(he);
		}
	}

	private ApplicationListener applistener = new AppListener();

	public ApplicationListener getApplicationListener() {
		return applistener;
	}

	public class AppListener implements ApplicationListener {
		@Override
		public void handleAbout(ApplicationEvent event) {
			event.setHandled(true);
			showMessageDialog(ProfileFrame.this,
					"<html><div style=\"width:500px\"><h2>" + APP_NAME + ", "
							+ getProperty("app.version")
							+ "</h2><p>Copyright \u00a9 "
							+ getProperty("app.year")
							+ ", The University of Manchester.<p><p>"
							+ "<small>" + getProperty("app.description")
							+ "</small></div>");
		}

		@Override
		public void handleOpenApplication(ApplicationEvent event) {
			// Ignore
		}

		@Override
		public void handleOpenFile(ApplicationEvent event) {
			event.setHandled(true);
			if (isModified()) {
				switch (confirmForSave("opening another profile")) {
				case CANCEL_OPTION:
					return;
				case YES_OPTION:
					if (!save())
						return;
				}
			}
			try {
				loadFile(fileDialog.getSelectedFile());
			} catch (Exception e) {
				errorDialog(e, "Error in Opening");
			}
		}

		@Override
		public void handlePreferences(ApplicationEvent event) {
			// Ignore
		}

		@Override
		public void handlePrintFile(ApplicationEvent event) {
			// Ignore
		}

		@Override
		public void handleQuit(ApplicationEvent event) {
			event.setHandled(true);
			quitAction.actionPerformed(null);
		}

		@Override
		public void handleReOpenApplication(ApplicationEvent event) {
			event.setHandled(true);
			setVisible(true);
		}
	}
}
