package decker.view;
import decker.model.*;
import java.awt.*;




class UIGenericComponent extends DisplayedComponent
{
	UIGenericComponent (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent, current_clip_source);
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		super.update(customSettings, current_clip_source);
		updateChildren(current_clip_source);
	}
}