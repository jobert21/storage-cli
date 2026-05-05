package ja.tools.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import ja.tools.storage.dto.StoredItem;

@FunctionalInterface
public interface IStorage {
	/**
	 * List items.
	 * 
	 * @return
	 */
	List<StoredItem> listItems();

	/**
	 * Delete item.
	 * 
	 * @param name
	 * @throws IOException
	 */
	default void delete(String name) throws IOException {

	}

	/**
	 * Upload from inputstream.
	 * 
	 * @param in
	 * @param name
	 * @throws IOException
	 */
	default void upload(InputStream in, String name) throws IOException {
		upload(in, name, true);
	}

	/**
	 * Upload from inputstream.
	 * 
	 * @param in
	 * @param name
	 * @throws IOException
	 */
	default void upload(InputStream in, String name, boolean overwrite) throws IOException {
	}

	/**
	 * Upload from path.
	 * 
	 * @param path
	 * @throws IOException
	 */
	default void uploadFromPath(Path path) throws IOException {
		uploadFromPath(path, path.getFileName().toString());
	}

	/**
	 * Upload from path.
	 * 
	 * @param path
	 * @param overwrite
	 * @throws IOException
	 */
	default void uploadFromPath(Path path, boolean overwrite) throws IOException {
		uploadFromPath(path, path.getFileName().toString(), overwrite);
	}

	/**
	 * Upload from path.
	 * 
	 * @param path
	 * @param name
	 * @throws IOException
	 */
	default void uploadFromPath(Path path, String name) throws IOException {
		uploadFromPath(path, name, true);
	}

	/**
	 * Upload from path
	 * 
	 * @param path
	 * @param name
	 * @param overwrite
	 * @throws IOException
	 */
	default void uploadFromPath(Path path, String name, boolean overwrite) throws IOException {
		if (!Files.isDirectory(path)) {
			upload(Files.newInputStream(path), name, true);
		}
	}
}
