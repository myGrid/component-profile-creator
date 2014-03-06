package org.taverna.component.profile_creator;

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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
		constr.weighty = 1;
		constr.fill = HORIZONTAL;
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

	public ProfileCreator() throws JAXBException {
		context = JAXBContext.newInstance(Profile.class);
		factory = new ObjectFactory();
		profile = createDefault(factory);
		JMenu fileMenu;
		this.setJMenuBar(new JMenuBar());
		this.getJMenuBar().add(fileMenu = new JMenu("File"));
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
						// FIXME Update displayed list
						setModified(true);
					}
				});
			}
		};
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
		setTitle("Taverna Component Profile Creator");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
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
		// TODO Display ontology list
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
