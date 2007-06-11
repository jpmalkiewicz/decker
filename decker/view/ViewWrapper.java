package decker.view;
import java.awt.*;
import java.awt.event.*;
import decker.model.*;
import decker.util.*;



public final class ViewWrapper extends Canvas
{
// methods other parts of this program will call ************************************************************************


	public ViewWrapper () {
		enableEvents(AWTEvent.COMPONENT_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
		// add a dummy AbstractView to avoid NullPointerExceptions
		view = new AbstractView();
	}


	protected AWTEvent getLastEvent () { return lastEvent; }
	public AbstractView getView () { return view; }


	public void setView (final AbstractView view) {
		if (view == null)
			throw new RuntimeException("component must not be null");
		if (view == this.view)
			return;
		this.view = view;
		repaint();
	}


// methods and variables which implement the class functionality **********************************************************************


	private Image buffer;
	private boolean painting, repaint;
	private AbstractView view;
	private AWTEvent lastEvent;
	private final Queue events = new Queue();


	private void handleUserInput () {
		while (events.size() > 0) {
			final AWTEvent e = (AWTEvent) events.remove();
			final AWTEvent e2 = lastEvent;
			lastEvent = e;
			final AbstractView v = view;
			boolean discardEvent = true;
			if (v != null) {
				discardEvent = false;
				try {
					switch (e.getID()) {
						case ComponentEvent.COMPONENT_RESIZED :
							break;
						case MouseEvent.MOUSE_DRAGGED :
							v.eventMouseDragged(((MouseEvent)e).getX(), ((MouseEvent)e).getY());
							break;
						case MouseEvent.MOUSE_ENTERED :
						case MouseEvent.MOUSE_MOVED :
							v.eventMouseMoved(((MouseEvent)e).getX(), ((MouseEvent)e).getY());
							break;
						case MouseEvent.MOUSE_EXITED :
							v.eventMouseMoved(-100000, -100000);
							break;
						case MouseEvent.MOUSE_PRESSED :
							v.eventMousePressed(((MouseEvent)e).getX(), ((MouseEvent)e).getY());
							break;
						case MouseEvent.MOUSE_RELEASED :
							v.eventMouseReleased(((MouseEvent)e).getX(), ((MouseEvent)e).getY());
							break;
						case KeyEvent.KEY_PRESSED :
							v.eventKeyPressed(((KeyEvent)e).getKeyChar(), ((KeyEvent)e).getKeyCode(), ((KeyEvent)e).isAltDown());
							break;
						case KeyEvent.KEY_RELEASED :
							v.eventKeyReleased(((KeyEvent)e).getKeyChar(), ((KeyEvent)e).getKeyCode(), ((KeyEvent)e).isAltDown());
							break;
						default :
							discardEvent = true;
					}
				} catch (Throwable t) {
					System.err.println(t);
				}
			}
			if (discardEvent) {
				lastEvent = e2;
			}
		}
	}




	public void paint (final Graphics g) {
		update(g);
	}


	public void processEvent (final AWTEvent e) {
		events.add(e);
		repaint();
	}


	private void synchronizedUpdate (final Graphics g) {
		if (!isVisible()) {
			return;
		}
		painting = true;
		do {
			try {
				repaint = false;

				handleUserInput();

				final int w = getSize().width, h = getSize().height;
				if (w > 0 && h > 0) {
					// draw the next frame
					if (buffer == null || buffer.getWidth(this) != w || buffer.getHeight(this) != h) {
						try {
							buffer = createImage(w, h);
						} catch (Throwable t) {
							// this ought to be extremely rare
							repaint = true;
							painting = false;
							repaint();
							return;
						}
					}

					final Graphics bg = buffer.getGraphics();
					bg.setFont(getFont());
					bg.setColor(getBackground());
					bg.fillRect(0, 0, w, h);
					bg.setColor(getForeground());
					if (view != null) {
						view.drawContent(bg); // call drawContent() instead of paint(), because the coordinate system already sits where it should
					}
					g.drawImage(buffer, 0, 0, this);
				}
			} catch (Throwable t) {
				t.printStackTrace();
System.out.println("exiting from program instead of trying to repaint after error");
System.exit(1);
				repaint = true;
			}
		} while (repaint);
		painting = false;
	}


	public void update (final Graphics g) {
		if (getSize().width == 0 || getSize().height == 0)
			return;
		if(painting) {
			repaint = true;
			return;
		}
		synchronizedUpdate(g);
	}
}