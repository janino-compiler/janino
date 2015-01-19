
/*
 * JDISASM - A Java[TM] class file disassembler
 *
 * Copyright (c) 2015, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unkrig.jdisasm.protocol.zip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import de.unkrig.commons.nullanalysis.Nullable;

/**
 * A {@link URLStreamHandler} for the protocol "{@code zip}".
 * <p>The path component is interpreted as follows:</p>
 * <quote><code><i>container-url</i>!<i>entry-name</i></code></quote>
 * <p>The contents of the container must be in zip archive format.</p>
 */
public
class Handler extends URLStreamHandler {

    /**
     * See {@link URL#URL(String, String, int, String)}
     */
    private static final String SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS = "java.protocol.handler.pkgs";

    static {

        String packagE;
        {
            packagE = Handler.class.getPackage().getName();
            if (!packagE.endsWith(".zip")) throw new ExceptionInInitializerError();
            packagE = packagE.substring(0, packagE.length() - 4);
        }

        String phps = System.getProperty(Handler.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS);
        if (phps == null) {
            System.setProperty(Handler.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS, packagE);
        } else {
            if (!Arrays.asList(phps.split("\\|")).contains(packagE)) {
                System.setProperty(Handler.SYSTEM_PROPERTY_PROTOCOL_HANDLER_PKGS, phps + '|' + packagE);
            }
        }
    }

    /**
     * Registers this {@link URLStreamHandler} with the JVM.
     */
    public static void
    registerMe() {

        // Nothing to do, because the work is done in the static initializer.
    }

    @Override protected URLConnection
    openConnection(@Nullable URL url) {
        assert url != null;

        return new URLConnection(url) {

            @Nullable private URL    zipContainer;
            @Nullable private String entryName;

            @Override public void
            connect() throws IOException {

                if (this.connected) return;

                final String authority = this.url.getAuthority();
                final String host      = this.url.getHost();
                final String path      = this.url.getPath();
                final int    port      = this.url.getPort();
                final String query     = this.url.getQuery();
                final String ref       = this.url.getRef();
                final String userInfo  = this.url.getUserInfo();

                if (authority != null) throw new IllegalArgumentException("'Authority' not allowed in 'zip' scheme");
                if (host.length() > 0) throw new IllegalArgumentException("'Host' not allowed in 'zip' scheme");
                if (port != -1)        throw new IllegalArgumentException("'Port' not allowed in 'zip' scheme");
                if (query != null)     throw new IllegalArgumentException("'Query' not allowed in 'zip' scheme");
                if (ref != null)       throw new IllegalArgumentException("'Fragment' not allowed in 'zip' scheme");
                if (userInfo != null)  throw new IllegalArgumentException("'User info' not allowed in 'zip' scheme");

                int excl = path.lastIndexOf('!');
                if (excl == -1) throw new IllegalArgumentException("'!' missing in 'zip' url");

                this.zipContainer = new URL(path.substring(0, excl));
                this.entryName    = path.substring(excl + 1);

                this.connected = true;
            }

            @Override public InputStream
            getInputStream() throws IOException {

                // Implicitly invoke "connect()"
                this.connect();
                URL    zipContainer = this.zipContainer;
                String entryName    = this.entryName;
                assert zipContainer != null;
                assert entryName != null;

                // Open the ZIP container.
                ZipInputStream zis = new ZipInputStream(zipContainer.openStream());

                // Fast forward up to the requested entry.
                for (;;) {
                    ZipEntry ze = zis.getNextEntry();
                    if (ze == null) {

                        // ZIP archive end-of-input.
                        try { zis.close(); } catch (Exception e) {}
                        throw new FileNotFoundException(zipContainer.toString() + '!' + entryName);
                    }
                    if (ze.getName().equals(entryName)) return zis;
                }
            }
        };
    }
}
