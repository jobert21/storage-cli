/**
 * 
 */
package ja.tools.storage.dto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ja.tools.storage.IStorage;
import ja.tools.storage.StorageType;
import ja.tools.storage.impl.AzureStorage;

/**
 * @author jobert
 * 
 */
public class StorageConfig {
	public static final String STORAGE_CONFIG_FILENAME = "storage-config.json";
	private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);
	private StorageType type;
	private Map<String, Object> config;

	public StorageType getType() {
		return type;
	}

	public void setType(StorageType type) {
		this.type = type;
	}

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	public static IStorage resolve(ObjectMapper mapper) throws Exception {
		return resolve(mapper, Paths.get(STORAGE_CONFIG_FILENAME));
	}

	public static IStorage resolve(ObjectMapper mapper, Path path) throws Exception {
		if (path == null || !Files.exists(path)) {
			log.info("Using default root path of {}.", STORAGE_CONFIG_FILENAME);
			path = Paths.get(STORAGE_CONFIG_FILENAME);
		}
		StorageConfig storageConfig = mapper.readValue(Files.newInputStream(path), StorageConfig.class);
		IStorage storage = null;
		switch (storageConfig.getType()) {
		case AZURE:
			storage = new AzureStorage(storageConfig);
			break;
		case AWS:
			break;
		}
		return storage;
	}

}
