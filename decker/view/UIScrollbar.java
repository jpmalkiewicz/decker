package decker.view;
import decker.model.*;
import java.awt.*;




class UIScrollbar extends DisplayedComponent
{
	private boolean vertical;
	private int slider_position, slider_max, slider_stepping;
	private UIScrollbarButton slider, minus_button, plus_button;
	private final Value slider_value = new Value(), minus_button_value = new Value(), plus_button_value = new Value();


/*
	ENGINE.scrollbarDrawFunction = FUNCTION (_scrollbar)
		if _scrollbar.vertical
			LOCAL.physical_range = _scrollbar.height - pixelheight(_scrollbar.component[0]) - pixelheight(_scrollbar.component[1]) - pixelheight(_scrollbar.component[2])
			_scrollbar.component[0].y = pixelheight(_scrollbar.component[1]) + (physical_range * _scrollbar.slider_position + _scrollbar.slider_max/2) / _scrollbar.slider_max
		else
			LOCAL.physical_range = _scrollbar.width - pixelwidth(_scrollbar.component[0]) - pixelwidth(_scrollbar.component[1]) - pixelwidth(_scrollbar.component[2])
			_scrollbar.component[0].x = pixelwidth(_scrollbar.component[1]) + (physical_range * _scrollbar.slider_position + _scrollbar.slider_max/2) / _scrollbar.slider_max

component[0] = scroller, [1] = up arrow (optional), [2] = down arrow (optional), [3] is an invisible rectangle that spans the whole screen to capture on_mouse_dragged events

	COMPONENT // this is an invisible rectangle that spans the whole screen to capture on_mouse_dragged events
		x = -1000000
		y = x
		width = -2*x
		height = width
		on_mouse_dragged = FUNCTION (mouse_x, mouse_y, dx, dy)
			if slider_dragging != UNDEFINED
				// calculate the coordinates of the mouse relative to the scrollbar
				LOCAL.dm = ( vertical ? mouse_y+y : mouse_x+x ) - slider_dragging
				LOCAL.pmin = vertical ? pixelheight(SCROLLBAR.component[1]) : pixelwidth(SCROLLBAR.component[1]) // physical minimum of the slider position, determined by the height/width of the up/left arrow
				// adjust the logical slider position to its new physical position
				if vertical
					LOCAL.logical = ((dm-pmin) * slider_max) / (SCROLLBAR.height - pmin - pixelheight(component[0]) - pixelheight(component[2]))
				else
					LOCAL.logical = ((dm-pmin) * slider_max) / (SCROLLBAR.width - pmin - pixelwidth(component[0]) - pixelwidth(component[2]))
				if logical < 0
					logical = 0
				if logical > slider_max
					logical = slider_max
				if slider_position != logical
					slider_position = logical
				if effect != UNDEFINED
					effect(SCROLLBAR.this, slider_position)
		on_mouse_up = FUNCTION
			slider_dragging = UNDEFINED    // capture on_mouse_up events here, to stop dragging the slider


	_down_right_arrow.on_mouse_down = FUNCTION
		setSliderPosition(SCROLLBAR.this, slider_position + slider_stepping)

	_slider.on_mouse_down = FUNCTION (mouse_x, mouse_y)
		SCROLLBAR.slider_dragging = vertical ? mouse_y : mouse_x
*/

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
			if (sp != old_slider_position && component.type() == Value.STRUCTURE && component.get("structure_type").equals("SCROLLBAR")) {
				component.get("slider_position").set(sp);
				update(0, getCurrentClipSource());
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




	void sliderDragged (final int mouse_x, final int mouse_y) {
System.out.println("x");
		if (vertical) {
			int sp = mouse_y - y - slider.drag_offset;
			final int slidermin = (minus_button==null) ? 0 : minus_button.h;
			final int slidermax = h - slider.h - ((plus_button==null) ? 0 : plus_button.h);
			if (sp < slidermin)
				sp = slidermin;
			if (sp > slidermax)
				sp = slidermax;
			if (sp != slider.y) {
				if (slidermin != slidermax) {
					final int logical_value = (slider_max * sp + (slidermax-slidermin+1)/2) / (slidermax-slidermin);
					if (logical_value != slider_position) {
						slider_position = logical_value;
						component.get("slider_position").set(logical_value);
						Value v;
						if ((v=component.get("effect")).type() == Value.FUNCTION)
							FunctionCall.executeFunctionCall(v.function(), null, component.structure());
					}
System.out.println(sp+"  "+logical_value);
				}
				slider.component.get("y").set(sp);
				slider.update(0, getCurrentClipSource());
			}
		}
		else {
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
				final int slider_range = h - slider.h - ((minus_button!=null)?minus_button.h:0) - ((plus_button!=null)?plus_button.h:0);
				final int sx = update_buttons ? 0 : determineX(slider.component, x, w, slider.w); // there is no need to determine slider.x here if we have to update the slider anyway
				final int sy = y + ((minus_button!=null)?minus_button.h:0) + ((slider_max==0) ? 0 : ((slider_range*slider_position+(slider_max+1)/2)/slider_max));
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
				final int slider_range = w - slider.w - ((minus_button!=null)?minus_button.w:0) - ((plus_button!=null)?plus_button.w:0);
				final int sx = x + ((minus_button!=null)?minus_button.w:0) + ((slider_max==0) ? 0 : ((slider_range*slider_position+(slider_max+1)/2)/slider_max));
				final int sy = update_buttons ? 0 : determineY(slider.component, y, h, slider.h);
				if (update_buttons || slider.x != sx || slider.y != sy) {
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