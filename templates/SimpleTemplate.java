package com.simple.collection;

import sun.misc.Unsafe;
import com.simple.util.HashHelper;
import com.simple.util.Util;

/**
 * User: milan.zivkovic
 */
public class Simple${UNSAFE_KEY_TYPE}${UNSAFE_ENTRY_TYPE}Hash {

	private static final Unsafe unsafe = Util.getUnsafe();

	private static long offset = 0;

	private static final long hopInfoOffset = offset += 0;
	private static final long overflowOffset = offset += 4;
	private static final long entryOffset = offset += 4;
	private static final long keyOffset = offset += ${VALUE_SIZE};
	private static final long helperEntryOffset = offset += ${KEY_SIZE};
	private static final long helperKeyOffset = offset +=  ${VALUE_SIZE};
	private static final long objectSize = offset += ${KEY_SIZE};

	private final long address;

	//TODO Consider changing this to long
	private final int size;
	private final int mask;
	private final int h;

	private int helperFreePointer;

	private static ${VALUE_TYPE} emptyIndicator = ${VALUE_EMPTY_INDICATOR};
	private static final int NOT_FOUND_INDICATOR = Integer.MIN_VALUE;


	private static int[] MULTIPLY_DEBRUJIN_BIT_POSITION_LOOKUP = { 0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9 };

	public Simple${UNSAFE_KEY_TYPE}${UNSAFE_ENTRY_TYPE}Hash ( int size, int h ) {
		this( size, h, emptyIndicator );
	}


	public Simple${UNSAFE_KEY_TYPE}${UNSAFE_ENTRY_TYPE}Hash ( final int size, final int h, final ${VALUE_TYPE} emptyValue ) {
		this.size = size;
		this.h = h;
		this.mask = size - 1;
		emptyIndicator = emptyValue;
		address = unsafe.allocateMemory( size * objectSize );
		for ( int i = 0; i < size * objectSize; i++ )
			unsafe.putByte( address + i, ( byte ) 0x00 );
		for ( int i = 0; i < size; i++ ) {
			setOverflow( address, i, 0 );
			setEntry( address, i, emptyIndicator );
			setHelperEntry( address, i, emptyIndicator );
		}

	}



	public void put ( final ${KEY_TYPE} key, final ${VALUE_TYPE} item ) {
		final int hashEntry = getHashValue( key, mask );
		int existingPosition = getPlaceToOEntryIfExists( address, size, mask, h, key, hashEntry );

		if ( existingPosition == NOT_FOUND_INDICATOR ) {
				insertNewItem(key,item,hashEntry);
		} else if ( existingPosition >= 0 ) {
				setEntry( address, existingPosition, item );
		} else if ( existingPosition < 0 ) {
				setHelperEntry( address,existingPosition,item );
		}

	}



	private  void insertNewItem ( final ${KEY_TYPE} key, final ${VALUE_TYPE} item, final int hashEntry ) {
		if ( getEntry( address, hashEntry ) == emptyIndicator ) {
			setEntry( address, hashEntry, key, item );
			updateHopInfo( address, hashEntry, 0 );
			return;
		}

		int emptyEntry = hashEntry;
		while ( getEntry( address, emptyEntry ) != emptyIndicator ) {
			emptyEntry = nextEntry( mask, emptyEntry );
		}

		while ( emptyEntry != -1 && isFreeEntryFound( h, size, hashEntry, emptyEntry ) ) {

			int replacement = -1;
			for ( int candidate = calculateStartCandidateIndex( h, size, emptyEntry ), i = 0; replacement == -1 && i < h; candidate = nextEntry( mask, candidate ), i++ ) {
				if ( getHopInfo( address, candidate ) != 0 ) {
					int candidateReplacement = mask & ( getReplacement( address, candidate ) + candidate );
					if ( candidateReplacement < emptyEntry && candidateReplacement >= candidate ) {
						replacement = candidateReplacement;
						switchEntries( address, emptyEntry, replacement );
						updateHopSetClear( address, candidate, calculateNewBit( size, emptyEntry, candidate ), calculateNewBit( size, replacement, candidate ) );
					}
				}
			}
			emptyEntry = replacement;
		}

		if ( emptyEntry == -1 ) {
			while ( getHelperEntry( address, helperFreePointer ) != emptyIndicator ) {
				helperFreePointer = nextEntry( mask, helperFreePointer );
			}
			setHelperEntry( address, helperFreePointer, key, item );
			setOverflow( address, hashEntry, 1 );
		} else {
			setEntry( address, emptyEntry, key, item );
			updateHopInfo( address, emptyEntry, hashEntry );
		}

	}


	public ${VALUE_TYPE} get ( final ${KEY_TYPE} key ) {

		return get( key, getHashValue( key, mask ) );
	}

	public ${VALUE_TYPE} get ( final ${KEY_TYPE} key, final int hashEntry ) {
		int allEntries = getHopInfo( address, hashEntry );
		if ( allEntries == 0  )
			return emptyIndicator;
        else if ( allEntries == 1 && getKey( address, hashEntry ) == key)
		        return getEntry( address, hashEntry );
		int counter = 0;
		while ( allEntries != 0 ) {
			if ( ( allEntries & 0x1 ) != 0 ) {
				int index = mask & ( hashEntry + counter );
				if ( getKey( address, index ) == key )
					return getEntry( address, index );
			}
			if ( counter++ >= h )
				break;
			allEntries >>>= 1;
		}

		if ( getOverflow( address, hashEntry ) != 0 ) {
			for ( int i = 0; i < size; i++ ) {
				if ( getHelperKey( address, i ) == key ) {
					return getHelperEntry( address, i );
				}
			}
		}
		return emptyIndicator;
	}

	/**
	 * @param key       Key that is being inserted
	 * @param hashEntry calculated hashValue for the key
	 * @return 0 if key does not exist. Value larger than 0 if the position is not in the helper array regular place,
	 *         value less then zero if the position is in the helper array in this case position should be treated as an absolute value;
	 */
	private static int getPlaceToOEntryIfExists ( final long address, final int size, final int mask, final int h, final ${KEY_TYPE} key, final int hashEntry ) {
		int allEntries = getHopInfo( address, hashEntry );
		if ( allEntries == 0 )
			return NOT_FOUND_INDICATOR;
		else if ( allEntries == 1 && getKey( address, hashEntry ) == key ) {
			return hashEntry;
		}

		int counter = 0;
		while ( allEntries != 0 ) {
			if ( ( allEntries & 0x1 ) != 0 ) {
				int index = mask & ( hashEntry + counter );
				if ( getKey( address, index ) == key ) {
					return index;
				}
			}
			if ( counter++ >= h )
				break;
			allEntries >>>= 1;
		}

		if ( getOverflow( address, hashEntry ) != 0 ) {
			for ( int i = 0; i < size; i++ ) {
				if ( getHelperKey( address, i ) == key ) {
					return -i;
				}
			}
		}
		return NOT_FOUND_INDICATOR;
}


	private static void setHelperEntry ( final long address, final int index, final ${KEY_TYPE} key, final ${VALUE_TYPE} item ) {
		setHelperEntry( address, index, item );
		setHelperKey( address, index, key );
	}

	private static void updateHopInfo ( final long address, final int index, final int position ) {
		int hopInfo = getHopInfo( address, index );
		setHopInfo( address, index, hopInfo | ( 1 << position ) );
	}

	private static void setEntry ( final long address, final int position, final ${KEY_TYPE} key, final ${VALUE_TYPE} value ) {
		setKey( address, position, key );
		setEntry( address, position, value );
	}

	private static int getHashValue ( final ${KEY_TYPE} key, final int mask ) {
		return HashHelper.getHash( key, mask);
	}

	public static int nextEntry ( final int mask, final int entry ) {
		return mask & ( entry + 1 );
	}

	private static boolean isFreeEntryFound ( final int h, final int size, final int hashEntry, int emptyEntry ) {
		if ( emptyEntry < hashEntry ) {
			emptyEntry += size;
		}
		return emptyEntry - h + 1 > hashEntry;
	}

	private static int calculateStartCandidateIndex ( final int h, final int size, final int startIndex ) {
		int candidate = startIndex - h + 1;
		if ( candidate < 0 ) {
			candidate += size;
		}
		return candidate;
	}

	public int getReplacement ( final long address, final int candidate ) {
		int hop = getHopInfo( address, candidate );
		if(hop==0) {
			throw new IllegalArgumentException("Candidate hopInfo was 0!");
		}
		return MULTIPLY_DEBRUJIN_BIT_POSITION_LOOKUP[ ( ( hop & -hop ) * 0x077CB531 ) >>> 27 ] + 1;

	}

	private static void switchEntries ( final long address, final int emptyEntry, final int replacement ) {
		setEntry( address, emptyEntry, getEntry( address, replacement ) );
		setKey( address, emptyEntry, getKey( address, replacement ) );
	}

	private static int calculateNewBit ( final int size, final int replacement, final int entry ) {
		int calculated = replacement - entry;
		return calculated >= 0 ? calculated : calculated + size;
	}

	private static void updateHopSetClear ( final long address, final int entry, final int positionToSet, final int positionToClear ) {
		setHopInfo( address, entry, getHopInfo( address, entry ) | ( 1 << positionToSet ) );
		setHopInfo( address, entry, getHopInfo( address, entry ) & ~( 1 << positionToClear ) );
	}

	//---Begin Of Heap lookup----------------------------------------------------------------------------------------------------


	private static ${VALUE_TYPE} getEntry ( final long address, final int index ) {
		return unsafe.get${UNSAFE_ENTRY_TYPE}( address + index * objectSize + entryOffset );
	}

	private static ${KEY_TYPE} getKey ( final long address, final int index ) {
		return unsafe.get${UNSAFE_KEY_TYPE}( address + index * objectSize + keyOffset );
	}

	private static void setEntry ( final long address, final int index, final ${VALUE_TYPE} value ) {
		unsafe.put${UNSAFE_ENTRY_TYPE}( address + index * objectSize + entryOffset, value );
	}

	private static void setKey ( final long address, final int index, final ${KEY_TYPE} value ) {
		unsafe.put${UNSAFE_KEY_TYPE}( address + index * objectSize + keyOffset, value );
	}

	private static int getHopInfo ( final long address, final int index ) {
		return unsafe.getInt( address + index * objectSize + hopInfoOffset );
	}

	private static void setHopInfo ( final long address, final int index, final int value ) {
		unsafe.putInt( address + index * objectSize + hopInfoOffset, value );
	}

	private static ${VALUE_TYPE} getHelperEntry ( final long address, final int index ) {
		return unsafe.get${UNSAFE_ENTRY_TYPE}( address + index * objectSize + helperEntryOffset );
	}

	private static ${KEY_TYPE} getHelperKey ( final long address, final int index ) {
		return unsafe.get${UNSAFE_KEY_TYPE}( address + index * objectSize + helperKeyOffset );
	}

	private static void setHelperEntry ( final long address, final int index, final ${VALUE_TYPE} value ) {
		unsafe.put${UNSAFE_ENTRY_TYPE}( address + index * objectSize + helperEntryOffset, value );
	}

	private static void setHelperKey ( final long address, final int index, final ${KEY_TYPE} value ) {
		unsafe.put${UNSAFE_KEY_TYPE}( address + index * objectSize + helperKeyOffset, value );
	}

	private static int getOverflow ( final long address, final int index ) {
		return unsafe.getInt( address + index * objectSize + overflowOffset );
	}

	private static void setOverflow ( final long address, final int index, final int value ) {
		unsafe.putInt( address + objectSize * index + overflowOffset, value );
	}
	//---End Of Heap lookup------------------------------------------------------------------------------------------------------

}
