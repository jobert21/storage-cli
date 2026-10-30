# storage-cli

CLI tool for uploading, deleting, and listing files in Azure Blob Storage and Amazon S3.

Requires Java 21.

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/storage-cli-0.2.jar -a list -c storage-config.json
```

## Usage

```bash
# List files
java -jar target/storage-cli-0.2.jar -a list -c storage-config.json

# Upload a file
java -jar target/storage-cli-0.2.jar -a upload -p /path/to/file.txt -c storage-config.json

# Upload a file under a different object name
java -jar target/storage-cli-0.2.jar -a upload -p /path/to/file.txt -n folder/file.txt -c storage-config.json

# Delete a file
java -jar target/storage-cli-0.2.jar -a delete -n filename.txt -c storage-config.json

# Help
java -jar target/storage-cli-0.2.jar -h
```

## Options

- `-a, --action`   Action to execute: `list`, `upload`, `delete`. Defaults to `list` when omitted.
- `-p, --path`     Local file or directory to upload. A directory upload includes the files in that directory.
- `-n, --name`     Object name in storage. Optional for upload (defaults to the filename). Delete uses this name.
- `-c, --config`   Path to a storage config JSON file. If omitted, `storage-config.json` in the working directory is used.
- `-h, --help`     Print help.

## Config

### Azure

```json
{
  "type": "AZURE",
  "config": {
    "containerName": "container",
    "connectionString": ""
  }
}
```

### AWS S3

`region` is optional and defaults to `us-east-1`.

```json
{
  "type": "AWS",
  "config": {
    "bucket": "bucket",
    "accountKey": "",
    "accountSecret": "",
    "region": "us-east-1"
  }
}
```
