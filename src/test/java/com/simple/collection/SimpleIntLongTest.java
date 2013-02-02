package com.simple.collection;

import org.junit.Test;

/**
 * Author: Milan Zivkovic
 */
public class SimpleIntLongTest {


	@Test
	/**
	 * Dummy test , should be deleted
	 */
	public void correctnessTest(){

		int size = 256;
		SimpleIntLongHash hash = new SimpleIntLongHash( size,32 );


		for ( int i=0;i<size;i++){
			hash.put( i,i );
		}

		for ( int i=0;i<size;i++){
			System.out.println( hash.get( i ));
			System.out.println(hash.remove( i ));
			System.out.println( "Size:"+hash.getSize());
		}

		for ( int i=256;i< 256+size;i++){
			hash.put( i,i );
		}


		for ( int i=256;i< 256+size;i++){
			System.out.println(hash.get( i ));
		}




	}

}
