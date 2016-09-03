/*
 * Copyright (c) 2016 Nova Ordis LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.novaordis.jboss.cli;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandContextFactory;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.parsing.ParserUtil;
import org.jboss.as.cli.parsing.operation.OperationFormat;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ovidiu Feodorov <ovidiu@novaordis.com>
 * @since 9/2/16
 */
public class JBossControllerClientImpl implements JBossControllerClient {

    // Constants -------------------------------------------------------------------------------------------------------

    private static final Logger log = LoggerFactory.getLogger(JBossControllerClientImpl.class);

    // Static ----------------------------------------------------------------------------------------------------------

    // Attributes ------------------------------------------------------------------------------------------------------

    private String host;
    private int port;
    private String username;
    private char[] password;
    private boolean disableLocalAuthentication;
    private boolean initializeConsole;
    private int connectionTimeout;

    private CommandContext commandContext;

    private boolean connected;

    // Constructors ----------------------------------------------------------------------------------------------------

    public JBossControllerClientImpl() {

        this.host = JBossControllerClient.DEFAULT_HOST;
        this.port = JBossControllerClient.DEFAULT_PORT;
        this.username = null;
        this.password = null;

        this.disableLocalAuthentication = false;
        this.initializeConsole = false;
        this.connectionTimeout = -1;
        this.connected = false;
    }

    // JBossControllerClient implementation ----------------------------------------------------------------------------

    @Override
    public void setHost(String host) {

        this.host = host;
    }

    @Override
    public String getHost() {

        return host;
    }

    @Override
    public void setPort(int port) {

        this.port = port;
    }

    @Override
    public int getPort() {

        return port;
    }

    @Override
    public void setUsername(String username) {

        this.username = username;
    }

    @Override
    public String getUsername() {

        return username;
    }

    @Override
    public void setPassword(char[] password) {

        this.password = new char[password.length];
        System.arraycopy(password, 0, this.password, 0, password.length);
    }

    @Override
    public char[] getPassword() {

        return password;
    }

    @Override
    public void connect() throws JBossCliException {

        if (connected) {
            log.warn(this + " already connected");
            return;
        }

        try {

            CommandContextFactory factory = CommandContextFactory.getInstance();
            commandContext = factory.newCommandContext(
                    host, port, username, password, disableLocalAuthentication, initializeConsole, connectionTimeout);
            commandContext.connectController();
            connected = true;
        }
        catch (Exception e) {
            throw new JBossCliException(e);
        }
    }

    @Override
    public void disconnect() {

        commandContext.disconnectController();
        connected = false;
    }

    @Override
    public boolean isConnected() {

        return connected;
    }

    @Override
    public String getAttributeValue(String path, String attributeName) throws JBossCliException {

        if (!connected) {

            throw new JBossCliException(this + " not connected");
        }

        String command = path + ":read-attribute(name=" + attributeName + ")";

        boolean validate = true;
        //noinspection ConstantConditions
        DefaultCallbackHandler parsedCommand = new DefaultCallbackHandler(validate);

        try {
            ParserUtil.parse(command, parsedCommand);
        }
        catch(Exception e) {
            throw new JBossCliException(e);
        }

        if (parsedCommand.getFormat() != OperationFormat.INSTANCE ) {

            //
            // we got this from the CLI code, what is "INSTANCE"?
            //

            throw new RuntimeException("NOT YET IMPLEMENTED");
        }

        ModelNode request;

        try {

            request = parsedCommand.toOperationRequest(commandContext);
        }
        catch (Exception e) {
            throw new JBossCliException(e);
        }

        ModelControllerClient client = commandContext.getModelControllerClient();

        ModelNode result;

        try {

            result = client.execute(request);
        }
        catch (Exception e) {
            throw new JBossCliException(e);
        }

        if(Util.isSuccess(result)) {

            System.out.println("success: " + result);

        } else {

            String failureDescription = Util.getFailureDescription(result);
            System.out.println("failure: " + failureDescription);
        }

        return "?";
    }

    // Public ----------------------------------------------------------------------------------------------------------

    // Package protected -----------------------------------------------------------------------------------------------

    // Protected -------------------------------------------------------------------------------------------------------

    // Private ---------------------------------------------------------------------------------------------------------

    // Inner classes ---------------------------------------------------------------------------------------------------

}