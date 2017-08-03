package edu.cmu.ml.rtw.util;

import java.util.Comparator;
/***
 * Wrapper for three typed objects.
 * 
 * @author Malcolm
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public class Triple<A,B,C> {
	public A a;
	public B b;
	public C c;
	
	public Triple(A a, B b, C c){
		this.a = a;
		this.b = b;
		this.c = c;
	}
}
