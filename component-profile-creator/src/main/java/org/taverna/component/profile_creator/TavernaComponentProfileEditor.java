package org.taverna.component.profile_creator;

import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.slf4j.LoggerFactory.getLogger;

import javax.swing.UIManager;

/**
 * Startup code. Separated out because OSX is sometimes weird like that.
 * 
 * @author Donal Fellows
 * @see ProfileFrame
 */
public abstract class TavernaComponentProfileEditor {
	public static void main(final String... args) throws Exception {
		// // PROPERTY DOES NOT WORK! Why? Why? Why?
		// System.setProperty("com.apple.mrj.application.apple.menu.about.name",
		// "Taverna Component Profile Editor");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			ProfileFrame pc = new ProfileFrame();
			pc.setVisible(true);
			if (args.length > 0)
				pc.loadFile(args[0]);
		} catch (Exception e) {
			getLogger(ProfileFrame.class).error("problem during startup", e);
			showMessageDialog(null, e.getMessage(), "Problem loading file",
					ERROR_MESSAGE);
		}
	}
}
