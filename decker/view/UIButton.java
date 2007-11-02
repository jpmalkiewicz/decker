package decker.view;
import decker.model.*;
import java.awt.*;


final class UIButton extends DisplayedComponent
{
	public final static String[] BUTTON_STATE_CONSTANT = {"IDLE", "PRESSED", "DISABLED", "HOVER"};


	private int state; // 0 = IDLE, 1 = PRESSED, 2 = DISABLED, 3 = HOVER
	private final DisplayedComponent[] face = new DisplayedComponent[4]; // one for each state
	private boolean fixed_width, fixed_height;
//	private UIBorder border;


	UIButton (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent, current_clip_source);
		Value v;
		fixed_width = w != 0 || (v=_component.get("width")) == null || !v.equalsConstant("UNDEFINED");
		fixed_height = h != 0 || (v=_component.get("height")) == null || !v.equalsConstant("UNDEFINED");
	}


	void draw (final Graphics g) {
		// draw the border
// *******
System.out.println("draw button ("+BUTTON_STATE_CONSTANT+")");
		// draw the button face
		if (face[state] != null)
			face[state].draw(g);
		// draw the child components of this view component
		final int cc = child_count;
		if (cc > 0) {
			final DisplayedComponent[] c = child;
			for (int i = 0; i < cc; i++)
				c[i].draw(g);
		}
	}
}