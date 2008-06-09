/* $RCSfile: JmolStatusListener.java,v $
 * $Author: qxie $
 * $Date: 2006-11-29 22:46:13 $
 * $Revision: 1.10 $
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

package org.myjmol.api;

public interface JmolStatusListener {
/*
 * These methods specifically provide notification from 
 * Viewer.StatusManager to the two main classes, applet or app.
 * so that they can handle them slightly differently. This might be
 * a callback for the applet, for example, but not for the app.
 * ALL viewer-type processing, including status queue writing
 * has been done PRIOR to these functions being called.   Bob Hanson
 * 
 */
  public void handlePopupMenu(int x, int y);

  public void notifyAtomPicked(int atomIndex, String strInfo);

  public void notifyAtomHovered(int atomIndex, String strInfo);

  public void notifyFileLoaded(String fullPathName, String fileName,
      String modelName, Object clientFile,
      String errorMessage);

  public void notifyFrameChanged(int frameNo);

  public void notifyNewDefaultModeMeasurement(int count, String strInfo);

  public void notifyNewPickingModeMeasurement(int iatom, String strMeasure);

  public void notifyScriptStart(String statusMessage, String additionalInfo);
  
  public void notifyScriptTermination(String statusMessage, int msWalltime);

  public void sendConsoleEcho(String strEcho);

  public void sendConsoleMessage(String strStatus);

  public void sendSyncScript(String script, String appletName);

  public void showUrl(String url);

  public void showConsole(boolean showConsole);
  
  public float functionXY(String functionName, int x, int y);
  
  public String eval(String strEval);
  
  public void setCallbackFunction(String callbackType, String callbackFunction);
  
  public void createImage(String file, String type, int quality);
  
}
