package decker.view;
import decker.model.*;
import java.awt.*;
import java.awt.event.*;



/** UIButtons suppress all mouse events (except for mouse wheel events) and don't call any scripted functions while DISABLED */
final class UIButton extends DisplayedComponent
{
	public final static int IDLE_STATE_ID = 0, PRESSED_STATE_ID = 1, DISABLED_STATE_ID = 2, HOVER_STATE_ID = 3;
	public final static String[] BUTTON_STATE_CONSTANT = { "IDLE", "PRESSED", "DISABLED", "HOVER" };
	public final static String[] BUTTON_STATE_FACE_VARIABLE = new String[BUTTON_STATE_CONSTANT.length];
	private final static Value NO_FACE = new Value();

	private int state; // 0 = IDLE, 1 = PRESSED, 2 = DISABLED, 3 = HOVER
	private final DisplayedComponent[] face = new DisplayedComponent[BUTTON_STATE_CONSTANT.length]; // one for each state
	private UIBorder border;
	// this is the current face for each state
	private Value[] current_face = new Value[BUTTON_STATE_CONSTANT.length];
	// the button may change its size and position depending on its state. these are the values for each state
	private final int[] sx = new int[BUTTON_STATE_CONSTANT.length];
	private final int[] sw = new int[BUTTON_STATE_CONSTANT.length];
	private final int[] sy = new int[BUTTON_STATE_CONSTANT.length];
	private final int[] sh = new int[BUTTON_STATE_CONSTANT.length];
	// and this is the clipped area for each state
	private final int[] scx = new int[BUTTON_STATE_CONSTANT.length];
	private final int[] scw = new int[BUTTON_STATE_CONSTANT.length];
	private final int[] scy = new int[BUTTON_STATE_CONSTANT.length];
	private final int[] sch = new int[BUTTON_STATE_CONSTANT.length];




	static {
		for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
			BUTTON_STATE_FACE_VARIABLE[i] = BUTTON_STATE_CONSTANT[i].toLowerCase();
		}
	}




	UIButton (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		// determine the type. it may be a BORDER_BUTTON
		final String type = _component.get("structure_type").string();
		// add the border if it's a BORDER_BUTTON
		if (type.equals("BORDER_BUTTON"))
			border = new UIBorder(this, current_clip_source, true);
		// fill in the remaining data
		update(0, current_clip_source);
updateButtonState();
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		final int border_thickness = component.get("structure_type").equals("BORDER_BUTTON") ? ScriptNode.getValue("DEFAULT_BORDER_THICKNESS").integer() : 0;
		Value v;
		final int padding        = ((v=component.get("padding"))        != null && v.type() == Value.INTEGER) ? v.integer() : 0;
		final int padding_left   = ((v=component.get("padding_left"))   != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		final int padding_right  = ((v=component.get("padding_right"))  != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		final int padding_top    = ((v=component.get("padding_top"))    != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		final int padding_bottom = ((v=component.get("padding_bottom")) != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		// if the width or height is fixed, use that value for all button states
		if (width_already_determined || height_already_determined) {
			for (int i = sw.length; --i >= 0; ) {
				sw[i] = width_already_determined ? w : (face[i].w+2*border_thickness+padding_left+padding_right);
				sh[i] = height_already_determined ? h : (face[i].h+2*border_thickness+padding_top+padding_bottom);
			}
		}
		if (face[state] != null) {
			// use the width and height of the current state for the button
			if (( !width_already_determined && face[state].w <= 0 )||( !height_already_determined && face[state].h <= 0 )) {
				face[state].update(0, current_clip_source);
				sw[state] = face[state].w + 2*border_thickness + padding_left+padding_right;
				sh[state] = face[state].h + 2*border_thickness + padding_top+padding_bottom;
			}
			else if (sw[state] <= 0 || sh[state] <= 0) {
				sw[state] = face[state].w+2*border_thickness + padding_left+padding_right;
				sh[state] = face[state].h+2*border_thickness + padding_top+padding_bottom;
			}
			w = sw[state];
			h = sh[state];
		}
	}




	void draw (final Graphics g) {
		// draw the border
		if (border != null)
			border.draw(g);
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




	boolean eventUserInput (final int event_id, final AWTEvent e, final int mouse_x, final int mouse_y, final int mouse_dx, final int mouse_dy) {
		if (state == DISABLED_STATE_ID) // this should not be possible, but better to be safe than sorry
			return true;
		String s;
if (component.type() != Value.STRUCTURE ||( !(s=component.get("structure_type").string()).equals("BUTTON") && !s.equals("BORDER_BUTTON") )) {
System.out.println("this button is not a button anymore");
return true;
}
		final Value v = component.get("state");
		switch (event_id) {
			case ON_MOUSE_DOWN :
					v.setConstant("PRESSED");
				break;
			case ON_MOUSE_ENTERED :
					// if one of the mouse buttons is pressed, press the button
					final int m = ((MouseEvent)e).getModifiersEx();
					if (( (m&MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK )||( (m&MouseEvent.BUTTON2_DOWN_MASK) == MouseEvent.BUTTON2_DOWN_MASK )||( (m&MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK ))
						v.setConstant("PRESSED");
					else
						v.setConstant("HOVER");
				break;
			case ON_MOUSE_EXITED :
					v.setConstant("IDLE");
				break;
			case ON_MOUSE_UP :
					v.setConstant("HOVER");
				break;
		}
		updateButtonState();
		return true;
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		// determine the button state
		state = IDLE_STATE_ID;
		Value v = component.get("state");
		if (v != null && v.type() == Value.CONSTANT) {
			final String s = v.constant();
			for (int i = BUTTON_STATE_CONSTANT.length; --i >= 1; ) {
				if (s.equals(BUTTON_STATE_CONSTANT[i])) {
					state = i;
					break;
				}
			}
		}
		// update the button face for each state
		// fetch them from the displayed component
		final Value[] state_value = new Value[BUTTON_STATE_CONSTANT.length];
		int count = 0;
		for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
			state_value[i] = component.get(BUTTON_STATE_FACE_VARIABLE[i]);
			if (state_value[i] != null && !state_value[i].equalsConstant("UNDEFINED"))
				count++;
			else
				state_value[i] = NO_FACE; // use a dummy value to avoid NullPointerException
		}
		// if one or more button faces are undefined, and we have a button face for the idle state, fill the missing faces in
		if (count < BUTTON_STATE_CONSTANT.length && state_value[0] != NO_FACE) {
			// if the idle face is an image named "<name>0" or "<name>0<file extension", try to deduce the missing image names from it
			// state_value[0] cannot be UNDEFINED here
			if (state_value[0].type() != Value.STRUCTURE ||( state_value[0].type() == Value.STRUCTURE && state_value[0].get("structure_type").equals("IMAGE") )) {
				final String image_name = (state_value[0].type()==Value.STRUCTURE) ? state_value[0].get("image").toString() : state_value[0].toString();
				if (image_name.endsWith("0")) {
					final String prefix = image_name.substring(0, image_name.length()-1);
					for (int i = BUTTON_STATE_CONSTANT.length; --i >= 1; ) {
						if (state_value[i] == NO_FACE && AbstractView.getImage(prefix+i) != null) {
							state_value[i] = new Value().set(prefix+i);
						}
					}
				}
				else {
					final int suffix_offset = image_name.lastIndexOf('0');
					if (suffix_offset > -1 && suffix_offset == image_name.length()-5) {
						final String suffix = image_name.substring(suffix_offset+1).trim();
						if (suffix.length() == 4 && AbstractView.SUPPORTED_IMAGE_TYPES.indexOf(suffix.toLowerCase()) > -1) {
							final String prefix = image_name.substring(0, suffix_offset);
							for (int i = BUTTON_STATE_CONSTANT.length; --i >= 1; ) {
								if (state_value[i] == NO_FACE && AbstractView.getImage(prefix+i+suffix) != null) {
									state_value[i] = new Value().set(prefix+i+suffix);
								}
							}
						}
					}
				}
			}
			// replace all remaining missing faces with the idle face
			for (int i = BUTTON_STATE_CONSTANT.length; --i >= 1; ) {
				if (state_value[i] == NO_FACE) {
					state_value[i] = state_value[0];
				}
			}
		}
		// check whether the button faces have changed. if so, recreate them
		for (int i = 0; i < current_face.length; i++) {
			if (current_face[i] == null || !current_face[i].equals(state_value[i])) {
				// just use the idle face if this face is identical to it
				if (i > 0 &&( state_value[i] == state_value[0] || state_value[i].equals(state_value[0]) )) {
					face[i] = face[0];
				}
				// otherwise make a new face
				else {
					if (state_value[i].type() == Value.STRUCTURE)
						face[i] = DisplayedComponent.createDisplayedComponent(state_value[i], this, current_clip_source);
					else
						face[i] = new UIImage(state_value[i], this, current_clip_source);
				}
			}
		}
		// make the new face values the current ones
		System.arraycopy(state_value, 0, current_face, 0, state_value.length);

		// put markers in the button face width arrays so we'll know whether DisplayedComponent.update() has called determineSize()
		for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
			sw[i] = Integer.MIN_VALUE;
		}
		// let DisplayedComponent fill in the standard data
		super.update(customSettings|CUSTOM_SIZE, current_clip_source);

		// if determineSize() did not get called, the width and height of the button must be fixed values. use them for all button states
		for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
			if (sw[i] == Integer.MIN_VALUE) {
				sw[i] = w;
				sh[i] = h;
				face[i].update(0, current_clip_source);
			}
		}

		// give each button state a position and a clip rectangle
		for (int i = 0; i < BUTTON_STATE_CONSTANT.length; i++) {
			if (i == state) {
				sx[i] = x;
				sy[i] = y;
				scx[i] = cx;
				scy[i] = cy;
				scw[i] = cw;
				sch[i] = ch;
			}
			else {
				// determine w and x
				if (sw[i] == sw[state]) {
					sx[i] = x;
					scx[i] = cx;
					scw[i] = cw;
				}
				else if (i > 0 && sw[i] == sw[0]) {
					sx[i] = sx[0];
					scx[i] = scx[0];
					scw[i] = scw[0];
				}
				else {
					sx[i] = determineX(component, parent.x, parent.w, sw[i]);
					if (current_clip_source.cw <= 0) {
						// scx[i] = 0; // scx[i] does not have any effect in this case and will never be used
						scw[i] = 0;
					}
					else {
						if (current_clip_source.cx <= sx[i]) {
							scx[i] = sx[i];
							scw[i] = scx[i] + current_clip_source.cw - sx[i];
							if (scw[i] > sw[i]) {
								scw[i] = sw[i];
							}
						}
						// the current clip rectangle's x lies inside or right of our component
						else {
							scx[i] = current_clip_source.cx;
							scw[i] = current_clip_source.cw;
							if (sx[i]+sw[i] < scx[i]+scw[i]) {
								scw[i] = sx[i]+sw[i]-scx[i]; // will result in values <= 0 if the clip rectangle lies right of the component
							}
						}
					}
				}
				// determine y and h
				if (sh[i] == sh[state]) {
					sy[i] = y;
					scy[i] = cy;
					sch[i] = ch;
				}
				else if (i > 0 && sh[i] == sh[0]) {
					sy[i] = sy[0];
					scy[i] = scy[0];
					sch[i] = sch[0];
				}
				else {
					sy[i] = determineY(component, parent.y, parent.h, sh[i]);
					if (current_clip_source.ch <= 0) {
						// scy[i] = 0; // scy[i] does not have any effect in this case and will never be used
						sch[i] = 0;
					}
					else {
						if (current_clip_source.cy <= sy[i]) {
							scy[i] = sy[i];
							sch[i] = scy[i] + current_clip_source.ch - sy[i];
							if (sch[i] > sh[i]) {
								sch[i] = sh[i];
							}
						}
						// the current clip rectangle's y lies inside or below our component
						else {
							scy[i] = current_clip_source.cy;
							sch[i] = current_clip_source.ch;
							if (sy[i]+sh[i] < scy[i]+sch[i]) {
								sch[i] = sy[i]+sh[i]-scy[i]; // will result in values <= 0 if the clip rectangle lies below the component
							}
						}
					}
				}
			}
		}

		// center the button faces if they have no explicit position
		final int border_thickness = component.get("structure_type").equals("BORDER_BUTTON") ? ScriptNode.getValue("DEFAULT_BORDER_THICKNESS").integer() : 0;
		final int padding        = ((v=component.get("padding"))        != null && v.type() == Value.INTEGER) ? v.integer() : 0;
		final int padding_left   = ((v=component.get("padding_left"))   != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		final int padding_right  = ((v=component.get("padding_right"))  != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		final int padding_top    = ((v=component.get("padding_top"))    != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		final int padding_bottom = ((v=component.get("padding_bottom")) != null && v.type() == Value.INTEGER) ? v.integer() : padding;
		if (face[0] != null) { // if face[0] exists all the others do, too
			for (int i = 0; i < BUTTON_STATE_CONSTANT.length; i++) {
				int fx = 0;
				if (!hasExplicitX(face[i].component)) {
					face[i].x = x + padding_left + ((w-padding_left-padding_right)-face[i].w)/2;
				}
				else {
					face[i].x = determineX(face[i].component, sx[i]+border_thickness+padding_left, sw[i]-2*border_thickness-padding_left-padding_right, face[i].w);
				}
				if (!hasExplicitY(face[i].component)) {
					face[i].y = y + padding_top + (h-padding_top-padding_bottom-face[i].h)/2;
				}
				else {
					face[i].y = determineY(face[i].component, sy[i]+border_thickness+padding_top, sh[i]-2*border_thickness-padding_top-padding_bottom, face[i].h);
				}
			}
		}

		// adjust the border size
		if (border != null) {
			border.x = x;
			border.y = y;
			border.w = w;
			border.h = h;
		}

/*
		// if the width and height are fixed, things are easy
		if (!variable_width) {
			for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
				sw[i] = w;
				sx[i] = x;
				scw[i] = cw;
				scx[i] = cx;
			}
		}
		if (!variable_height) {
			for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
				sh[i] = h;
				sy[i] = y;
				sch[i] = ch;
				scy[i] = cy;
			}
		}
		if (variable_width || variable_height) {
			for (int i = 0; i < BUTTON_STATE_CONSTANT.length; i++) {
				// just copy the values if this face is identical to the idle face
				if (i > 0 &&( state_value[i] == state_value[0] || state_value[i].equals(state_value[0]) )) {
					sw[i] = sw[0];
					sx[i] = sx[0];
					sh[i] = sh[0];
					sy[i] = sy[0];
					scw[i] = scw[0];
					scx[i] = scx[0];
					sch[i] = sch[0];
					scy[i] = scy[0];
				}
				// otherwise calculate the values for it
				else {
					if (variable_width) {
						sw[i] = AbstractView.width(state_value[i], _parent.w) + 2*border_thickness;
						sx[i] = DefaultView.x(_component, _parent.w, sw[i]) + _parent.x;
						if (current_clip_source.cw > 0) {
							if (current_clip_source.cx <= sx[i]) {
								scx[i] = sx[i];
								scw[i] = scx[i] + current_clip_source.cw - sx[i];
								if (scw[i] > sw[i]) {
									scw[i] = sw[i];
								}
							}
							else {
								scx[i] = current_clip_source.cx;
								scw[i] = current_clip_source.cw;
								if (sx[i]+sw[i] < scx[i]+scw[i]) {
									scw[i] = sx[i]+sw[i]-scx[i];
								}
							}
						}
					}
					if (variable_height) {
						sh[i] = AbstractView.height(state_value[i], _parent.h) + 2*border_thickness;
						sy[i] = DefaultView.y(_component, _parent.h, sh[i]) + _parent.y;
						if (current_clip_source.ch > 0) {
							if (current_clip_source.cy <= sy[i]) {
								scy[i] = sy[i];
								sch[i] = scy[i] + current_clip_source.ch - sy[i];
								if (sch[i] > sh[i]) {
									sch[i] = sh[i];
								}
							}
							else {
								scy[i] = current_clip_source.cy;
								sch[i] = current_clip_source.ch;
								if (sy[i]+sh[i] < scy[i]+sch[i]) {
									sch[i] = sy[i]+sh[i]-scy[i];
								}
							}
						}
					}
				}
			}
		}
		// add the button faces
		for (int i = 0; i < BUTTON_STATE_CONSTANT.length; i++) {
			if (state_value[i] != NO_FACE) {
				// just reuse it if this face is identical to the idle face
				if (i > 0 &&( state_value[i] == state_value[0] || state_value[i].equals(state_value[0]) )) {
					face[i] = face[0];
				}
				else {
					// create the DisplayedComponent that represents this face
					x = sx[i] + border_thickness;
					w = sw[i] - 2*border_thickness;
					y = sy[i] + border_thickness;
					h = sh[i] - 2*border_thickness;
					face[i] = DisplayedComponent.createDisplayedComponent(state_value[i], this, current_clip_source);
					// center the face if it doesn't have a defined position
					if (state_value[i].type() != Value.STRUCTURE ||(( (k=state_value[i].get("x")) == null || k.equalsConstant("UNDEFINED") )&&( (k=state_value[i].get("h_align")) == null || k.equalsConstant("UNDEFINED") ))) {
						face[0].x = sx[0] + (sw[0]-face[0].w)/2;
					}
					if (state_value[i].type() != Value.STRUCTURE ||(( (k=state_value[i].get("y")) == null || k.equalsConstant("UNDEFINED") )&&( (k=state_value[i].get("v_align")) == null || k.equalsConstant("UNDEFINED") ))) {
						face[0].y = sy[0] + (sh[0]-face[0].h)/2;
					}
				}
			}
		}
		// set the initial state of the button
		state = 3;
		updateButtonState();
		if (state == 3) {
			state = 0;
			updateButtonState();
		}
*/
	}




	private void updateButtonState () {
		final Value v = component.get("state");
		if (v != null && v.type() == Value.CONSTANT) {
			final String s = v.toString();
			for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
				if (i == 0 || s.equals(BUTTON_STATE_CONSTANT[i])) {
					// display the current state
					if (state != i) {
						state = i;
						x = sx[i];
						y = sy[i];
						w = sw[i];
						h = sh[i];
						cx = scx[i];
						cy = scy[i];
						cw = scw[i];
						ch = sch[i];
						// update the border size
						if (border != null) {
							border.x = x;
							border.y = y;
							border.w = w;
							border.h = h;
							border.inverted = state == PRESSED_STATE_ID;
						}
					}
					break;
				}
			}
		}
	}




	void updateEventListeners () {
		// (un)register the hardcoded event listener function
		if (state == DISABLED_STATE_ID) {
			for (int i = hasHardcodedEventFunction.length; --i >= 0; ) {
				if (eventFunctionRegistered[i] && hasHardcodedEventFunction[i]) {
					hasHardcodedEventFunction[i] = false;
					scriptedEventFunction[i] = null;
					removeEventListener(i);
				}
			}
		}
		else {
			hasHardcodedEventFunction[ON_MOUSE_DOWN] = true;
			hasHardcodedEventFunction[ON_MOUSE_ENTERED] = true;
			hasHardcodedEventFunction[ON_MOUSE_EXITED] = true;
			hasHardcodedEventFunction[ON_MOUSE_UP] = true;
			for (int i = hasHardcodedEventFunction.length; --i >= 0; ) {
				if (hasHardcodedEventFunction[i] && !eventFunctionRegistered[i]) {
					final Value e = component.get(EVENT_FUNCTION_NAME[i]);
					scriptedEventFunction[i] = (e==null||e.type()!=Value.FUNCTION) ? null : e.function();
					addEventListener(i);
				}
			}
		}
	}
}