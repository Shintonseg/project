package com.tible.ocm.services.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import com.tible.ocm.models.RvmSupplierYml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

/**
 * The class Machine connection.
 *
 * @author tible
 */
public class RvmSupplierFtpConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(RvmSupplierFtpConnection.class);

    private ChannelSftp channel;
    private RvmSupplierYml rvmSupplierYml;

    private static final String NO_SUCH_FILE = "No such file";
    private static final String FILE_MISSING = ": file missing: ";

    /**
     * Instantiates a new Machine connection.
     *
     * @param channel the channel
     * @param rvmSupplierYml the RVM supplier
     */
    /*package-private*/ RvmSupplierFtpConnection(ChannelSftp channel, RvmSupplierYml rvmSupplierYml) {
        this.channel = channel;
        this.rvmSupplierYml = rvmSupplierYml;
    }

    /**
     * List machine connection.
     *
     * @param path   the path
     * @param action the action
     * @return the machine connection
     */
    @SuppressWarnings("unchecked")
    public RvmSupplierFtpConnection list(String path, Consumer<LsEntry> action) {
        try {
            ((List<LsEntry>) channel.ls(path)).forEach(action);
        } catch (SftpException e) {
            LOGGER.error(rvmSupplierYml.getName() + ": Reading directory " + path + " failed", e);
        }
        return this;
    }

    /**
     * Cd machine connection.
     *
     * @param dir the dir
     * @return the machine connection
     */
    public RvmSupplierFtpConnection cd(String dir) {
        try {
            channel.cd(dir);
        } catch (SftpException e) {
            LOGGER.warn(rvmSupplierYml.getName() + ": Change to directory (cd) " + dir + " failed", e);
        }
        return this;
    }

    public boolean exists(String path) {
        Vector res = null;
        try {
            res = channel.ls(path);
        } catch (SftpException e) {
            if (e.id == SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            LOGGER.error("Unexpected exception during ls files on sftp: [{}:{}]", e.id, e.getMessage());
        }
        return res != null && !res.isEmpty();
    }

    /**
     * Gets channel.
     *
     * @return the channel
     */
    public ChannelSftp getChannel() {
        return channel;
    }

    /**
     * Move from ftp.
     *
     * @param outputLocation the output location
     * @param target         the target
     * @param removeFromTarget remove the target
     * @throws SftpException the sftp exception
     */
    public boolean getAndRemoveFromFtp(String outputLocation, String target, boolean removeFromTarget) throws SftpException {
        try {
            channel.get(target, outputLocation);
            if (removeFromTarget) {
                channel.rm(target);
            }
            return true;
        } catch (SftpException e) {
            if (!NO_SUCH_FILE.equals(e.getMessage())) {
                throw e;
            }
            LOGGER.warn(rvmSupplierYml.getName() + FILE_MISSING + target, e);
        }
        return false;
    }

    /**
     * Move from ftp.
     *
     * @param outputLocation the output location
     * @param target         the target
     * @throws SftpException the sftp exception
     */
    public boolean getFromFtp(String outputLocation, String target) throws SftpException {
        try {
            channel.get(target, outputLocation);
            return true;
        } catch (SftpException e) {
            if (!NO_SUCH_FILE.equals(e.getMessage())) {
                throw e;
            }
            LOGGER.warn(rvmSupplierYml.getName() + FILE_MISSING + target, e);
        }
        return false;
    }

    /**
     * Remove from ftp.
     *
     * @param target the target
     * @throws SftpException the sftp exception
     */
    public boolean removeFromFtp(String target) throws SftpException {
        try {
            channel.rm(target);
            return true;
        } catch (SftpException e) {
            if (!NO_SUCH_FILE.equals(e.getMessage())) {
                throw e;
            }
            LOGGER.warn(rvmSupplierYml.getName() + FILE_MISSING + target, e);
        }
        return false;
    }

    public void copyToFtp(Path source, String output, boolean removeFromSource) throws SftpException {
        try {
            channel.put(source.toAbsolutePath().toString(), output);
            if (removeFromSource) {
                Files.delete(source);
            }
        } catch (SftpException | IOException e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    public RvmSupplierYml getRvmSupplierYml() {
        return rvmSupplierYml;
    }
}
