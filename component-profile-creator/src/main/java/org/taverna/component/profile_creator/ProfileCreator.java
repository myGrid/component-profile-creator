package org.taverna.component.profile_creator;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.HORIZONTAL;
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.EventObject;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.taverna.component.profile_creator.AddOntologyDialog.AddOntology;

import uk.org.taverna.ns._2012.component.profile.ObjectFactory;
import uk.org.taverna.ns._2012.component.profile.Ontology;
import uk.org.taverna.ns._2012.component.profile.Profile;

@SuppressWarnings("serial")
public class ProfileCreator extends JFrame {
	private final JAXBContext context;
	final ObjectFactory factory;
	private final JLabel id;
	private final JTextField title;
	private final JTextArea description;
	private final DefaultTableModel ontologyList;
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

	private void add(JComponent c, int x, int y) {
		GridBagConstraints constr = new GridBagConstraints();
		constr.gridx = x;
		constr.gridy = y;
		constr.weightx = (x == 0 ? 0 : 1);
		constr.weighty = (y < 2 ? 0 : 1);
		constr.fill = (x == 0 ? HORIZONTAL : BOTH);
		constr.anchor = GridBagConstraints.NORTH;
		add(c, constr);
	}

	abstract class WatchingAction extends AbstractAction implements
			PropertyChangeListener {
		public WatchingAction(String string, int key) {
			super(string);
			if (key != 0) {
				putValue(
						Action.ACCELERATOR_KEY,
						getKeyStroke(key, getDefaultToolkit()
								.getMenuShortcutKeyMask()));
				putValue(Action.MNEMONIC_KEY, key);
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
				new AddOntologyDialog(ProfileCreator.this, new AddOntology() {
					@Override
					public void add(Ontology ont) {
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
				});
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

		setLayout(new GridBagLayout());
		add(new JLabel("ID"), 0, 0);
		add(id = new JLabel(profile.getId()), 1, 0);
		add(new JLabel("Name"), 0, 1);
		add(title = new JTextField(), 1, 1);
		add(new JLabel("Description"), 0, 2);
		add(new JScrollPane(description = new JTextArea(3, 40)), 1, 2);
		add(new JButton(addOnt), 0, 3);
		JTable jt;
		add(new JScrollPane(jt = new JTable(
				ontologyList = new DefaultTableModel())), 1, 3);
		jt.setPreferredScrollableViewportSize(new Dimension(256, 64));
		ontologyList.addColumn("Name");
		ontologyList.addColumn("Location");
		ontologyList.addColumn("");
		jt.getColumn("Location").setMinWidth(192);
		// jt.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		TableColumn bcol = jt.getColumn("");
		bcol.setMaxWidth(new JButton("Del").getPreferredSize().width);
		bcol.setCellRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table,
					Object value, boolean isSelected, boolean hasFocus,
					int row, int column) {
				return (Component) value;
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
			public Component getTableCellEditorComponent(JTable table,
					Object value, boolean isSelected, int row, int column) {
				return (Component) value;
			}
		});

		DocumentListener l = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				setModified(true);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setModified(true);
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				setModified(true);
			}
		};
		title.getDocument().addDocumentListener(l);
		description.getDocument().addDocumentListener(l);

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
		ontologyList.setRowCount(0);
		for (Ontology o : p.getOntology())
			addOntologyRow(o);
		// TODO Display rest of content of profile
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
