package ja.tools.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ja.tools.storage.dto.StoredItem;

class IStorageDefaultsTest {

	@TempDir
	Path tempDir;

	@Test
	void uploadFromPathUsesFilename() throws IOException {
		Path file = Files.writeString(tempDir.resolve("notes.txt"), "hello");
		RecordingStorage storage = new RecordingStorage();

		storage.uploadFromPath(file);

		assertEquals(List.of("notes.txt"), storage.names);
	}

	@Test
	void uploadFromPathUsesExplicitName() throws IOException {
		Path file = Files.writeString(tempDir.resolve("notes.txt"), "hello");
		RecordingStorage storage = new RecordingStorage();

		storage.uploadFromPath(file, "folder/notes.txt");

		assertEquals(List.of("folder/notes.txt"), storage.names);
	}

	@Test
	void uploadFromPathSkipsDirectory() throws IOException {
		Path dir = Files.createDirectory(tempDir.resolve("folder"));
		Files.writeString(dir.resolve("inside.txt"), "hello");
		RecordingStorage storage = new RecordingStorage();

		storage.uploadFromPath(dir);

		assertTrue(storage.names.isEmpty());
	}

	private static final class RecordingStorage implements IStorage {
		private final List<String> names = new ArrayList<>();

		@Override
		public List<StoredItem> listItems() {
			return List.of();
		}

		@Override
		public void upload(InputStream in, String name, boolean overwrite) throws IOException {
			names.add(name);
			in.close();
		}
	}
}
