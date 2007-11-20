package decker.model;
import decker.util.*;
import decker.view.*;
import java.awt.*;
import java.io.*;
import java.util.Locale;
import java.util.Random;
import java.util.TreeSet;


/** contains three sections :
*      data methods and variables
*      view methods and variables
*      private methods
*/
public final class Global
{
	final static Random random = new Random();
	final static String COMMANDS = " if for while with global print copy structure break constant "; // the string must start and end with spaces
	final static String BLOCK_INDENT = "   ";
	final static int PARSER_EXPRESSION_STACK_SIZE = 100;

	// id codes for hard coded functions. used by FunctionCall.executeFunctionCall() and Global.initializedataModel()
	final static int F_SIZE = 0, F_FILELIST = 1, F_SUBSTRING = 2, F_PIXELWIDTH = 3, F_PIXELHEIGHT = 4, F_EXIT_PROGRAM = 5, F_REPAINT = 6, F_INDEXOF = 7, F_IMAGE_EXISTS = 8, F_TO_LOWER_CASE = 9, F_TO_UPPER_CASE = 10, F_DATE_TEXT = 11, F_DEBUG = 12, F_INSERT = 13, F_RANDOM = 14, F_VALUE_TYPE = 15, F_DATE_DAY_OF_MONTH = 16, F_DATE_DAYS_IN_MONTH = 17, F_DELETE = 18, F_GET_STRUCTURE_STACK = 19, F_IS_EXPANDABLE = 20, F_HAS_VARIABLE = 21;
	final static String[] FUNCTION_NAME = { "size", "filelist", "substring", "pixelwidth", "pixelheight", "exit_program", "repaint", "indexof", "image_exists", "to_lower_case", "to_upper_case", "date_text", "debug", "insert", "random", "value_type", "date_day_of_month", "date_days_in_month", "delete", "getStructureStack", "isExpandable", "hasVariable" };

	public static Locale[] accepted_locales = { Locale.getDefault(), new Locale("en") };
	public static Ruleset[] ruleset = new Ruleset[0];
	private final static Ruleset engine = new Ruleset("");
	private static Ruleset current_ruleset = new Ruleset("(dummy)");



	static {
		Value.setGlobalValues(current_ruleset.data.get("GLOBAL_VALUES").structure());
	}



//	final static void addStructureType (final StructureDefinition sd)  { current_ruleset.addStructureType(sd); }
public static Ruleset getCurrentRuleset ()  { return current_ruleset; }
	public static Structure getEngineData ()  { return ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT]; }



	/** sets things up for the game to launch and load the rulesets */
	public final static void initializeDataModel ()  {
		// set up the data stack
		engine.data.add("date_day_of_month").set(new Function(F_DATE_DAY_OF_MONTH, new String[]{ "year", "month", "day" }));
		engine.data.add("date_days_in_month").set(new Function(F_DATE_DAYS_IN_MONTH, new String[]{ "year", "month", "day" }));
		engine.data.add("date_text").set(new Function(F_DATE_TEXT, new String[]{ "year", "month", "day" }));
		engine.data.add("exit_program").set(new Function(F_EXIT_PROGRAM, new String[0]));
		engine.data.add("debug").set(new Function(F_DEBUG, new String[]{ "print_this", "to_console" }));
		engine.data.add("delete").set(new Function(F_DELETE, new String[]{ "array", "index" }));
		engine.data.add("filelist").set(new Function(F_FILELIST, new String[]{ "directory" }));
		engine.data.add("getStructureStack").set(new Function(F_GET_STRUCTURE_STACK, new String[0]));
		engine.data.add("hasVariable").set(new Function(F_HAS_VARIABLE, new String[]{ "structure", "variable" }));
		engine.data.add("image_exists").set(new Function(F_IMAGE_EXISTS, new String[]{ "name" }));
		engine.data.add("indexof").set(new Function(F_INDEXOF, new String[]{ "what", "where", "direction", "start_at" }));
		engine.data.add("insert").set(new Function(F_INSERT, new String[]{ "array", "index" }));
		engine.data.add("isExpandable").set(new Function(F_IS_EXPANDABLE, new String[]{ "component" }));
		engine.data.add("pixelheight").set(new Function(F_PIXELHEIGHT, new String[]{ "component" }));
		engine.data.add("pixelwidth").set(new Function(F_PIXELWIDTH, new String[]{ "component" }));
		engine.data.add("random").set(new Function(F_RANDOM, new String[]{ "range_start", "range_end" }));
		engine.data.add("repaint").set(new Function(F_REPAINT, new String[0]));
		engine.data.add("size").set(new Function(F_SIZE, new String[]{ "thing" }));
		engine.data.add("substring").set(new Function(F_SUBSTRING, new String[]{ "string" , "from", "to" }));
		engine.data.add("to_lower_case").set(new Function(F_TO_LOWER_CASE, new String[]{ "text" }));
		engine.data.add("to_upper_case").set(new Function(F_TO_UPPER_CASE, new String[]{ "text" }));
		engine.data.add("value_type").set(new Function(F_VALUE_TYPE, new String[]{ "value" }));
		engine.data.add("displayed_screen").set(new Structure("VIEW")); // initialized with a dummy screen to avoid errors
		ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT] = engine.data;
		ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] = current_ruleset.data;
		ScriptNode.stack[ScriptNode.GLOBAL_STACK_SLOT] = new Structure("GLOBAL");
		ScriptNode.stack_size = ScriptNode.DEFAULT_GLOBAL_STACK_SIZE;
		ScriptNode.global_stack_size = ScriptNode.DEFAULT_GLOBAL_STACK_SIZE;
	}


	public final static void initializeRulesets ()  {
System.out.println(ruleset.length+" rulesets");
		final Ruleset r = current_ruleset;
		// first run the global scripts
System.out.println("running global scripts");
		setCurrentRuleset(engine);
		current_ruleset.initialize(accepted_locales);
		// then run the scripts of each ruleset
		for (int i = 0; i < ruleset.length; i++)  {
System.out.println("initializing ruleset "+ruleset[i].data.get("RULESET_NAME").toString());
			setCurrentRuleset(ruleset[i]);
			current_ruleset.initialize(accepted_locales);
		}
		setCurrentRuleset(r);
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

			// add the global scripts which sit directly in the rulesets folder to the engine ruleset
			System.out.println("loading engine scripts");
			for (int i = 0; i < dir_list.length; i++)
				if (dir_list[i].getName().toLowerCase().endsWith(".txt"))
					engine.addScript(ScriptParser.parse(dir_list[i]));
		}
	}


	@SuppressWarnings("unchecked")
	public static void setCurrentRuleset (final Ruleset r)  {
		final Structure old_globals = (current_ruleset==null) ? null : ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT].get("GLOBAL_VALUES").structure();
final Ruleset old_ruleset = current_ruleset;
		current_ruleset = r;
		ScriptNode.stack[ScriptNode.RULESET_STACK_SLOT] = r.data;
		final Structure new_globals = r.data.get("GLOBAL_VALUES").structure();
// set the current screen or call ruleset.setup() or something
		// replace all the global values in ENGINE with global values from the current ruleset
		if (old_globals != null && old_globals != new_globals) {
//System.out.println("replacing globals in ENGINE      ("+old_ruleset.getName()+" -> "+r.getName()+")  "+(old_globals==Value.getGlobalValues())+"   "+Value.getGlobalValues());
			// create the set of structures and array wrappers which have already been remapped
			final TreeSet remapped_containers = new TreeSet();
			// add the variables which should be omitted during the remapping to the set
			final Structure e = ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT];
			remapped_containers.add(e.get("GLOBAL_VALUES").structure());
			// set all the ruleset specific values to their default values
			Value v;
			if ((v=e.get("displayed_screen")) != null)
				v.setConstant("UNDEFINED");
			if ((v=e.get("previous_displayed_screen"))!=null)
				v.set(new ArrayWrapper(new Value[0]));
			if ((v=e.get("DEFAULT_BORDER_THICKNESS"))!=null)
				v.set(2);
			if ((v=e.get("screen_overlays"))!=null)
				v.setConstant("UNDEFINED");
			// now remap the things that will need remapping. things like displayed_screen are omitted here, because they need to be set manually by the new ruleset
			setCurrentRuleset_remapGlobals(ScriptNode.stack[ScriptNode.ENGINE_STACK_SLOT], remapped_containers, old_globals, new_globals);
		}
		// finally tell Value where the global values are
		Value.setGlobalValues(new_globals);
	}


	@SuppressWarnings("unchecked")
	private static void setCurrentRuleset_remapGlobals (final Structure t, final TreeSet remapped_containers, final Structure old_globals, final Structure new_globals)  {
		remapped_containers.add(t);
		final StringTreeMap.Iterator i = t.getStringTreeMapIterator();
		StringTreeMap.TreeNode n;
		while ((n=i.nextNode()) != null) {
			final Value v = (Value) n.getValue();
			if (v.isGlobal()) {
				// find the value in the old globals
				final String global_value_name = old_globals.getKey(v);
				if (global_value_name == null) {
					throw new RuntimeException("found a global value that does not exist in the GLOBAL_VALUES of the old ruleset : "+n.getKey()+"("+n.getValue()+")");
				}
				// fetch or make the value of the same name in new_globals
				Value w = new_globals.get(global_value_name);
				if (w == null) {
					w = new Value(new_globals);
					new_globals.putDirectlyIntoStringTreeMap(global_value_name, w);
				}
				// remap the value
				t.putDirectlyIntoStringTreeMap(n.getKey(), w);
			}
			else {
				final int type = v.type();
				if (type == Value.STRUCTURE) {
					if (!remapped_containers.contains(v.structure())) {
						setCurrentRuleset_remapGlobals(v.structure(), remapped_containers, old_globals, new_globals);
					}
				}
				else if (type == Value.ARRAY) {
					if (!remapped_containers.contains(v.arrayWrapper())) {
						setCurrentRuleset_remapGlobals(v.arrayWrapper(), remapped_containers, old_globals, new_globals);
					}
				}
			}
		}
	}


	@SuppressWarnings("unchecked")
	private static void setCurrentRuleset_remapGlobals (final ArrayWrapper aw, final TreeSet remapped_containers, final Structure old_globals, final Structure new_globals)  {
		remapped_containers.add(aw);
		final Value[] a = aw.array;
		for (int i = aw.array.length; --i >= 0; ) {
			final Value v = a[i];
			if (v.isGlobal()) {
				// find the value in the old globals
				final String global_value_name = old_globals.getKey(v);
				if (global_value_name == null) {
					throw new RuntimeException("found a global value that does not exist in the GLOBAL_VALUES of the old ruleset : array["+i+"] ("+v+")");
				}
				// fetch or make the value of the same name in new_globals
				Value w = new_globals.get(global_value_name);
				if (w == null) {
					w = new Value(new_globals);
					new_globals.putDirectlyIntoStringTreeMap(global_value_name, w);
				}
				// remap the value
				a[i] = w;
			}
			else {
				final int type = v.type();
				if (type == Value.STRUCTURE) {
					if (!remapped_containers.contains(v.structure())) {
						setCurrentRuleset_remapGlobals(v.structure(), remapped_containers, old_globals, new_globals);
					}
				}
				else if (type == Value.ARRAY) {
					if (!remapped_containers.contains(v.arrayWrapper())) {
						setCurrentRuleset_remapGlobals(v.arrayWrapper(), remapped_containers, old_globals, new_globals);
					}
				}
			}
		}
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