package decker.view;
import decker.model.*;
import java.awt.*;



final class UIButton extends DisplayedComponent
{
	public final static String[] BUTTON_STATE_CONSTANT = { "IDLE", "PRESSED", "DISABLED", "HOVER" };
	public final static String[] BUTTON_STATE_FACE_VARIABLE = new String[BUTTON_STATE_CONSTANT.length];
	private final static Value NO_FACE = new Value();

	private int state; // 0 = IDLE, 1 = PRESSED, 2 = DISABLED, 3 = HOVER
	private final DisplayedComponent[] face = new DisplayedComponent[BUTTON_STATE_CONSTANT.length]; // one for each state
	private UIBorder border;
	private DisplayedComponent clip_source;
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
		super(_component, _parent, current_clip_source);
		clip_source = current_clip_source;
		Value v, k;
		// determine the type. it may be a BORDER_BUTTON
		final String type = _component.get("structure_type").string();
		// add the border if it's a BORDER_BUTTON
		if (type.equals("BORDER_BUTTON"))
			border = new UIBorder(this, current_clip_source, true);
		final int border_thickness = type.equals("BORDER_BUTTON") ? ScriptNode.getValue("DEFAULT_BORDER_THICKNESS").integer() : 0;

		// fetch the button face for each state
		final Value[] state_value = new Value[BUTTON_STATE_CONSTANT.length];
		int count = 0;
		for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
			state_value[i] = _component.get(BUTTON_STATE_FACE_VARIABLE[i]);
			if (state_value[i] != null && !state_value[i].equalsConstant("UNDEFINED"))
				count++;
			else
				state_value[i] = NO_FACE; // use a dummy value to avoid NullPointerException
		}
		// if one or more button faces are undefined, and we have a button face for the idle state, fill the missing faces in
		if (count < BUTTON_STATE_CONSTANT.length && state_value[0] != NO_FACE) {
			// if the idle face is an image name with the syntax "<name>0" or "<name>0<file extension", try to deduce the missing image names from it
			if (state_value[0].type() != Value.STRUCTURE) { // state_value[0] cannot be UNDEFINED here, otherwise we would have to check for that
				final String image_name = state_value[0].toString();
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

		// if the width or height is not fixed, determine the button position and size for each individual state. also determine the clipping area
		final boolean variable_width  = w == 0 &&( (v=_component.get("width"))  == null || !v.equals(0) );
		final boolean variable_height = h == 0 &&( (v=_component.get("height")) == null || !v.equals(0) );
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
						sx[i] = DefaultView.x(_component, _parent.w, sw[i]);
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
						sy[i] = DefaultView.y(_component, _parent.h, sh[i]);
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


	private void updateButtonState () {
		final int old_state = state;
		final Value v = component.get("state");
		if (v != null && v.type() == Value.CONSTANT) {
			final String s = v.toString();
			for (int i = BUTTON_STATE_CONSTANT.length; --i >= 0; ) {
				if (i == 0 || s.equals(BUTTON_STATE_CONSTANT[i])) {
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