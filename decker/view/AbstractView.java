package decker.view;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import decker.model.*;
import decker.util.*;


/** override drawContent() and the event messages relevant to your View class */
public class AbstractView implements ComponentListener
{
//*******************************************************************************************************************************************************
// static view related data and methods *****************************************************************************************************************
//*******************************************************************************************************************************************************


public final static int CENTER = Integer.MIN_VALUE;
public final static int NONE = Integer.MIN_VALUE+5;
public final static int ABSOLUTE_MIN_VALUE = Integer.MIN_VALUE+6; // coordinate values >= ABSOLUTE_MIN_VALUE will be treated as absolute values
	public final static String SUPPORTED_IMAGE_TYPES = " .png .gif .bmp .jpg .jpeg ";


	private final static StringTreeMap COLORS = new StringTreeMap(true);
	private final static StringTreeMap IMAGES = new StringTreeMap(true);
	private final static StringTreeMap FONTS = new StringTreeMap();
	private final static StringTreeMap FONTMETRICS = new StringTreeMap();
	private static Font last_font = new Font("Arial", 13, Font.PLAIN); // this is the last font that has been displayed. it will be used to fill in any missing data when fetching a new font
	private static Color last_color = Color.white;


	public final static Color getColor (String color) {
		if (color.length() == 7)
			color = color.substring(0,1) + "ff" + color.substring(1);
		Color ret = (Color) COLORS.get(color);
		if (ret == null) {
			try {
				if (color.charAt(0) == '#' && color.length() == 9) {
					final int a = Integer.parseInt(color.substring(1,3),16);
					final int r = Integer.parseInt(color.substring(3,5),16);
					final int g = Integer.parseInt(color.substring(5,7),16);
					final int b = Integer.parseInt(color.substring(7,9),16);
					ret = new Color(r,g,b,a);
					COLORS.put(color, ret);
				}
			} catch (Throwable t) {}
			ret = last_color;
		}
		return ret;
	}


	public final static Font getFont (final String description) {
		return getFont(description, last_font, false);
	}


	public final static Font getFont (final String description, final boolean new_default_font) {
		return getFont(description, last_font, new_default_font);
	}


	/** parses the description, fills in missing data using the base_font and returns the new Font
	*   if base_font is null, last_font is used instead */
	public final static Font getFont (final String description, Font base_font) {
		return getFont(description, base_font, false);
	}


	/** parses the description, fills in missing data using the base_font and returns the new Font
	*   if base_font is null, last_font is used instead */
	public final static Font getFont (final String description, Font base_font, final boolean new_default_font) {
		if (base_font == null)
			base_font = last_font;
		String face = base_font.getFamily();
		int style = Font.PLAIN, size = base_font.getSize();
		final String s = description.trim();
		String s2, s3;
		int start = 0, end;
		while (start < s.length()) {
			end = s.indexOf(';', start);
			if (end == -1)
				end = s.length();
			s2 = s.substring(start, end).trim();
			s3 = s2.toLowerCase();
			if (s3.equals("plain"))
				style = Font.PLAIN;
			else if (s3.equals("bold"))
				style = (style==Font.ITALIC) ? (Font.ITALIC|Font.BOLD) : Font.BOLD;
			else if (s3.equals("italic"))
				style = (style==Font.BOLD) ? (Font.ITALIC|Font.BOLD) : Font.ITALIC;
			else if (s2.endsWith("pt") && Global.isInteger(s2.substring(0,s2.length()-2).trim()))
				size = Integer.parseInt(s2.substring(0,s2.length()-2).trim());
			else if (Global.isInteger(s2.trim()))
				size = Integer.parseInt(s2.trim());
			else if (s2.length() > 0)
				face = s2;
			start = end + 1;
		}
		return getFont(face, style, size, new_default_font);
	}


	public final static Font getFont (final String face, final int style, final int size, final boolean new_default_font) {
		Font f = (Font) FONTS.get(face+";"+style+";"+size);
		if (f == null) {
			f = new Font(face, style, size);
			FONTS.put(face+";"+style+";"+size, f);
		}
		if (new_default_font)
			last_font = f;
		return f;
	}


	public final static FontMetrics getFontMetrics (final Font f) {
		return getFontMetrics(f.getFamily(), f.getStyle(), f.getSize(), null);
	}


	public final static FontMetrics getFontMetrics (final String face, final int style, final int size, Graphics g) {
		FontMetrics fm = (FontMetrics) FONTMETRICS.get(face+";"+style+";"+size);
		if (fm == null) {
			if (g == null)
				g = Global.getDisplayedComponent().getGraphics();
			fm = g.getFontMetrics(getFont(face, style, size, false));
			FONTMETRICS.put(face+";"+style+";"+size, fm);
		}
		return fm;
	}


	public final static Image getImage (final String name) {
		return getImage(name, false, 0xffff00ff);
	}


	public final static Image getImage (final String name, final boolean buffered_image) {
		return getImage(name, buffered_image, 0xffff00ff);
	}


	public final static Image getImage (final String name, final boolean buffered_image, final int transparent_color) {
		if (name == null)
			return null;
		Object o = IMAGES.get(name);
if (name.equals("UNDEFINED")) {
System.out.println("fetching image UNDEFINED");
new Throwable().printStackTrace();
}
		// there is no image of that name. if the name has no suffix, try to fetch one with a suffix
		if (o == null) {
			int suffix = name.lastIndexOf('.');
			if (suffix == -1 ||( suffix != name.length()-4 && !name.toLowerCase().endsWith(".jpeg") )) {
				final String[] type = { ".png", ".gif", ".bmp", ".jpg", ".jpeg" };
				for (int i = 0; i < type.length; i++) {
					if ((o=IMAGES.get(name+type[i])) != null) {
						IMAGES.put(name, o);
						break;
					}
				}
			}
		}
		// if we haven't done so before, fetch the image now
		if (o instanceof File) {
			final String path = ((File)o).getPath();
			final Component c = Global.getDisplayedComponent();
			if (path.toLowerCase().endsWith(".bmp")) {
				final InputStream stream = AbstractView.class.getClassLoader().getResourceAsStream(path);
				o = BMPReader.readBMP(stream, c, transparent_color);
			}
			else {
				final MediaTracker mt = new MediaTracker(c);
				mt.addImage((Image)(o = c.getToolkit().getImage(AbstractView.class.getClassLoader().getResource( path ))), 0);
				try {
					mt.waitForAll();
				} catch (InterruptedException e) {
					System.err.println("interrupted while loading image "+path);
					System.exit(1);
				}
			}
			IMAGES.put(name, o);
		}
		if (o instanceof Image) {
			final Image image = (Image) o;
			if (!buffered_image || image instanceof BufferedImage)
				return image;
			// create a transparent BufferedImage and then draw the image onto it
			final int w = image.getWidth(null), h = image.getHeight(null);
			BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			for (int x = 0; x < w; x++)
				for (int y = 0; y < h; y++)
					buffered.setRGB(x,y,0);
			buffered.getGraphics().drawImage(image, 0, 0, null);
			IMAGES.put(name, buffered);
			return buffered;
		}
		return null;
	}


	/** reloads the list of all available images and sounds */
	public static void reloadArtwork (final boolean prefetchImages)  {
		IMAGES.clear();
		try {
			final Component c = Global.getDisplayedComponent();
			final MediaTracker mt = ( prefetchImages ? new MediaTracker(c) : null );
			File f = new File("rulesets"+File.separator+Global.getCurrentRuleset().getName()+File.separator+"artwork");
			if (f.isDirectory())
				reloadArtwork(f, "", mt);
// add loading mod artwork here
			if (mt != null) {
				mt.waitForAll();
				// check whether errors have occurred
				if (mt.isErrorAny()) {
					final Object[] o = mt.getErrorsAny();
					System.err.println("unable to load the image"+((o.length==1)?"":"s")+" below :");
					for (int i = 0; i < o.length; i++)
						System.err.println(o.toString());
					System.exit(1);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
System.out.println("finished reloading artwork");
	}


	private static void reloadArtwork (final File dir, String path, final MediaTracker mt) throws IOException  {
		// do not look for images in directories generated by the subversion system
		if (dir.getName().toLowerCase().endsWith(".svn"))
			return;
System.out.println("loading artwork from "+(path.length()>0?path:"."));
		final Component c = Global.getDisplayedComponent();
		final File[] f = dir.listFiles();
		final String path_prefix = (path.length()==0) ? "" : (path + "/");
		for (int i = 0; i < f.length; i++) {
			final String name = f[i].getName();
			if (f[i].isDirectory())
				reloadArtwork(f[i], path_prefix+name, mt);
			else {
				// determine the file type
				final int pos = name.lastIndexOf('.');
				if (pos > -1) {
					final String file_type = name.substring(pos).toLowerCase();
					// check whether it is an image
					if (SUPPORTED_IMAGE_TYPES.indexOf(" "+file_type+" ") > -1) {
						if (mt == null)
							IMAGES.put(path_prefix+name, f[i]);
						else {
							if (file_type.equals(".bmp")) {
								final InputStream stream = AbstractView.class.getClassLoader().getResourceAsStream(f[i].getPath());
								IMAGES.put(path_prefix+name, BMPReader.readBMP(stream, c, 0xffff00ff));
							}
							else {
								Image k = c.getToolkit().getImage(AbstractView.class.getClassLoader().getResource(f[i].getPath()));
								mt.addImage(k, 0);
								IMAGES.put(path_prefix+name, k);
							}
						}
					}
				}
			}
		}
	}


//*******************************************************************************************************************************************************
//*******************************************************************************************************************************************************
//*******************************************************************************************************************************************************


	private int frame_x, frame_y;


	public AbstractView ()  {
		if (Global.getDisplayedComponent() instanceof Frame)
			Global.getDisplayedComponent().addComponentListener(this);
	}


	public void componentHidden (ComponentEvent e)  {}


	public void componentMoved (ComponentEvent e)  {
		final Component c = e.getComponent();
		if (e.getComponent().getX() != frame_x)
			Global.getEngineData().get("display_center_x").set(c.getX()+c.getWidth()/2);
		if (e.getComponent().getY() != frame_y)
			Global.getEngineData().get("display_center_y").set(c.getY()+c.getHeight()/2);
	}


	public void componentResized (ComponentEvent e)  {}


	public void componentShown (ComponentEvent e)  {}


	public void displayTickerMessage (final String message)  {}


	public void drawContent(final Graphics g)  {}


	public void eventKeyPressed (final char c, final int code, final boolean isAltDown)  {}
	public void eventKeyReleased (final char c, final int code, final boolean isAltDown)  {}
	public void eventMouseDragged (final int x, final int y, final int dx, final int dy)  {}
	public void eventMouseMoved (final int x, final int y)  {}
	public void eventMousePressed (final int x, final int y)  {}
	public void eventMouseReleased (final int x, final int y)  {}


	public int getDrawOffsetX ()  { return 0; }


	public int getDrawOffsetY ()  { return 0; }


	public final int height (Object visible_object) {
		Value v;
		if (visible_object instanceof Value) {
			v = (Value) visible_object;
			if (v.type() == Value.STRUCTURE)
				visible_object = v.structure();
			else if (!v.equalsConstant("UNDEFINED"))
				visible_object = v.toString();
		}
		// is it a string with the name of an image?
		if (visible_object instanceof String) {
			final Image image = getImage((String)visible_object);
			if (image != null)
				return image.getHeight(null);
		}
		// is it a structure?
		if (visible_object instanceof Structure) {
			final Structure d = (Structure) visible_object;
			// check whether the height is explicitly defined
			v = d.get("height");
			if (v != null && v.type() == Value.INTEGER)
				return v.integer();
			// if this is a BUTTON, use the definition for the idle state instead
			if (d.get("structure_type").equals("BUTTON") && !d.get("idle").equalsConstant("UNDEFINED"))
				return height(d.get("idle"));
			// if this is a STRING, determine its height
			if (d.get("structure_type").equals("TEXT")) {
				if ((v=d.get("font")) != null && v.type() == Value.STRING)
					return getFontMetrics(getFont(v.string())).getAscent();
				else
					return getFontMetrics(getFont("")).getHeight(); // use the last font that was used to draw a string
			}
			// if not, use the height of the first entry in the object array
			if (d.get("object") != null && d.get("object").get("size").integer() > 0)
				return height(d.get("object").get("0"));
		}
		// everything has failed, assume a height of 0 for the structure
		return 0;
	}


	public void repaint()  {
		Global.getViewWrapper().repaint();
	}


	/** adjusts the Bounds of the Frame when the top level view's bounds settings change */
	public void setScreenSize (int new_width, int new_height)  {
		if (new_width <= 10)
			new_width = 11;
		if (new_height <= 11)
			new_height = 11;
		// the Frame should be the top-most parent object of the ViewWrapper
		Component parent = Global.getViewWrapper().getParent();
		while (parent.getParent() != null)
			parent = parent.getParent();
		if (parent != null && parent instanceof Frame)  {
			// we need to add the size of the Frame's border to the bounds
			final Insets i = ((Frame)parent).getInsets();
			new_width  += i.left + i.right;
			new_height += i.top  + i.bottom;
			// adjust x and y
			frame_x = Global.getEngineData().get("display_center_x").integer() - new_width/2;
			frame_y = Global.getEngineData().get("display_center_y").integer() - new_height/2;
			if (frame_x+new_width > parent.getToolkit().getScreenSize().width)
				frame_x = parent.getToolkit().getScreenSize().width - new_width;
			if (frame_y+new_height > parent.getToolkit().getScreenSize().height)
				frame_y = parent.getToolkit().getScreenSize().height - new_height;
			if (frame_x < 0)
				frame_x = 0;
			if (frame_y < 0)
				frame_y = 0;
			// set the Frame bounds to the new values
			((Frame)parent).setBounds(frame_x, frame_y, new_width, new_height);
			((Frame)parent).doLayout();
		}
	}


	/** sets the text in the title bar of the Frame */
	public void setTitle (final String new_title)  {
		// the Frame should be the top-most parent object of the ViewWrapper
		Component parent = Global.getViewWrapper().getParent();
		while (parent.getParent() != null)
			parent = parent.getParent();
		if (parent != null && parent instanceof Frame)
			((Frame)parent).setTitle(new_title);
	}


	public final int width (Object visible_object) {
		Value v;
		if (visible_object instanceof Value) {
			v = (Value) visible_object;
			if (v.type() == Value.STRUCTURE)
				visible_object = v.structure();
			else if (!v.equalsConstant("UNDEFINED"))
				visible_object = v.toString();
		}
		// is it a string with the name of an image?
		if (visible_object instanceof String) {
			final Image image = getImage((String)visible_object);
			if (image != null)
				return image.getWidth(null);
		}
		// is it a structure?
		if (visible_object instanceof Structure) {
			final Structure d = (Structure) visible_object;
			// check whether the width is explicitly defined
			v = d.get("width");
			if (v != null && v.type() == Value.INTEGER)
				return v.integer();
			// if this is a BUTTON, use the definition for the idle state instead
			if (d.get("structure_type").equals("BUTTON") && !d.get("idle").equalsConstant("UNDEFINED"))
				return width(d.get("idle"));
			// if this is a STRING, calculate its width
			if (d.get("structure_type").equals("TEXT")) {
				FontMetrics fm;
				if ((v=d.get("font")) != null && v.type() == Value.STRING)
					fm = getFontMetrics(getFont(v.string()));
				else
					fm = getFontMetrics(getFont("")); // use the last font that was used to draw a string
				return fm.stringWidth(d.get("text").toString());
			}
			// if not, use the height of the first entry in the object array
			if (d.get("object") != null && d.get("object").get("size").integer() > 0)
				return width(d.get("object").get("0"));
		}
		// everything has failed, assume a width of 0 for the structure
		return 0;
	}
}