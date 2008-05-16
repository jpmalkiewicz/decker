package decker;
import decker.model.*;
import decker.view.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import decker.view.SplashScreen;

/**
 *  Main class, that is used to start the game.
 */
public class Decker
{
	public static void main(String[] args) {

		try {
			System.setErr(new PrintStream(new FileOutputStream("ERROR.JAVA", false)));
		} catch (FileNotFoundException ex) {
			System.exit(1);
		}

		// parse the command line switches
		String initial_ruleset = "decker";
		for (int i = 0; i < args.length; i++) {
			int equal_sign_position = args[i].indexOf('=');
			if (equal_sign_position > 0) {
				final String variable = args[i].substring(0,equal_sign_position);
				final String value = args[i].substring(equal_sign_position+1);
				if (variable.equals("initial_ruleset"))
					initial_ruleset = value;
			}
		}

		// display the splash screen, load the rulesets
		SplashScreen ss = new SplashScreen();
		Global.setDisplayedComponent(ss);
		Global.initializeDataModel();
		Global.loadRulesets();
		// center the game window on the screen by default
		Global.getEngineData().add("display_center_x").set(ss.getToolkit().getScreenSize().width / 2);
		Global.getEngineData().add("display_center_y").set(ss.getToolkit().getScreenSize().height / 2);
		Global.initializeRulesets();
		// load up the initial ruleset
		final Ruleset[] r = decker.model.Global.ruleset;
		for (int i = r.length; --i >= 0; )
			if (i == 0 || r[i].getName().equalsIgnoreCase(initial_ruleset)) {
				Global.setCurrentRuleset(decker.model.Global.ruleset[i]);
				break;
			}
		// launch the game
		ss.setVisible(false);
		Frame frame = new Frame();
		Global.setDisplayedComponent(frame);
		frame.setLayout(new BorderLayout());
		frame.add(decker.model.Global.getViewWrapper());
		new decker.view.FPSThread(decker.model.Global.getViewWrapper());
		frame.setBounds(Global.getEngineData().get("display_center_x").integer() - 100 / 2, Global.getEngineData().get("display_center_y").integer() - 70 / 2, 100, 70);
		frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				System.exit(0);
			}
		});
		frame.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				Global.getViewWrapper().requestFocus();
			}
		});
		AbstractView.reloadArtwork(true);
		Global.getDisplayedScreen().set(Global.getCurrentRuleset().data.get("initial_screen"));
		Global.getViewWrapper().repaint();
		Global.getViewWrapper().requestFocus();
	}


	public Decker () {}
}