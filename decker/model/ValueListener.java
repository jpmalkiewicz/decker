package decker.model;


public interface ValueListener
{
	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value);
}