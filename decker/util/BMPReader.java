package decker.util;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.InputStream;
import java.io.IOException;


/** BMP parser for 24, 8 and 4 bit windows .bmp images
* 	this is a modified version of the method from
* 	   http://www.javaworld.com/javaworld/javatips/jw-javatip43.html
*
* 	reads a image in bmp format from an InputStream into a java.awt.Image object
* 	sets the pixels with argb value "transparent_color" to argb value 0
*
*  info about 16 bit bmps fetched from
*     http://atlc.sourceforge.net/bmp.html
*/

public class BMPReader
{
	public static Image readBMP(final InputStream stream, final Component component, final int transparent_color)
	{
		Image image;

		try
		{
			// read the 14-byte BITMAPFILEHEADER and the 40-byte BITMAPINFOHEADER
			byte bf[] = new byte[14];
			read(stream, bf, 0, bf.length);
			byte bi[] = new byte[40];
			read(stream, bi, 0, bi.length);

			// Interperet data.
			final int nsize = parseValue(bf, 2, 4);
			final int nbisize = parseValue(bi, 0, 4);
			final int nwidth = parseValue(bi, 4, 4);
			final int nheight = parseValue(bi, 8, 4);
			final int nplanes = parseValue(bi, 12, 2);
			final int nbitcount = parseValue(bi, 14, 2);
			final int ncompression = parseValue(bi, 16, 4); // Look for non-zero values to indicate compression
			final int nsizeimage_original = parseValue(bi, 20, 4);
			final int nxpm = parseValue(bi, 24, 4);
			final int nypm = parseValue(bi, 28, 4);
			int nclrused = parseValue(bi, 32, 4);
			final int nclrimp = parseValue(bi, 36, 4);

			// if the bmp uses less than 24 bits per pixel and the number of colors used is zero,
			// the number of colors used is the maximum possible for the number of bits per pixel
			if(nclrused == 0)
				nclrused = (1<<nbitcount);

			// Some bitmaps do not have the sizeimage field calculated
			final int nsizeimage = ((((nwidth*nbitcount)+31) & ~31 ) >> 3) * nheight;

			// read the palette if the image uses less then 24 bits per pixel
			int[] npalette = null;

			if (nbitcount <= 8)
			{
				// Read the palette colors
				npalette = new int[nclrused];
				byte bpalette[] = new byte[nclrused*4];
				read(stream, bpalette, 0, nclrused*4);
				for (int n = 0; n < nclrused; n++)
				{
					npalette[n] = parseValue(bpalette, 4*n, 3) | 0xff000000;
					if(npalette[n] == transparent_color)
						npalette[n] = 0;
				}
			}


			// read the image data
			if (nbitcount==32)
			{
				// No Palette data for 32-bit format.
				// no padding out neccessary
				// the data will be interpreted as aarrggbb
				int ndata[] = new int [nheight * nwidth];
				byte brgb[] = new byte [nwidth * 4 * nheight];
				read(stream, brgb, 0, brgb.length);
				int nindex = 0;
				for (int j = 0; j < nheight; j++)
				{
					for (int i = 0; i < nwidth; i++)
					{
						ndata[nwidth*(nheight-j-1)+i] = parseValue(brgb, nindex, 4);
						nindex += 4;
					}
				}
				image = new BufferedImage(nwidth, nheight, BufferedImage.TYPE_INT_ARGB);
				((BufferedImage)image).setRGB(0, 0, nwidth, nheight, ndata, 0, nwidth);
				return image;
			}
			else if (nbitcount==24)
			{
				// No Palette data for 24-bit format but scan lines are
				// padded out to even 4-byte boundaries.
				int npad = (nsizeimage / nheight) - nwidth * 3;
				int ndata[] = new int [nheight * nwidth];
				byte brgb[] = new byte [(nwidth * 3 + npad) * nheight];
				read(stream, brgb, 0, brgb.length);
				int nindex = 0;
				for (int j = 0; j < nheight; j++)
				{
					for (int i = 0; i < nwidth; i++)
					{
						ndata[nwidth*(nheight-j-1)+i] = parseValue(brgb, nindex, 3) | 0xff000000;
						if(ndata[nwidth*(nheight-j-1)+i] == transparent_color)
							ndata[nwidth*(nheight-j-1)+i] = 0;
						nindex += 3;
					}
					nindex += npad;
				}
				image = component.createImage(new MemoryImageSource(nwidth, nheight, ndata, 0, nwidth));
			}
			else if (nbitcount==16)
			{
				// No Palette data for 16-bit format but scan lines are
				// padded out to even 4-byte boundaries.
				int npad = (nsizeimage / nheight) - nwidth * 2;
				int ndata[] = new int [nheight * nwidth];
				byte brgb[] = new byte [(nwidth * 2 + npad) * nheight];
				read(stream, brgb, 0, brgb.length);
				int nindex = 0;
				for (int y = 0; y < nheight; y++)
				{
					for (int x = 0; x < nwidth; x++)
					{
						final int value = parseValue(brgb, nindex, 2);
						final int red = (int)Math.round(((value&0x7c00)>>10)*0xff/31.0);
						final int green = (int)Math.round(((value&0x03e0)>>5)*0xff/31.0);
						final int blue = (int)Math.round((value&0x1f)*0xff/31.0);
						ndata[nwidth*(nheight-y-1)+x] =  (red<<16) | (green<<8) | blue | 0xff000000;
						if(ndata[nwidth*(nheight-y-1)+x] == transparent_color)
							ndata[nwidth*(nheight-y-1)+x] = 0;
						nindex += 2;
					}
					nindex += npad;
				}
				image = component.createImage(new MemoryImageSource(nwidth, nheight, ndata, 0, nwidth));
			}
			else if(nbitcount == 8)
			{
				// Read the image data (actually indexes into the palette)
				// Scan lines are still padded out to even 4-byte
				// boundaries.
				int npad8 = (nsizeimage / nheight) - nwidth;

				int  ndata8[] = new int [nwidth*nheight];
				byte bdata[] = new byte [(nwidth+npad8)*nheight];
				int nindex8 = 0;
				read(stream, bdata, 0, (nwidth+npad8)*nheight);
				for (int j8 = 0; j8 < nheight; j8++)
				{
					for (int i8 = 0; i8 < nwidth; i8++)
					{
						ndata8 [nwidth*(nheight-j8-1)+i8] =
						npalette [((int)bdata[nindex8]&0xff)];
						nindex8++;
					}
					nindex8 += npad8;
				}
				image = component.createImage(new MemoryImageSource(nwidth, nheight, ndata8, 0, nwidth));
			}
			else if(nbitcount == 4)
			{
				// Read the image data (actually indexes into the palette)
				// Scan lines are still padded out to even 4-byte
				// boundaries.
				int npad4 = (nsizeimage / nheight) - (nwidth+1)/2;

				int  ndata4[] = new int [nwidth*nheight];
				byte bdata[] = new byte [nsizeimage];
				read(stream, bdata, 0, nsizeimage);
				int nindex4 = 0;
				for (int j4 = 0; j4 < nheight; j4++)
				{
					for (int i4 = 0; i4 < nwidth; i4++)
					{
						if( (i4&1) == 0 )
							ndata4[nwidth*(nheight-j4-1)+i4] = npalette[((int)bdata[nindex4]&0xf0)>>4];
						else
							ndata4[nwidth*(nheight-j4-1)+i4] = npalette[((int)bdata[nindex4]&0x0f)];
						if( (i4&1) == 1 || i4+1 == nwidth )
							nindex4++;
					}
					nindex4 += npad4;
				}
				image = component.createImage(new MemoryImageSource(nwidth, nheight, ndata4, 0, nwidth));
			}
			else
			{
				System.out.println ("InputStream does not contain a 32-bit, 24-bit, 16-bit, 8-bit or 4-bit Windows Bitmap. cannot parse "+nbitcount+" bit bitmaps");
				System.exit(1);
				image = null;
			}

			stream.close();
			image.getWidth(null); // force the MemoryImageSource to create the image
			return image;
		}
		catch (Exception e)
		{
			System.out.println("Caught exception in BMPReader.readBMP");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}


    /** this method interprets a section of a byte buffer as an integer, low byte first */
    private static int parseValue(byte[] buffer, int position, int bytes)
    {
		int ret = 0;
		for(int i = 0; i < bytes; i++)
			ret |= (((int)buffer[position+i]&0xff)<<(8*i));
		return ret;
	}


	/** this method keeps reading until it has read the requested number of bytes from a stream */
	private static void read(InputStream stream, byte[] buffer, int offset, int length)
	throws IOException
	{
		int bytes_read = 0, new_bytes;
		do{
			new_bytes = stream.read(buffer, offset+bytes_read, length-bytes_read);
			if( new_bytes > -1 )
				bytes_read += new_bytes;
			else
				throw new IOException("could only read "+bytes_read+" of expected "+length+" bytes of pixel data");

			if( bytes_read < length )
				Thread.yield();
		}while(bytes_read < length);
	}
}