package photoorganizer.ipod;

import java.util.Collection;

public class ItemList<IC> {
	String name;
	Collection<IC> items;
	
	ItemList(Collection<IC> items, String name) {
		this.items = items;
		this.name = name;
	}
	
	public final String getName() {
		return name;
	}
	public final Collection<IC> getItems() {
		return items;
	}
	
	@Override
	public String toString() {
		return getName();
	}
}
