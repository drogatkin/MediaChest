package photoorganizer;

public interface Persistancable {
	/**
	 * Tells that a container wants to save a component state.
	 * 
	 */
	public void save();

	/**
	 * Tells that a container wants to load a component state.
	 * 
	 * 
	 */
	public void load();
}
