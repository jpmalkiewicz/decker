package decker.view;
import decker.model.*;
import java.awt.*;




class UIScrollbar extends DisplayedComponent
{
	private boolean vertical;
	private int slider_position, slider_max, slider_stepping;
	private UIScrollbarButton slider, minus_button, plus_button;
	private final Value slider_value = new Value(), minus_button_value = new Value(), plus_button_value = new Value();




	UIScrollbar (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		vertical = _component.get("vertical").equals(true);
		update(CUSTOM_SIZE, current_clip_source);
	}




	void buttonPressed (UIScrollbarButton button, final int mouse_x, final int mouse_y) {
		if (button == minus_button || button == plus_button) {
// smooth scrolling !
			final int old_slider_position = slider_position;
			int sp = slider_position + (button==minus_button?(-1):1) * slider_stepping;
			if (sp < 0)
				sp = 0;
			if (sp > slider_max)
				sp = slider_max;
			if (component.type() == Value.STRUCTURE && component.get("structure_type").equals("SCROLLBAR")) {
				if (sp != old_slider_position) {
					component.get("slider_position").set(sp);
					update(0, getCurrentClipSource());
				}
				// if the slider is already in the area of the min or max value, but is not at the end of the scale when the button is pressed, move it to the end
				if (vertical) {
					if (sp == 0 && button == minus_button && slider.y > y + minus_button.h) {
						slider.component.get("y").set(minus_button.h);
						slider.update(0, getCurrentClipSource());
					}
					if (sp == slider_max && button == plus_button && slider.y < y + h - slider.h - ((plus_button==null)?0:plus_button.h)) {
						slider.component.get("y").set(h - slider.h - ((plus_button==null)?0:plus_button.h));
						slider.update(0, getCurrentClipSource());
					}
				}
				else {
					if (sp == 0 && button == minus_button && slider.x > x + minus_button.w) {
						slider.component.get("x").set(minus_button.w);
						slider.update(0, getCurrentClipSource());
					}
					if (sp == slider_max && button == plus_button && slider.x < x + w - slider.w - ((plus_button==null)?0:plus_button.w)) {
						slider.component.get("x").set(w - slider.w - ((plus_button==null)?0:plus_button.w));
						slider.update(0, getCurrentClipSource());
					}
				}
			}
		}
		else if (button == slider) {
			button.listens_everywhere = true;
			button.drag_offset = vertical ? (mouse_y-button.y) : (mouse_x-button.x);
		}
	}




	void buttonReleased (UIScrollbarButton button) {
		if (button == minus_button || button == plus_button) {
// smooth scrolling !
		}
		else if (button == slider) {
			button.listens_everywhere = false;
			button.update(0, getCurrentClipSource());
		}
	}




	private Value createButton (final Value data) {
		if (data.type() == Value.STRUCTURE &&( data.get("structure_type").equals("BUTTON") || data.get("structure_type").equals("BORDER_BUTTON") ))
			return data;
		Structure t = new Structure("BUTTON");
		t.get("idle").set(data);
		t.get("x").setConstant("CENTER");
		t.get("y").setConstant("CENTER");
		return new Value().set(t);
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		if (component.type() == Value.STRUCTURE && component.get("structure_type").equals("SCROLLBAR")) {
			Value v;
			if (vertical) {
				if (!width_already_determined) {
					if ((v=component.get("width")).type() != Value.INTEGER || v.integer() != w) {
						if (v.type() == Value.INTEGER)
							w = v.integer();
						else {
							// the width of this scrollbar is the maximum width of its arrows and slider
							w = 0;
							if (slider != null)
								w = slider.w;
							if (minus_button != null && minus_button.w > w)
								w = minus_button.w;
							if (plus_button != null && plus_button.w > w)
								w = plus_button.w;
						}
					}
				}
			}
			else { // it's a horizontal scrollbar
				if (!height_already_determined) {
					if ((v=component.get("height")).type() != Value.INTEGER || v.integer() != h) {
						if (v.type() == Value.INTEGER)
							h = v.integer();
						else {
							// the width of this scrollbar is the maximum width of its arrows and slider
							h = 0;
							if (slider != null)
								h = slider.h;
							if (minus_button != null && minus_button.h > h)
								h = minus_button.h;
							if (plus_button != null && plus_button.h > h)
								h = plus_button.h;
						}
					}
				}
			}
		}
	}




	void draw (final Graphics g) {
		if (minus_button != null)
			minus_button.draw(g);
		if (plus_button != null)
			plus_button.draw(g);
		if (slider != null)
			slider.draw(g);
	}




	void eventPositionChanged (final DisplayedComponent current_clip_source, final int dx, final int dy) {
		super.eventPositionChanged(current_clip_source, dx, dy);
		if (slider != null)
			slider.eventPositionChanged(current_clip_source, dx, dy);
		if (minus_button != null)
			minus_button.eventPositionChanged(current_clip_source, dx, dy);
		if (plus_button != null)
			plus_button.eventPositionChanged(current_clip_source, dx, dy);
	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	public void eventValueChanged (final int index, final ArrayWrapper wrapper, final Value old_value, final Value new_value) {
		update(0, getCurrentClipSource());
	}




	int getSliderPosition () {
		return slider_position;
	}




	void sliderDragged (final int mouse_x, final int mouse_y) {
		if (vertical) {
			final int m = (minus_button==null) ? 0 : minus_button.h;
			final int slider_range = h - m - slider.h - ((plus_button==null) ? 0 : plus_button.h) + 1;
			int sp = mouse_y - y - m - slider.drag_offset;
			if (sp >= slider_range)
				sp = slider_range-1;
			if (sp < 0)
				sp = 0;
			if (sp != slider.y) {
				if (slider_range > 0) {
					// there will be rounding errors, so we may have to adjust the logical value up or down
					int logical_value = ((slider_max+1) * sp ) / slider_range;
					while ((slider_range*logical_value)/(slider_max+1) > sp)
						logical_value--;
					while ((slider_range*(logical_value+1))/(slider_max+1) <= sp)
						logical_value++;
					if (logical_value != slider_position) {
						slider_position = logical_value;
						component.get("slider_position").set(logical_value);
						Value v;
						if ((v=component.get("effect")).type() == Value.FUNCTION)
							FunctionCall.executeFunctionCall(v.function(), null, component.structure());
					}
				}
				slider.component.get("y").set(sp+m);
				slider.update(0, getCurrentClipSource());
			}
		}
		else {
			final int m = (minus_button==null) ? 0 : minus_button.w;
			final int slider_range = w - m - slider.w - ((plus_button==null) ? 0 : plus_button.w) + 1;
			int sp = mouse_x - x - m - slider.drag_offset;
			if (sp >= slider_range)
				sp = slider_range-1;
			if (sp < 0)
				sp = 0;
			if (sp != slider.x) {
				if (slider_range > 0) {
					// there will be rounding errors, so we max have to adjust the logical value up or down
					int logical_value = ((slider_max+1) * sp ) / slider_range;
					while ((slider_range*logical_value)/(slider_max+1) > sp)
						logical_value--;
					while ((slider_range*(logical_value+1))/(slider_max+1) <= sp)
						logical_value++;
					if (logical_value != slider_position) {
						slider_position = logical_value;
						component.get("slider_position").set(logical_value);
						Value v;
						if ((v=component.get("effect")).type() == Value.FUNCTION)
							FunctionCall.executeFunctionCall(v.function(), null, component.structure());
					}
				}
				slider.component.get("x").set(sp+m);
				slider.update(0, getCurrentClipSource());
			}
		}
	}



	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		if (component.type() != Value.STRUCTURE)
			return;
		final Structure d = component.structure();
		if (!d.get("structure_type").equals("SCROLLBAR"))
			return;
		Value v;
		boolean update_buttons = false;
		final boolean slider_listens_everywhere = ( slider != null && slider.listens_everywhere);
		// if one of the scrollbar elements has changed, rebuild the buttons
		if (slider == null || !d.get("slider").equals(slider_value) || !d.get("minus_button").equals(minus_button) || !d.get("plus_button").equals(plus_button)) {
			update_buttons = true;
			if (!d.get("slider").equals(slider_value)) {
				slider_value.set(d.get("slider"));
				slider = new UIScrollbarButton(createButton(slider_value), this, current_clip_source, slider_listens_everywhere, true);
			}
			if (!d.get("minus_button").equals(minus_button_value)) {
				minus_button_value.set(d.get("minus_button"));
				minus_button = new UIScrollbarButton(createButton(minus_button_value), this, current_clip_source, false, false);
			}
			if (!d.get("plus_button").equals(plus_button_value)) {
				plus_button_value.set(d.get("plus_button"));
				plus_button = new UIScrollbarButton(createButton(plus_button_value), this, current_clip_source, false, false);
			}
		}
		// if the scrollbar orientation or size has changed, update its size
		final boolean old_vertical = vertical;
		final int old_x = x, old_y = y;
		final int old_w = w, old_h = h;
		vertical = d.get("vertical").equals(true);
		super.update(CUSTOM_SIZE, current_clip_source);
		// update the logical (as opposed to pixel position) maximum position for the slider
		slider_max = 0;
		if ((v=d.get("slider_max")).type() == Value.INTEGER) {
			slider_max = v.integer();
			if (slider_max < 0) {
				slider_max = 0;
				v.set(0);
			}
		}
		// update the logical (as opposed to pixel position) position for the slider
		final int old_slider_position = slider_position;
		slider_position = 0;
		if ((v=d.get("slider_position")).type() == Value.INTEGER) {
			slider_position = v.integer();
			if (slider_position < 0) {
				slider_position = 0;
				v.set(0);
			}
			else if (slider_position > slider_max) {
				slider_position = slider_max;
				v.set(slider_position);
			}
		}
		// update the amount by which the logical slider position changes when the plus or minus button is pressed
		slider_stepping = 1;
		if ((v=d.get("slider_stepping")).type() == Value.INTEGER) {
			slider_stepping = v.integer();
			if (slider_stepping < 1) {
				slider_stepping = 1;
				v.set(1);
			}
		}
		// place the scrollbar buttons
		if (vertical) {
			if (slider != null) {
				final int m = (minus_button!=null) ? minus_button.h : 0;
				final int slider_range = h - slider.h - m - ((plus_button!=null)?plus_button.h:0) + 1;
				final int sx = update_buttons ? 0 : determineX(slider.component, x, w, slider.w); // there is no need to determine slider.x here if we have to update the slider anyway
				int sy = y + m + ((slider_range*slider_position)/(slider_max+1));
				// check whether there is more than one pixel position that corresponds to the current logical position, and whether the slider is within the pixel range of the current value
				int sy_plus_1 = y + m + ((slider_range*(slider_position+1))/(slider_max+1));
				if (slider.y > sy && slider.y < sy_plus_1)
					sy = slider.y;
				else {
					if (slider_position == 0)
						; // do nothing
					else if (slider_position == slider_max)
						// put the slider at the end of the range
						sy = y + m + slider_range - 1;
					else
						// put the slider in the center of the range for the current logical value
						sy = (sy+sy_plus_1)/2;
				}
				// update the slider, if its position has changed
				if (update_buttons || sx != slider.x || sy != slider.y) {
					if (sy != slider.y)
						slider.component.get("y").set(sy-y);
					slider.update(0, current_clip_source);
				}
			}
			if (minus_button != null) {
				final int mx = update_buttons ? 0 : determineX(minus_button.component, x, w, minus_button.w);
				if (update_buttons || mx != minus_button.x || y != minus_button.y) {
					if (minus_button.y != y)
						minus_button.component.get("y").set(0);
					minus_button.update(0, current_clip_source);
				}
			}
			if (plus_button != null) {
				final int px = update_buttons ? 0 : determineX(plus_button.component, x, w, plus_button.w);
				final int py = y + h - plus_button.h;
				if (update_buttons || px != plus_button.x || py != plus_button.y) {
					if (py != plus_button.y)
						plus_button.component.get("y").set(py-y);
					plus_button.update(0, current_clip_source);
				}
			}
		}
		else {
			if (slider != null) {
				final int m = (minus_button!=null) ? minus_button.w : 0;
				final int slider_range = w - slider.w - m - ((plus_button!=null)?plus_button.w:0) + 1;
				final int sy = update_buttons ? 0 : determineY(slider.component, y, h, slider.h); // there is no need to determine slider.y here if we have to update the slider anyway
				int sx = x + m + ((slider_range*slider_position)/(slider_max+1));
				// check whether there is more than one pixel position that corresponds to the current logical position, and whether the slider is within the pixel range of the current value
				int sx_plus_1 = x + m + ((slider_range*(slider_position+1))/(slider_max+1));
				if (slider.x > sx && slider.x < sx_plus_1)
					sx = slider.x;
				else {
					if (slider_position == 0)
						; // do nothing
					else if (slider_position == slider_max)
						// put the slider at the end of the range
						sx = x + m + slider_range - 1;
					else
						// put the slider in the center of the range for the current logical value
						sx = (sx+sx_plus_1)/2;
				}
				// update the slider, if its position has changed
				if (update_buttons || sx != slider.x || sy != slider.y) {
					if (sx != slider.x)
						slider.component.get("x").set(sx-x);
					slider.update(0, current_clip_source);
				}
			}
			if (minus_button != null) {
				final int my = update_buttons ? 0 : determineY(minus_button.component, y, h, minus_button.h);
				if (update_buttons || x != minus_button.x || my != minus_button.y) {
					if (minus_button.x != x)
						minus_button.component.get("x").set(0);
					minus_button.update(0, current_clip_source);
				}
			}
			if (plus_button != null) {
				final int px = x + w - plus_button.w;
				final int py = update_buttons ? 0 : determineY(plus_button.component, y, h, plus_button.h);
				if (update_buttons || px != plus_button.x || py != plus_button.y) {
					if (px != plus_button.x)
						plus_button.component.get("x").set(px-x);
					plus_button.update(0, current_clip_source);
				}
			}
		}
		// if the slider position has changed and there is a function listening to it, call that function
		if (slider_position != old_slider_position && (v=component.get("effect")).type() == Value.FUNCTION)
			FunctionCall.executeFunctionCall(v.function(), null, component.structure());
	}
}