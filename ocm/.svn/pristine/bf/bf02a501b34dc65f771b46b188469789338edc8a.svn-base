package com.tible.ocm.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.tible.ocm.utils.ImportHelper.getFilename;

@Slf4j
public class RejectedFilesUtils {

    private RejectedFilesUtils() {
    }

    public static boolean deleteRejectedTransactionFiles(Path path) {
        String fileNameBase = getFilename(path);
        Path parentPath = path.getParent();
        Path csvPath = parentPath.resolve(fileNameBase + ".csv");
        Path hashPath = parentPath.resolve(fileNameBase + ".hash");
        try {
            Files.deleteIfExists(csvPath);
            Files.deleteIfExists(hashPath);
            Files.deleteIfExists(path);

            return true;
        } catch (IOException | UncheckedIOException e) {
            log.warn("Could not remove transactions rejected files from company ip directory", e);
            return false;
        }
    }

    public static boolean deleteRejectedBagFiles(Path path) {
        String fileNameBase = getFilename(path);
        Path parentPath = path.getParent();
        try {
            Files.deleteIfExists(parentPath.resolve(fileNameBase + ".batch"));
            Files.deleteIfExists(parentPath.resolve(fileNameBase + "_batch.hash"));
            Files.deleteIfExists(parentPath.resolve(fileNameBase + ".sls"));
            Files.deleteIfExists(parentPath.resolve(fileNameBase + "_sls.hash"));
            Files.deleteIfExists(parentPath.resolve(fileNameBase + ".nls"));
            Files.deleteIfExists(parentPath.resolve(fileNameBase + "_nls.hash"));
            Files.deleteIfExists(parentPath.resolve(fileNameBase + ".ready"));
            Files.deleteIfExists(parentPath.resolve(fileNameBase + "_ready.hash"));
            Files.deleteIfExists(path);

            return true;
        } catch (IOException e) {
            log.warn("Could not remove bags rejected files from company ip directory", e);
            return false;
        }
    }
}
