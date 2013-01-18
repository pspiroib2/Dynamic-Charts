package scale.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Alternatively, you could just override iterator method and sort list at that point. */
public class OrderdSet<T> implements Set<T> {
	HashSet<T> m_set = new HashSet<T>();
	ArrayList<T> m_list = new ArrayList<T>();

	@Override public boolean add(T o) {
		m_list.add( o);
		return m_set.add( o);
	}

	@Override public boolean addAll(Collection<? extends T> c) {
		return false;
	}

	@Override public void clear() {
		m_list.clear();
		m_set.clear();
	}

	@Override public boolean contains(Object o) {
		return m_set.contains( o);
	}

	@Override public boolean containsAll(Collection<?> c) {
		return false;
	}

	@Override public boolean isEmpty() {
		return m_set.isEmpty();
	}

	@Override public Iterator<T> iterator() {
		return m_list.iterator();
	}

	@Override public boolean remove(Object o) {
		m_list.remove( o);
		return m_set.remove( o);
	}

	@Override public boolean removeAll(Collection<?> c) {
		m_list.removeAll( c);
		return m_set.removeAll( c);
	}

	@Override public boolean retainAll(Collection<?> c) {
		m_list.retainAll( c);
		return m_set.retainAll( c);
	}

	@Override public int size() {
		return m_list.size();
	}

	@Override public Object[] toArray() {
		return m_list.toArray();
	}

	@Override public <T> T[] toArray(T[] a) {
		return m_list.toArray( a);
	}

}
