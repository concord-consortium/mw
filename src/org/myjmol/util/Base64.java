// Version 1.0a
// Copyright (C) 1998, James R. Weeks and BioElectroMech.
// Visit BioElectroMech at www.obrador.com.  Email James@obrador.com.

// See license.txt for details about the allowed used of this software.
// This software is based in part on the work of the Independent JPEG Group.
// See IJGreadme.txt for details about the Independent JPEG Group's license.

// This encoder is inspired by the Java Jpeg encoder by Florian Raemy,
// studwww.eurecom.fr/~raemy.
// It borrows a great deal of code and structure from the Independent
// Jpeg Group's Jpeg 6a library, Copyright Thomas G. Lane.
// See license.txt for details.

package org.myjmol.util;

public class Base64 {

	private static String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	public static StringBuffer getBase64(StringBuffer str) {
		return getBase64(toBytes(str));
	}

	public static StringBuffer getBase64(byte[] bytes) {
		long nBytes = bytes.length;
		StringBuffer sout = new StringBuffer();
		if (nBytes == 0)
			return sout;
		for (int i = 0, nPad = 0; i < nBytes && nPad == 0;) {
			if (false) {
				sout.append((char) bytes[i++]);
			}
			else {
				if (i % 75 == 0 && i != 0)
					sout.append("\r\n");
				nPad = (i + 2 == nBytes ? 1 : i + 1 == nBytes ? 2 : 0);
				int outbytes = (((bytes[i++]) << 16) & 0xFF0000) | ((nPad >= 1 ? 0 : (bytes[i++]) << 8) & 0x00FF00)
						| ((nPad == 2 ? 0 : (int) bytes[i++]) & 0x0000FF);

				sout.append(base64.charAt((outbytes >> 18) & 0x3F));

				sout.append(base64.charAt((outbytes >> 12) & 0x3F));
				sout.append(nPad == 2 ? '=' : base64.charAt((outbytes >> 6) & 0x3F));
				sout.append(nPad >= 1 ? '=' : base64.charAt(outbytes & 0x3F));
			}
		}
		return sout;
	}

	public static byte[] toBytes(StringBuffer sb) {
		byte[] b = new byte[sb.length()];
		for (int i = sb.length(); --i >= 0;)
			b[i] = (byte) sb.charAt(i);
		return b;
	}
}