package com.tible.ocm.utils;

import com.tible.ocm.dto.helper.AAFiles;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;

import static com.tible.ocm.utils.ImportHelper.*;

@Slf4j
public class OcmFileUtils {

    /**
     * Check or create dir boolean.
     *
     * @param path the path
     * @return the boolean
     */
    public static boolean checkOrCreateDirWithFullPermissions(Path path) {
        Set<PosixFilePermission> fullPermission = new HashSet<PosixFilePermission>();
        fullPermission.add(PosixFilePermission.OWNER_EXECUTE);
        fullPermission.add(PosixFilePermission.OWNER_READ);
        fullPermission.add(PosixFilePermission.OWNER_WRITE);

        fullPermission.add(PosixFilePermission.GROUP_EXECUTE);
        fullPermission.add(PosixFilePermission.GROUP_READ);
        fullPermission.add(PosixFilePermission.GROUP_WRITE);

        fullPermission.add(PosixFilePermission.OTHERS_EXECUTE);
        fullPermission.add(PosixFilePermission.OTHERS_READ);
        fullPermission.add(PosixFilePermission.OTHERS_WRITE);

        if (Files.exists(path)) {
            return true;
        }

        if (!Files.isDirectory(path)) {
            try {
                Files.createDirectories(path, PosixFilePermissions.asFileAttribute(fullPermission));
                Files.setPosixFilePermissions(path, fullPermission);
            } catch (IOException e) {
                log.error("Creating directory failed: " + path.toString(), e);
                return false;
            } catch (UnsupportedOperationException e) {
                // This is for windows to create the directories
                log.debug("Posix permissions not supported, trying other way: " + path.toString(), e);
                try {
                    Files.createDirectories(path);
                }  catch (IOException error) {
                    log.error("Creating directory failed: " + path.toString(), e);
                    return false;
                }
            }
        }

        return true;
    }

    public static AAFiles getAAFiles(Path readyPath, Path parent, String fileNameBase) {
        return AAFiles.builder().readyPath(readyPath).readyHashPath(parent.resolve(fileNameBase + READY_HASH_FILE_FORMAT))
                .batchPath(parent.resolve(fileNameBase + BATCH_FILE_FORMAT)).batchHashPath(parent.resolve(fileNameBase + BATCH_HASH_FILE_FORMAT))
                .slsPath(parent.resolve(fileNameBase + SLS_FILE_FORMAT)).slsHashPath(parent.resolve(fileNameBase + SLS_HASH_FILE_FORMAT))
                .nlsPath(parent.resolve(fileNameBase + NLS_FILE_FORMAT)).nlsHashPath(parent.resolve(fileNameBase + NLS_HASH_FILE_FORMAT))
                .errorFile(parent.resolve(fileNameBase + ERROR_FILE_FORMAT)).build();
    }

    public static void clearDirectory(Path dir) {
        log.info("Trying to clear the {} folder", dir.toString());
        try {
            com.tible.hawk.core.utils.FileUtils.delete(dir);
            com.tible.hawk.core.utils.FileUtils.checkOrCreateDir(dir);
        } catch (IOException e) {
            log.warn("Exception during clearing: ", e);
        }
    }
}
