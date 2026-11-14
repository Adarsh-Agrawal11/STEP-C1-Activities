package com.gdb.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Manages timestamped backups of data files and maintains retention limits.
 */
public class BackupManager {

    // ============================================================
    // 📝 STEP 6: Implement backup(String path, int maxBackups)
    //
    // INSTRUCTIONS:
    //   1. If target path does not exist, return early.
    //   2. Copy target file to path + ".bak." + System.currentTimeMillis().
    //   3. Find all existing backup files for target path.
    //   4. If backup count exceeds maxBackups, delete the oldest backups.
    // ============================================================
    public static void backup(String path, int maxBackups) throws IOException {
        Path source = Paths.get(path);
        if (!Files.exists(source)) return;
        if (maxBackups <= 0) return;
        String backupName = path + ".bak." + System.currentTimeMillis();
        Files.copy(source, Paths.get(backupName), StandardCopyOption.REPLACE_EXISTING);
        File parent = source.toAbsolutePath().getParent().toFile();
        String prefix = source.getFileName().toString() + ".bak.";
        File[] backups = parent.listFiles((dir, name) -> name.startsWith(prefix));
        if (backups != null && backups.length > maxBackups) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < backups.length - maxBackups; i++) Files.deleteIfExists(backups[i].toPath());
        }
    }
}
