package decker.model;
import decker.util.*;
import decker.view.*;
import java.awt.*;
import java.io.*;
import java.util.Locale;
import java.util.Random;


/** contains three sections :
*      data methods and variables
*      view methods and variables
*      private methods
*/
public final class Global
{
	final static Random random = new Random();
	final static String COMMANDS = " if while trigger global_trigger print copy display structure constant "; // the string must start and end with spaces
	final static String BLOCK_INDENT = "   ";
	final static int PARSER_EXPRESSION_STACK_SIZE = 100;

	// id codes for hard coded functions. used by FunctionCall.executeFunctionCall() and Global.initializedataModel()
	final static int F_SIZE = 0, F_FILELIST = 1, F_SUBSTRING = 2, F_PIXELWIDTH = 3, F_PIXELHEIGHT = 4, F_EXIT_PROGRAM = 5, F_REPAINT = 6, F_INDEXOF = 7, F_IMAGE_EXISTS = 8, F_TO_LOWER_CASE = 9, F_TO_UPPER_CASE = 10, F_DATE_TEXT = 11, F_DEBUG = 12, F_INSERT = 13, F_RANDOM = 14, F_VALUE_TYPE = 15, F_DATE_DAY_OF_MONTH = 16, F_DATE_DAYS_IN_MONTH = 17, F_DELETE = 18, F_GET_STRUCTURE_STACK = 19;
	final static String[] FUNCTION_NAME = { "size", "filelist", "substring", "pixelwidth", "pixelheight", "exit_program", "repaint", "indexof", "image_exists", "to_lower_case", "to_upper_case", "date_text", "debug", "insert", "random", "value_type", "date_day_of_month", "date_days_in_month", "delete" };

	public static Locale[] accepted_locales = { Locale.getDefault(), new Locale("en") };
	public static Ruleset[] ruleset = new Ruleset[0];
	private final static Ruleset engine = new Ruleset("");
	private static Ruleset current_ruleset = new Ruleset("(dummy)");

	static boolean test_triggers = true;
	private static boolean test_triggers_again = false;



	final static void addStructureType (final StructureDefinition sd)  { current_ruleset.addStructureType(sd); }
	final static void addTrigger (final TriggerCommand tc)  { if (tc.isGlobal()) engine.addTrigger(tc); else current_ruleset.addTrigger(tc); }
public static Ruleset getCurrentRuleset ()  { return current_ruleset; }
	public static Structure getEngineData ()  { return ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT]; }



	/** sets things up for the game to launch and load the rulesets */
	public final static void initializeDataModel ()  {
		// set up the data stack
		engine.data.addDirectly("date_day_of_month").set(new Function(F_DATE_DAY_OF_MONTH, new String[]{ "year", "month", "day" }));
		engine.data.addDirectly("date_days_in_month").set(new Function(F_DATE_DAYS_IN_MONTH, new String[]{ "year", "month", "day" }));
		engine.data.addDirectly("date_text").set(new Function(F_DATE_TEXT, new String[]{ "year", "month", "day" }));
		engine.data.addDirectly("debug").set(new Function(F_DEBUG, new String[]{ "print_this", "to_console" }));
		engine.data.addDirectly("delete").set(new Function(F_DELETE, new String[]{ "array", "index" }));
		engine.data.addDirectly("filelist").set(new Function(F_FILELIST, new String[]{ "directory" }));
		engine.data.addDirectly("getStructureStack").set(new Function(F_GET_STRUCTURE_STACK, new String[0]));
		engine.data.addDirectly("image_exists").set(new Function(F_IMAGE_EXISTS, new String[]{ "name" }));
		engine.data.addDirectly("indexof").set(new Function(F_INDEXOF, new String[]{ "what", "where", "direction", "start_at" }));
		engine.data.addDirectly("insert").set(new Function(F_INSERT, new String[]{ "array", "index" }));
		engine.data.addDirectly("pixelheight").set(new Function(F_PIXELHEIGHT, new String[]{ "object" }));
		engine.data.addDirectly("pixelwidth").set(new Function(F_PIXELWIDTH, new String[]{ "object" }));
		engine.data.addDirectly("random").set(new Function(F_RANDOM, new String[]{ "range_start", "range_end" }));
		engine.data.addDirectly("size").set(new Function(F_SIZE, new String[]{ "thing" }));
		engine.data.addDirectly("substring").set(new Function(F_SUBSTRING, new String[]{ "string" , "from", "to" }));
		engine.data.addDirectly("exit_program").set(new Function(F_EXIT_PROGRAM, new String[0]));
		engine.data.addDirectly("repaint").set(new Function(F_REPAINT, new String[0]));
		engine.data.addDirectly("to_lower_case").set(new Function(F_TO_LOWER_CASE, new String[]{ "text" }));
		engine.data.addDirectly("to_upper_case").set(new Function(F_TO_UPPER_CASE, new String[]{ "text" }));
		engine.data.addDirectly("value_type").set(new Function(F_VALUE_TYPE, new String[]{ "value" }));
		engine.data.addDirectly("displayed_screen").set(new Structure("VIEW")); // initialized with a dummy screen to avoid errors
		ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT] = engine.data;
		ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] = current_ruleset.data;
		ScriptNode.stack[ScriptNode.GLOBAL_STACK_SLOT] = new Structure("GLOBAL");
		ScriptNode.stack_size = ScriptNode.DEFAULT_GLOBAL_STACK_SIZE;
		ScriptNode.global_stack_size = ScriptNode.DEFAULT_GLOBAL_STACK_SIZE;
	}


	public final static void initializeRulesets ()  {
System.out.println(ruleset.length+" rulesets");
		test_triggers = false; // switch trigger testing off during the initialization
		final Ruleset r = current_ruleset;
		// first run the global scripts
System.out.println("running global scripts");
		current_ruleset = engine;
		ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] = current_ruleset.data;
		current_ruleset.initialize(accepted_locales);
		// then run the scripts of each ruleset
		for (int i = 0; i < ruleset.length; i++)  {
System.out.println("initializing ruleset "+ruleset[i].data.get("RULESET_NAME").toString());
			current_ruleset = ruleset[i];
			ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] = current_ruleset.data;
			current_ruleset.initialize(accepted_locales);
		}
		current_ruleset = r;
		ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] = r.data;
		test_triggers = true;
// print all ENGINE level structure types
// engine.data.get("STRUCTURE_TYPES").structure().print(System.err, "", true);
	}


	/** checks whether a given string contains an integer value */
	public final static boolean isInteger (final Object s)  {
		if (s != null && s instanceof String) {
			try {
				Integer.parseInt((String)s);
				return true;
			} catch (NumberFormatException ex) {}
		}
		return false;
	}


	public static void loadRulesets ()  {
		// switch off trigger checking while the data is being loaded
		test_triggers = false;
		// load the scripts from the rulesets subfolder of the folder the jar sits in
		final File rulsets_dir = new File("rulesets");
		if (rulsets_dir.exists() && rulsets_dir.isDirectory()) {
			final File[] dir_list = rulsets_dir.listFiles();
			bubblesort(dir_list);
			for (int d = 0; d < dir_list.length; d++) {
				if (dir_list[d].isDirectory() && !dir_list[d].getName().toLowerCase().endsWith(".svn")) {
					System.out.println("loading Ruleset "+dir_list[d].getName());
					final Ruleset r = new Ruleset(dir_list[d].getName());
					final File[] script_list = dir_list[d].listFiles();
					bubblesort(script_list);
					// load the scripts from the ruleset
					boolean has_scripts = false;
					for (int i = 0; i < script_list.length; i++)
						if (script_list[i].getName().toLowerCase().endsWith(".txt")) {
							r.addScript(ScriptParser.parse(script_list[i]));
							has_scripts = true;
						}
					// if the ruleset has at least one script, add it to the list of available rulesets
					if (has_scripts)
						ruleset = (Ruleset[]) ArrayModifier.addElement(ruleset, new Ruleset[ruleset.length+1], r);
				}
			}

			// add the global scripts which sit directly in the rulesets folder to the dummy ruleset that holds the global triggers
			System.out.println("loading global scripts");
			for (int i = 0; i < dir_list.length; i++)
				if (dir_list[i].getName().toLowerCase().endsWith(".txt"))
					engine.addScript(ScriptParser.parse(dir_list[i]));
		}
		// switch trigger checking back on
		test_triggers = true;
	}


public static void setCurrentRuleset (final Ruleset r)  {
current_ruleset = r;
ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] = r.data;
testTriggers();
}


	static void testTriggers ()  {
		if (!test_triggers) {
			test_triggers_again = true;
			return;
		}
		test_triggers = false;
		// remove all but global structures from the stack
		final Structure[] old_stack = ScriptNode.removeLocalStackItems();
		do {
			test_triggers = false;
			do {
				test_triggers_again = false;
				// test the global triggers first
				final Ruleset crs = current_ruleset;
				engine.testTriggers();
				// if the current ruleset hasn't changed, tell it that one of its values has changed
				if (current_ruleset == crs)
					crs.testTriggers();
			} while (test_triggers_again);
			test_triggers = true;
		} while (test_triggers_again); // in case there's some weird multi-thread problem
		// restore the stack
		ScriptNode.restoreLocalStack(old_stack);
	}


// view methods ***************************************************************************************************************************************


	private final static ViewWrapper view_wrapper = new ViewWrapper();
	private static Component displayed_component;



	final static void displayTickerMessage (final String message) { view_wrapper.getView().displayTickerMessage(message); }


	public static Component getDisplayedComponent ()  { return displayed_component; }


	public static Value getDisplayedScreen ()  { final Structure e = ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT]; return (e==null) ? null : e.get("displayed_screen"); }


	public final static ViewWrapper getViewWrapper ()  { return view_wrapper; }


	public static void setDisplayedComponent (final Component c)  { displayed_component = c; }


// private methods ************************************************************************************************************************************


	private static void bubblesort (final File[] list)  {
		boolean list_modified = true;
		for(int i = 0; i+1 < list.length && list_modified; i++) {
			list_modified = false;
			for(int j = list.length-2; j >= i; j--) {
				if(list[j].getName().toLowerCase().compareTo(list[j+1].getName().toLowerCase()) > 0) {
					File x = list[j];
					list[j] = list[j+1];
					list[j+1] = x;
					list_modified = true;
				}
			}
		}
	}
}