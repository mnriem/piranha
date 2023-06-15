/*
 * Copyright (c) 2002-2023 Manorrock.com. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 *   2. Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *   3. Neither the name of the copyright holder nor the names of its
 *      contributors may be used to endorse or promote products derived from
 *      this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package cloud.piranha.dist.platform;

import java.io.File;
import java.lang.System.Logger.Level;

import static javax.naming.Context.INITIAL_CONTEXT_FACTORY;
import cloud.piranha.core.api.WebApplicationExtension;
import static java.lang.System.Logger.Level.WARNING;

/**
 * The builder so you can easily build instances of
 * {@link cloud.piranha.dist.platform.PlatformPiranha}.
 *
 * @author Manfred Riem (mriem@manorrock.com)
 * @see cloud.piranha.dist.platform.PlatformPiranha
 */
public class PlatformPiranhaBuilder {

    /**
     * Stores the logger.
     */
    private static final System.Logger LOGGER = System.getLogger(PlatformPiranhaBuilder.class.getName());

    /**
     * Stores the default extension class.
     */
    private Class<? extends WebApplicationExtension> defaultExtensionClass;

    /**
     * Stores the exit on stop flag.
     */
    private boolean exitOnStop = false;

    /**
     * Stores the HTTP port.
     */
    private int httpPort = 8080;
    
    /**
     * Stores the HTTP server class.
     */
    private String httpServerClass;

    /**
     * Stores the HTTPS keystore file.
     */
    private String httpsKeystoreFile;

    /**
     * Stores the HTTPS keystore password.
     */
    private String httpsKeystorePassword;

    /**
     * Stores the HTTPS port.
     */
    private int httpsPort = -1;

    /**
     * Stores the HTTPS server class.
     */
    private String httpsServerClass;

    /**
     * Stores the HTTPS truststore file.
     */
    private String httpsTruststoreFile;

    /**
     * Stores the HTTPS truststore password.
     */
    private String httpsTruststorePassword;

    /**
     * Stores the InitialContext factory.
     */
    private String initialContextFactory = "cloud.piranha.naming.thread.ThreadInitialContextFactory";

    /**
     * Stores the JPMS flag.
     */
    private boolean jpms = false;

    /**
     * Stores the verbose flag.
     */
    private boolean verbose = false;

    /**
     * Stores the web applications directory.
     */
    private String webAppsDir = "webapps";

    /**
     * Build the server.
     *
     * @return the server.
     */
    public PlatformPiranha build() {
        if (verbose) {
            showArguments();
        }
        System.setProperty(INITIAL_CONTEXT_FACTORY, initialContextFactory);
        PlatformPiranha piranha = new PlatformPiranha();
        piranha.setDefaultExtensionClass(defaultExtensionClass);
        piranha.setExitOnStop(exitOnStop);
        piranha.setHttpPort(httpPort);
        piranha.setHttpServerClass(httpServerClass);
        piranha.setHttpsPort(httpsPort);
        piranha.setHttpsServerClass(httpsServerClass);
        piranha.setJpmsEnabled(jpms);
        if (httpsKeystoreFile != null) {
            piranha.setHttpsKeystoreFile(httpsKeystoreFile);
        }
        if (httpsKeystorePassword != null) {
            piranha.setHttpsKeystorePassword(httpsKeystorePassword);
        }
        if (httpsTruststoreFile != null) {
            piranha.setHttpsTruststoreFile(httpsTruststoreFile);
        }
        if (httpsTruststorePassword != null) {
            piranha.setHttpsTruststorePassword(httpsTruststorePassword);
        }
        piranha.setWebAppsDir(new File(webAppsDir));
        return piranha;
    }

    /**
     * Set the default extension class.
     *
     * @param defaultExtensionClass the default extension class.
     * @return the builder.
     */
    public PlatformPiranhaBuilder defaultExtensionClass(Class<? extends WebApplicationExtension> defaultExtensionClass) {
        this.defaultExtensionClass = defaultExtensionClass;
        return this;
    }

    /**
     * Set the default extension class.
     *
     * @param defaultExtensionClassName the default extension class name.
     * @return the builder.
     */
    public PlatformPiranhaBuilder defaultExtensionClass(String defaultExtensionClassName) {
        try {
            this.defaultExtensionClass = Class.forName(defaultExtensionClassName)
                    .asSubclass(WebApplicationExtension.class);
        } catch (ClassNotFoundException cnfe) {
            LOGGER.log(WARNING, "Unable to load default extension class", cnfe);
        }
        return this;
    }

    /**
     * Set the exit on stop flag.
     *
     * @param exitOnStop the exit on stop flag.
     * @return the builder.
     */
    public PlatformPiranhaBuilder exitOnStop(boolean exitOnStop) {
        this.exitOnStop = exitOnStop;
        return this;
    }

    /**
     * Set the HTTP server port.
     *
     * @param httpPort the HTTP server port.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpPort(int httpPort) {
        this.httpPort = httpPort;
        return this;
    }

    /**
     * Set the HTTP server class.
     * 
     * @param httpServerClass the HTTP server class.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpServerClass(String httpServerClass) {
        this.httpServerClass = httpServerClass;
        return this;
    }
    
    /**
     * Set the HTTPS keystore file.
     *
     * @param httpsKeystoreFile the HTTPS keystore file.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpsKeystoreFile(String httpsKeystoreFile) {
        this.httpsKeystoreFile = httpsKeystoreFile;
        return this;
    }

    /**
     * Set the HTTPS keystore password.
     *
     * @param httpsKeystorePassword the HTTPS keystore password.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpsKeystorePassword(String httpsKeystorePassword) {
        this.httpsKeystorePassword = httpsKeystorePassword;
        return this;
    }

    /**
     * Set the HTTPS server port.
     *
     * @param httpsPort the HTTPS server port.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpsPort(int httpsPort) {
        this.httpsPort = httpsPort;
        return this;
    }

    /**
     * Set the HTTPS server class.
     * 
     * @param httpsServerClass the HTTPS server class.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpsServerClass(String httpsServerClass) {
        this.httpsServerClass = httpsServerClass;
        return this;
    }

    /**
     * Set the HTTPS truststore file.
     *
     * @param httpsTruststoreFile the HTTPS truststore file.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpsTruststoreFile(String httpsTruststoreFile) {
        this.httpsTruststoreFile = httpsTruststoreFile;
        return this;
    }

    /**
     * Set the HTTPS truststore password.
     *
     * @param httpsTruststorePassword the HTTPS truststore password.
     * @return the builder.
     */
    public PlatformPiranhaBuilder httpsTruststorePassword(String httpsTruststorePassword) {
        this.httpsTruststorePassword = httpsTruststorePassword;
        return this;
    }

    /**
     * Enable/disable JPMS.
     *
     * @param jpms the JPMS flag.
     * @return the builder.
     */
    public PlatformPiranhaBuilder jpms(boolean jpms) {
        this.jpms = jpms;
        return this;
    }

    /**
     * Show the arguments used.
     */
    private void showArguments() {
        LOGGER.log(Level.INFO, """
                
                PIRANHA
                
                Arguments
                =========
                
                Default extension class   : %s
                Exit on stop              : %s
                HTTP port                 : %s
                HTTP server class         : %s
                HTTPS keystore file       : %s
                HTTPS keystore password   : ****
                HTTPS port                : %s
                HTTPS server class        : %s
                HTTPS truststore file     : %s
                HTTPS truststore password : ****
                JPMS enabled              : %s
                Web applications dir      : %s
                
                """.formatted(
                defaultExtensionClass.getName(),
                exitOnStop,
                httpPort,
                httpServerClass,
                httpsKeystoreFile,
                httpsPort,
                httpsServerClass,
                httpsTruststoreFile,
                jpms,
                webAppsDir
        ));
    }

    /**
     * Set the SSL truststore file.
     *
     * @param sslTruststoreFile the SSL truststore file.
     * @return the builder.
     * @deprecated
     */
    @Deprecated(since = "23.7.0", forRemoval = true)
    public PlatformPiranhaBuilder sslTruststoreFile(String sslTruststoreFile) {
        this.httpsTruststoreFile = sslTruststoreFile;
        return this;
    }

    /**
     * Set the SSL truststore password.
     *
     * @param sslTruststorePassword the SSL truststore password.
     * @return the builder.
     * @deprecated
     */
    @Deprecated(since = "23.7.0", forRemoval = true)
    public PlatformPiranhaBuilder sslTruststorePassword(String sslTruststorePassword) {
        this.httpsTruststorePassword = sslTruststorePassword;
        return this;
    }

    /**
     * Set the verbose flag.
     *
     * @param verbose the verbose flag.
     * @return the builder.
     */
    public PlatformPiranhaBuilder verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }

    /**
     * Set the web applications directory.
     *
     * @param webAppsDir the web applications directory.
     * @return the builder.
     */
    public PlatformPiranhaBuilder webAppsDir(String webAppsDir) {
        this.webAppsDir = webAppsDir;
        return this;
    }
}
