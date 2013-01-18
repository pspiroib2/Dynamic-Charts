package test;


public class LinkedList {
	static class Node {
		int m_val;
		Node m_next;

		public Node(int val, Node next) {
			m_val = val;
			m_next = next;
		}
	}

	static void show( Node node) {
		while( node != null) {
			System.out.println( node.m_val);
			node = node.m_next;
		}
	}

	static Node rev( Node node) {
		Node next = node.m_next;
		node.m_next = null;
		while( next != null) {
			Node p3 = next.m_next;
			next.m_next = node;
			node = next;
			next = p3;
		}
		return node;
	}

	public static void main(String[] args) {
		Node n3 = new Node( 3, null);
		Node n2 = new Node( 2, n3);
		Node n1 = new Node( 1, n2);

		show( n1);
		show( rev( n1) );
	}
}
