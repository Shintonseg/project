package com.tible.ocm.utils;

import com.google.common.base.Strings;
import com.jcraft.jsch.SftpException;
import com.tible.ocm.services.impl.RvmSupplierFtpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class ImportRvmSupplierHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportRvmSupplierHelper.class);

    public static final DateTimeFormatter DATETIMEFORMATTERMILLIS = new DateTimeFormatterBuilder().appendPattern("yyyyMMddHHmmss")
            .appendValue(ChronoField.MILLI_OF_SECOND, 3).toFormatter();

    public static final DateTimeFormatter DATETIMEFORMATTER = new DateTimeFormatterBuilder().appendPattern("yyyyMMddHHmmss").toFormatter();
    public static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void copyFilesFromRvmSupplier(Path copyToPath, String fromDirectory, RvmSupplierFtpConnection connection, boolean removeFromTarget) {
        String target = copyToPath.toAbsolutePath().toString();
        connection.cd(fromDirectory).list("*.csv", entry -> {
            String fileNameBase = entry.getFilename().substring(0, entry.getFilename().lastIndexOf('.'));
            try {
                boolean csvFileRetrieved = connection.getFromFtp(target, fileNameBase + ".csv");
                if (csvFileRetrieved) {
                    LOGGER.info("{} moved", fileNameBase + ".csv");
                    if (removeFromTarget) {
                        boolean csvFileRemoved = connection.removeFromFtp(fileNameBase + ".csv");
                        if (csvFileRemoved) {
                            LOGGER.info("{} file removed from ftp", fileNameBase + ".csv");
                        }
                    }
                } else {
                    LOGGER.info("{} failed to be retrieved", fileNameBase + ".csv");
                }

                if (connection.exists(fileNameBase + ".hash")) {
                    boolean hashFileRetrieved =connection.getFromFtp(target, fileNameBase + ".hash");
                    if (hashFileRetrieved) {
                        LOGGER.info("{} moved", fileNameBase + ".hash");
                        if (removeFromTarget) {
                            boolean hashFileRemoved = connection.removeFromFtp(fileNameBase + ".hash");
                            if (hashFileRemoved) {
                                LOGGER.info("{} file removed from ftp", fileNameBase + ".hash");
                            }
                        }
                    }
                }
            } catch (SftpException e) {
                LOGGER.error("Moving csv files failed, " + fileNameBase, e);
            }
        }).cd("..");
    }

    public static void copyFilesToRvmSupplier(Path copyFromPath, String copyToPath, String copyToSubPath, RvmSupplierFtpConnection connection, boolean removeFromSource) {
        connection.cd(copyToPath);
        if (copyToSubPath != null) {
            connection.cd(copyToSubPath);
        }
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(copyFromPath)) {
            for (final Path filePath : directoryStream) {
                if (Files.isDirectory(filePath)) {
                    continue;
                }

                final String fileName = filePath.getFileName().toString();
                try {
                    connection.copyToFtp(filePath, fileName, removeFromSource);
                    LOGGER.info("{} moved to RVM supplier", fileName);
                } catch (SftpException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }

        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        if (copyToSubPath != null) {
            connection.cd("..");
        }
        connection.cd("..");
    }

    // append zeroes in right side of string
    public static String padZeroesInRight(String string, int length) {
        return Strings.padEnd(string == null ? "" : string, length, '0');
    }
}
