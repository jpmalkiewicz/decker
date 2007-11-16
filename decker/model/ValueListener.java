package decker.model;


public interface ValueListener
{
	/** if a value gets added or removed, old_value or new_value will be null */
	public void eventValueChanged (final String variable_name, final Structure structure,  final Value old_value, final Value new_value);
	public void eventValueChanged (final int index,            final ArrayWrapper wrapper, final Value old_value, final Value new_value);
}