public class StringST<T>
{
	private static final int R = 256;
	private Node root = new Node();
	
	private static class Node {
		public Object val = null;
		public Node[] next = new Node[R];
	}

	private Node get(Node x, String key, int d) {
		if (x == null) {
			return null;
		}
		
		if (d == key.length()) {
			return x;
		}
		
		final char c = key.charAt(d);
		return this.get(x.next[c], key, d+1);
	}
	
	public StringST() {
	}

	public T get(String key) {
		final Node x = this.get(this.root, key, 0);
		if (x == null) {
			return null;
		}
		return (T)x.val;
	}
	
	private Node put(Node x, String key, T val, int d) {
		
		if (d == key.length()) {
			x.val = val;
		}
		else {
			final char c = key.charAt(d);
			if (x.next[c] == null) {
				x.next[c] = new Node(); 
			}
			x.next[c] = this.put(x.next[c], key, val, d+1);
		}
		return x;
	}
	
	public void put(String key, T val) {
		this.root = this.put(this.root, key, val, 0);
	}
	
	public boolean contains(String key) {
		return this.get(key) != null;
	}
	
	private boolean hasChildren(Node x) {
		if (x == null) {
			return false;
		}
		for (int i = 0; i < R; ++i) {
			if (x.next[i] != null) {
				return true;
			}
		}
		return false;
	}
	
 	private Node delete(Node x, String key, int d) {
		if (x == null) {
			return null;
		}
		
		if (d == key.length()) {
			x.val = null;
		} else {
			final char c = key.charAt(d);
			x.next[c] = this.delete(x.next[c], key, d+1);
		}
		
		if (!this.hasChildren(x)) {
			x = null;
		}
		
		return x;
	}
	
	public void delete(String key) {
		this.root = this.delete(this.root, key, 0);
	}
}