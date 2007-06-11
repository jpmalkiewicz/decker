package decker.model;
import java.util.Locale;
import decker.util.StringTreeMap;


final class Script
{
	private String filename;
	private final StringTreeMap localizations = new StringTreeMap(true); // <- the (true) parameter means the language names are treated case-insensitive when fetching localizations



	Script (final String _filename)  { filename = _filename; }


	void execute (final Locale[] accepted_localizations)  {
		for(int i = 0; i < accepted_localizations.length; i++)  {
			final Object ls = getLocalization(accepted_localizations[i].getDisplayLanguage(accepted_localizations[i]));
			if (ls != null) {
				// we need a LOCAL object on the stack for the script execution
System.out.print("   running script from file : "+filename);
				final Structure local = new Structure("LOCAL", true);
				ScriptNode.addStackItem(local);
				((Block)ls).execute();
				ScriptNode.removeStackItem(local, (Block)ls);
System.out.println("   DONE");
				return;
			}
		}
System.err.println("The mission "+filename+" doesn't support any of the localizations you have chosen");
	}


	void addLocalization (final String language, final Object script)  { localizations.put(language, script); }
	Block getLocalization (final String language)  { return (Block) localizations.get(language); }
	int localizationCount ()  { return localizations.size(); }
}