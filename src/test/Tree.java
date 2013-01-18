package test;

import java.util.ArrayList;
import java.util.Collections;

import lib.S;

public class Tree {
	Node m_node;
	
	Tree() {
		m_node = new Node();
	}
	
	Tree( Node node) {
		m_node = node; 
	}
	
	private Tree copy() {
		return new Tree( m_node.copy() );
	}

	private void show() {
		m_node.show( "");
	}

	static class Node implements Comparable<Node> {
		int m_v;
		ArrayList<Node> m_list = new ArrayList<Node>();
		
		
		
		Node() {
		}
		
		Node( int i) {
			m_v = i;
		}
		
		void add( Node node) {
			m_list.add( node);
		}

		public void show(String s) {
			S.err( s + m_v);
			for( Node node : m_list) {
				node.show( s + " ");
			}
		}

		public Node copy() {
			Node newnode = new Node( m_v);
			for( Node node : m_list) {
				newnode.add( node.copy() );
			}
			Collections.sort( newnode.m_list);
			return newnode;
		}

		@Override public int compareTo(Node o) {
			return Integer.valueOf( m_v).compareTo( o.m_v);
		}
	}

	public static void main(String[] args) {
		Tree tree = new Tree();
		Node n4 = new Node( 4);

		Node n1 = new Node( 1);
		Node n2 = new Node( 2);
		Node n3 = new Node( 3);
		Node n5 = new Node( 5);
		Node n6 = new Node( 6);
		tree.m_node.add( n3);
		tree.m_node.add( n1);
		tree.m_node.add( n2);
		n5.add( n6);
		n3.add( n4);
		n3.add( n5);
		tree.show();
		
		Tree newtree = tree.copy();
		newtree.show();
	}
}
