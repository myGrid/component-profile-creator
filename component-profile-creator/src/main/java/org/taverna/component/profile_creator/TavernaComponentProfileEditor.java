package org.taverna.component.profile_creator;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.slf4j.LoggerFactory.getLogger;

import javax.swing.UIManager;

public abstract class TavernaComponentProfileEditor {
	public static void main(final String... args) throws Exception {
		// // PROPERTY DOES NOT WORK!
		// System.setProperty("com.apple.mrj.application.apple.menu.about.name",
		// "Taverna Component Profile Editor");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			ProfileCreator pc = new ProfileCreator();
			pc.setVisible(true);
			if (args.length > 0)
				pc.loadFile(args[0]);
		} catch (Exception e) {
			getLogger(ProfileCreator.class).error("problem during startup", e);
			showMessageDialog(null, e.getMessage(), "Problem loading file",
					ERROR_MESSAGE);
		}
	}
}
