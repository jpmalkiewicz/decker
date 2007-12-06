package decker.view;
import decker.model.*;
import java.awt.*;
import java.awt.event.*;




class UIScrollbarButton extends UIButton
{
	boolean listens_everywhere; // true for dragged sliders
	int drag_offset; // position of the mouse within the button while dragging it




	UIScrollbarButton (final Value _component, final UIScrollbar _parent, final DisplayedComponent current_clip_source, final boolean _listens_everywhere, final boolean _is_slider) {
		super(_component, _parent, current_clip_source);
		if (_listens_everywhere) {
			listens_everywhere = true;
			cx = Integer.MIN_VALUE / 2;
			cw = Integer.MAX_VALUE;
			cy = cx;
			ch = cw;
		}
		if (_is_slider && !hasHardcodedEventFunction[ON_MOUSE_DRAGGED]) {
			hasHardcodedEventFunction[ON_MOUSE_DRAGGED] = true;
			addEventListener(ON_MOUSE_DRAGGED);
		}
	}




	void determineClip (final DisplayedComponent current_clip_source) {
		if (!listens_everywhere)
			super.determineClip(current_clip_source);
		else {
			cx = Integer.MIN_VALUE / 2;
			cw = Integer.MAX_VALUE;
			cy = cx;
			ch = cw;
		}
	}




	boolean eventUserInput (final int event_id, final AWTEvent e, final int mouse_x, final int mouse_y, final int mouse_dx, final int mouse_dy) {
		String s;
		if (component.type() != Value.STRUCTURE ||( !(s=component.get("structure_type").string()).equals("BUTTON") && !s.equals("BORDER_BUTTON") ))
			return true;
		final int old_state = getState();
		super.eventUserInput(event_id, e, mouse_x, mouse_y, mouse_dx, mouse_dy);
		final int state = getState();
		if (old_state != state) {
if (state == PRESSED_STATE_ID && event_id == ON_MOUSE_DOWN)
				((UIScrollbar)parent).buttonPressed(this,mouse_x, mouse_y);
if (old_state == PRESSED_STATE_ID && event_id == ON_MOUSE_UP)
				((UIScrollbar)parent).buttonReleased(this);
		}
		if (listens_everywhere) {
			cx = Integer.MIN_VALUE / 2;
			cw = Integer.MAX_VALUE;
			cy = cx;
			ch = cw;
			if (event_id == ON_MOUSE_DRAGGED) {
				((UIScrollbar)parent).sliderDragged(mouse_x, mouse_y);
			}
		}
		return true;
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		super.update(customSettings, current_clip_source);
		if (listens_everywhere) {
			cx = Integer.MIN_VALUE / 2;
			cw = Integer.MAX_VALUE;
			cy = cx;
			ch = cw;
		}
	}
}