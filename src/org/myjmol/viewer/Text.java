/* $RCSfile: Text.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:08 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2002-2005  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
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
package org.myjmol.viewer;

import java.awt.FontMetrics;
import javax.vecmath.Point3f;

import org.myjmol.g3d.Font3D;
import org.myjmol.g3d.Graphics3D;

class Text {

  final static int POINTER_NONE = 0;
  final static int POINTER_ON = 1;
  final static int POINTER_BACKGROUND = 2;
  
  final static String[] hAlignNames = {"", "left", "center", "right", ""};

  final static int XY = 0;
  final static int LEFT = 1;
  final static int CENTER = 2;
  final static int RIGHT = 3;
  final static int XYZ = 4;

  final static String[] vAlignNames = {"xy", "top", "bottom", "middle"};

  final static int TOP = 1;
  final static int BOTTOM = 2;
  final static int MIDDLE = 3;

  boolean atomBased;
  Graphics3D g3d;
  Point3f xyz;
  String target;
  String text;
  
  String[] lines;
  int align;
  int valign;
  int pointer;
  int movableX;
  int movableY;
  int movableXPercent = Integer.MAX_VALUE;
  int movableYPercent = Integer.MAX_VALUE;
  int offsetX;
  int offsetY;
  int z;
  int zSlab; // z for slabbing purposes -- may be near an atom

  int windowWidth;
  int windowHeight;
  boolean adjustForWindow;
  int boxX, boxY, boxWidth, boxHeight;
  
  Font3D font;
  FontMetrics fm;
  byte fid;
  int ascent;
  int descent;
  int lineHeight;

  short colix;
  short bgcolix;

  int[] widths;
  int textWidth;
  int textHeight;

  // for labels and hover
  Text(Graphics3D g3d, Font3D font, String text, short colix,
      short bgcolix, int offsetX, int offsetY, int z, int zSlab, int textAlign) {
    windowWidth = g3d.getRenderWidth();
    windowHeight = g3d.getRenderHeight();
    atomBased = true;
    this.g3d = g3d;
    this.text = fixText(text);
    this.colix = colix;
    this.bgcolix = bgcolix;
    setXYZs(offsetX, offsetY, z, zSlab);
    align = textAlign;
    setFont(font);
  }

  // for echo
  Text(Graphics3D g3d, Font3D font, String target, short colix, int valign, int align) {
    windowWidth = g3d.getRenderWidth();
    windowHeight = g3d.getRenderHeight();
    atomBased = false;
    this.g3d = g3d;
    this.target = target;
    if (target.equals("error"))
      valign = TOP; 
    this.align = align;
    this.valign = valign;
    this.font = font;
    this.colix = colix;
    this.z = 2;
    this.zSlab = Integer.MIN_VALUE;
    getFontMetrics();
  }

  void getFontMetrics() {
    fm = font.fontMetrics;
    descent = fm.getDescent();
    ascent = fm.getAscent();
    lineHeight = ascent + descent;
  }

  void setFid(byte fid) {
    if (this.fid == fid)
      return;
    this.fid = fid;
    recalc();
  }

  void setXYZ(Point3f xyz) {
    valign = XYZ;
    this.xyz = xyz;
  }
  
  void setAdjustForWindow(boolean TF) {
    adjustForWindow = TF;
  }
  
  void setColix(short colix) {
    this.colix = colix;
  }

  void setColix(Object value) {
    colix = Graphics3D.getColix(value);
  }

  void setBgColix(short colix) {
    this.bgcolix = colix;
  }

  void setBgColix(Object value) {
    bgcolix = (value == null ? (short) 0 : Graphics3D.getColix(value));
  }

  void setMovableX(int x) {
    valign = (valign == XYZ ? XYZ : XY);
    movableX = x;
    movableXPercent = Integer.MAX_VALUE;
  }

  void setMovableY(int y) {
    valign = (valign == XYZ ? XYZ : XY);
    movableY = y;
    movableYPercent = Integer.MAX_VALUE;
  }
  
  void setMovableXPercent(int x) {
    valign = (valign == XYZ ? XYZ : XY);
    movableX = Integer.MAX_VALUE;
    movableXPercent = x;
  }

  void setMovableYPercent(int y) {
    valign = (valign == XYZ ? XYZ : XY);
    movableY = Integer.MAX_VALUE;
    movableYPercent = y;
  }
  
  void setXY(int x, int y) {
    setMovableX(x);
    setMovableY(y);
  }

  void setZs(int z, int zSlab) {
    this.z = z;
    this.zSlab = zSlab;
  }

  void setXYZs(int x, int y, int z, int zSlab) {
    setMovableX(x);
    setMovableY(y);
    setZs(z, zSlab);
  }

  void setPositions() {
    int xLeft, xCenter, xRight;
    if (valign == XY || valign == XYZ) {
      int x = (movableXPercent == Integer.MAX_VALUE ?  movableX 
          : movableXPercent * windowWidth / 100);
      xLeft = xRight = xCenter = x + offsetX;
    } else {
      xLeft = 5;
      xCenter = windowWidth / 2;
      xRight = windowWidth - 5;
    }
    
    // set box X from alignments
    
      boxX = xLeft;
      switch (align) {
      case CENTER:
        boxX = xCenter - boxWidth / 2; 
        break;
      case RIGHT:
        boxX = xRight - boxWidth;        
      }
    
    // set box Y from alignments
    
    boxY = 0;
    switch (valign) {
    case TOP:
      break;
    case MIDDLE:
      boxY = windowHeight / 2;
      break;
    case BOTTOM:
      boxY = windowHeight;
      break;
    default:
      int y = (movableYPercent == Integer.MAX_VALUE ?  movableY 
          : movableYPercent * windowHeight / 100);
      boxY = (atomBased ? y : (windowHeight - y)) + offsetY;
    }

    // adjust positions if necessary
    
    setBoxOffsetsInWindow();

  }
  void setOffset(int offset) {
    //Labels only
    offsetX = getXOffset(offset);
    offsetY = getYOffset(offset);
    valign = XY;
  }

  final static int getXOffset(int offset) {
    switch (offset) {
    case 0:
      return JmolConstants.LABEL_DEFAULT_X_OFFSET;
    case Short.MAX_VALUE:
      return 0;
    default:
      return (byte) (offset >> 8);
    }
  }

  final static int getYOffset(int offset) {
    switch (offset) {
    case 0:
      return -JmolConstants.LABEL_DEFAULT_Y_OFFSET;
    case Short.MAX_VALUE:
      return 0;
    default:
      return -(int)((byte) (offset & 0xFF));
    }
  }

  void setText(String text) {
    text = fixText(text);
    if (this.text != null && this.text.equals(text))
      return;
    this.text = text;
    recalc();
  }

  void setFont(Font3D f3d) {
    font = f3d;
    getFontMetrics();
    recalc();
  }

  boolean setAlignment(String align) {
    if ("left".equals(align))
      return setAlignment(LEFT);
    if ("center".equals(align))
      return setAlignment(CENTER);
    if ("right".equals(align))
      return setAlignment(RIGHT);
    return false;
  }

  static String getAlignment(int align) {
    return hAlignNames[align & 3];
  }
  
  boolean setAlignment(int align) {
    this.align = align;
    recalc();
    return true;
  }

  void setPointer(int pointer) {
    this.pointer = pointer;
  }
  
  static String getPointer(int pointer) {
    return ((pointer & POINTER_ON) == 0 ? ""
        : (pointer & POINTER_BACKGROUND) > 0 ? "background" : "on");
  }
  
  String fixText(String text) {
    if (text == null)
      return null;
    int pt;
    while ((pt = text.indexOf("\n")) >= 0)
      text = text.substring(0, pt) + "|" + text.substring(pt + 1);
    return text;  
  }
  
  void recalc() {
    if (text == null) {
      text = null;
      lines = null;
      widths = null;
      return;
    }
    lines = split(text, '|');
    textWidth = 0;
    widths = new int[lines.length];
    for (int i = lines.length; --i >= 0;) {
      widths[i] = fm.stringWidth(lines[i]);
      textWidth = Math.max(textWidth, widths[i]);
    }
    textHeight = lines.length * lineHeight;
    boxWidth = textWidth + 8;
    boxHeight = textHeight + 8;
  }

  void render() {
    if (text == null)
      return;

    setPositions();

    // draw the box if necessary
    
    if (bgcolix != 0)
      drawBox();
    
    // now set x and y positions for text from (new?) box position
    
    int x0 = boxX + 4;
    switch (align) {
    case CENTER:
      x0 = boxX + boxWidth / 2;
      break;
    case RIGHT:
      x0 = boxX + boxWidth - 4;
    }
    
    // now write properly aligned text
    
    int x = x0;
    int y = boxY + ascent + 4;
    for (int i = 0; i < lines.length; i++) {
      switch (align) {
      case CENTER:
        x = x0 - widths[i] / 2;
        break;
      case RIGHT:
        x = x0 - widths[i];
      }
      g3d.drawString(lines[i], font, colix, x, y, z, zSlab);
      y += lineHeight;
    }
    
    // now daw the pointer, if requested
        
    if ((pointer & POINTER_ON) != 0) {
      short pointerColix = ((pointer & POINTER_BACKGROUND) != 0 && bgcolix != 0 ? bgcolix : colix);
      if (boxX > movableX)
        g3d.drawLine(pointerColix, movableX, movableY, zSlab, boxX, boxY + boxHeight / 2, zSlab);
      else if (boxX + boxWidth < movableX)
        g3d.drawLine(pointerColix, movableX, movableY, zSlab, boxX + boxWidth, boxY + boxHeight
            / 2, zSlab);
    }
  }

  private void drawBox() {
    g3d.fillRect(bgcolix, boxX, boxY, z + 2, zSlab, boxWidth, boxHeight);
    g3d.drawRect(colix, boxX + 1, boxY + 1, z + 1, zSlab, boxWidth - 2,
        boxHeight - 2);
  }

  void setBoxOffsetsInWindow() {
    if (!adjustForWindow)
      boxY -= lineHeight;
    if (atomBased && align == XY) {
      boxX += JmolConstants.LABEL_DEFAULT_X_OFFSET;
      boxY -= JmolConstants.LABEL_DEFAULT_Y_OFFSET + 4;
    }
    if (valign == XYZ) {
      boxY += ascent/2; 
    }
    if (!adjustForWindow)
      return;  // labels
    
    // these coordinates are (0,0) in top left
    // (user coordinates are (0,0) in bottom left)
    boxY -= textHeight;
    int x = boxX;
    if (x + boxWidth + 5 > windowWidth)
      x = windowWidth - boxWidth - 5;
    if (x < 5)
      x = 5;
    int y = boxY;
    if (y + boxHeight > windowHeight)
      y = windowHeight - boxHeight;
    int y0 = (atomBased ? 16 + lineHeight : 0);
    if (y < y0)
      y = y0;
    // (echo is not atomBased -- positioned right on)
    boxX = x;
    boxY = y;
  }

  final static void renderSimple(Graphics3D g3d, Font3D font,
                                 String strLabel, short colix, short bgcolix,
                                 int x, int y, int z, int zSlab, int xOffset,
                                 int yOffset, int ascent, int descent,
                                 boolean doPointer, short pointerColix) {

    // old static style -- quick, simple, no line breaks, odd alignment?
    if (strLabel == null || strLabel.length() == 0)
      return;
    int x0 = x;
    int y0 = y;
    int boxWidth = font.fontMetrics.stringWidth(strLabel) + 8;
    int boxHeight = ascent + descent + 8;
    int xBoxOffset, yBoxOffset;
    
    // these are based on a standard |_ grid, so y is reversed.
    if (xOffset > 0) {
      xBoxOffset = xOffset;
    } else {
      xBoxOffset = -boxWidth;
      if (xOffset == 0)
        xBoxOffset /= 2;
      else
        xBoxOffset += xOffset;
    }

    if (yOffset > 0) {
      yBoxOffset = yOffset;
    } else {
      if (yOffset == 0)
        yBoxOffset = -boxHeight / 2 - 2;
      else
        yBoxOffset = -boxHeight + yOffset;
    }

    x += xBoxOffset;
    y += yBoxOffset;

    if (bgcolix != 0) {
      g3d.fillRect(bgcolix, x, y, z, zSlab, boxWidth, boxHeight);
      g3d.drawRect(colix, x + 1, y + 1, z - 1, zSlab, boxWidth - 2,
          boxHeight - 2);
    }
    
    if (doPointer) {
      if (xOffset > 0)
        g3d.drawLine(pointerColix, x0, y0, zSlab, x, y + boxHeight / 2, zSlab);
      else if (xOffset < 0)
        g3d.drawLine(pointerColix, x0, y0, zSlab, x + boxWidth, y + boxHeight
            / 2, zSlab);
    }
    g3d.drawString(strLabel, font, colix, x + 4, y + 4 + ascent, z - 1,
            zSlab);
  }
  
  String[] split(String text, char ch) {
    int n = 1;
    int i = text.indexOf(ch);
    String[] lines;
    if (i < 0) {
      lines = new String[1];
      lines[0] = text;
      return lines;
    }
    int len = text.length();
    for (; i < len; i++)
      if (text.charAt(i) == ch)
        n++;
    lines = new String[n];
    i = 0;
    len = 0;
    int pt = 0;
    for (; (len = text.indexOf(ch, i)) >= 0;) {
      lines[pt++] = text.substring(i, len);
      i = len + 1;
    }
    lines[pt] = text.substring(i, text.length());
    return lines;
  }
  
  String getState(boolean isDefine) {
    StringBuffer s = new StringBuffer();
    if (text == null || atomBased || target.equals("error"))
      return "";
    //set echo top left
    //set echo myecho x y
    //echo .....

    if (isDefine) {
      String strOff = null;
      switch (valign) {
      case XY:
        strOff = (movableXPercent == Integer.MAX_VALUE ? movableX + " "
            : movableXPercent + "% ");
        strOff += (movableYPercent == Integer.MAX_VALUE ? movableY + ""
            : movableYPercent + "%");
      //fall through
      case XYZ:
        if (strOff == null)
          strOff = StateManager.escape(xyz);
        s.append("set echo " + target + " " + strOff);
        if (align != LEFT)
          s.append("set echo " + target + " " + hAlignNames[align]);
        break;
      default:
        s.append("set echo " + vAlignNames[valign] + " " + hAlignNames[align]);
      }
      s.append(";echo " + StateManager.escape(text) + ";\n");
    }
    //isDefine and target==top: do all
    //isDefine and target!=top: just start
    //!isDefine and target==top: do nothing
    //!isDefine and target!=top: do just this
    //fluke because top is defined with default font
    //in initShape(), so we MUST include its font def here
    if (isDefine != target.equals("top"))
      return s.toString();
    // these may not change much:
    s.append(Shape.getFontCommand("echo", font) + ";\n");
    s.append("color echo [x" + g3d.getHexColorFromIndex(colix) + "]");
    if (bgcolix != 0)
      s.append(";background echo [x" + g3d.getHexColorFromIndex(bgcolix) + "]");
    s.append(";\n");
    return s.toString();
  }
}
