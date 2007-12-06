package decker.view;
import decker.model.*;
import java.awt.*;




class UIScrollpaneScrollbar extends UIScrollbar
{
	private int old_slider_position;




	UIScrollpaneScrollbar (final Value _component, final UIScrollpane _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent, current_clip_source);
		old_slider_position = getSliderPosition();
	}




	void buttonPressed (UIScrollbarButton button, final int mouse_x, final int mouse_y) {
		super.buttonPressed(button, mouse_x, mouse_y);
		if (getSliderPosition() != old_slider_position) {
			old_slider_position = getSliderPosition();
			((UIScrollpane)parent).sliderMoved(this, old_slider_position);
		}
	}




	void sliderDragged (final int mouse_x, final int mouse_y) {
		super.sliderDragged(mouse_x, mouse_y);
		if (getSliderPosition() != old_slider_position) {
			old_slider_position = getSliderPosition();
			((UIScrollpane)parent).sliderMoved(this, old_slider_position);
		}
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		super.update(customSettings, current_clip_source);
		if (getSliderPosition() != old_slider_position) {
			old_slider_position = getSliderPosition();
			((UIScrollpane)parent).sliderMoved(this, old_slider_position);
		}
	}
}