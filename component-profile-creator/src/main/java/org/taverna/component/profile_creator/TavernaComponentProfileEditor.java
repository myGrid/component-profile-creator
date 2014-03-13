package org.taverna.component.profile_creator;

import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static org.slf4j.LoggerFactory.getLogger;

import javax.swing.UIManager;

import org.simplericity.macify.eawt.Application;
import org.simplericity.macify.eawt.DefaultApplication;

/**
 * Startup code. Separated out because OSX is sometimes weird like that.
 * 
 * @author Donal Fellows
 * @see ProfileFrame
 */
public abstract class TavernaComponentProfileEditor {
	public static boolean isOnMac() {
		return getProperty("os.name").toLowerCase().startsWith("mac os x");
	}

	private static void macSetup(String appName) {
		if (!isOnMac())
			return;
		setProperty("apple.laf.useScreenMenuBar", "true");
		// // PROPERTY DOES NOT WORK! Why? Why? Why?
		setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
	}

	public static void main(final String... args) throws Exception {
		getProperties().load(
				TavernaComponentProfileEditor.class
						.getResourceAsStream("/app.properties"));
		macSetup("Taverna Component Profile Editor");
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		try {
			Application app = new DefaultApplication();
			ProfileFrame pc = new ProfileFrame();
			pc.setVisible(true);
			app.addApplicationListener(pc.getApplicationListener());
			app.setEnabledPreferencesMenu(false);
			if (args.length > 0)
				pc.loadFile(args[0]);
		} catch (Exception e) {
			getLogger(ProfileFrame.class).error("problem during startup", e);
			showMessageDialog(null, e.getMessage(), "Problem loading file",
					ERROR_MESSAGE);
		}
	}
}
