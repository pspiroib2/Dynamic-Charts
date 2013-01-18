package test;

import java.util.Arrays;

public class Permutation {
	public static void main(String[] args) {
		String[] a = { "a", "b", "c" };
		String[] b = { "1", "2", "3" };
		String[] c = { "x", "y" };
		
		String[][] ar = { a, b, c };
		
		try2( ar);
	}
	
	/** My recursive solution. */
	private static void try3( String[][] ar) {
		try3_( "", ar);
	}
	
	private static void try3_( String s, String[][] ar) {
		if( ar.length == 0) {
			System.out.println( s);
			return;
		}
		
		String[] a1 = ar[0];
		
		String[][] rest = new String[ar.length-1][];
		System.arraycopy(ar, 1, rest, 0, ar.length-1);
		
		for( String a : a1) {
			try3_( s + " " + a, rest);
		}
	}
	
	/** My iterative solution. */
	void try1(String[][] ar) {
		
		int[] index = new int[ar.length];
		Arrays.fill( index, 0);
		
lab:	while( true) {
			for( int i = 0; i < ar.length; i++) {
				System.out.print( ar[i][index[i]] + " ");
			}
			System.out.println( "");
			
			index[0]++;
			
			for( int i = 0; i < index.length; i++) {
				if( index[i] > ar[i].length - 1) {
					if( i >= index.length - 1) {
						break lab;
					}
					for( int j = i; j >= 0; j--) {
						index[j] = 0;
					}
					
					index[i+1]++;
				}
			}
		}
	}
	
	/** Provided by Sajil. */
	static void try2(String[][] ars) {
		String[] main = null;
		
		for( String[] ar : ars) {
			if( main == null) {
				main = ar;
			}
			else {
				main = build( main, ar);
			}
		}

		for( String a : main) {
			System.out.println( a);
		}
	}
	
	static String[] build( String[] a1, String[] a2) {
		int i = 0;
		String[] rc = new String[a1.length * a2.length];
		for( String s1 : a1) {
			for( String s2 : a2) {
				rc[i++] = s1 + " " + s2;
			}
		}
		return rc;
	}

}

