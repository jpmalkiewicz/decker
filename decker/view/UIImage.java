package decker.view;
import decker.model.*;
import java.awt.*;


class UIImage extends DisplayedComponent
{
	private Image image;




	UIImage (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		if (!_component.equalsConstant("UNDEFINED")) {
			image = AbstractView.getImage((_component.type()==Value.STRUCTURE&&_component.get("structure_type").equals("IMAGE")) ? _component.get("image").toString() : _component.toString());
			if (image == null)
				System.out.println("UIImage : undefined image "+_component);
		}
		update(0, current_clip_source);
	}




	void determineSize (final boolean width_already_determined, final boolean height_already_determined, final DisplayedComponent current_clip_source) {
		if (image != null) {
			if (!width_already_determined)
				w = image.getWidth(null);
			if (!height_already_determined)
				h = image.getHeight(null);
		}
	}




	public void draw (final Graphics g) {
		if (image != null)
			g.drawImage(image, x, y, null);
	}




	public void eventValueChanged (final String variable_name, final Structure container, final Value old_value, final Value new_value) {
		if (variable_name.equals("image")) {
			Value v;
			int k;
			final int old_width = w, old_height = h;
			final boolean old_rtpw = relative_to_parent_width, old_rtph = relative_to_parent_height;
			image = AbstractView.getImage(new_value.toString());
			if ((v=component.get("width")) == null || v.equalsConstant("UNDEFINED")) {
				w = image.getWidth(null);
				relative_to_parent_width = (v=component.get("x")) != null && (k=getPercentageValue(v.toString())) != Integer.MIN_VALUE && k != 0;
			}
			if ((v=component.get("height")) == null || v.equalsConstant("UNDEFINED")) {
				h = image.getHeight(null);
				relative_to_parent_height = (v=component.get("y")) != null && (k=getPercentageValue(v.toString())) != Integer.MIN_VALUE && k != 0;
			}
			if (w != old_width || h != old_height || old_rtpw != relative_to_parent_width || old_rtph != relative_to_parent_height) {
				eventSizeChanged(getCurrentClipSource(), old_width, old_height, old_rtpw, old_rtph);
			}
		}
		else {
			super.eventValueChanged(variable_name, container, old_value, new_value);
		}
	}




	void update (final int customSettings, final DisplayedComponent current_clip_source) {
		if (component.type() == Value.STRUCTURE)
			super.update(customSettings|CUSTOM_SIZE, current_clip_source);
		else {
			// it's just a name
			x = parent.x;
			y = parent.y;
			image = AbstractView.getImage(component.toString());
			if (image == null) {
				w = 0;
				h = 0;
			}
			else {
				w = image.getWidth(null);
				h = image.getHeight(null);
			}
		}
	}
}
