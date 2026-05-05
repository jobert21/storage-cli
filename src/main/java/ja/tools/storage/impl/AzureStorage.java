/**
 * 
 */
package ja.tools.storage.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;

import ja.tools.storage.IStorage;
import ja.tools.storage.dto.StorageConfig;
import ja.tools.storage.dto.StoredItem;

/**
 * @author jobert
 * 
 */
public class AzureStorage implements IStorage {
	private static final Logger log = LoggerFactory.getLogger(AzureStorage.class);
	public static final String KEY_CONN_STRING = "connectionString";
	public static final String KEY_CONTAINER_NAME = "containerName";
	private StorageConfig storageConfig;
	private BlobServiceClient serviceClient;
	private Map<String, BlobContainerClient> clients;
	private String defaultContainerName;

	public AzureStorage(StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
		clients = new HashMap<>();
		initialize();
	}

	private void initialize() {
		String connectionString = get(KEY_CONN_STRING);
		if (StringUtils.isEmpty(connectionString)) {
			throw new RuntimeException("Connection string for azure blob store cannot be empty.");
		}
		log.info("Initialize blob service client with -> {}", connectionString);
		serviceClient = new BlobServiceClientBuilder().connectionString(connectionString).buildClient();
	}

	@SuppressWarnings("unchecked")
	private <T> T get(String key) {
		T t = null;
		if (storageConfig != null && storageConfig.getConfig() != null) {
			t = (T) storageConfig.getConfig().get(key);
		}
		return t;
	}

	private BlobContainerClient getClient(String containerName) {
		return clients.computeIfAbsent(containerName, (name) -> serviceClient.getBlobContainerClient(name));
	}

	@Override
	public void upload(InputStream in, String name, boolean overwrite) throws IOException {
		String containerName = getContainerNameOrDefault();
		if (StringUtils.isEmpty(containerName)) {
			log.warn("Container name does not exist in the configuration.");
			return;
		}
		log.info("Container client -> {}.", containerName);
		BlobContainerClient containerClient = getClient(containerName);

		log.info("Blob client -> {}.", name);
		BlobClient blobClient = containerClient.getBlobClient(name);

		log.info("Uploading {}.", name);
		blobClient.upload(in, overwrite);
		log.info("Upload done.");
	}

	@Override
	public void uploadFromPath(Path path, String name, boolean overwrite) throws IOException {
		if (!Files.isDirectory(path)) {
			upload(Files.newInputStream(path), name, overwrite);
			return;
		}
		String containerName = getContainerNameOrDefault();
		if (StringUtils.isEmpty(containerName)) {
			log.warn("Container name does not exist in the configuration.");
			return;
		}
		log.info("Container client -> {}.", containerName);
		BlobContainerClient containerClient = getClient(containerName);

		Files.walkFileTree(path, Set.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(file)) {
					Path newPath = Paths.get(name).resolve(file.getFileName().toString());
					log.info("Blob client -> {}.", newPath);
					
					BlobClient blobClient = containerClient.getBlobClient(newPath.toString());
					log.info("Uploading {}.", file);
					blobClient.uploadFromFile(file.toString(), overwrite);
					log.info("Upload done.");
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public List<StoredItem> listItems() {
		String containerName = getContainerNameOrDefault();
		if (StringUtils.isEmpty(containerName)) {
			log.warn("Container name does not exist in the configuration.");
			return List.of();
		}
		log.info("Container client -> {}.", containerName);
		BlobContainerClient containerClient = getClient(containerName);

		return containerClient.listBlobs().stream().map(blob -> {
			StoredItem item = new StoredItem();
			item.setName(blob.getName());
			item.setLastModified(Date.from(blob.getProperties().getLastModified().toInstant()));
			item.setCreated(Date.from(blob.getProperties().getCreationTime().toInstant()));
			item.setContentLength(blob.getProperties().getContentLength());
			item.setItemRef(blob);
			return item;
		}).collect(Collectors.toList());
	}

	@Override
	public void delete(String name) throws IOException {
		String containerName = getContainerNameOrDefault();
		if (StringUtils.isEmpty(containerName)) {
			log.warn("Container name does not exist in the configuration.");
			return;
		}
		log.info("Container client -> {}.", containerName);
		BlobContainerClient containerClient = getClient(containerName);

		log.info("Blob client -> {}.", name);
		BlobClient blobClient = containerClient.getBlobClient(name);

		log.info("Deleting {}.", name);
		blobClient.deleteIfExists();
		log.info("Delete done.");
	}

	protected String getContainerNameOrDefault() {
		String containerName = get(KEY_CONTAINER_NAME);
		if (StringUtils.isEmpty(containerName)) {
			log.info("Using a default container.");
			if (StringUtils.isEmpty(defaultContainerName)) {
				Optional<BlobContainerItem> firstItem = serviceClient.listBlobContainers().stream().findFirst();
				if (firstItem.isPresent()) {
					defaultContainerName = firstItem.get().getName();
				}
			}
			if (StringUtils.isEmpty(defaultContainerName)) {
				defaultContainerName = UUID.randomUUID().toString();
				log.info("Create a default container -> {}.", defaultContainerName);
				serviceClient.createBlobContainerIfNotExists(defaultContainerName);
			}
			containerName = defaultContainerName;
		}
		return containerName;
	}
}
