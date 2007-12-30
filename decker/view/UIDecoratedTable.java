package decker.view;
import decker.model.*;

import java.awt.*;

class UIDecoratedTable extends DisplayedComponent
{
//	private DisplayedComponent[] header;
//	private DisplayedComponent table;
//	private DisplayedComponent;

	
	UIDecoratedTable (Value _component, DisplayedComponent _parent, DisplayedComponent current_clip_source) {
		super(_component, _parent);
	}

	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		final Structure d = component.structure();
	}
}