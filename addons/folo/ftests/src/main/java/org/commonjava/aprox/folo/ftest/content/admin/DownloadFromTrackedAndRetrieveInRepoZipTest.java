/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.folo.ftest.content.admin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.folo.client.AproxFoloAdminClientModule;
import org.commonjava.aprox.folo.client.AproxFoloContentClientModule;
import org.commonjava.aprox.folo.ftest.content.AbstractFoloContentManagementTest;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.commonjava.aprox.model.core.StoreType.remote;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DownloadFromTrackedAndRetrieveInRepoZipTest
        extends AbstractFoloContentManagementTest
{

    @Test
    public void run()
            throws Exception
    {
        final String trackingId = newName();
        String path = "org/commonjava/commonjava/2/commonjava-2.pom";

        InputStream result =
                client.module( AproxFoloContentClientModule.class ).get( trackingId, remote, CENTRAL, path );
        assertThat( result, notNullValue() );

        final String pom = IOUtils.toString( result );
        result.close();
        assertThat( pom.contains( "<groupId>org.commonjava</groupId>" ), equalTo( true ) );

        result = client.module( AproxFoloAdminClientModule.class ).getTrackingRepoZip( trackingId );

        assertThat( result, notNullValue() );

        // ZipInputStream wrapping this resulting InputStream didn't seem to work...I was probably doing something wrong
        File f = getTemp().newFile( "downloaded.zip" );
        try (FileOutputStream fos = new FileOutputStream( f ))
        {
            IOUtils.copy( result, fos );
        }

        ZipFile zf = new ZipFile( f );
        ZipEntry entry = zf.getEntry( path );
        assertThat( entry, notNullValue() );

        try(InputStream stream = zf.getInputStream( entry ))
        {
            String fromZip = IOUtils.toString( stream );
            assertThat( "zip contents differ from direct download!", fromZip, equalTo( pom ) );
        }
    }

}