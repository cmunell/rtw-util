package edu.cmu.ml.rtw.util;

import java.util.Comparator;
	/***
	 * -- makes one of the triple elements the key
	 * -- can sort on this key becuase all elements are comparable
	 * 
	 * @author Malcolm
	 *
	 * @param <A>
	 * @param <B>
	 * @param <C>
	 */
	public class TripleComparator<A extends Comparable<? super A>,
								 							  B extends Comparable<? super B>,
																C extends Comparable<? super C>>
								implements Comparator<Triple<A,B,C>> {
			// Added NEGCENTER and NEGLEFT and NEGRIGHT, which are very useful 
		  // for sorting in descending order. (mwgreave)
	    public enum Side {
	        LEFT,    CENTER,    RIGHT,
	        NEGLEFT, NEGCENTER, NEGRIGHT
	    }

	    public Side comparision;
	    
	    public TripleComparator(Side comparisionType) {
	    	this.comparision = comparisionType;
	    }

			@Override
			public int compare(Triple<A, B, C> triple1, Triple<A, B, C> triple2) {
				switch(comparision){
					case LEFT:
						return triple1.a.compareTo(triple2.a);
					case CENTER:
						return triple1.b.compareTo(triple2.b);
					case RIGHT:
						return triple1.c.compareTo(triple2.c);
					case NEGLEFT:
						return - triple1.a.compareTo(triple2.a);
					case NEGCENTER:
						return - triple1.b.compareTo(triple2.b);
					case NEGRIGHT:
						return - triple1.c.compareTo(triple2.c);
					default:
						throw new RuntimeException("comparision enum outside programmed range:   " +
								"chceck source code in TripleComparator, see why the range os side is lager than this compare " +
								"function's programmed range");
				}
				
			}
	}