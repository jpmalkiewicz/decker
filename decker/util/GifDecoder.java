package decker.util;
import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

/**
 * Class GifDecoder - Decodes a GIF file into one or more frames.
 *
 * download source : http://www.fmsware.com/stuff/gif.html
 *
 * <br><pre>
 * Example:
 *    GifDecoder d = new GifDecoder();
 *    d.read("sample.gif");
 *    int n = d.getFrameCount();
 *    for (int i = 0; i < n; i++) {
 *       BufferedImage frame = d.getFrame(i);  // frame i
 *       int t = d.getFrameDuration(i);  // display duration of frame in milliseconds
 *       // do something with frame
 *    }
 * </pre>
 * No copyright asserted on the source code of this class.  May be used for
 * any purpose, however, refer to the Unisys LZW patent for any additional
 * restrictions.  Please forward any corrections to kweiner@fmsware.com.
 *
 * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's ImageMagick.
 * @version 1.03 November 2003
 *
 */

public class GifDecoder {

	public static final int STATUS_OK = 0;
	public static final int STATUS_IMAGE_CORRUPT = 1;
	public static final int STATUS_IMAGE_SOURCE_UNAVAILABLE = 2;



	public static void main(String[] args) {
		GifDecoder d = new GifDecoder();
		d.read("splash.gif");
		int n = d.getFrameCount();
		System.out.println("frame count : "+n);
		for (int i = 0; i < n; i++) {
			BufferedImage frame = d.getFrameImage(i);  // frame i
			int t = d.getFrameDuration(i);  // display duration of frame in milliseconds
			System.out.println("frame "+i+" duration  : "+t);
		}
	}


	public int getFrameCount () { return frameCount; }


	/** returns the duration of frame frame_index in milliseconds */
	public int getFrameDuration(int frame_index) { return frames[frame_index].delay; }


	/** Gets the first (or only) image read */
	public BufferedImage getImage() { return getFrameImage(0); }


	/** Gets the "Netscape" iteration count, if any.
	 *  A count of 0 means repeat indefinitiely.
	 *  @return iteration count if one was specified, else 1. */
	public int getLoopCount () { return loopCount; }


	/** returns the image of frame frame_index */
	public BufferedImage getFrameImage (int frame_index) { return frames[frame_index].image; }


	public int getHeight () { return height; }


	public int getWidth () { return width; }


	/**
	 * Reads GIF image from stream
	 *
	 * @param BufferedInputStream containing GIF file.
	 * @return read status code (0 = no errors)
	 */
	public int read(BufferedInputStream is) {
		init();
		if (is != null) {
			in = is;
			readHeader();
			if (!err()) {
				readContents();
				if (frameCount < 0) {
					status = STATUS_IMAGE_CORRUPT;
				}
			}
		} else {
			status = STATUS_IMAGE_SOURCE_UNAVAILABLE;
		}
		try {
			is.close();
		} catch (IOException e) {
		}
		return status;
	}


	/**
	 * Reads GIF image from stream
	 *
	 * @param InputStream containing GIF file.
	 * @return read status code (0 = no errors)
	 */
	public int read(InputStream is) {
		init();
		if (is != null) {
			if (!(is instanceof BufferedInputStream))
				is = new BufferedInputStream(is);
			in = (BufferedInputStream) is;
			readHeader();
			if (!err()) {
				readContents();
				if (frameCount < 0) {
					status = STATUS_IMAGE_CORRUPT;
				}
			}
		} else {
			status = STATUS_IMAGE_SOURCE_UNAVAILABLE;
		}
		try {
			is.close();
		} catch (IOException e) {
		}
		return status;
	}


	/**
	 * Reads GIF file from specified file/URL source
	 * (URL assumed if name contains ":/" or "file:")
	 *
	 * @param name String containing source
	 * @return read status code (0 = no errors)
	 */
	public int read(String name) {
		status = STATUS_OK;
		try {
			name = name.trim().toLowerCase();
			if ((name.indexOf("file:") >= 0) ||
				(name.indexOf(":/") > 0)) {
				URL url = new URL(name);
				in = new BufferedInputStream(url.openStream());
			} else {
				in = new BufferedInputStream(new FileInputStream(name));
			}
			status = read(in);
		} catch (IOException e) {
			status = STATUS_IMAGE_SOURCE_UNAVAILABLE;
		}

		return status;
	}


//****************************************************************************************************************************
//    private methods and data    ********************************************************************************************
//****************************************************************************************************************************



	private BufferedInputStream in;
	private int status;

	private int width; // full image width
	private int height; // full image height
	private boolean gctFlag; // global color table used
	private int gctSize; // size of global color table
	private int loopCount = 1; // iterations; 0 = repeat forever

	private int[] gct; // global color table
	private int[] lct; // local color table
	private int[] act; // active color table

	private int bgIndex; // background color index
	private int bgColor; // background color
	private int lastBgColor; // previous bg color
	private int pixelAspect; // pixel aspect ratio

	private boolean lctFlag; // local color table flag
	private boolean interlace; // interlace flag
	private int lctSize; // local color table size

	private int ix, iy, iw, ih; // current image rectangle
	private Rectangle lastRect; // last image rect

	private byte[] block = new byte[256]; // current data block
	private int blockSize = 0; // block size

	// last graphic control extension info
	private int dispose = 0;
	// 0=no action; 1=leave in place; 2=restore to bg; 3=restore to prev
	private int lastDispose = 0;
	private boolean transparency = false; // use transparent color
	private int delay = 0; // delay in milliseconds
	private int transIndex; // transparent color index

	private static final int MaxStackSize = 4096;
	// max decoder pixel stack size

	// LZW decoder working arrays
	private short[] prefix;
	private byte[] suffix;
	private byte[] pixelStack;
	private byte[] pixels;

	private GifFrame[] frames; // frames read from current file
	private int frameCount;


	private static class GifFrame {
		GifFrame (BufferedImage im, int del) {
			image = im;
			delay = del;
		}
		BufferedImage image;
		int delay;
	}


	/**
	 * Creates new frame image from current data (and previous
	 * frames as specified by their disposition codes).
	 */
	private void setPixels(final BufferedImage image, final int frame_index) {
		// expose destination image's pixels as int array
		int[] dest = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

		// fill in starting image contents based on last image's dispose code
		if (lastDispose > 0) {
			BufferedImage lastImage = (frame_index>0) ? frames[frame_index-1].image : null;
			if (lastDispose == 3) {
				// use image before last
// shouldn't it be 2 instead of 3?
				lastImage = (frame_index>=3) ? frames[frame_index-3].image : null;
			}

			if (lastImage != null) {
				// copy pixels
				final int[] prev = ((DataBufferInt) lastImage.getRaster().getDataBuffer()).getData();
				System.arraycopy(prev, 0, dest, 0, width * height);

				if (lastDispose == 2) {
					// fill last image rect area with background color
					Graphics2D g = image.createGraphics();
					Color c = null;
					if (transparency) {
						c = new Color(0, 0, 0, 0); 	// assume background is transparent
					} else {
						c = new Color(lastBgColor); // use given background color
					}
					g.setColor(c);
					g.setComposite(AlphaComposite.Src); // replace area
					g.fill(lastRect);
					g.dispose();
				}
			}
		}

		// copy each source line to the appropriate place in the destination
		int pass = 1;
		int inc = 8;
		int iline = 0;
		for (int i = 0; i < ih; i++) {
			int line = i;
			if (interlace) {
				if (iline >= ih) {
					pass++;
					switch (pass) {
						case 2 :
							iline = 4;
							break;
						case 3 :
							iline = 2;
							inc = 4;
							break;
						case 4 :
							iline = 1;
							inc = 2;
					}
				}
				line = iline;
				iline += inc;
			}
			line += iy;
			if (line < height) {
				int k = line * width;
				int dx = k + ix; // start of line in dest
				int dlim = dx + iw; // end of dest line
				if ((k + width) < dlim) {
					dlim = k + width; // past dest edge
				}
				int sx = i * iw; // start of line in source
				while (dx < dlim) {
					// map color and insert in destination
					int index = ((int) pixels[sx++]) & 0xff;
					int c = act[index];
					if (c != 0) {
						dest[dx] = c;
					}
					dx++;
				}
			}
		}
	}

	/**
	 * Decodes LZW image data into pixel array.
	 * Adapted from John Cristy's ImageMagick.
	 */
	private void decodeImageData() {
		int NullCode = -1;
		int npix = iw * ih;
		int available,
			clear,
			code_mask,
			code_size,
			end_of_information,
			in_code,
			old_code,
			bits,
			code,
			count,
			i,
			datum,
			data_size,
			first,
			top,
			bi,
			pi;

		if ((pixels == null) || (pixels.length < npix)) {
			pixels = new byte[npix]; // allocate new pixel array
		}
		if (prefix == null) prefix = new short[MaxStackSize];
		if (suffix == null) suffix = new byte[MaxStackSize];
		if (pixelStack == null) pixelStack = new byte[MaxStackSize + 1];

		//  Initialize GIF data stream decoder.

		data_size = read();
		clear = 1 << data_size;
		end_of_information = clear + 1;
		available = clear + 2;
		old_code = NullCode;
		code_size = data_size + 1;
		code_mask = (1 << code_size) - 1;
		for (code = 0; code < clear; code++) {
			prefix[code] = 0;
			suffix[code] = (byte) code;
		}

		//  Decode GIF pixel stream.

		datum = bits = count = first = top = pi = bi = 0;

		for (i = 0; i < npix;) {
			if (top == 0) {
				if (bits < code_size) {
					//  Load bytes until there are enough bits for a code.
					if (count == 0) {
						// Read a new data block.
						count = readBlock();
						if (count <= 0)
							break;
						bi = 0;
					}
					datum += (((int) block[bi]) & 0xff) << bits;
					bits += 8;
					bi++;
					count--;
					continue;
				}

				//  Get the next code.

				code = datum & code_mask;
				datum >>= code_size;
				bits -= code_size;

				//  Interpret the code

				if ((code > available) || (code == end_of_information))
					break;
				if (code == clear) {
					//  Reset decoder.
					code_size = data_size + 1;
					code_mask = (1 << code_size) - 1;
					available = clear + 2;
					old_code = NullCode;
					continue;
				}
				if (old_code == NullCode) {
					pixelStack[top++] = suffix[code];
					old_code = code;
					first = code;
					continue;
				}
				in_code = code;
				if (code == available) {
					pixelStack[top++] = (byte) first;
					code = old_code;
				}
				while (code > clear) {
					pixelStack[top++] = suffix[code];
					code = prefix[code];
				}
				first = ((int) suffix[code]) & 0xff;

				//  Add a new string to the string table,

				if (available >= MaxStackSize)
					break;
				pixelStack[top++] = (byte) first;
				prefix[available] = (short) old_code;
				suffix[available] = (byte) first;
				available++;
				if (((available & code_mask) == 0)
					&& (available < MaxStackSize)) {
					code_size++;
					code_mask += available;
				}
				old_code = in_code;
			}

			//  Pop a pixel off the pixel stack.

			top--;
			pixels[pi++] = pixelStack[top];
			i++;
		}

		for (i = pi; i < npix; i++) {
			pixels[i] = 0; // clear missing pixels
		}

	}

	/**
	 * Returns true if an error was encountered during reading/decoding
	 */
	private boolean err() {
		return status != STATUS_OK;
	}

	/**
	 * Initializes or re-initializes reader
	 */
	private void init() {
		status = STATUS_OK;
		frameCount = 0;
		frames = new GifFrame[20];
		gct = null;
		lct = null;
	}

	/**
	 * Reads a single byte from the input stream.
	 */
	private int read() {
		int curByte = 0;
		try {
			curByte = in.read();
		} catch (IOException e) {
			status = STATUS_IMAGE_CORRUPT;
		}
		return curByte;
	}

	/**
	 * Reads next variable length block from input.
	 *
	 * @return number of bytes stored in "buffer"
	 */
	private int readBlock() {
		blockSize = read();
		int n = 0;
		if (blockSize > 0) {
			try {
				int count = 0;
				while (n < blockSize) {
					count = in.read(block, n, blockSize - n);
					if (count == -1)
						break;
					n += count;
				}
			} catch (IOException e) {
			}

			if (n < blockSize) {
				status = STATUS_IMAGE_CORRUPT;
			}
		}
		return n;
	}

	/**
	 * Reads color table as 256 RGB integer values
	 *
	 * @param ncolors int number of colors to read
	 * @return int array containing 256 colors (packed ARGB with full alpha)
	 */
	private int[] readColorTable(int ncolors) {
		int nbytes = 3 * ncolors;
		int[] tab = null;
		byte[] c = new byte[nbytes];
		int n = 0;
		try {
			n = in.read(c);
		} catch (IOException e) {
		}
		if (n < nbytes) {
			status = STATUS_IMAGE_CORRUPT;
		} else {
			tab = new int[256]; // max size to avoid bounds checks
			int i = 0;
			int j = 0;
			while (i < ncolors) {
				int r = ((int) c[j++]) & 0xff;
				int g = ((int) c[j++]) & 0xff;
				int b = ((int) c[j++]) & 0xff;
				tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
			}
		}
		return tab;
	}

	/**
	 * Main file parser.  Reads GIF content blocks.
	 */
	private void readContents() {
		// read GIF file content blocks
		boolean done = false;
		while (!(done || err())) {
			int code = read();
			switch (code) {

				case 0x2C : // image separator
					if (frameCount > frames.length) {
						final GifFrame[] f = frames;
						frames = new GifFrame[f.length*2+1];
						System.arraycopy(f, 0, frames, 0, f.length);
					}
					frames[frameCount] = new GifFrame(readImage(), delay);
					break;

				case 0x21 : // extension
					code = read();
					switch (code) {
						case 0xf9 : // graphics control extension
							readGraphicControlExt();
							break;

						case 0xff : // application extension
							readBlock();
							String app = "";
							for (int i = 0; i < 11; i++) {
								app += (char) block[i];
							}
							if (app.equals("NETSCAPE2.0")) {
								readNetscapeExt();
							}
							else
								skip(); // don't care
							break;

						default : // uninteresting extension
							skip();
					}
					break;

				case 0x3b : // terminator
					done = true;
					break;

				case 0x00 : // bad byte, but keep going and see what happens
					break;

				default :
					status = STATUS_IMAGE_CORRUPT;
			}
		}
		if (frameCount < frames.length) {
			final GifFrame[] f = frames;
			frames = new GifFrame[frameCount];
			System.arraycopy(f, 0, frames, 0, frameCount);
		}
	}


	/**
	 * Reads Graphics Control Extension values
	 */
	private void readGraphicControlExt() {
		read(); // block size
		int packed = read(); // packed fields
		dispose = (packed & 0x1c) >> 2; // disposal method
		if (dispose == 0) {
			dispose = 1; // elect to keep old image if discretionary
		}
		transparency = (packed & 1) != 0;
		delay = readShort() * 10; // delay in milliseconds
		transIndex = read(); // transparent color index
		read(); // block terminator
	}

	/**
	 * Reads GIF file header information.
	 */
	private void readHeader() {
		String id = "";
		for (int i = 0; i < 6; i++) {
			id += (char) read();
		}
		if (!id.startsWith("GIF")) {
			status = STATUS_IMAGE_CORRUPT;
			return;
		}

		readLSD();
		if (gctFlag && !err()) {
			gct = readColorTable(gctSize);
			bgColor = gct[bgIndex];
		}
	}

	/**
	 * Reads next frame image
	 */
	private BufferedImage readImage() {
		ix = readShort(); // (sub)image position & size
		iy = readShort();
		iw = readShort();
		ih = readShort();

		int packed = read();
		lctFlag = (packed & 0x80) != 0; // 1 - local color table flag
		interlace = (packed & 0x40) != 0; // 2 - interlace flag
		// 3 - sort flag
		// 4-5 - reserved
		lctSize = 2 << (packed & 7); // 6-8 - local color table size

		if (lctFlag) {
			lct = readColorTable(lctSize); // read table
			act = lct; // make local table active
		} else {
			act = gct; // make global table active
			if (bgIndex == transIndex)
				bgColor = 0;
		}
		int save = 0;
		if (transparency) {
			save = act[transIndex];
			act[transIndex] = 0; // set transparent color if specified
		}

		if (act == null) {
			status = STATUS_IMAGE_CORRUPT; // no color table defined
		}

		if (err()) return null;

		decodeImageData(); // decode pixel data
		skip();

		if (err()) return null;

		frameCount++;

		// create new image to receive frame data
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);

		setPixels(image, frameCount); // transfer pixel data to image

		if (transparency) {
			act[transIndex] = save;
		}
		resetFrame();

		return image;
	}

	/**
	 * Reads Logical Screen Descriptor
	 */
	private void readLSD() {

		// logical screen size
		width = readShort();
		height = readShort();

		// packed fields
		int packed = read();
		gctFlag = (packed & 0x80) != 0; // 1   : global color table flag
		// 2-4 : color resolution
		// 5   : gct sort flag
		gctSize = 2 << (packed & 7); // 6-8 : gct size

		bgIndex = read(); // background color index
		pixelAspect = read(); // pixel aspect ratio
	}

	/**
	 * Reads Netscape extenstion to obtain iteration count
	 */
	private void readNetscapeExt() {
		do {
			readBlock();
			if (block[0] == 1) {
				// loop count sub-block
				int b1 = ((int) block[1]) & 0xff;
				int b2 = ((int) block[2]) & 0xff;
				loopCount = (b2 << 8) | b1;
			}
		} while ((blockSize > 0) && !err());
	}

	/**
	 * Reads next 16-bit value, LSB first
	 */
	private int readShort() {
		// read 16-bit value, LSB first
		return read() | (read() << 8);
	}

	/**
	 * Resets frame state for reading next image.
	 */
	private void resetFrame() {
		lastDispose = dispose;
		lastRect = new Rectangle(ix, iy, iw, ih);
		lastBgColor = bgColor;
		int dispose = 0;
		boolean transparency = false;
		int delay = 0;
		lct = null;
	}

	/**
	 * Skips variable length blocks up to and including
	 * next zero length block.
	 */
	private void skip() {
		do {
			readBlock();
		} while ((blockSize > 0) && !err());
	}
}
