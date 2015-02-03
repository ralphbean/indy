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
package org.commonjava.aprox.boot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class BootOptions
{

    public static final String BIND_PROP = "bind";

    public static final String PORT_PROP = "port";

    public static final String CONFIG_PROP = "config";

    public static final String WORKERS_PROP = "workers";

    public static final String CONTEXT_PATH_PROP = "context-path";

    public static final String DEFAULT_BIND = "0.0.0.0";

    public static final int DEFAULT_PORT = 8080;

    public static final int DEFAULT_WORKERS_COUNT = 5;

    @Option( name = "-h", aliases = { "--help" }, usage = "Print this and exit" )
    private boolean help;

    @Option( name = "-i", aliases = { "--interface", "--bind", "--listen" }, usage = "Bind to a particular IP address (default: 0.0.0.0, or all available)" )
    private String bind;

    @Option( name = "-p", aliases = { "--port" }, usage = "Use different port (default: 8080)" )
    private Integer port;

    @Option( name = "-c", aliases = { "--config" }, usage = "Use an alternative configuration file (default: <aprox-home>/etc/aprox/main.conf)" )
    private String config;

    @Option( name = "-w", aliases = { "--workers" }, usage = "Number of worker threads to serve content (default: 5)" )
    private Integer workers;

    @Option( name = "-C", aliases = { "--context-path" }, usage = "Specify a root context path for all of aprox to use" )
    private String contextPath;

    private StringSearchInterpolator interp;

    private Properties bootProps;

    private String aproxHome;

    public static final BootOptions loadFromSysprops()
        throws AproxBootException
    {
        final String bootDef = System.getProperty( BootInterface.BOOT_DEFAULTS_PROP );
        File bootDefaults = null;
        if ( bootDef != null )
        {
            bootDefaults = new File( bootDef );
        }

        try
        {
            final String aproxHome =
                System.getProperty( BootInterface.APROX_HOME_PROP, new File( "." ).getCanonicalPath() );

            return new BootOptions( bootDefaults, aproxHome );
        }
        catch ( final IOException e )
        {
            throw new AproxBootException( "ERROR LOADING BOOT DEFAULTS: %s.\nReason: %s\n\n", e, bootDefaults,
                                          e.getMessage() );
        }
        catch ( final InterpolationException e )
        {
            throw new AproxBootException( "ERROR RESOLVING BOOT DEFAULTS: %s.\nReason: %s\n\n", e, bootDefaults,
                                          e.getMessage() );
        }
    }

    public void setSystemProperties()
    {
        final Properties properties = System.getProperties();

        System.out.printf( "\n\nUsing AProx configuration: %s\n", config );
        properties.setProperty( BootInterface.CONFIG_PATH_PROP, config );
        properties.setProperty( BootInterface.APROX_HOME_PROP, aproxHome );
        System.setProperties( properties );
    }

    public BootOptions()
    {

    }

    public BootOptions( final String aproxHome )
        throws IOException, InterpolationException
    {
        this( null, aproxHome );
    }

    public BootOptions( final File bootDefaults, final String aproxHome )
        throws IOException, InterpolationException
    {
        this.aproxHome = aproxHome;
        this.bootProps = new Properties();

        if ( bootDefaults != null && bootDefaults.exists() )
        {
            FileInputStream stream = null;
            try
            {
                stream = new FileInputStream( bootDefaults );

                bootProps.load( stream );
            }
            finally
            {
                IOUtils.closeQuietly( stream );
            }
        }

        if ( bind == null )
        {
            bind = resolve( bootProps.getProperty( BIND_PROP, DEFAULT_BIND ) );
        }

        if ( port == null )
        {
            port = Integer.parseInt( resolve( bootProps.getProperty( PORT_PROP, Integer.toString( DEFAULT_PORT ) ) ) );
        }

        if ( workers == null )
        {
            workers =
                Integer.parseInt( resolve( bootProps.getProperty( WORKERS_PROP,
                                                                  Integer.toString( DEFAULT_WORKERS_COUNT ) ) ) );
        }

        if ( config == null )
        {
            final String defaultConfigPath = new File( aproxHome, "etc/aprox/main.conf" ).getPath();
            config = resolve( bootProps.getProperty( CONFIG_PROP, defaultConfigPath ) );
        }

        contextPath = bootProps.getProperty( CONTEXT_PATH_PROP, contextPath );
    }

    public String resolve( final String value )
        throws InterpolationException
    {
        if ( value == null || value.trim()
                                   .length() < 1 )
        {
            return null;
        }

        if ( bootProps == null )
        {
            if ( aproxHome == null )
            {
                return value;
            }
            else
            {
                bootProps = new Properties();
            }
        }

        bootProps.setProperty( "aprox.home", aproxHome );

        if ( interp == null )
        {
            interp = new StringSearchInterpolator();
            interp.addValueSource( new PropertiesBasedValueSource( bootProps ) );
        }

        return interp.interpolate( value );
    }

    public int getWorkers()
    {
        return workers;
    }

    public boolean isHelp()
    {
        return help;
    }

    public String getBind()
    {
        return bind;
    }

    public int getPort()
    {
        return port;
    }

    public String getConfig()
    {
        return config;
    }

    public BootOptions setHelp( final boolean help )
    {
        this.help = help;
        return this;
    }

    public BootOptions setBind( final String bind )
    {
        this.bind = bind;
        return this;
    }

    public BootOptions setPort( final int port )
    {
        this.port = port;
        return this;
    }

    public BootOptions setConfig( final String config )
    {
        this.config = config;
        return this;
    }

    public BootOptions setWorkers( final int workers )
    {
        this.workers = workers;
        return this;
    }

    public String getContextPath()
    {
        if ( contextPath == null )
        {
            return null;
        }

        if ( !contextPath.startsWith( "/" ) )
        {
            contextPath = "/" + contextPath;
        }

        return contextPath;
    }

    public void setContextPath( final String contextPath )
    {
        this.contextPath = contextPath;
    }

    public boolean parseArgs( final String[] args )
        throws AproxBootException
    {
        final CmdLineParser parser = new CmdLineParser( this );
        boolean canStart = true;
        try
        {
            parser.parseArgument( args );
        }
        catch ( final CmdLineException e )
        {
            throw new AproxBootException( "Failed to parse command-line args: %s", e, e.getMessage() );
        }

        if ( isHelp() )
        {
            printUsage( parser, null );
            canStart = false;
        }

        return canStart;
    }

    public static void printUsage( final CmdLineParser parser, final CmdLineException error )
    {
        if ( error != null )
        {
            System.err.println( "Invalid option(s): " + error.getMessage() );
            System.err.println();
        }

        System.err.println( "Usage: $0 [OPTIONS] [<target-path>]" );
        System.err.println();
        System.err.println();
        // If we are running under a Linux shell COLUMNS might be available for the width
        // of the terminal.
        parser.setUsageWidth( ( System.getenv( "COLUMNS" ) == null ? 100 : Integer.valueOf( System.getenv( "COLUMNS" ) ) ) );
        parser.printUsage( System.err );
        System.err.println();
    }

    public String getAproxHome()
    {
        return aproxHome;
    }

    public void setAproxHome( final String aproxHome )
    {
        this.aproxHome = aproxHome;
    }

    public void setPort( final Integer port )
    {
        this.port = port;
    }

    public void setWorkers( final Integer workers )
    {
        this.workers = workers;
    }

}