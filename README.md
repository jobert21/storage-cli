# storage-cli

CLI tool for uploading, deleting, and listing files in Azure Blob Storage.

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/storage-cli-0.1.jar -a list
```

## Usage

```bash
# List files
java -jar target/storage-cli-0.1.jar -a list -c storage-config.json

# Upload file
java -jar target/storage-cli-0.1.jar -a upload -p /path/to/file.txt -c storage-config.json

# Delete file
java -jar target/storage-cli-0.1.jar -a delete -n filename.txt -c storage-config.json
```

## Options

- `-a, --action`   Action to execute: list, upload, delete (required)
- `-p, --path`     Path of the file to upload or delete
- `-n, --name`     Name of the file in storage (optional, defaults to filename)
- `-c, --config`   Path to storage-config.json

## Config

Create a `storage-config.json` file:

```json
{
  "storageType": "AZURE_BLOB",
  "connectionString": "DefaultEndpointsProtocol=...",
  "containerName": "my-container"
}
```