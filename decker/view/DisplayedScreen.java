package decker.view;
import decker.model.*;
import java.awt.*;


/** static class that serves as the display system interface */
public class DisplayedScreen
{
	private static DisplayedComponent currentScreen;
	private static String oldScreenTitle;




	public final static void drawScreen (final Graphics g) {
		if (currentScreen != null)
			currentScreen.child[0].draw(g);
	}



	public static void setDisplayedScreen (final Value screen) {
		// clear the listener lists
/*		while (keyDownListenerCount > 0)
			keyDownListener[--keyDownListenerCount] = null;
		while (mouseDraggedListenerCount > 0)
			mouseDraggedListener[--mouseDraggedListenerCount] = null;
		while (mouseEnteredListenerCount > 0)
			mouseEnteredListener[--mouseEnteredListenerCount] = null;
		while (mouseExitedListenerCount > 0)
			mouseExitedListener[--mouseExitedListenerCount] = null;
		while (mouseMovedListenerCount > 0)
			mouseMovedListener[--mouseMovedListenerCount] = null;
		while (mouseDownListenerCount > 0)
			mouseDownListener[--mouseDownListenerCount] = null;
		while (mouseUpListenerCount > 0)
			mouseUpListener[--mouseUpListenerCount] = null;
*/
		currentScreen = new DisplayedComponent(screen);
/*
System.out.println(keyDownListenerCount);
System.out.println(mouseDraggedListenerCount);
System.out.println(mouseEnteredListenerCount);
System.out.println(mouseExitedListenerCount);
System.out.println(mouseMovedListenerCount);
System.out.println(mouseDownListenerCount);
System.out.println(mouseUpListenerCount);
System.out.println(currentScreen!=null);
*/
	}
}