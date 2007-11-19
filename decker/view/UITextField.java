package decker.view;
import decker.model.*;
import java.awt.*;


final class UITextField extends DisplayedComponent
{
	private Value cursor, text;



	UITextField (final Value _component, final DisplayedComponent _parent, final DisplayedComponent current_clip_source) {
		super(_component, _parent, current_clip_source);
		// fetch the
		// register it as a hard coded key listener
		hasHardcodedEventFunction[ON_KEY_DOWN] = true;
		if (!eventFunctionRegistered[ON_KEY_DOWN])
			addEventListener(ON_KEY_DOWN);
//		updateChildren(current_clip_source);
	}


	void draw (final Graphics g) {
//				cursor.x = user_input.x + pixelwidth(user_input)
	}


/*
			on_key_down = FUNCTION (key)
				if size(key) == 1 &&( char_limit == UNDEFINED || size(user_input.text) < char_limit )
					user_input.text = user_input.text + key
					if (TEXTFIELD.width != UNDEFINED) && (user_input.x + pixelwidth(user_input) + pixelwidth(cursor) > width)
						user_input.text = substring(user_input.text, 0, size(user_input.text)-1)    // the typed character doesn't fit into the field, discard it
				else if key == "BACKSPACE" && size(user_input.text) > 0
					user_input.text = substring(user_input.text, 0, size(user_input.text)-1)
*/

	void update (final DisplayedComponent current_clip_source) {
		super.update(current_clip_source);
	}
}