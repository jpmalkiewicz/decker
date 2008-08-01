package decker.view;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import decker.model.*;
import decker.util.*;


/** override drawContent() and the event messages relevant to your View class */
public class AbstractView
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
		// remove the alpha if it is 100%
		if (color.length() == 9 && color.substring(1,3).equalsIgnoreCase("ff"))
			color = color.substring(0,1) + color.substring(3);
		Color ret = (Color) COLORS.get(color);
		if (ret == null) {
			try {
				if (color.charAt(0) == '#') {
					if (color.length() == 7) {
						final int r = Integer.parseInt(color.substring(1,3),16);
						final int g = Integer.parseInt(color.substring(3,5),16);
						final int b = Integer.parseInt(color.substring(5,7),16);
						ret = new Color(r,g,b);
						COLORS.put(color, ret);
					}
					else if (color.length() == 9) {
						final int a = Integer.parseInt(color.substring(1,3),16);
						final int r = Integer.parseInt(color.substring(3,5),16);
						final int g = Integer.parseInt(color.substring(5,7),16);
						final int b = Integer.parseInt(color.substring(7,9),16);
						ret = new Color(r,g,b,a);
						COLORS.put(color, ret);
					}
				}
			} catch (Throwable t) {}
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
		if (name.equals("UNDEFINED"))
			return null;
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


	public final static Image getTurnedImage (final String name, final int angle) {
		// if the image isn't turned, return the normal image
		if (angle == 0 ||( angle != 90 && angle != 180 && angle != 270 )) {
			return getImage(name, false, 0);
		}
		// try to fetch the turned image
		Image image = (Image) IMAGES.get(name+"�"+angle);
		if (image != null) {
			return image;
		}
		else {
			// fetch the un-turned image
			image = getImage(name, false, 0);
			if (image == null) {
				return null;
			}
			// fetch the pixels into an array
			PixelGrabber pg = new PixelGrabber(image, 0, 0, -1, -1, true);
			try {
				pg.grabPixels();
			} catch (InterruptedException e) {
				// this should never happen
				System.err.println("interrupted while turning the image "+name+" by "+angle+" degrees");
				System.exit(1);
			}
			int[] pixels_old = (int[]) pg.getPixels();
			int[] pixels = new int[pixels_old.length];
			final int w = pg.getWidth(), h = pg.getHeight();
			// turn it
			if (angle == 90) {
				for(int y = h; --y >= 0; ) {
					final int offset = y*w;
					for(int x = w; --x >= 0; ) {
						// the new position of the pixel at (x,y) is (h-1-y,x)
						pixels[(h-1-y)+x*h] = pixels_old[x+offset];
					}
				}
				image = Global.getDisplayedComponent().createImage(new MemoryImageSource(h, w, pixels, 0, h));
			}
			else if (angle == 180) {
				for(int y = h; --y >= 0; ) {
					final int offset = y*w;
					for(int x = w; --x >= 0; ) {
						// the new position of the pixel at (x,y) is (w-1-x,h-1-y)
						pixels[(w-1-x)+(h-1-y)*w] = pixels_old[x+offset];
					}
				}
				image = Global.getDisplayedComponent().createImage(new MemoryImageSource(w, h, pixels, 0, w));
			}
			else { // angle == 270
				for(int y = h; --y >= 0; ) {
					final int offset = y*w;
					for(int x = w; --x >= 0; ) {
						// the new position of the pixel at (x,y) is (y,w-1-x)
						pixels[y+(w-1-x)*h] = pixels_old[x+offset];
					}
				}
				image = Global.getDisplayedComponent().createImage(new MemoryImageSource(h, w, pixels, 0, h));
			}
			// add it to the image list
			IMAGES.put(name+"�"+angle, image);
			return image;
		}
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
if (Global.debug_level > 0)
System.out.println("finished reloading artwork");
	}


	private static void reloadArtwork (final File dir, String path, final MediaTracker mt) throws IOException  {
		// do not look for images in directories generated by the subversion system
		if (dir.getName().toLowerCase().endsWith(".svn"))
			return;
if (Global.debug_level > 0)
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


	public void displayTickerMessage (final String message)  {}


	public void drawContent(final Graphics g)  {}


	public void eventKeyPressed (final char c, final int code, final boolean isAltDown)  {}
	public void eventKeyReleased (final char c, final int code, final boolean isAltDown)  {}
	public void eventMouseDragged (final int x, final int y, final int dx, final int dy)  {}
	public void eventMouseMoved (final int x, final int y)  {}
	public void eventMousePressed (final int x, final int y)  {}
	public void eventMouseReleased (final int x, final int y)  {}


	public final static int height (Object visible_object, final int parent_height) {
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
			final String type = d.get("structure_type").string();
			// check whether the height is explicitly defined
			v = d.get("height");
			if (v != null) {
				if (v.type() == Value.INTEGER)
					return v.integer();
				// check whether it's a percentage value
				if (v.type() == Value.STRING) {
					final String s = v.string();
					if (s.endsWith("%")) {
						try {
							return (Integer.parseInt(s.substring(0, s.length()-1)) * parent_height + 50)/100;
						} catch (NumberFormatException ex) {}
					}
				}
			}
			// if this is a BUTTON, use the definition for the idle state instead
/*			if (type.equals("BUTTON") && !d.get("idle").equalsConstant("UNDEFINED"))
				return height(d.get("idle"));
			if (type.equals("BORDER_BUTTON") && !d.get("idle").equalsConstant("UNDEFINED"))
				return height(d.get("idle")) + 2*ScriptNode.getValue("DEFAULT_BORDER_THICKNESS").integer();
*/			// if this is a STRING, determine its height
			if (type.equals("TEXT")) {
				if ((v=d.get("font")) != null && v.type() == Value.STRING)
					return getFontMetrics(getFont(v.string())).getAscent();
				else
					return getFontMetrics(getFont("")).getAscent(); // use the last font that was used to draw a string
			}
			else if (type.equals("IMAGE")) {
				Image image;
				if ((v=d.get("image")) != null && (image=getImage(v.toString())) != null)
					return image.getHeight(null);
			}
			// if its structure type has a special pixelheight function, call it
			final Value t = ScriptNode.getStructureType(type);
			if (t != null) {
				if ((v=t.get("pixelheight")) != null && v.type() == Value.FUNCTION) {
					v = FunctionCall.executeFunctionCall(v, null, (Structure) visible_object);
					if (v.type() == Value.INTEGER)
						return v.integer();
				}
			}
			// if not, use the height of the first sub-component
/*			if ((v=d.get("component")) != null) {
				if (v.type() == Value.ARRAY && v.array().length > 0)
					return height(v.get(0));
				else
					return height(v);
			}
*/		}
		// everything has failed, assume a height of 0 for the structure
		return 0;
	}


	public final static int width (Object visible_object, final int parent_width) {
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
			final String type = d.get("structure_type").string();
			// check whether the width is explicitly defined
			v = d.get("width");
			if (v != null) {
				if (v.type() == Value.INTEGER)
					return v.integer();
				// check whether it's a percentage value
				if (v.type() == Value.STRING) {
					final String s = v.string();
					if (s.endsWith("%")) {
						try {
							return (Integer.parseInt(s.substring(0, s.length()-1)) * parent_width + 50)/100;
						} catch (NumberFormatException ex) {}
					}
				}
			}
			// if this is a BUTTON, use the definition for the idle state instead
/*			if (type.equals("BUTTON") && !d.get("idle").equalsConstant("UNDEFINED"))
				return width(d.get("idle"));
			if (type.equals("BORDER_BUTTON") && !d.get("idle").equalsConstant("UNDEFINED"))
				return width(d.get("idle")) + 2*ScriptNode.getValue("DEFAULT_BORDER_THICKNESS").integer();
*/			// if this is a STRING, calculate its width
			if (type.equals("TEXT")) {
				FontMetrics fm;
				if ((v=d.get("font")) != null && v.type() == Value.STRING)
					fm = getFontMetrics(getFont(v.string()));
				else
					fm = getFontMetrics(getFont("")); // use the last font that was used to draw a string
				return fm.stringWidth(d.get("text").toString());
			}
			else if (type.equals("IMAGE")) {
				Image image;
				if ((v=d.get("image")) != null && (image=getImage(v.toString())) != null)
					return image.getWidth(null);
			}
			// if its structure type has a special pixelwidth function, call it
			final Value t = ScriptNode.getStructureType(type);
			if (t != null) {
				if ((v=t.get("pixelwidth")) != null && v.type() == Value.FUNCTION) {
					v = FunctionCall.executeFunctionCall(v, null, (Structure) visible_object);
					if (v.type() == Value.INTEGER)
						return v.integer();
				}
			}
			// if not, use the width of the first sub-component
/*			if ((v=d.get("component")) != null) {
				if (v.type() == Value.ARRAY && v.array().length > 0)
					return width(v.get(0));
				return width(v);
			}
*/		}
		// everything has failed, assume a width of 0 for the structure
		return 0;
	}
}