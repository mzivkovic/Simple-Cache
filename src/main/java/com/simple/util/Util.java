package com.simple.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * User: milan.zivkovic
 * Date: 2/2/13
 * Time: 12:06 AM
 */
public class Util {
	private static final Unsafe UNSAFE;

	static {
		try {
			Field field = Unsafe.class.getDeclaredField( "theUnsafe" );
			field.setAccessible( true );
			UNSAFE = ( Unsafe ) field.get( null );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	public static Unsafe getUnsafe(){
		return  UNSAFE;
	}

}
