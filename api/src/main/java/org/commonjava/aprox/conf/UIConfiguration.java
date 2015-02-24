/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.conf;

import java.io.File;
import java.io.InputStream;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigNames;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "ui" )
@Alternative
@Named( "unused" )
public class UIConfiguration
{

    @javax.enterprise.context.ApplicationScoped
    public static class UIFeatureConfig
        extends AbstractAproxFeatureConfig<UIConfiguration, UIConfiguration>
    {
        @Inject
        private UIConfigInfo info;

        public UIFeatureConfig()
        {
            super( UIConfiguration.class );
        }

        @Produces
        @Default
        public UIConfiguration getFlatFileConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public AproxConfigClassInfo getInfo()
        {
            return info;
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class UIConfigInfo
        extends AbstractAproxConfigInfo
    {
        public UIConfigInfo()
        {
            super( UIConfiguration.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return AproxConfigInfo.APPEND_DEFAULTS_TO_MAIN_CONF;
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-ui.conf" );
        }
    }

    public static final File DEFAULT_DIR = new File( System.getProperty( "aprox.home", "/var/lib/aprox" ), "ui" );

    private File uiDir;

    public UIConfiguration()
    {
    }

    @ConfigNames( "ui.dir" )
    public UIConfiguration( final File uiDir )
    {
        this.uiDir = uiDir;
    }

    public File getUIDir()
    {
        return uiDir == null ? DEFAULT_DIR : uiDir;
    }

    public void setUIDir( final File uiDir )
    {
        this.uiDir = uiDir;
    }

}