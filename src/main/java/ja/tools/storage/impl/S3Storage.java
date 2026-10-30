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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ja.tools.storage.IStorage;
import ja.tools.storage.dto.StorageConfig;
import ja.tools.storage.dto.StoredItem;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

/**
 * @author jobert
 * 
 */
public class S3Storage implements IStorage {
	private static final Logger log = LoggerFactory.getLogger(S3Storage.class);
	private static final String KEY_REGION = "region";
	private static final String KEY_ACCOUNT = "accountKey";
	private static final String KEY_SECRET = "accountSecret";
	private static final String KEY_BUCKET = "bucket";
	private StorageConfig storageConfig;
	private volatile S3Client client;
	private volatile S3AsyncClient clientAsync;
	private volatile S3TransferManager tm;

	public S3Storage(StorageConfig storageConfig) {
		this.storageConfig = storageConfig;
	}

	private S3Client s3() {
		S3Client clientTemp = client;
		if (clientTemp == null) {
			synchronized (this) {
				clientTemp = client;
				if (clientTemp == null) {
					String regionName = get(KEY_REGION);
					String accountKey = get(KEY_ACCOUNT);
					String accountSecret = get(KEY_SECRET);

					Region region = resolveRegion(regionName);
					AwsCredentials credentials = AwsBasicCredentials.create(accountKey, accountSecret);
					log.trace("Creating S3 using Region: '{}'.", region);
					clientTemp = S3Client.builder()
							.overrideConfiguration(ClientOverrideConfiguration.builder()
									.retryStrategy(AwsRetryStrategy.defaultRetryStrategy().toBuilder().build()).build())
							.region(region).credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
					client = clientTemp;
				}
			}
		}
		return client;
	}

	private S3AsyncClient s3Async() {
		S3AsyncClient clientAsyncTemp = clientAsync;
		if (clientAsyncTemp == null) {
			synchronized (this) {
				clientAsyncTemp = clientAsync;
				if (clientAsyncTemp == null) {
					String regionName = get(KEY_REGION);
					String accountKey = get(KEY_ACCOUNT);
					String accountSecret = get(KEY_SECRET);

					Region region = resolveRegion(regionName);
					AwsCredentials credentials = AwsBasicCredentials.create(accountKey, accountSecret);
					log.trace("Creating S3 async using Region: '{}'.", region);
					clientAsyncTemp = S3AsyncClient.crtBuilder().region(region)
							.credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
					clientAsync = clientAsyncTemp;
				}
			}

		}
		return clientAsync;
	}

	private S3TransferManager tm() {
		S3TransferManager tmTemp = tm;
		if (tm == null) {
			synchronized (this) {
				tmTemp = tm;
				if (tmTemp == null) {
					tmTemp = S3TransferManager.builder().s3Client(s3Async()).build();
					tm = tmTemp;
				}
			}
		}
		return tm;
	}

	@Override
	public void upload(InputStream in, String name, boolean overwrite) throws IOException {
		log.info("Upload -> {}.", name);
		String bucketName = get(KEY_BUCKET);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		BoundedInputStream counting = BoundedInputStream.builder().setInputStream(in).setPropagateClose(true).get();

		AsyncRequestBody body = AsyncRequestBody.fromInputStream(counting, null, executor);
		UploadRequest request = UploadRequest.builder()
				.putObjectRequest(
						r -> r.bucket(bucketName).key(StringUtils.isEmpty(name) ? UUID.randomUUID().toString() : name))
				.requestBody(body).build();

		Upload upload = tm().upload(request);
		CompletedUpload completed = upload.completionFuture().join();
		completed.response();
		log.info("Done upload for {}.", name);
	}

	@Override
	public void uploadFromPath(Path path, String name, boolean overwrite) throws IOException {
		if (!Files.isDirectory(path)) {
			upload(Files.newInputStream(path), StringUtils.isEmpty(name) ? path.getFileName().toString() : name,
					overwrite);
			return;
		}

		Files.walkFileTree(path, Set.of(FileVisitOption.FOLLOW_LINKS), 1, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(file)) {
					Path newPath = Paths.get(name).resolve(file.getFileName().toString());
					try (InputStream in = Files.newInputStream(file)) {
						upload(in, newPath.toString(), overwrite);
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	@Override
	public void delete(String name) throws IOException {
		String bucketName = get(KEY_BUCKET);
		s3().deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(name).build());
	}

	@Override
	public List<StoredItem> listItems() {
		String bucketName = get(KEY_BUCKET);
		List<StoredItem> objects = s3().listObjectsV2Paginator(b -> b.bucket(bucketName)).stream()
				.flatMap(r -> r.contents().stream()).map(obj -> {
					StoredItem item = new StoredItem();
					item.setName(obj.key());
					item.setLastModified(Date.from(obj.lastModified()));
					item.setCreated(Date.from(obj.lastModified()));
					item.setContentLength(obj.size());
					item.setItemRef(obj);
					return item;
				}).toList();
		return objects;
	}

	private Region resolveRegion(String regionName) {
		Region region = null;
		if (regionName == null || regionName.trim().isEmpty()) {
			region = Region.US_EAST_1;
		} else {
			region = Region.of(regionName);
		}
		return region;
	}

	@SuppressWarnings("unchecked")
	private <T> T get(String key) {
		T t = null;
		if (storageConfig != null && storageConfig.getConfig() != null) {
			t = (T) storageConfig.getConfig().get(key);
		}
		return t;
	}

}
