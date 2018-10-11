package ptatoolkit.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class UnionFindSet<E> {

	private Map<E, Entry> entries = new HashMap<>();
	private int nrsets; // number of disjoint sets
	
	public UnionFindSet(Collection<E> elems) {
		elems.forEach(elem -> entries.put(elem, new Entry(elem)));
		nrsets = entries.size();
	}
	
	public boolean union(E e1, E e2) {
		Entry root1 = findRoot(entries.get(e1));
		Entry root2 = findRoot(entries.get(e2));
		if (root1 == root2) {
			return false;
		} else { // union by rank
			if (root1.rank < root2.rank) {
				root1.parent = root2;
			} else if (root1.rank > root2.rank) {
				root2.parent = root1;
			} else {
				root2.parent = root1;
				++root2.rank;
			}
			--nrsets;
			return true;
		}
	}
	
	public boolean isConnected(E e1, E e2) {
		Entry root1 = findRoot(entries.get(e1));
		Entry root2 = findRoot(entries.get(e2));
		return root1 == root2;
	}
	
	public E find(E e) {
		Entry ent = findRoot(entries.get(e));
		return ent.elem;
	}
	
	public int numberOfSets() {
		return nrsets;
	}
	
	public Collection<Set<E>> getDisjointSets() {
		return entries.keySet()
				.stream()
				.collect(Collectors.groupingBy(this::find, Collectors.toSet()))
				.values();
	}
	
	private Entry findRoot(Entry ent) {
		if (ent.parent != ent) { // path compression
			ent.parent = findRoot(ent.parent);
		}
		return ent.parent;
	}
	
	private class Entry {
		
		private final E elem;
		private Entry parent;
		private int rank;
		
		private Entry(E elem) {
			this.elem = elem;
			this.parent = this;
			this.rank = 0;
		}
		
	}
	
}
