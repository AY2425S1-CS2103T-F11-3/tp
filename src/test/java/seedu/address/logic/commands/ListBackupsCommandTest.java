import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ListBackupsCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.TypicalPersons;

class ListBackupsCommandTest {

    private Model model;
    private Path backupDirectory = Path.of("backups");

    @BeforeEach
    public void setUp() throws IOException {
        // Clear the backup directory before each test
        Files.list(backupDirectory)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete file " + path);
                    }
                });

        // Initialize model with storage containing backup path
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(Path.of("data/addressbook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(Path.of("data/userprefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);
        model = new ModelManager(TypicalPersons.getTypicalAddressBook(), new UserPrefs(), storage);
    }

    @Test
    public void execute_noBackups_showsNoBackupsMessage() throws CommandException {
        ListBackupsCommand command = new ListBackupsCommand();
        CommandResult result = command.execute(model);
        assertTrue(result.getFeedbackToUser().contains(ListBackupsCommand.MESSAGE_NO_BACKUPS));
    }

    @Test
    public void execute_withBackups_showsBackupList() throws Exception {
        model.backupData("testBackup"); // Ensure there is at least one backup
        ListBackupsCommand command = new ListBackupsCommand();
        CommandResult result = command.execute(model);

        // Verify the result starts with the success message
        assertTrue(result.getFeedbackToUser().startsWith("Available backups:\n"));
    }

    @Test
    public void execute_storageNotInitialized_throwsCommandException() throws IOException {
        Model modelWithoutStorage = new ModelManager(); // No storage provided
        ListBackupsCommand command = new ListBackupsCommand();
        assertThrows(CommandException.class, () -> command.execute(modelWithoutStorage));
    }
}