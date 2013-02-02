package com.simple;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

/**
 * User: mzivkovic
 * Date: 1/27/13
 * Time: 1:58 PM
 */
public class CollectionGenerator {

	//TODO this should be read from property file
	private static final String SIMPLE_TEMPLATE_NAME = "SimpleTemplate.java";

	public static class TypeDescription {

		private final String type;
		private final String size;
		private final String unsafeType;
		private final String emptyIndicator;


		public TypeDescription ( String type, String size, String unsafeType, String emptyIndicator ) {
			this.type = type;
			this.size = size;
			this.unsafeType = unsafeType;
			this.emptyIndicator = emptyIndicator;
		}

		public String getType () {
			return type;
		}

		public String getSize () {
			return size;
		}

		public String getUnsafeType () {
			return unsafeType;
		}

		public String getEmptyIndicator () {
			return emptyIndicator;
		}
	}

	public static final TypeDescription[] SIMPLE_TYPES = {
			new TypeDescription( "int", "4", "Int", "-1" ),
			new TypeDescription( "long", "8", "Long", "-1" ),
			new TypeDescription( "byte", "1", "Byte", "-1" ),
			new TypeDescription( "short", "2", "Short", "-1" ),
			new TypeDescription( "float", "4", "Float", "-1" ),
			new TypeDescription( "double", "8", "Double", "-1" ),
			new TypeDescription( "char", "2", "Char", "'\\uffff'" )
	};


	public static void main ( String[] args ) throws IOException {

		if ( args.length < 2 ) {
			throw new IllegalArgumentException( "Need to have template directory and source directory" );
		}

		String templatePath = args[ 0 ];
		String sourcePath = args[ 1 ];

		generateSimpleTypes( templatePath, sourcePath );
		System.out.println( args[ 0 ] + args[ 1 ] );

	}

	private static void generateSimpleTypes ( final String templatePath, final String sourcePath ) throws IOException {

		for ( TypeDescription key : SIMPLE_TYPES ) {
			for ( TypeDescription value : SIMPLE_TYPES ) {
				String content = readFile( templatePath + "/" + SIMPLE_TEMPLATE_NAME );
				generateSimpleCollection( key, value, content, sourcePath );
			}
		}

	}

	private static void generateSimpleCollection ( final TypeDescription key, final TypeDescription value, String content, final String sourcePath ) throws IOException {

		content = content.replace( "${VALUE_SIZE}", value.getSize() );
		content = content.replace( "${VALUE_TYPE}", value.getType() );
		content = content.replace( "${UNSAFE_ENTRY_TYPE}", value.getUnsafeType() );
		content = content.replace( "${VALUE_EMPTY_INDICATOR}", value.getEmptyIndicator() );

		content = content.replace( "${KEY_SIZE}", key.getSize() );
		content = content.replace( "${KEY_TYPE}", key.getType() );
		content = content.replace( "${UNSAFE_KEY_TYPE}", key.getUnsafeType() );

		String name = "Simple" + key.getUnsafeType() + value.getUnsafeType() + "Hash";
		writeStringToFile( sourcePath, name, content );
		System.out.println( "Generated:" + name );

	}

	private static void writeStringToFile ( final String sourcePath, final String name, final String content ) throws IOException {
		PrintWriter out = null;
		File file = null;
		try {

			file = new File( sourcePath + "/" + name + ".java" );
			if ( !file.exists()){
				file.createNewFile();
			}
			out = new PrintWriter( file );
			out.println( content );

		} finally {
			if ( out != null ) {
				out.flush();
				out.close();
			}

		}
	}

	private static String readFile ( final String path ) throws IOException {
		FileInputStream stream = new FileInputStream( new File( path ) );
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map( FileChannel.MapMode.READ_ONLY, 0, fc.size() );
			return Charset.defaultCharset().decode( bb ).toString();
		} finally {
			stream.close();
		}
	}


}
