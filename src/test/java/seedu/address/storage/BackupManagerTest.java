package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for BackupManager.
 */
public class BackupManagerTest {

    private static final Path TEMP_BACKUP_DIR = Paths.get("test-backups");
    private static final Path TEMP_FILE = TEMP_BACKUP_DIR.resolve("test-addressbook.json");
    private BackupManager backupManager;

    @BeforeEach
    public void setUp() throws IOException {
        // Ensure backup directory exists before each test
        Files.createDirectories(TEMP_BACKUP_DIR);
        backupManager = new BackupManager(TEMP_BACKUP_DIR);

        // Create a temporary file to simulate address book data
        Files.writeString(TEMP_FILE, "Sample AddressBook Data");
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up the test backup directory and files
        try (Stream<Path> paths = Files.walk(TEMP_BACKUP_DIR)) {
            paths.sorted((path1, path2) -> path2.compareTo(path1)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    @Test
    public void backupCreation_createsBackupFileSuccessfully() throws IOException {
        backupManager.saveBackup(TEMP_FILE);
        long backupCount = Files.list(TEMP_BACKUP_DIR).count();
        assertEquals(2, backupCount, "Backup file should be created successfully.");
    }

    @Test
    public void backupCreation_throwsExceptionIfFileNotFound() {
        Path nonExistentFile = TEMP_BACKUP_DIR.resolve("non-existent-file.json");
        assertThrows(IOException.class, () -> backupManager.saveBackup(nonExistentFile));
    }

    @Test
    public void cleanOldBackups_throwsExceptionForInvalidMaxBackups() {
        assertThrows(IllegalArgumentException.class, () -> backupManager.cleanOldBackups(0));
    }

    @Test
    public void backupDirectoryInitialization_createsDirectorySuccessfully() throws IOException {
        Path newBackupDir = TEMP_BACKUP_DIR.resolve("new-backup-dir");
        new BackupManager(newBackupDir);
        assertTrue(Files.exists(newBackupDir), "Backup directory should be created successfully.");
    }

    @Test
    public void backupDirectoryInitialization_throwsExceptionForNullPath() {
        assertThrows(IOException.class, () -> new BackupManager(null));
    }

    @Test
    public void restoreMostRecentBackup_noBackupAvailable_returnsEmptyOptional() throws IOException {
        Files.deleteIfExists(TEMP_FILE); // Ensure no backups exist
        Optional<Path> restoredBackup = backupManager.restoreMostRecentBackup();
        assertFalse(restoredBackup.isPresent(), "No backup should be available.");
    }

    @Test
    public void getFileCreationTime_fileNotFound_returnsCurrentTime() {
        // Provide a non-existent path to simulate IOException
        Path nonExistentPath = TEMP_BACKUP_DIR.resolve("non-existent-file.json");

        // Call the method and capture the result
        FileTime result = backupManager.getFileCreationTime(nonExistentPath);

        // Verify that the fallback time is set to the current time
        long now = System.currentTimeMillis();
        assertTrue(Math.abs(result.toMillis() - now) < 1000,
                "The returned FileTime should be close to the current system time.");
    }

}
