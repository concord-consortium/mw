/*
 *   Copyright (C) 2006  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * END LICENSE */

package org.concord.jmol;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.concord.modeler.ConnectionManager;
import org.concord.modeler.util.FileUtilities;
import org.concord.modeler.util.GifDecoder;

class ImageComponent {

	public final static int FRONT = 1;
	public final static int BACK = 2;

	private GifDecoder gifDecoder;
	private boolean selected;
	private int layer = FRONT;
	private Image[] images;
	private double x, y;
	private int frameCounter;
	private long previousFrameTime, currentTime;
	private URL url;
	private String filename;

	public ImageComponent(URL url) throws IOException {
		if (url == null)
			throw new IllegalArgumentException("You must pass in a valid URI");
		if (url.getFile().toLowerCase().endsWith(".gif")) {
			gifDecoder = new GifDecoder();
			gifDecoder.read(url);
			images = gifDecoder.getImages();
		}
		else {
			images = new Image[1];
			images[0] = Toolkit.getDefaultToolkit().createImage(url);
		}
		this.url = url;
	}

	public ImageComponent(String filename) throws IOException {
		if (filename == null)
			throw new IllegalArgumentException("You must pass in a valid file name");
		if (ConnectionManager.sharedInstance().isCachingAllowed()) {
			if (FileUtilities.isRemote(filename)) {
				File file = ConnectionManager.sharedInstance().shouldUpdate(filename);
				if (file == null)
					file = ConnectionManager.sharedInstance().cache(filename);
				if (file != null)
					filename = file.toString();
			}
		}
		if (filename.toLowerCase().endsWith(".gif")) {
			gifDecoder = new GifDecoder();
			gifDecoder.read(filename);
			images = gifDecoder.getImages();
		}
		else {
			images = new Image[1];
			if (FileUtilities.isRemote(filename)) {
				try {
					images[0] = Toolkit.getDefaultToolkit().createImage(new URL(filename));
				}
				catch (MalformedURLException mue) {
					mue.printStackTrace(System.err);
				}
			}
			else {
				images[0] = Toolkit.getDefaultToolkit().createImage(filename);
			}
		}
		try {
			url = new URL(filename);
		}
		catch (Exception e) {
			url = null;
			this.filename = filename;
		}
	}

	/** return the index of the current frame. */
	public int getCurrentFrame() {
		return frameCounter;
	}

	public void setCurrentFrame(int i) {
		frameCounter = i;
	}

	/** return the number of frames. */
	public int getFrames() {
		if (images == null)
			return 0;
		return images.length;
	}

	/** show the next frame of this animated image. */
	public void nextFrame() {

		int n = images.length;
		if (n <= 1)
			return;
		if (gifDecoder == null)
			return;

		currentTime = System.currentTimeMillis();
		if (previousFrameTime != 0L) {
			if (currentTime - previousFrameTime >= gifDecoder.getDelay(frameCounter)) {
				frameCounter++;
				/*
				 * if(frameCounter==n) { if(loopCount==0){ frameCounter=0; } else { if(loopCounter<loopCount-1){
				 * frameCounter=0; loopCounter++; } else { frameCounter=n-1; } } }
				 */
				previousFrameTime = currentTime;
			}
		}
		else {
			previousFrameTime = currentTime;
		}

	}

	/**
	 * render this image or image sets onto a graphics context, with the specified component <code>c</code> as the
	 * <code>ImageObserver</code>.
	 */
	public void paint(Component c, Graphics g) {
		if (images == null)
			return;
		int n = images.length;
		if (n <= 0)
			return;
		if (n == 1) {
			if (images[0] != null)
				g.drawImage(images[0], (int) x, (int) y, c);
		}
		else {
			g.drawImage(images[frameCounter], (int) getXFrame(), (int) getYFrame(), c);
		}
	}

	/** restart painting from the first frame, and restore the loop count */
	public void reset() {
		frameCounter = 0;
	}

	/** return the current x coordinate of this image */
	public double getRx() {
		return x;
	}

	/** return the current y coordinate of this image */
	public double getRy() {
		return y;
	}

	/** set the x coordinate of this image */
	public void setRx(double x) {
		this.x = x;
	}

	/** set the y coordinate of this image */
	public void setRy(double y) {
		this.y = y;
	}

	/** set the location of this image */
	public void setLocation(double x, double y) {
		setRx(x);
		setRy(y);
	}

	/** same as <code>setLocation(double, double)</code> */
	public void translateTo(double rx, double ry) {
		setLocation(rx, ry);
	}

	public void translateBy(double dx, double dy) {
		x += dx;
		y += dy;
	}

	/** return the location of this image */
	public Point getLocation() {
		return new Point((int) x, (int) y);
	}

	/** return the width of the current frame. */
	public int getWidth() {
		if (images == null)
			return 0;
		if (images.length == 0)
			return 0;
		if (images[0] == null)
			return 0;
		if (images.length == 1)
			return images[0].getWidth(null);
		return images[frameCounter].getWidth(null);
	}

	/** return the height of the current frame. */
	public int getHeight() {
		if (images == null)
			return 0;
		if (images.length == 0)
			return 0;
		if (images[0] == null)
			return 0;
		if (images.length == 1)
			return images[0].getHeight(null);
		return images[frameCounter].getHeight(null);
	}

	/** @return the width of the whole image. */
	public int getGlobalWidth() {
		if (images == null)
			return 0;
		if (images.length == 0)
			return 0;
		if (images[0] == null)
			return 0;
		if (images.length == 1)
			return images[0].getWidth(null);
		if (gifDecoder == null)
			return 0;
		return gifDecoder.getLogicalScreenWidth();
	}

	/** @return the height of the whole image. */
	public int getGlobalHeight() {
		if (images == null)
			return 0;
		if (images.length == 0)
			return 0;
		if (images[0] == null)
			return 0;
		if (images.length == 1)
			return images[0].getHeight(null);
		if (gifDecoder == null)
			return 0;
		return gifDecoder.getLogicalScreenHeight();
	}

	public void setSelected(boolean b) {
		selected = b;
	}

	public boolean isSelected() {
		return selected;
	}

	/** @return the x coordinate of the upper-left corner of the current frame. */
	public double getXFrame() {
		if (images.length == 1)
			return x;
		if (gifDecoder == null)
			return x;
		return x + gifDecoder.getXOffset(frameCounter);
	}

	/** @return the y coordinate of the upper-left corner of the current frame. */
	public double getYFrame() {
		if (images.length == 1)
			return y;
		if (gifDecoder == null)
			return y;
		return y + gifDecoder.getYOffset(frameCounter);
	}

	public boolean contains(double rx, double ry) {
		if (images == null)
			return false;
		if (images.length == 0)
			return false;
		if (images[0] == null)
			return false;
		return rx >= getXFrame() && rx <= getXFrame() + images[0].getWidth(null) && ry >= getYFrame()
				&& ry <= getYFrame() + images[0].getHeight(null);
	}

	public void setLayer(int i) {
		layer = i;
	}

	public int getLayer() {
		return layer;
	}

	public URL getURL() {
		return url;
	}

	public String toString() {
		if (url != null)
			return url.toString();
		return filename;
	}

}
