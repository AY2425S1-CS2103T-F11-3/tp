package seedu.address.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import seedu.address.commons.core.LogsCenter;

/**
 * Handles the creation, cleanup, and restoration of backups for the AddressBook data files.
 */
public class BackupManager {

    private static final Logger logger = LogsCenter.getLogger(BackupManager.class);
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss");
    private static final String BACKUP_FILE_REGEX =
            "(\\d+)_(.*?)_(\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}-\\d{3})\\.json";
    private static final Pattern BACKUP_FILE_PATTERN =
            Pattern.compile(BACKUP_FILE_REGEX);

    private static final int MAX_BACKUPS = 10; // indexed from 0 to 9
    private static final int MAX_FILENAME_LENGTH = 250;
    private final Path backupDirectory;
    private int currentIndex;


    /**
     * Constructs a {@code BackupManager} with the specified backup directory.
     *
     * @param backupDirectory The path to the backup directory. Must not be {@code null}.
     * @throws IOException If the backup directory path is {@code null} or if an error occurs
     *                     while creating the directory.
     */
    public BackupManager(Path backupDirectory) throws IOException {
        if (backupDirectory == null) {
            throw new IOException("Backup directory path cannot be null.");
        }

        this.backupDirectory = backupDirectory;
        if (!Files.exists(backupDirectory)) {
            Files.createDirectories(backupDirectory);
            logger.info("Created backup directory at: " + backupDirectory);
        }
        initializeCurrentIndex();
    }

    /**
     * Initializes the current index to the next available position based on existing backups.
     * Starts from the index following the oldest backup.
     *
     * @throws IOException If an error occurs while accessing the backup directory.
     */
    private void initializeCurrentIndex() throws IOException {
        try (Stream<Path> backups = Files.list(backupDirectory)) {
            List<Path> sortedBackups = backups
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::getFileTimestamp)) // Sort by timestamp
                    .collect(Collectors.toList());

            if (sortedBackups.isEmpty()) {
                currentIndex = 0;
            } else {
                // Find the oldest backup by timestamp and use its index for the next overwrite
                Path oldestBackup = sortedBackups.get(0);
                int oldestIndex = extractIndex(oldestBackup);
                currentIndex = (oldestIndex + 1) % MAX_BACKUPS;
            }
        }
    }

    /**
     * Retrieves the timestamp from the backup file name for sorting purposes.
     *
     * @param backupPath The path of the backup file.
     * @return LocalDateTime representing the timestamp, or LocalDateTime.MIN if parsing fails.
     */
    private LocalDateTime getFileTimestamp(Path backupPath) {
        String filename = backupPath.getFileName().toString();
        Matcher matcher = BACKUP_FILE_PATTERN.matcher(filename);
        if (matcher.matches()) {
            String timestampStr = matcher.group(3);
            try {
                return LocalDateTime.parse(timestampStr, FILE_TIMESTAMP_FORMATTER);
            } catch (Exception e) {
                logger.warning("Failed to parse timestamp from filename: " + filename + " - " + e.getMessage());
                return LocalDateTime.MIN;
            }
        } else {
            logger.warning("Invalid backup file format: " + filename);
            return LocalDateTime.MIN;
        }
    }

    /**
     * Creates a backup of the specified source file with a fixed index (from 0 to 9),
     * replacing any existing backup at that index. Each backup file name includes
     * the action description and a timestamp, allowing easy identification of backups.
     *
     * @param sourcePath        The path of the source file to back up.
     * @param actionDescription A description of the backup action.
     * @return The index used for the backup.
     * @throws IOException If an error occurs during file copying or deletion.
     */
    public int createIndexedBackup(Path sourcePath, String actionDescription) throws IOException {
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
        String backupFileName = String.format("%d_%s_%s.json", currentIndex, actionDescription, timestamp);

        // Check if the file name exceeds the limit
        if (backupFileName.length() > MAX_FILENAME_LENGTH) {
            throw new IOException("Backup file name exceeds the maximum length of "
                    + MAX_FILENAME_LENGTH
                    + " characters. Please shorten your description.");
        }

        Path backupPath = backupDirectory.resolve(backupFileName);

        // Delete existing backup at currentIndex if it exists
        deleteBackupByIndex(currentIndex);

        // Copy source to the backup path
        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Backup created with index: " + currentIndex);

        int usedIndex = currentIndex;
        // Update currentIndex
        currentIndex = (currentIndex + 1) % MAX_BACKUPS;

        return usedIndex;
    }

    /**
     * Deletes a backup file by index if it exists.
     *
     * @param index The index of the backup to delete.
     * @throws IOException If an error occurs during file deletion.
     */
    private void deleteBackupByIndex(int index) throws IOException {
        try (Stream<Path> backups = Files.list(backupDirectory)) {
            backups.filter(path -> extractIndex(path) == index)
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            logger.info("Deleted old backup at index " + index + ": " + path);
                        } catch (IOException e) {
                            logger.warning("Failed to delete backup: " + path + " - " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Extracts the index from a backup file name.
     *
     * @param backupPath The path of the backup file.
     * @return The integer index, or -1 if the filename is invalid.
     */
    protected int extractIndex(Path backupPath) {
        String filename = backupPath.getFileName().toString();
        Matcher matcher = BACKUP_FILE_PATTERN.matcher(filename);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            logger.warning("Invalid backup file format: " + filename);
            return -1;
        }
    }

    /**
     * Extracts the action description from a backup file name.
     *
     * @param backupPath The path of the backup file.
     * @return The action description, or "Unknown" if the filename is invalid.
     */
    protected String extractActionDescription(Path backupPath) {
        String filename = backupPath.getFileName().toString();
        Matcher matcher = BACKUP_FILE_PATTERN.matcher(filename);
        if (matcher.matches()) {
            return matcher.group(2);
        } else {
            logger.warning("Invalid backup file format: " + filename);
            return "Unknown";
        }
    }

    /**
     * Restores a backup by its index.
     *
     * @param index The index of the backup to restore.
     * @return The path to the backup file.
     * @throws IOException If the backup file is not found or cannot be accessed.
     */
    public Path restoreBackupByIndex(int index) throws IOException {
        try (Stream<Path> backups = Files.list(backupDirectory)) {
            Optional<Path> backupToRestore = backups
                    .filter(path -> extractIndex(path) == index)
                    .findFirst();

            if (backupToRestore.isPresent()) {
                return backupToRestore.get();
            } else {
                throw new IOException("Backup with index " + index + " not found.");
            }
        }
    }

    /**
     * Retrieves a formatted list of all backup files in the backups directory, sorted by timestamp
     * in descending order so that the most recent backups appear first. Each backup entry includes
     * the index, description, and timestamp.
     *
     * @return A formatted string with each backup file listed on a new line. If no backups are found,
     *         an empty string is returned.
     * @throws IOException If an error occurs while accessing or reading the backup directory.
     */
    public String getFormattedBackupList() throws IOException {
        if (!Files.exists(backupDirectory)) {
            return "";
        }

        try (Stream<Path> stream = Files.list(backupDirectory)) {
            List<Path> backupFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(this::getFileTimestamp).reversed())
                    .collect(Collectors.toList());

            if (backupFiles.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            for (Path path : backupFiles) {
                String filename = path.getFileName().toString();
                Matcher matcher = BACKUP_FILE_PATTERN.matcher(filename);
                if (matcher.matches()) {
                    int index = Integer.parseInt(matcher.group(1));
                    String description = matcher.group(2);
                    String timestampStr = matcher.group(3);
                    LocalDateTime timestamp = LocalDateTime.parse(timestampStr, FILE_TIMESTAMP_FORMATTER);

                    String formattedTimestamp = timestamp.format(DISPLAY_TIMESTAMP_FORMATTER);
                    sb.append(String.format("%d [%s] Created on: %s\n",
                            index,
                            description,
                            formattedTimestamp));
                } else {
                    logger.warning("Invalid backup file format: " + filename);
                }
            }

            return sb.toString().trim();
        }
    }

    /**
     * Cleans up old backups, keeping only the most recent `maxBackups`.
     *
     * @param maxBackups The number of backups to retain.
     * @throws IOException If an error occurs during cleanup.
     */
    public void cleanOldBackups(int maxBackups) throws IOException {
        if (maxBackups < 1) {
            throw new IllegalArgumentException("maxBackups must be at least 1.");
        }

        try (Stream<Path> backups = Files.list(backupDirectory)) {
            List<Path> backupFiles = backups.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(this::getFileCreationTime).reversed())
                    .collect(Collectors.toList());

            for (int i = maxBackups; i < backupFiles.size(); i++) {
                Files.deleteIfExists(backupFiles.get(i));
                logger.info("Deleted old backup: " + backupFiles.get(i));
            }
        }
    }

    /**
     * Retrieves the file creation time of a given backup file.
     *
     * @param path The path to the file.
     * @return The file's creation time.
     */
    private FileTime getFileCreationTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            logger.warning("Failed to get file creation time for " + path + ": " + e.getMessage());
            return FileTime.fromMillis(0);
        }
    }

    /**
     * Checks whether a backup file exists at the specified index.
     * This method scans the backup directory to determine if a backup
     * corresponding to the given index is available.
     *
     * @param index The index of the backup to check.
     * @return {@code true} if a backup exists at the specified index, {@code false} otherwise.
     * @throws IOException If an error occurs while accessing the backup directory.
     */
    public boolean isBackupAvailable(int index) {
        try (Stream<Path> backups = Files.list(backupDirectory)) {
            return backups.anyMatch(path -> extractIndex(path) == index);
        } catch (IOException e) {
            logger.warning("Failed to check backup availability: " + e.getMessage());
            return false;
        }
    }

}
