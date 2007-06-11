package decker.model;
import decker.util.*;
import java.util.Locale;



public final class Ruleset
{
	public final Structure data = new Structure("RULESET", true);
	final StringTreeMap structure_types = new StringTreeMap();
	private TriggerCommand[] trigger = new TriggerCommand[100];
	private int trigger_count;
	private Script[] script = new Script[0]; // the list of scripts in this ruleset


	Ruleset (final String ruleset_name)  {
		data.add("RULESET_NAME").set(ruleset_name);
		// add the only standard constant, UNDEFINED
		final Structure constants = new Structure("SET");
		constants.add("UNDEFINED");
		data.add("CONSTANTS").set(constants);
		// add the set of structure types
		final Structure structure_types = new Structure("SET");
		data.add("STRUCTURE_TYPES").set(structure_types);
		// add the standard structure type ARRAY
		final Structure array = new Structure("ARRAY");
		array.addDirectly("size").set(0);
		structure_types.addDirectly("ARRAY").set(array);
	}


	void addScript (final Script new_script)  {
		script = (Script[]) ArrayModifier.addElement(script, new Script[script.length+1], new_script);
	}


	void addStructureType (final StructureDefinition sd)  {
		structure_types.put(sd.getStructureType(), sd);
	}


	void addTrigger (final TriggerCommand t)  {
		if (trigger.length == trigger_count) {
			final TriggerCommand[] k = trigger;
			trigger = new TriggerCommand[trigger_count*2];
			System.arraycopy(k, 0, trigger, 0, trigger_count);
		}
		trigger[trigger_count++] = t;
	}


	void initialize (final Locale[] accepted_localizations)  {
		for (int i = 0; i < script.length; i++)
			script[i].execute(accepted_localizations);
	}


	public String getName()  { return data.get("RULESET_NAME").toString(); }


	void testTriggers ()  {
		for (int i = trigger_count; --i >= 0; ) {
			if (trigger[i].testTrigger()) {
				// the trigger wants to be removed from the list, so let#s do that
				System.arraycopy(trigger, i+1, trigger, i, trigger_count-i-1);
				trigger_count--;
			}
		}
	}
}