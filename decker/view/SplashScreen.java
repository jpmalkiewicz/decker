package decker.view;
import java.awt.*;
import java.io.*;
import decker.util.BMPReader;



public class SplashScreen extends Frame
{
	private Image splashscreen;


	public SplashScreen ()  {
		super("loading Decker");
		splashscreen = loadImage("splashscreen", "rulesets"+File.separator, 0x00ff00ff);
		if (splashscreen != null) {
			setUndecorated(true);
			setSize(splashscreen.getWidth(null), splashscreen.getHeight(null));
		}
		setBounds((getToolkit().getScreenSize().width-getWidth())/2, (getToolkit().getScreenSize().height-getHeight())/2, getWidth(), getHeight());
		setVisible(true);
	}


	private Image loadImage(final String name, final String path, final int transparent_color)
	{
		// if there is no suffix, try finding a .png, .gif, .bmp, .jpg or .jpeg
		int suffix = name.lastIndexOf('.');
		if (suffix == -1 ||( suffix != name.length()-4 && !name.toLowerCase().endsWith(".jpeg") )) {
			final String[] type = { ".png", ".gif", ".bmp", ".jpg", ".jpeg" };
			Image ret;
			for (int i = 0; i < type.length; i++)
				if ((ret=loadImage(name+type[i], path, transparent_color)) != null)
					return ret;
			return null;
		}

		// load it with the BMPReader if it's a .bmp, otherwise use the system Toolkit
		InputStream stream = ClassLoader.getSystemResourceAsStream(path+name);
		if (stream != null) {
			if (name.toLowerCase().endsWith(".bmp"))
				return BMPReader.readBMP(stream, this, transparent_color);
			else {
				final MediaTracker mt = new MediaTracker(this);
				Image ret;
				mt.addImage(ret = getToolkit().getImage(ClassLoader.getSystemResource( name )), 0);
				try {
					mt.waitForAll();
				} catch (InterruptedException e) {
					System.err.println("interrupted while loading an image");
					System.exit(1);
				}
				return ret;
			}
		}
System.out.println("return an \"image missing\" image instead of null");
return null;
	}


	public void paint (final Graphics g)  {
		if (splashscreen != null)
			g.drawImage(splashscreen, 0, 0, this);
	}
}