package decker.view;
import java.awt.Component;
import decker.model.*;


/** tells the component to repaint whenver the fps setting requires a new frame */
public class FPSThread extends Thread
{
	private Component component;
	private boolean running = true;


	public FPSThread (final Component c) {
		component = c;
		setDaemon(true);
		setPriority(getPriority()-1);
		start();
	}


	public void kill () {
		running = false;
		interrupt();
	}


	public void run () {
		long time;
		while (running) {
			time = System.nanoTime();
			component.repaint();
			final Value vfps = ScriptNode.getValue("FRAMES_PER_SECOND");
			final long fps = (vfps==null||vfps.type()!=Value.INTEGER) ? 0 : vfps.integer();
			if (fps > 0) {
				final long nanoseconds_per_frame = 1000000000L / fps;
				time += nanoseconds_per_frame - System.nanoTime();
				if (time >= 1000000L) { // if the next frame is at least 1 millisecond away, wait
					try {
						sleep(time/1000000L, (int)(time%1000000L));
					} catch (InterruptedException ex) {}
				}
			}
			else {
				try {
					sleep(25); // wait 25 milliseconds before checking again whether there is valid fps value
				} catch (InterruptedException ex) {}
			}
		}
	}
}
