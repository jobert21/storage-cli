package ja.tools.storage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonProcessingException;

import ja.tools.storage.dto.StorageConfig;
import ja.tools.storage.impl.S3Storage;

class StorageConfigTest {

	@TempDir
	Path tempDir;

	@Test
	void resolvesAwsConfig() throws Exception {
		Path config = writeConfig("""
				{
				  "type": "AWS",
				  "config": {
				    "bucket": "bucket",
				    "accountKey": "key",
				    "accountSecret": "secret",
				    "region": "us-east-1"
				  }
				}
				""");

		IStorage storage = StorageConfig.resolve(StorageCli.createMapper(), config);

		assertInstanceOf(S3Storage.class, storage);
	}

	@Test
	void azureEmptyConnectionStringThrows() throws Exception {
		Path config = writeConfig("""
				{
				  "type": "AZURE",
				  "config": {
				    "containerName": "container",
				    "connectionString": ""
				  }
				}
				""");

		RuntimeException ex = assertThrows(RuntimeException.class,
				() -> StorageConfig.resolve(StorageCli.createMapper(), config));
		assertTrue(ex.getMessage().contains("cannot be empty"));
	}

	@Test
	void unknownTypeFailsDeserialization() throws Exception {
		Path config = writeConfig("""
				{
				  "type": "GCP",
				  "config": {}
				}
				""");

		assertThrows(JsonProcessingException.class, () -> StorageConfig.resolve(StorageCli.createMapper(), config));
	}

	@Test
	void missingConfigFallsBackToDefaultFile() {
		Assumptions.assumeFalse(Files.exists(Path.of(StorageConfig.STORAGE_CONFIG_FILENAME)));
		Path missing = tempDir.resolve("missing-storage-config.json");

		assertThrows(Exception.class, () -> StorageConfig.resolve(StorageCli.createMapper(), missing));
	}

	private Path writeConfig(String json) throws Exception {
		Path config = tempDir.resolve("storage-config.json");
		Files.writeString(config, json);
		return config;
	}
}
