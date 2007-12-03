package decker.view;
import decker.model.*;
import java.awt.*;
import java.awt.event.*;




class UIScrollbarButton extends UIButton
{
	UIScrollbarButton (final Value _component, final UIScrollbar _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent, current_clip_source);
	}




	boolean eventUserInput (final int event_id, final AWTEvent e, final int mouse_x, final int mouse_y, final int mouse_dx, final int mouse_dy) {
		super.eventUserInput(event_id, e, mouse_x, mouse_y, mouse_dx, mouse_dy);
		String s;
		if (getState() == DISABLED_STATE_ID || component.type() != Value.STRUCTURE ||( !(s=component.get("structure_type").string()).equals("BUTTON") && !s.equals("BORDER_BUTTON") ))
			return true;
		if (event_id == ON_MOUSE_DOWN) {
			((UIScrollbar)parent).buttonPressed(this);
		}
		return true;
	}
}