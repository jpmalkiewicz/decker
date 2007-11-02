package decker.view;
import decker.model.*;
import java.awt.*;

/*
			initializer = FUNCTION
				component = ARRAY
					createBorder(0,0)
					UNDEFINED    // placeholder for the button face
				component[0].background_color = @BACKGROUND_COLOR
			on_draw = FUNCTION
				LOCAL.c
				if state == IDLE
					c = idle
				if state == PRESSED
					c = BUTTON.pressed  // using "BUTTON." to make sure we're not using the "pressed" variable from some other structure
					if c == UNDEFINED
						c = deduceImageName(idle, "1")    // deduces the name of the image for the pressed state from the name of the idle state image
				if state == DISABLED
					LOCAL.c = BUTTON.disabled
					if c == UNDEFINED
						c = deduceImageName(idle, "2")
				if state == HOVER
					LOCAL.c = BUTTON.hover
					if c == UNDEFINED
						c = deduceImageName(idle, "3")
				// if it's the name of an image, put a wrapper around it so we can position it within the button area
				if value_type(c) != "STRING"
					component[1] = c
				else if component[1].structure_type == COMPONENT
					component[1].component = c
				else
					component[1] = COMPONENT
						component = c
				component[0].inverted = state == PRESSED
				LOCAL.padding = BORDER_BUTTON.text_padding != UNDEFINED && component[1].structure_type == "TEXT" ? text_padding : 0
				// if the button has an explicitly defined width, set the border width to the same value and center the button image / text horizontally
				if BORDER_BUTTON.width != UNDEFINED
					component[0].width = width
					component[1].x = CENTER
				else
					component[0].width = pixelwidth(c) + 4 + 2*padding
					component[1].x = 2 + padding
				// if the button has an explicitly defined height, set the border height to the same value and center the button image / text vertically
				if BORDER_BUTTON.height != UNDEFINED
					component[0].height = height
					component[1].y = text_padding == UNDEFINED ? CENTER : 2 + text_padding
				else     // otherwise, wrap a border around the content
					component[0].height = pixelheight(c) + 4 + 2*padding
					component[1].y = 2 + padding/2
*/

final class UIButton extends DisplayedComponent
{
	public final static String[] BUTTON_STATE_CONSTANT = { "IDLE", "PRESSED", "DISABLED", "HOVER" };


	private int state; // 0 = IDLE, 1 = PRESSED, 2 = DISABLED, 3 = HOVER
	private final DisplayedComponent[] face = new DisplayedComponent[BUTTON_STATE_CONSTANT.length]; // one for each state
	private boolean fixed_width, fixed_height;
	private int[] sx, sy, sw, sh; // the button may change its size and position depending on its state. these are the values for each state
	private UIBorder border;
	private DisplayedComponent clip_source;


	UIButton (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent, current_clip_source);
		clip_source = current_clip_source;
		Value v;
		// determine the type. it may be a BORDER_BUTTON
		final String type = _component.get("structure_type").string();
		// add the border if it's a BORDER_BUTTON
		if (type.equals("BORDER_BUTTON"))
			border = new UIBorder(this, current_clip_source, true);
		// determine the width and height of the button for each state, if they are not fixed
		fixed_width = w != 0 ||( (v=_component.get("width")) == null && !v.equalsConstant("UNDEFINED") );
System.out.println("fixed_width="+fixed_width);
		if (!fixed_width) {
			sx = new int[BUTTON_STATE_CONSTANT.length];
			sw = new int[BUTTON_STATE_CONSTANT.length];
final int base_width = type.equals("BORDER_BUTTON") ? (ScriptNode.getValue("DEFAULT_BORDER_THICKNESS").integer()*2) : 0;
			if (!(v=_component.get("idle")).equalsConstant("UNDEFINED")) {
				sw[0] = AbstractView.width(v) + base_width;
				w = sw[0];
				sx[0] = DefaultView.x(_component, _parent.w);
				x = sx[0];
			}
		}
		fixed_height = h != 0 || (v=_component.get("height")) == null || !v.equalsConstant("UNDEFINED");
		// set the initial state of the button
		state = 3;
		updateButtonState();
		if (state == 3) {
			state = 0;
			updateButtonState();
		}
if (border != null)
System.out.println("x="+x+" w="+w+"   "+"bx="+border.x+" bw="+border.w+"    "+_component.get("state"));
	}


	void draw (final Graphics g) {
		// draw the border
		if (border != null)
{
System.out.print("("+border.w+" "+border.h+") ");
			border.draw(g);
}
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


	private void updateButtonState () {
		final int old_state = state;
		final Value v = component.get("state");
		if (v != null && v.type() == Value.CONSTANT) {
			final String s = v.toString();
			for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
				if (i == 0 || s.equals(BUTTON_STATE_CONSTANT[i])) {
					if (state != i) {
						state = i;
// update the clipping area if the button shape has changed
						// update the border size
						if (border != null) {
							border.x = x;
							border.y = y;
							border.w = w;
							border.h = h;
						}
					}
					break;
				}
			}
		}
if (state != old_state)
System.out.println("state change "+old_state+" -> "+state);
	}
}