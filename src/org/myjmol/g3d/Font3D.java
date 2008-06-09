/* $RCSfile: Font3D.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:14 $
 * $Revision: 1.10 $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: miguel@jmol.org
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.myjmol.g3d;

import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;

import org.myjmol.util.Logger;
/**
 *<p>
 * Provides font support using a byte fid
 * (<strong>F</strong>ont <strong>ID</strong>) as an index into font table.
 *</p>
 *<p>
 * Supports standard font faces, font styles, and font sizes.
 *</p>
 *
 * @author Miguel, miguel@jmol.org
 */
final public class Font3D {

  public final byte fid;
  public final String fontFace;
  public final String fontStyle;
  public final short fontSize;
  public final Font font;
  public final FontMetrics fontMetrics;

  private Font3D(byte fid,
                 int idFontFace, int idFontStyle, int fontSize,
                 Font font, FontMetrics fontMetrics) {
    this.fid = fid;
    this.fontFace = fontFaces[idFontFace];
    this.fontStyle = fontStyles[idFontStyle];
    this.fontSize = (short)fontSize;
    this.font = font;
    this.fontMetrics = fontMetrics;
  }

  ////////////////////////////////////////////////////////////////
  
  static Graphics graphicsOffscreen;
  static synchronized void initialize(Platform3D platform) {
    if (graphicsOffscreen == null)
      graphicsOffscreen = platform.allocateOffscreenImage(1, 1).getGraphics();
  }

  private final static int FONT_ALLOCATION_UNIT = 8;
  private static int fontkeyCount = 1;
  private static short[] fontkeys = new short[FONT_ALLOCATION_UNIT];
  private static Font3D[] font3ds = new Font3D[FONT_ALLOCATION_UNIT];

  static Font3D getFont3D(int fontface, int fontstyle, int fontsize,
                          Platform3D platform) {
    if (graphicsOffscreen == null)
      initialize(platform);
    /*
    Logger.debug("Font3D.getFont3D("  + fontFaces[fontface] + "," +
                       fontStyles[fontstyle] + "," + fontsize + ")");
    */
    if (fontsize > 63)
      fontsize = 63;
    short fontkey =
      (short)(((fontface & 3) << 8) | ((fontstyle & 3) << 6) | fontsize);
    // watch out for race condition here!
    for (int i = fontkeyCount; --i > 0; )
      if (fontkey == fontkeys[i])
        return font3ds[i];
    return allocFont3D(fontkey, fontface, fontstyle, fontsize);
  }
  
  /*
  FontMetrics getFontMetrics(Font font) {
    if (gOffscreen == null)
      checkOffscreenSize(16, 64);
    return gOffscreen.getFontMetrics(font);
  }
  */
  
  public static synchronized Font3D allocFont3D(short fontkey, int fontface,
                                                int fontstyle, int fontsize) {
    // recheck in case another process just allocated one
    for (int i = fontkeyCount; --i > 0; )
      if (fontkey == fontkeys[i])
        return font3ds[i];
    int fontIndexNext = fontkeyCount++;
    if (fontIndexNext == fontkeys.length) {
      short[] t0 = new short[fontIndexNext + FONT_ALLOCATION_UNIT];
      System.arraycopy(fontkeys, 0, t0, 0, fontIndexNext);
      fontkeys = t0;
      
      Font3D[] t1 = new Font3D[fontIndexNext + FONT_ALLOCATION_UNIT];
      System.arraycopy(font3ds, 0, t1, 0, fontIndexNext);
      font3ds = t1;
    }
    Font font = new Font(fontFaces[fontface], fontstyle, fontsize);
    if (graphicsOffscreen == null)
      Logger.error("Font3D.graphicsOffscreen not initialized");
    FontMetrics fontMetrics = graphicsOffscreen.getFontMetrics(font);
    Font3D font3d = new Font3D((byte)fontIndexNext,
                               fontface, fontstyle, fontsize,
                               font, fontMetrics);
    // you must set the font3d before setting the fontkey in order
    // to prevent a race condition with getFont3D
    font3ds[fontIndexNext] = font3d;
    fontkeys[fontIndexNext] = fontkey;
    return font3d;
  }

  public final static int FONT_FACE_SANS  = 0;
  public final static int FONT_FACE_SERIF = 1;
  public final static int FONT_FACE_MONO  = 2;
  
  public final static String[] fontFaces =
  {"SansSerif", "Serif", "Monospaced", ""};

  public final static int FONT_STYLE_PLAIN      = 0;
  public final static int FONT_STYLE_BOLD       = 1;
  public final static int FONT_STYLE_ITALIC     = 2;
  public final static int FONT_STYLE_BOLDITALIC = 3;
  
  public final static String[] fontStyles =
  {"Plain", "Bold", "Italic", "BoldItalic"};
  
  public static int getFontFaceID(String fontface) {
    if ("Monospaced".equalsIgnoreCase(fontface))
      return FONT_FACE_MONO;
    if ("Serif".equalsIgnoreCase(fontface))
      return FONT_FACE_SERIF;
    return FONT_FACE_SANS;
  }

  public static int getFontStyleID(String fontstyle) {
    int i = 4;
    while (--i > 0)
      if (fontStyles[i].equalsIgnoreCase(fontstyle))
        break;
    return i;
  }

  public static Font3D getFont3D(byte fontID) {
    return font3ds[fontID & 0xFF];
  }
}

