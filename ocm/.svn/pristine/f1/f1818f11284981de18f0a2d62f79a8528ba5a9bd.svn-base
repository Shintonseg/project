package com.tible.ocm.services.impl;

import com.jcraft.jsch.*;
import com.tible.hawk.core.models.BaseEventLog;
import com.tible.hawk.core.services.BaseEventLogService;
import com.tible.ocm.models.RvmSupplierYml;
import com.tible.ocm.services.DirectoryService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The class Machine connector.
 *
 * @author tible
 */
@Service
public class RvmSupplierFtpConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(RvmSupplierFtpConnector.class);

    private static final String KEY_FILE = "/id_rsa";

    @Value("${sftp-rvm.main-directory:#{null}}")
    private String mainDirectory;

    // 300000 milliseconds = 5 minutes
    @Value("${sftp-rvm.session.alive-interval:300000}")
    private int sessionAliveInterval;

    @Value("${sftp-rvm.session.alive-count-max:5}")
    private int sessionAliveCountMax;

    private final BaseEventLogService<BaseEventLog> eventLogService;

    private final DirectoryService directoryService;

    public RvmSupplierFtpConnector(BaseEventLogService<BaseEventLog> eventLogService,
                                   DirectoryService directoryService) {
        this.eventLogService = eventLogService;
        this.directoryService = directoryService;
    }

    /**
     * Connect and run.
     *
     * @param rvmSupplierYml the RVM supplier to connect to
     * @param toExecute the to execute
     */
    public void connectAndRun(RvmSupplierYml rvmSupplierYml, Consumer<RvmSupplierFtpConnection> toExecute) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = jsch.getSession(rvmSupplierYml.getUsername(), rvmSupplierYml.getIp(), 22);
            if (StringUtils.isEmpty(rvmSupplierYml.getPassword())) {
                //TODO: do something like https://www.programcreek.com/java-api-examples/?class=com.jcraft.jsch.JSch&method=addIdentity
                try (InputStream keyFile = getClass().getResourceAsStream(KEY_FILE)) {
                    FileUtils.copyInputStreamToFile(keyFile, directoryService.getRvmPath().resolve("id_rsa").toFile());
                    jsch.addIdentity(directoryService.getRvmPath().resolve("id_rsa").toFile().getAbsolutePath());
                }
            } else {
                session.setPassword(rvmSupplierYml.getPassword());
            }
            session.setConfig("StrictHostKeyChecking", "no");
            session.setServerAliveInterval(sessionAliveInterval);
            session.setServerAliveCountMax(sessionAliveCountMax);
            session.connect();
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            if (!StringUtils.isEmpty(mainDirectory)) {
                channel.cd(mainDirectory);
            }
            toExecute.accept(new RvmSupplierFtpConnection(channel, rvmSupplierYml));
            channel.disconnect();
            session.disconnect();
        } catch (JSchException | IOException | SftpException e) {
            LOGGER.warn("Failed to connect to " + rvmSupplierYml.getIp(), e);
            logFailedToConnect(rvmSupplierYml);
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void logFailedToConnect(final RvmSupplierYml rvmSupplierYml) {
        final Map<String, Object> changes = new HashMap<>();
        String message = "Failed to connect to machine (ip = " + rvmSupplierYml.getIp() + ")";
        changes.put("message", message);
        eventLogService.createEvent(BaseEventLog.BaseFunctionName.FAIL, rvmSupplierYml, changes, false,
                LocalDateTime.now(), null);
    }
}
