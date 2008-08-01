package decker.view;
import decker.model.*;
import java.awt.*;


class UIImage extends DisplayedComponent
{
	private Image image;




	UIImage (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent);
		if (!_component.equalsConstant("UNDEFINED")) {
			boolean load_normal_image = true;
			String image_name;
			int angle = 0;
			if (_component.type()==Value.STRUCTURE && _component.get("structure_type").equals("IMAGE")) {
				image_name = _component.get("image").toString();
				// if the image has been turned, try to load the turned version
				final Value v = _component.get("angle");
				if (v != null) {
					final int t = v.type();
					if (t == Value.INTEGER || t == Value.REAL) {
						angle = (t == Value.INTEGER) ? v.integer() : (int)v.real();
						// set the angle to 0, 90, 180 or 270. -45 is rounded to -90, +45 is rounded to +90
						angle = ( (((angle%360)+((angle<0)?404:45))%360) / 90 ) * 90;
					}
				}
			}
			else {
				image_name = _component.toString();
			}

			// fetch the image
			if (load_normal_image) {
				image = (angle == 0) ? AbstractView.getImage(image_name) : AbstractView.getTurnedImage(image_name, angle);
				if (image == null) {
					System.out.println("UIImage : undefined image "+((_component.type()==Value.STRUCTURE&&_component.get("structure_type").equals("IMAGE")) ? _component.get("image").toString() : _component.toString()));
				}
			}
		}
		if (_component.type() == Value.STRUCTURE)
			_component.structure().addValueListener(this);
		update(0, current_clip_source);
	}




	void destroy () {
		super.destroy();
		if (component.type() == Value.STRUCTURE)
			component.structure().removeValueListener(this);
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
		if (variable_name.equals("image") || variable_name.equals("angle")) {
			Value v;
			int k;
			final int old_width = w, old_height = h;
			final boolean old_rtpw = relative_to_parent_width, old_rtph = relative_to_parent_height;
			final Image old_image = image;
			final String image_name = variable_name.equals("image") ? new_value.toString() : component.get("image").toString();
			final int angle = (new_value.type() == Value.INTEGER || new_value.type() == Value.REAL) ? (int) new_value.real() : 0;
			image = AbstractView.getTurnedImage(image_name, angle);
			if ((v=component.get("width")) == null || v.equalsConstant("UNDEFINED")) {
				w = (image != null) ? image.getWidth(null) : 0;
				relative_to_parent_width = (v=component.get("x")) != null &&( v.equalsConstant("CENTER") || v.equalsConstant("RIGHT") ||( (k=getPercentageValue(v.toString())) != Integer.MIN_VALUE && k != 0 ));
			}
			if ((v=component.get("height")) == null || v.equalsConstant("UNDEFINED")) {
				h = (image != null) ? image.getHeight(null) : 0;
				relative_to_parent_height = (v=component.get("y")) != null &&( v.equalsConstant("CENTER") || v.equalsConstant("BOTTOM") ||( (k=getPercentageValue(v.toString())) != Integer.MIN_VALUE && k != 0 ));
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
