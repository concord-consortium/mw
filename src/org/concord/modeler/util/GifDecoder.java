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

package org.concord.modeler.util;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.concord.modeler.text.Page;
import org.w3c.dom.Node;

/**
 * This GIF decoder is able to read animated GIF files.
 * 
 * @author Charles Xie
 */

public class GifDecoder {

	private static ImageReader reader;
	private int logicalScreenWidth, logicalScreenHeight;
	private Image[] images;
	private Point[] offsets;
	private int[] delays;
	private String[] disposalMethods;

	public GifDecoder() throws IOException {
		if (reader == null) {
			Iterator it = ImageIO.getImageReadersBySuffix("gif");
			if (!it.hasNext())
				throw new IOException("No gif reader was found.");
			reader = (ImageReader) it.next();
			if (it.hasNext())
				System.out.println("Extra gif readers were found.");
		}
	}

	public void read(String path) throws IOException {
		if (path == null)
			return;
		if (Page.isApplet() || FileUtilities.isRemote(path)) {
			URL url = null;
			try {
				url = new URL(path);
			}
			catch (MalformedURLException e) {
			}
			if (url != null)
				read(url);
		}
		else {
			read(new File(path));
		}
	}

	public void read(URL url) throws IOException {
		read(ImageIO.createImageInputStream(url.openStream()));
	}

	public void read(File file) throws IOException {
		read(ImageIO.createImageInputStream(file));
	}

	public int getFrameCount() {
		return images == null ? 0 : images.length;
	}

	public Image[] getImages() {
		return images;
	}

	/** The time to delay between frames for the i-th frame, in hundredths of a second. */
	public int getDelay(int i) {
		if (delays == null)
			return 0;
		if (i >= 0 && i < delays.length)
			return delays[i];
		if (i < 0)
			return delays[0];
		if (i >= delays.length && delays.length > 0)
			return delays[delays.length - 1];
		return 0;
	}

	public int getLogicalScreenWidth() {
		return logicalScreenWidth;
	}

	public int getLogicalScreenHeight() {
		return logicalScreenHeight;
	}

	public int getXOffset(int i) {
		if (offsets == null)
			return 0;
		if (i >= 0 && i < offsets.length) {
			if (offsets[i] == null)
				return 0;
			return offsets[i].x;
		}
		if (i < 0) {
			if (offsets[0] == null)
				return 0;
			return offsets[0].x;
		}
		if (i >= offsets.length && offsets.length > 0) {
			if (offsets[offsets.length - 1] == null)
				return 0;
			return offsets[offsets.length - 1].x;
		}
		return 0;
	}

	public int getYOffset(int i) {
		if (offsets == null)
			return 0;
		if (i >= 0 && i < offsets.length) {
			if (offsets[i] == null)
				return 0;
			return offsets[i].y;
		}
		if (i < 0) {
			if (offsets[0] == null)
				return 0;
			return offsets[0].y;
		}
		if (i >= offsets.length && offsets.length > 0) {
			if (offsets[offsets.length - 1] == null)
				return 0;
			return offsets[offsets.length - 1].y;
		}
		return 0;
	}

	public String getDisposalMethod(int i) {
		if (disposalMethods == null)
			return "none";
		if (i >= 0 && i < disposalMethods.length)
			return disposalMethods[i];
		if (i < 0)
			return disposalMethods[0];
		if (i >= disposalMethods.length && disposalMethods.length > 0)
			return disposalMethods[disposalMethods.length - 1];
		return "none";
	}

	public void dispose() {
		reader.dispose();
		if (images != null) {
			for (int i = 0; i < images.length; i++)
				images[i].flush();
		}
	}

	private void getStreamAttributes() throws IOException {
		IIOMetadata meta = reader.getStreamMetadata();
		Node root = meta.getAsTree("javax_imageio_gif_stream_1.0");
		String name;
		for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
			name = c.getNodeName();
			if (c instanceof IIOMetadataNode) {
				IIOMetadataNode metaNode = (IIOMetadataNode) c;
				if ("LogicalScreenDescriptor".equals(name)) {
					logicalScreenWidth = parseInt(metaNode.getAttribute("logicalScreenWidth"), -1);
					logicalScreenHeight = parseInt(metaNode.getAttribute("logicalScreenHeight"), -1);
				}
			}
		}
	}

	private void getImageAttributes(int i) throws IOException {
		IIOMetadata meta = reader.getImageMetadata(i);
		Node root = meta.getAsTree("javax_imageio_gif_image_1.0");
		String name;
		for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
			name = c.getNodeName();
			if (c instanceof IIOMetadataNode) {
				IIOMetadataNode metaNode = (IIOMetadataNode) c;
				if ("ImageDescriptor".equals(name)) {
					int x = parseInt(metaNode.getAttribute("imageLeftPosition"), -1);
					int y = parseInt(metaNode.getAttribute("imageTopPosition"), -1);
					if (offsets[i] == null) {
						offsets[i] = new Point(x, y);
					}
					else {
						offsets[i].setLocation(x, y);
					}
				}
				else if ("GraphicControlExtension".equals(name)) {
					delays[i] = parseInt(metaNode.getAttribute("delayTime"), 0);
					disposalMethods[i] = metaNode.getAttribute("disposalMethod");
				}
			}
		}
	}

	private static int parseInt(String s, int defaultValue) {
		if (s == null)
			return defaultValue;
		try {
			return Integer.parseInt(s);
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private void read(ImageInputStream iis) throws IOException {
		reader.reset();
		reader.setInput(iis);
		int n = 0;
		try {
			n = reader.getNumImages(true);
		}
		catch (IllegalStateException e) {
			e.printStackTrace();
			return;
		}
		getStreamAttributes();
		images = new BufferedImage[n];
		offsets = new Point[n];
		delays = new int[n];
		disposalMethods = new String[n];
		for (int i = 0; i < n; i++) {
			images[i] = reader.read(i);
			getImageAttributes(i);
		}
	}

}