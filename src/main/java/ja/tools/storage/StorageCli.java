package ja.tools.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ja.tools.storage.dto.StorageAction;
import ja.tools.storage.dto.StorageConfig;
import ja.tools.storage.dto.StoredItem;

/**
 * 
 * @author jobert
 *
 */
public class StorageCli {
	private static final Logger log = LoggerFactory.getLogger(StorageCli.class);
	public static final String PATH_ARG = "path";
	public static final String PATH_ARG_SMALL = "p";
	public static final String ACTION_ARG = "action";
	public static final String ACTION_ARG_SMALL = "a";
	public static final String ITEM_NAME_ARG = "name";
	public static final String ITEM_NAME_ARG_SMALL = "n";
	public static final String CONFIG_ARG = "config";
	public static final String CONFIG_ARG_SMALL = "c";

	public static void main(String[] args) {
		Options options = new Options();
		options.addOption(PATH_ARG_SMALL, PATH_ARG, true, "The path of the file to upload or download.");
		options.addOption(ACTION_ARG_SMALL, ACTION_ARG, true, "The action to execute. Example: list, upload, delete");
		options.addOption(CONFIG_ARG_SMALL, CONFIG_ARG, true,
				"The config file. It should be the same format as " + StorageConfig.STORAGE_CONFIG_FILENAME + ".");
		options.addOption(ITEM_NAME_ARG_SMALL, ITEM_NAME_ARG, true,
				"The name with path. Optional. Will use the filename from the file.");

		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser cli = new DefaultParser();
		CommandLine cmd = null;

		StorageAction action = StorageAction.LIST;
		Path path = null;
		String nameAndPath = null;
		try {
			cmd = cli.parse(options, args, true);
			if (cmd.hasOption(ACTION_ARG_SMALL)) {
				action = StorageAction.valueOf(cmd.getOptionValue(ACTION_ARG_SMALL).toUpperCase());
			}
			if (action == StorageAction.UPLOAD) {
				String pathString = cmd.getOptionValue(PATH_ARG_SMALL);
				if (StringUtils.isEmpty(pathString) || !Files.exists(path = Paths.get(pathString))) {
					printHelpUploadDelete(formatter, options);
					throw new RuntimeException("Actions upload or delete requires the path of the file.");
				}
			}
			nameAndPath = cmd.getOptionValue(ITEM_NAME_ARG_SMALL);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		try {
			Path configPath = null;
			if (cmd.hasOption(CONFIG_ARG_SMALL)) {
				configPath = Paths.get(cmd.getOptionValue(CONFIG_ARG_SMALL));
			}
			ObjectMapper mapper = createMapper();
			IStorage storage = StorageConfig.resolve(mapper, configPath);
			
			if (StringUtils.isEmpty(nameAndPath) && path != null) {
				nameAndPath = path.getFileName().toString();
			}
			
			switch (action) {
			case LIST:
				log.info("Executing action = {}.", action);
				List<StoredItem> items = storage.listItems();
				items.forEach(item -> log.info(item.getName()));
				break;
			case UPLOAD:
				log.info("Executing action = {}, path = {}.", action, path);
				storage.uploadFromPath(path, nameAndPath);
				break;
			case DELETE:
				log.info("Executing action = {}, path = {}.", action, path);
				storage.delete(nameAndPath);
				break;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	static ObjectMapper createMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.setSerializationInclusion(Include.NON_NULL);
		return mapper;
	}

	static void printHelpGeneric(HelpFormatter formatter, Options options) {
		formatter.printHelp("java -jar storage-cli -p /path/to/file -a upload", options);
	}

	static void printHelpUploadDelete(HelpFormatter formatter, Options options) {
		formatter.printHelp("java -jar storage-cli -p /path/to/file -a upload|delete|list", options);
	}
}
