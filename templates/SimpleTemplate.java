package com.simple.collection;

import sun.misc.Unsafe;
import com.simple.util.HashHelper;
import com.simple.util.Util;

/**
 * author: Milan Zivkovic
 */
public class Simple${UNSAFE_KEY_TYPE}${UNSAFE_ENTRY_TYPE}Hash {

	private static final Unsafe unsafe = Util.getUnsafe();


	//***Begin Off Heap variables**********************************************************************************************************
	private static long offset = 0;

	/**
	 * Offset that is being used to get the hopInfo field
	 */
	private static final long HOP_INFO_OFFSET = offset += 0;
	/**
	 * Offset that is being used to get the overflow field
	 */
	private static final long OVERFLOW_OFFSET = offset += 4;
	/**
	 * Offset that is being used to get the value field
	 */
	private static final long VALUE_OFFSET = offset += 4;
	/**
	 * Offset that is being used to get the key field
	 */
	private static final long KEY_OFFSET = offset += ${VALUE_SIZE};
	/**
	 * Offset that is being used to get the helper value field
	 */
	private static final long HELPER_VALUE_OFFSET = offset += ${KEY_SIZE};
	/**
	 * Offset that is being used to get the helper key field
	 */
	private static final long HELPER_KEY_OFFSET = offset += ${VALUE_SIZE};
	/**
	 * Size of one element in the off heap ring buffer
	 */
	private static final long OBJECT_SIZE = offset += ${KEY_SIZE};

	/**
	 * Pointer to the off heap ring buffer
	 */
	private final long OFF_HEAP_POINTER;


	//***End Off Heap variables**********************************************************************************************************





	//TODO Consider changing these to long
	/**
	 * Capacity of the ring buffer. It must be always power of two
	 */
	private final int capacity;
	/**
	 * Mask used to calculate fast mod operation
	 */
	private final int mask;
	/**
	 * Size of h entries that are being followed in hop info variable
	 */
	private final int h;

	/**
	 * Represent the number of used entries in the ring buffer
	 */
	private int size;

	/**
	 * Pointer to the free slot in helper ring buffer
	 */
	private int helperFreePointer;

	/**
	 * Indicate that the value is empty
	 */
	private static ${VALUE_TYPE} EMPTY_VALUE_INDICATOR = ${VALUE_EMPTY_INDICATOR};
	/**
	 * Indicate that the key is empty
	 */
	private static ${KEY_TYPE} EMPTY_KEY_INDICATOR = ${KEY_EMPTY_INDICATOR};
	/**
	 * Indicate that key was not found
	 */
	private static final int NOT_FOUND_INDICATOR = Integer.MIN_VALUE;

	/**
	 * Helper lookup array that is used to calculate position of least significant bit that is set.
	 * More information on http://graphics.stanford.edu/~seander/bithacks.html
	 */
	private static int[] MULTIPLY_DEBRUJIN_BIT_POSITION_LOOKUP = { 0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9 };

	public Simple${UNSAFE_KEY_TYPE}${UNSAFE_ENTRY_TYPE}Hash (  final int capacity, final int h ) {
		this( capacity, h, EMPTY_VALUE_INDICATOR );
	}


	public Simple${UNSAFE_KEY_TYPE}${UNSAFE_ENTRY_TYPE}Hash ( final int capacity, final int h, final ${VALUE_TYPE} emptyValue ) {
		this.capacity = capacity;
		this.h = h;
		this.mask = capacity - 1;
		EMPTY_VALUE_INDICATOR = emptyValue;
		OFF_HEAP_POINTER = unsafe.allocateMemory( capacity * OBJECT_SIZE );
		for ( int i = 0; i < capacity * OBJECT_SIZE; i++ )
				unsafe.putByte( OFF_HEAP_POINTER + i, ( byte ) 0x00 );
		for ( int i = 0; i < capacity; i++ ) {
				setOverflow( OFF_HEAP_POINTER, i, 0 );
		setEntry( OFF_HEAP_POINTER, i, EMPTY_VALUE_INDICATOR );
		setHelperEntry( OFF_HEAP_POINTER, i, EMPTY_VALUE_INDICATOR );

		setKey( OFF_HEAP_POINTER, i, EMPTY_KEY_INDICATOR );
		setHelperKey( OFF_HEAP_POINTER, i, EMPTY_KEY_INDICATOR );
		}
	}


	public void put ( final ${KEY_TYPE} key, final ${VALUE_TYPE} item ) {
		final int hashEntry = getHashValue( key, mask );
		int existingPosition = getPlaceToOEntryIfExists( OFF_HEAP_POINTER, capacity, mask, h, key, hashEntry );

		if ( existingPosition == NOT_FOUND_INDICATOR ) {
				insertNewItem( key, item, hashEntry );
		size++;
		} else if ( existingPosition >= 0 ) {
				setEntry( OFF_HEAP_POINTER, existingPosition, item );
		} else if ( existingPosition < 0 ) {
				setHelperEntry( OFF_HEAP_POINTER, existingPosition, item );
		}
	}

	private void insertNewItem ( final ${KEY_TYPE} key, final ${VALUE_TYPE} item, final int hashEntry ) {
		if ( getEntry( OFF_HEAP_POINTER, hashEntry ) == EMPTY_VALUE_INDICATOR ) {
			setEntry( OFF_HEAP_POINTER, hashEntry, key, item );
			updateHopInfo( OFF_HEAP_POINTER, hashEntry, 0 );
			return;
		}

		int emptyEntry = hashEntry;
		while ( getEntry( OFF_HEAP_POINTER, emptyEntry ) != EMPTY_VALUE_INDICATOR ) {
			emptyEntry = nextEntry( mask, emptyEntry );
		}

		while ( emptyEntry != -1 && isFreeEntryFound( h, capacity, hashEntry, emptyEntry ) ) {

			int replacement = -1;
			for ( int candidate = calculateStartCandidateIndex( h, capacity, emptyEntry ), i = 0; replacement == -1 && i < h; candidate = nextEntry( mask, candidate ), i++ ) {
				if ( getHopInfo( OFF_HEAP_POINTER, candidate ) != 0 ) {
					int candidateReplacement = mask & ( getReplacement( OFF_HEAP_POINTER, candidate ) + candidate );
					if ( candidateReplacement < emptyEntry && candidateReplacement >= candidate ) {
						replacement = candidateReplacement;
						switchEntries( OFF_HEAP_POINTER, emptyEntry, replacement );
						updateHopSetClear( OFF_HEAP_POINTER, candidate, calculateNewBit( capacity, emptyEntry, candidate ), calculateNewBit( capacity, replacement, candidate ) );
					}
				}
			}
			emptyEntry = replacement;
		}

		if ( emptyEntry == -1 ) {
			while ( getHelperEntry( OFF_HEAP_POINTER, helperFreePointer ) != EMPTY_VALUE_INDICATOR ) {
				helperFreePointer = nextEntry( mask, helperFreePointer );
			}
			setHelperEntry( OFF_HEAP_POINTER, helperFreePointer, key, item );
			setOverflow( OFF_HEAP_POINTER, hashEntry, getOverflow( OFF_HEAP_POINTER, hashEntry ) + 1 );
		} else {
			setEntry( OFF_HEAP_POINTER, emptyEntry, key, item );
			updateHopInfo( OFF_HEAP_POINTER, emptyEntry, hashEntry );
		}

	}
	
	public boolean remove ( final ${KEY_TYPE} key ) {

		int hashEntry = getHashValue( key, mask );
		int existingPosition = getPlaceToOEntryIfExists( OFF_HEAP_POINTER, capacity, mask, h, key, hashEntry );
		if ( existingPosition == NOT_FOUND_INDICATOR ) {
			return false;
		} else if ( existingPosition >= 0 ) {
			setKey( OFF_HEAP_POINTER, existingPosition, EMPTY_KEY_INDICATOR );
			setEntry( OFF_HEAP_POINTER, existingPosition, EMPTY_VALUE_INDICATOR );
			int positionForCalculation = existingPosition;
			if ( positionForCalculation > hashEntry )
				positionForCalculation += capacity;
			int bitToClear = positionForCalculation - hashEntry;
			clearHopBit( OFF_HEAP_POINTER, hashEntry, bitToClear );

			int overflow = getOverflow( OFF_HEAP_POINTER, hashEntry );
			if ( overflow > 0 ) {
				for ( int i = 0; i < capacity; i++ ) {

					${KEY_TYPE} helperKey = getHelperKey( OFF_HEAP_POINTER, i );
					if ( getHashValue( helperKey, mask ) == hashEntry ) {
						setEntry( OFF_HEAP_POINTER, existingPosition, getHelperEntry( OFF_HEAP_POINTER, i ) );
						setKey( OFF_HEAP_POINTER, existingPosition, helperKey );

						setHelperKey( OFF_HEAP_POINTER, i, EMPTY_KEY_INDICATOR );
						setHelperEntry( OFF_HEAP_POINTER, i, EMPTY_VALUE_INDICATOR );
						setHopBit( OFF_HEAP_POINTER, hashEntry, bitToClear );
					}
				}
			}
			size--;
			return true;
		} else {
			existingPosition = -existingPosition;
			setHelperEntry( OFF_HEAP_POINTER, existingPosition, EMPTY_VALUE_INDICATOR );
			setHelperKey( OFF_HEAP_POINTER, existingPosition, EMPTY_KEY_INDICATOR );
			int overflow = getOverflow( OFF_HEAP_POINTER, hashEntry );
			setOverflow( OFF_HEAP_POINTER, hashEntry, overflow - 1 );
			size--;
			return true;
		}
	}

	public ${VALUE_TYPE} get ( final ${KEY_TYPE} key ) {

		return get( key, getHashValue( key, mask ) );
	}


	public ${VALUE_TYPE} get ( final ${KEY_TYPE} key, final int hashEntry ) {
		int allEntries = getHopInfo( OFF_HEAP_POINTER, hashEntry );
		if ( allEntries == 0 )
			return EMPTY_VALUE_INDICATOR;
		else if ( allEntries == 1 && getKey( OFF_HEAP_POINTER, hashEntry ) == key )
			return getEntry( OFF_HEAP_POINTER, hashEntry );
		int counter = 0;
		while ( allEntries != 0 ) {
			if ( ( allEntries & 0x1 ) != 0 ) {
				int index = mask & ( hashEntry + counter );
				if ( getKey( OFF_HEAP_POINTER, index ) == key )
					return getEntry( OFF_HEAP_POINTER, index );
			}
			if ( counter++ >= h )
				break;
			allEntries >>>= 1;
		}

		if ( getOverflow( OFF_HEAP_POINTER, hashEntry ) > 0 ) {
			for ( int i = 0; i < capacity; i++ ) {
				if ( getHelperKey( OFF_HEAP_POINTER, i ) == key ) {
					return getHelperEntry( OFF_HEAP_POINTER, i );
				}
			}
		}
		return EMPTY_VALUE_INDICATOR;
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

	public int getSize () {
			return size;
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

	private static boolean isFreeEntryFound ( final int h, final int capacity, final int hashEntry, int emptyEntry ) {
		if ( emptyEntry < hashEntry ) {
			emptyEntry += capacity;
		}
		return emptyEntry - h + 1 > hashEntry;
	}

	private static int calculateStartCandidateIndex ( final int h, final int capacity, final int startIndex ) {
		int candidate = startIndex - h + 1;
		if ( candidate < 0 ) {
			candidate += capacity;
		}
		return candidate;
	}

	public int getReplacement ( final long address, final int candidate ) {
		int hop = getHopInfo( address, candidate );
		if ( hop == 0 ) {
			throw new IllegalArgumentException( "Candidate hopInfo was 0!" );
		}
		return MULTIPLY_DEBRUJIN_BIT_POSITION_LOOKUP[ ( ( hop & -hop ) * 0x077CB531 ) >>> 27 ] + 1;

	}

	private static void switchEntries ( final long address, final int emptyEntry, final int replacement ) {
		setEntry( address, emptyEntry, getEntry( address, replacement ) );
		setKey( address, emptyEntry, getKey( address, replacement ) );
	}

	private static int calculateNewBit ( final int capacity, final int replacement, final int entry ) {
		int calculated = replacement - entry;
		return calculated >= 0 ? calculated : calculated + capacity;
	}

	private static void updateHopSetClear ( final long address, final int entry, final int positionToSet, final int positionToClear ) {
		setHopInfo( address, entry, getHopInfo( address, entry ) | ( 1 << positionToSet ) );
		setHopInfo( address, entry, getHopInfo( address, entry ) & ~( 1 << positionToClear ) );
	}
	
	private static void clearHopBit ( final long address, final int entry, final int positionToClear ) {
		setHopInfo( address, entry, getHopInfo( address, entry ) & ~( 1 << positionToClear ) );
	}

	private static void setHopBit ( final long address, final int entry, final int positionToSet ) {
		setHopInfo( address, entry, getHopInfo( address, entry ) | ( 1 << positionToSet ) );
	}

	//---Begin Of Heap lookup----------------------------------------------------------------------------------------------------


	private static ${VALUE_TYPE} getEntry ( final long address, final int index ) {
		return unsafe.get${UNSAFE_ENTRY_TYPE}( address + index * OBJECT_SIZE + VALUE_OFFSET );
	}

	private static ${KEY_TYPE} getKey ( final long address, final int index ) {
		return unsafe.get${UNSAFE_KEY_TYPE}( address + index * OBJECT_SIZE + KEY_OFFSET );
	}

	private static void setEntry ( final long address, final int index, final ${VALUE_TYPE} value ) {
		unsafe.put${UNSAFE_ENTRY_TYPE}( address + index * OBJECT_SIZE + VALUE_OFFSET, value );
	}

	private static void setKey ( final long address, final int index, final ${KEY_TYPE} value ) {
		unsafe.put${UNSAFE_KEY_TYPE}( address + index * OBJECT_SIZE + KEY_OFFSET, value );
	}

	private static int getHopInfo ( final long address, final int index ) {
		return unsafe.getInt( address + index * OBJECT_SIZE + HOP_INFO_OFFSET );
	}

	private static void setHopInfo ( final long address, final int index, final int value ) {
		unsafe.putInt( address + index * OBJECT_SIZE + HOP_INFO_OFFSET, value );
	}

	private static ${VALUE_TYPE} getHelperEntry ( final long address, final int index ) {
		return unsafe.get${UNSAFE_ENTRY_TYPE}( address + index * OBJECT_SIZE + HELPER_VALUE_OFFSET );
	}

	private static ${KEY_TYPE} getHelperKey ( final long address, final int index ) {
		return unsafe.get${UNSAFE_KEY_TYPE}( address + index * OBJECT_SIZE + HELPER_KEY_OFFSET );
	}

	private static void setHelperEntry ( final long address, final int index, final ${VALUE_TYPE} value ) {
		unsafe.put${UNSAFE_ENTRY_TYPE}( address + index * OBJECT_SIZE + HELPER_VALUE_OFFSET, value );
	}

	private static void setHelperKey ( final long address, final int index, final ${KEY_TYPE} value ) {
		unsafe.put${UNSAFE_KEY_TYPE}( address + index * OBJECT_SIZE + HELPER_KEY_OFFSET, value );
	}

	private static int getOverflow ( final long address, final int index ) {
		return unsafe.getInt( address + index * OBJECT_SIZE + OVERFLOW_OFFSET );
	}

	private static void setOverflow ( final long address, final int index, final int value ) {
		unsafe.putInt( address + OBJECT_SIZE * index + OVERFLOW_OFFSET, value );
	}
	//---End Of Heap lookup------------------------------------------------------------------------------------------------------

}
