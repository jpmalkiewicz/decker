package decker.view;
import decker.model.*;
import java.awt.*;


final class UIBorder extends DisplayedComponent
{
	private Color left, top, right, bottom, background;
	private int thickness;
	boolean inverted;


	UIBorder (final DisplayedComponent _parent, final DisplayedComponent current_clip_source, final boolean use_default_background_color) {
		super(null, _parent, current_clip_source);
		final Structure global_values = ScriptNode.getStackEntry(ScriptNode.RULESET_STACK_SLOT).get("GLOBAL_VALUES").structure();;
		left = AbstractView.getColor(global_values.get("BORDER_COLOR1").string());
		top = left;
		right = AbstractView.getColor(global_values.get("BORDER_COLOR2").string());
		bottom = right;
		inverted = false;
		thickness = 2;
		Value v;
		if ((v=ScriptNode.getValue("DEFAULT_BORDER_THICKNESS")).type() == Value.INTEGER)
			thickness = v.integer();
		if (use_default_background_color)
			background = AbstractView.getColor(global_values.get("BACKGROUND_COLOR").string());
	}


	UIBorder (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent, current_clip_source);
		updateBorder();
		updateChildren(current_clip_source);
if (child_count > 0 && inverted)
System.out.println("UIBorder "+x+","+y+"   "+child[1].x+","+child[1].y);
	}


	void draw (final Graphics g) {
		final int t = thickness;
		// draw the background
		if (background != null && w-2*t > 0 && h-2*t > 0) {
			g.setColor(background);
			g.fillRect(x+t, y+t, w-2*t, h-2*t);
		}
		// draw the border
		final int w1 = w-1, h1 = h-1;
		if (!inverted) {
			// draw the top border
			g.setColor(top);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i, x+w1-i, y+i);
			// draw the left border
			g.setColor(left);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i+1, x+i, y+h1-i);
			// draw the right border
			g.setColor(right);
			for (int i = t; --i >= 0; )
				g.drawLine(x+w1-i, y+i+1, x+w1-i, y+h1-i);
			// draw the bottom border
			g.setColor(bottom);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i+1, y+h1-i, x+w1-i-1, y+h1-i);
		}
		else {
			// draw the bottom border
			g.setColor(top);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+h1-i, x+w1-i, y+h1-i);
			// draw the right border
			g.setColor(left);
			for (int i = t; --i >= 0; )
				g.drawLine(x+w1-i, y+i, x+w1-i, y+h1-i-1);
			// draw the left border
			g.setColor(right);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i+1, x+i, y+h1-i-1);
			// draw the top border
			g.setColor(bottom);
			for (int i = t; --i >= 0; )
				g.drawLine(x+i, y+i, x+w1-i-1, y+i);
		}
		// draw the child components
		final DisplayedComponent[] c = child;
		final int cc = child_count;
		for (int i = 0; i < cc; i++)
			c[i].draw(g);
	}


	void update (final DisplayedComponent current_clip_source) {
		super.update(current_clip_source);
		updateBorder();
	}


	private void updateBorder () {
		if (component != null) {
			Value v;
			inverted = component.get("inverted").equals(true);
			background = AbstractView.getColor(component.get("background_color").toString());
			Value vtc = component.get("top_color"), vlc = component.get("left_color"), vrc = component.get("right_color"), vbc = component.get("bottom_color");
			if (vtc.equalsConstant("UNDEFINED"))
				vtc = vlc;
			else if (vlc.equalsConstant("UNDEFINED"))
				vlc = vtc;
			if (vrc.equalsConstant("UNDEFINED")) {
				if (!vbc.equalsConstant("UNDEFINED"))
					vrc = vbc;
				else
					vrc = vlc;
			}
			if (vbc.equalsConstant("UNDEFINED"))
				vbc = vrc;
			left = AbstractView.getColor(vlc.toString());
			top = AbstractView.getColor(vtc.toString());
			right = AbstractView.getColor(vrc.toString());
			bottom = AbstractView.getColor(vbc.toString());
			// determine the line thickness
			thickness = 2;
			if ((v=component.get("thickness")).type() == Value.INTEGER)
				thickness = v.integer();
			else if ((v=ScriptNode.getValue("DEFAULT_BORDER_THICKNESS")).type() == Value.INTEGER)
				thickness = v.integer();
		}
	}
}