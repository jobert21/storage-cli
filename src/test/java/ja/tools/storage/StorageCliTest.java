package ja.tools.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import ja.tools.storage.dto.StorageConfig;

class StorageCliTest {

	@Test
	void emptyArgsReturnsZero() {
		assertEquals(0, StorageCli.run(new String[0]));
	}

	@Test
	void helpReturnsZero() {
		assertEquals(0, StorageCli.run(new String[] { "-h" }));
		assertEquals(0, StorageCli.run(new String[] { "--help" }));
	}

	@Test
	void uploadMissingPathReturnsZero() {
		assumeNoDefaultConfig();
		assertEquals(0, StorageCli.run(new String[] { "-a", "upload", "-p", "/no/such/storage-cli-file" }));
	}

	@Test
	void unknownActionReturnsZero() {
		assumeNoDefaultConfig();
		assertEquals(0, StorageCli.run(new String[] { "-a", "not-an-action" }));
	}

	private static void assumeNoDefaultConfig() {
		Assumptions.assumeFalse(Files.exists(Path.of(StorageConfig.STORAGE_CONFIG_FILENAME)));
	}
}
