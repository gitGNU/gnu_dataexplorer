/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.utils;

import java.util.zip.Adler32;
import java.util.zip.CRC32;

/**
 * checksum calculation utility class
 * @author Winfried Brügmann
 */
public class Checksum {

	/**
	 * main method to test this class
	 * @param args
	 */
	public static void main(String[] args) {
		byte[] b = new byte[] { (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B,
				(byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1B, (byte) 0x06, (byte) 0x5C,
				(byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x00 };
		byte[] b1 = new byte[] { (byte) 0x1B, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C,
				(byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x1C, (byte) 0x06, (byte) 0x5C,
				(byte) 0x1C, (byte) 0x06, (byte) 0x5C, (byte) 0x07 };

		System.out.println("CRC16    = " + Integer.toHexString(Checksum.crc16(b)));
		System.out.println("CRC32 = " + Integer.toHexString(Checksum.CRC32(b)));
		System.out.println("Adler32  = " + Long.toHexString(Checksum.adler32(b)));
		System.out.println("ADD      = " + Integer.toHexString(Checksum.AND(b)));
		// Picolario seams to use XOR checksum
		System.out.println("XOR      = " + Integer.toHexString(Checksum.XOR(b)));
		System.out.println("XOR      = " + Integer.toHexString(Checksum.XOR(b1)));

	}

	/**
	 * generator polynomial
	 */
	private static final int	poly			= 0x1021;			/* x16 + x12 + x5 + 1 generator polynomial */
	/* 0x8408 used in European X.25 */

	/**
	* scrambler lookup table for fast computation.
	*/
	private static int[]			crcTable	= new int[256];
	static {
		// initialize lookup table
		for (int i = 0; i < 256; i++) {
			int fcs = 0;
			int d = i << 8;
			for (int k = 0; k < 8; k++) {
				if (((fcs ^ d) & 0x8000) != 0) {
					fcs = (fcs << 1) ^ poly;
				}
				else {
					fcs = (fcs << 1);
				}
				d <<= 1;
				fcs &= 0xffff;
			}
			crcTable[i] = fcs;
		}
	}

	/**
	 * calculate CRC with CCITT method.
	 * @param b byte array to compute CRC on
	 * @return 16-bit CRC, unsigned
	 */
	public static short crc16(byte[] b) {
		int work = 0xffff;
		for (int i = 0; i < b.length; i++) {
			// xor the next data byte with the high byte of what we have so far to
			// look up the scrambler.
			// xor that with the low byte of what we have so far.
			// Mask back to 16 bits.
			work = (crcTable[(b[i] ^ (work >>> 8)) & 0xff] ^ (work << 8)) & 0xffff;
		}
		return (short) work;
	}

	/**
	 * calculate CRC-32 with Java method
	 * @param b byte array to compute CRC on
	 * @return 32-bit CRC, signed
	 */
	public static int CRC32(byte[] b) {
		// create a new CRC-calculating object
		CRC32 crc = new CRC32();
		// loop, calculating CRC for each byte of the string
		for (int i = 0; i < b.length; i++) {
			crc.update(b[i]);
		}
		return (int) crc.getValue();
	}

	/**
	 * calculate Adler32 checksum
	 * @param b byte array to compute
	 * @return 64-bit result
	 */
	public static long adler32(byte[] b) {
		Adler32 digester = new Adler32();
		digester.update(b);
		return digester.getValue();
	}

	/**
	 * calculate OR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static byte OR(byte[] b) {
		int value = b[0];
		for (int i = 1; i < b.length - 1; i++) {
			value = value | b[i];
		}
		return (byte) value;
	}

	/**
	 * calculate XOR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static byte XOR(byte[] b) {
		int value = b[0];
		for (int i = 1; i < b.length - 1; i++) {
			value = value ^ b[i];
		}
		return (byte) value;
	}

	/**
	 * calculate XOR bit operation
	 * @param b byte array to compute
	 * @return 8-bit result
	 */
	public static byte AND(byte[] b) {
		int value = b[0];
		for (int i = 1; i < b.length - 1; i++) {
			value = value & b[i];
		}
		return (byte) value;
	}
}
