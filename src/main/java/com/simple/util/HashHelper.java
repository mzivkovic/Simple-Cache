package com.simple.util;

/**
 * User * Date: 1/25/13
 * : milan.zivkovic
 * Time: 10:11 AM
 */

/**
 * Class that is responsible for calculating hash values
 */
public class HashHelper {

//    public static int getHash(int value, int mask){
////        value += ~(value<<15);
////        value ^=  (value>>10);
////        value +=  (value<<3);
////        value ^=  (value>>6);
////        value += ~(value<<11);
////        value ^=  (value>>16);
//        return value & mask;
//    }


	public static int getHash ( final int value, final int mask ) {
		return ( value & 0x7fffffff ) & mask;
	}

	public static int getHash ( final long value, final int mask ) {
		return ( ( int ) ( value ^ ( value >>> 32 ) ) );
	}


	public static int getHash ( final float value, final int mask ) {
		return ( Float.floatToIntBits( value * 663608941.737f ) ) & mask;
	}

	public static int getHash ( final double value, final int mask ) {
		long bits = Double.doubleToLongBits( value );
		return ( ( int ) ( bits ^ ( bits >>> 32 ) ) ) & mask;
	}

	public static int getHash ( final byte value, final int mask ) {
		return value & mask;
	}

	public static int getHash ( final short value, final int mask ) {
		return value & mask;
	}

	public static int getHash ( final char value, final int mask ) {
		return value & mask;
	}

}
