package decker;
import decker.model.*;
import decker.view.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;



/** all this class does is start the program */
public class Decker
{
	public static void main(String[] args) {
System.out.println("there seems to be an 'else' clause missing in Node.cpp:411");
System.out.println();
System.out.println("the game should display a placeholder image when an image is missing, instead of just erroring out");
System.out.println("change : more than one ICE can trace the player simultaneously");
System.out.println("FunctionCall.execute_filelist() needs to take mods into account");
System.out.println("when a script doesn't have any accepted locales, say so in a message popup");
System.out.println("is Global.setCurrentRuleset() safe? what happens if the ruleset changes during a trigger check?");
System.out.println("AbstractView.getImage() : need to put localized images back in, no support for mod images yet, loaded images aren't stored for later reuse yet");
		System.out.println("Errors are written to ERROR.JAVA for testing purposes");
System.out.println();
		try {
			System.setErr(new PrintStream(new FileOutputStream("ERROR.JAVA",false)));
		} catch (FileNotFoundException ex) {
			System.exit(1);
		}
		// load the rulesets
		decker.view.SplashScreen ss = new decker.view.SplashScreen();
		Global.setDisplayedComponent(ss);
		Global.initializeDataModel();
		Global.loadRulesets();
		// center the game window on the screen by default
		Global.getEngineData().add("display_center_x").set(ss.getToolkit().getScreenSize().width/2);
		Global.getEngineData().add("display_center_y").set(ss.getToolkit().getScreenSize().height/2);
Global.initializeRulesets();
Global.setCurrentRuleset(decker.model.Global.ruleset[0]);
		ss.setVisible(false);
Frame f = new Frame();
Global.setDisplayedComponent(f);
f.setLayout(new BorderLayout());
f.add(decker.model.Global.getViewWrapper());
new decker.view.FPSThread(decker.model.Global.getViewWrapper());
f.setBounds(Global.getEngineData().get("display_center_x").integer()-100/2, Global.getEngineData().get("display_center_y").integer()-70/2, 100, 70);
f.setVisible(true);
f.addWindowListener(	new WindowAdapter() { public void windowClosing (WindowEvent event) { System.exit(0); }} );
f.addFocusListener( new FocusAdapter() { public void focusGained(FocusEvent e) { Global.getViewWrapper().requestFocus(); }} );
Global.getViewWrapper().setView(new DefaultView());
AbstractView.reloadArtwork(true);
Global.getDisplayedScreen().set(Global.getCurrentRuleset().data.get("initial_screen"));
Global.getViewWrapper().repaint();
Global.getViewWrapper().requestFocus();
	}


	public Decker () {}
}