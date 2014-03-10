package org.taverna.component.profile_creator;

import static java.awt.BorderLayout.CENTER;
import static java.awt.Toolkit.getDefaultToolkit;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_N;
import static java.awt.event.KeyEvent.VK_O;
import static java.awt.event.KeyEvent.VK_Q;
import static java.awt.event.KeyEvent.VK_S;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JOptionPane.CANCEL_OPTION;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.QUESTION_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_CANCEL_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;

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
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.taverna.component.profile_creator.EditOntologyDialog.EditOntology;
import org.taverna.component.profile_creator.EditPortDialog.EditPort;
import org.taverna.component.profile_creator.utils.GridPanel;

import uk.org.taverna.ns._2012.component.profile.Component;
import uk.org.taverna.ns._2012.component.profile.ComponentAnnotation;
import uk.org.taverna.ns._2012.component.profile.ComponentAnnotations;
import uk.org.taverna.ns._2012.component.profile.Extends;
import uk.org.taverna.ns._2012.component.profile.ObjectFactory;
import uk.org.taverna.ns._2012.component.profile.Ontology;
import uk.org.taverna.ns._2012.component.profile.Port;
import uk.org.taverna.ns._2012.component.profile.Profile;

@SuppressWarnings("serial")
public class ProfileCreator extends JFrame {
	private final JAXBContext context;
	final ObjectFactory factory;
	private final JLabel id;
	private final JTextField title, extend;
	private final JTextArea description;
	private final DefaultTableModel ontologyList, inputs, outputs;
	private final JCheckBox requireAuthor, requireDescription, requireTitle;
	private File file;
	private Profile profile;
	private boolean modified;

	boolean isModified() {
		return modified;
	}

	void setModified(boolean modified) {
		boolean oldMod = this.modified;
		this.modified = modified;
		firePropertyChange("modified", oldMod, modified);
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
			ProfileCreator.this.addPropertyChangeListener(this);
		}

		protected void respondToModifiedChanged() {
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("modified"))
				respondToModifiedChanged();
		}
	}

	private void addOntologyRow(Ontology ont) {
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
		ontologyList.addRow(new Object[] { ont.getId(), ont.getValue(), jb });
	}

	private void addPort(final DefaultTableModel table,
			final List<Port> portCollection, final Port port) {
		class Range {
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
		// FIXME annotations
		table.addRow(new Object[] {
				new Range(port.getMinOccurs(), port.getMaxOccurs()),
				new Range(port.getMinDepth(), port.getMaxDepth()),
				port.getName() == null ? "" : port.getName(), "", jb });
	}

	private JTable defineTableList(Object... columns) {
		final Object[] realColumns = new Object[columns.length + 1];
		System.arraycopy(columns, 0, realColumns, 0, columns.length);
		realColumns[columns.length] = "";
		JTable jt = new JTable(new DefaultTableModel(new Object[0][],
				realColumns) {
			@Override
			public boolean isCellEditable(int x, int y) {
				return y == (realColumns.length - 1);
			}
		});
		TableColumn bcol = jt.getColumn("");
		bcol.setMaxWidth(new JButton("Del").getPreferredSize().width);
		bcol.setCellRenderer(new TableCellRenderer() {
			@Override
			public JComponent getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				return (JComponent) value;
			}
		});
		bcol.setCellEditor(new TableCellEditor() {
			@Override
			public Object getCellEditorValue() {
				return null;
			}

			@Override
			public boolean isCellEditable(EventObject anEvent) {
				return true;
			}

			@Override
			public boolean shouldSelectCell(EventObject anEvent) {
				return false;
			}

			@Override
			public boolean stopCellEditing() {
				return true;
			}

			@Override
			public void cancelCellEditing() {
			}

			@Override
			public void addCellEditorListener(CellEditorListener l) {
			}

			@Override
			public void removeCellEditorListener(CellEditorListener l) {
			}

			@Override
			public JComponent getTableCellEditorComponent(JTable table,
					Object value, boolean isSelected, int row, int column) {
				return (JComponent) value;
			}
		});
		return jt;
	}

	public ProfileCreator() throws JAXBException {
		super("Taverna Component Profile Creator");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		context = JAXBContext.newInstance(Profile.class);
		factory = new ObjectFactory();
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
				installProfile(null, createDefault(factory));
			}

			@Override
			protected void respondToModifiedChanged() {
				setEnabled(isModified() || file != null);
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
		Action quitAction = new WatchingAction("Quit", VK_Q) {
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
		Action addOnt = new AbstractAction("Add Ontology") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new EditOntologyDialog(ProfileCreator.this, "Add an Ontology",
						new EditOntology() {
							@Override
							public void edited(Ontology ont) {
								doEdit(ont);
							}
						}).setVisible(true);
			}

			private void doEdit(Ontology ont) {
				for (Ontology o : profile.getOntology())
					if (o.getId().equals(ont.getId())) {
						showMessageDialog(ProfileCreator.this,
								"That ontology ID is already present!",
								"Duplicate Ontology", ERROR_MESSAGE);
						return;
					}
				profile.getOntology().add(ont);
				addOntologyRow(ont);
				setModified(true);
			}
		};
		Action addInput = new AbstractAction("Add Input") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new EditPortDialog(ProfileCreator.this,
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
							showMessageDialog(
									ProfileCreator.this,
									"That input port already has a constraint!",
									"Duplicate Input Port Constraint",
									ERROR_MESSAGE);
							return;
						}
				profile.getComponent().getInputPort().add(port);
				setModified(true);
			}
		};
		Action addOutput = new AbstractAction("Add Output") {
			@Override
			public void actionPerformed(ActionEvent e) {
				new EditPortDialog(ProfileCreator.this,
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
							showMessageDialog(
									ProfileCreator.this,
									"That output port already has a constraint!",
									"Duplicate Output Port Constraint",
									ERROR_MESSAGE);
							return;
						}
				profile.getComponent().getOutputPort().add(port);
				setModified(true);
			}
		};

		JMenu fileMenu;
		setJMenuBar(new JMenuBar());
		getJMenuBar().add(fileMenu = new JMenu("File"));
		fileMenu.add(newAction);
		fileMenu.add(openAction);
		fileMenu.add(new JSeparator());
		fileMenu.add(saveAction);
		fileMenu.add(saveAsAction);
		fileMenu.add(new JSeparator());
		fileMenu.add(quitAction);

		JTabbedPane tabs;
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(tabs = new JTabbedPane(), CENTER);
		GridPanel panel;
		tabs.add("Global", panel = new GridPanel(5));
		setLayout(new GridBagLayout());
		id = panel.add("ID:", new JLabel(profile.getId()), 0);
		title = panel.add("Name:", new JTextField(), 1);
		description = panel.add("Description:", new JTextArea(3, 40), 2);
		extend = panel.add("Extends:", new JTextField(), 3);
		JComponent jp = panel.add("Std. Annotations:", new JPanel(), 4);
		jp.add(requireAuthor = new JCheckBox("Author"));
		jp.add(requireDescription = new JCheckBox("Description"));
		jp.add(requireTitle = new JCheckBox("Title"));
		JTable jt = panel.add(addOnt, defineTableList("Name", "Location"), 5);
		ontologyList = (DefaultTableModel) jt.getModel();
		jt.setPreferredScrollableViewportSize(new Dimension(256, 48));
		jt.getColumn("Location").setMinWidth(192);

		tabs.add("Inputs", panel = new GridPanel(0));
		jt = panel.add(addInput,
				defineTableList("Cardinality", "Depth", "Name", "Annotations"),
				0);
		inputs = (DefaultTableModel) jt.getModel();

		tabs.add("Outputs", panel = new GridPanel(0));
		jt = panel.add(addOutput,
				defineTableList("Cardinality", "Depth", "Name", "Annotations"),
				0);
		outputs = (DefaultTableModel) jt.getModel();

		tabs.add("Activities", panel = new GridPanel(0));
		tabs.add("Annotations", panel = new GridPanel(0));
		tabs.add("Extra Features", panel = new GridPanel(0));

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
				for (ComponentAnnotation a : l) {
					if (a.getValue().equals(subject)) {
						if (listenTo.isSelected())
							anno = a;
						else
							l.remove(a);
						break;
					}
				}
				if (listenTo.isSelected() && anno == null) {
					anno = new ComponentAnnotation();
					anno.setValue(subject);
					l.add(anno);
				}
			}
		}
		new ComponentAnnotationChangeListener(requireAuthor, ComponentAnnotations.AUTHOR);
		new ComponentAnnotationChangeListener(requireDescription,
				ComponentAnnotations.DESCRIPTION);
		new ComponentAnnotationChangeListener(requireTitle, ComponentAnnotations.TITLE);

		pack();
		validate();
		setModified(true);
		setModified(false);
	}

	private static Profile createDefault(ObjectFactory factory) {
		Profile def = factory.createProfile();
		def.setId(UUID.randomUUID().toString());
		def.setName("");
		def.setDescription("");
		return def;
	}

	public static void main(String... args) throws Exception {
		ProfileCreator pc = new ProfileCreator();
		pc.setVisible(true);
		if (args.length > 0)
			pc.loadFile(args[0]);
	}

	private void loadFile(String filename) throws IOException, JAXBException {
		loadFile(new File(filename));
	}

	private void loadFile(File file) throws IOException, JAXBException {
		Profile p = (Profile) context.createUnmarshaller().unmarshal(file);
		installProfile(file, p);
	}

	private JFileChooser fileDialog = new JFileChooser();

	public boolean open() {
		if (fileDialog.showOpenDialog(this) != APPROVE_OPTION)
			return false;
		try {
			loadFile(fileDialog.getSelectedFile());
			return true;
		} catch (Exception e) {
			exceptionDialog(e, "Error in Opening");
			return false;
		}
	}

	public boolean save() {
		try {
			saveFile();
			return true;
		} catch (Exception e) {
			exceptionDialog(e, "Error in Saving");
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
			exceptionDialog(e, "Error in Saving");
			return false;
		}
	}

	private void saveFile() throws JAXBException {
		if (file == null) {
			if (fileDialog.showSaveDialog(this) == APPROVE_OPTION)
				saveFile(fileDialog.getSelectedFile());
			return;
		}
		if (isModified()) {
			context.createMarshaller().marshal(profile, file);
			setModified(false);
		}
	}

	private void saveFile(File f) throws JAXBException {
		context.createMarshaller().marshal(profile, f);
		file = f;
		setModified(false);
	}

	protected void installProfile(File f, Profile p) {
		id.setText(p.getId());
		title.setText(p.getName());
		description.setText(p.getDescription());
		Extends e = p.getExtends();
		extend.setText(e == null ? "" : e.getProfileId());
		ontologyList.setRowCount(0);
		for (Ontology o : p.getOntology())
			addOntologyRow(o);
		Component comp = p.getComponent();
		inputs.setRowCount(0);
		for (Port port : comp.getInputPort())
			addPort(inputs, comp.getInputPort(), port);
		outputs.setRowCount(0);
		for (Port port : comp.getOutputPort())
			addPort(outputs, comp.getOutputPort(), port);
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
		// TODO activity
		// TODO semantic annotation
		// TODO exception handling
		file = f;
		profile = p;
		setModified(false);
	}

	int confirmForSave(String msg) {
		return showConfirmDialog(this,
				"Do you want to save the current profile before " + msg + "?",
				"Save First?", YES_NO_CANCEL_OPTION, QUESTION_MESSAGE);
	}

	void exceptionDialog(Throwable t, String title) {
		showMessageDialog(this, t.getMessage(), title, ERROR_MESSAGE);
	}
}
