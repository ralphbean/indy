/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.tensor.webctl;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.maven.mae.MAEException;
import org.commonjava.aprox.tensor.maven.MavenComponentProvider;
import org.commonjava.tensor.data.TensorDataManager;
import org.commonjava.util.logging.Logger;

@WebListener
public class TensorInstallerListener
    implements ServletContextListener
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private TensorDataManager dataManager;

    @Inject
    private MavenComponentProvider mavenProvider;

    @Override
    public void contextInitialized( final ServletContextEvent sce )
    {
        logger.info( "Initializing Maven components" );
        try
        {
            mavenProvider.startMAE();
        }
        catch ( final MAEException e )
        {
            throw new RuntimeException( "Failed to initialize Maven components: " + e.getMessage(), e );
        }
    }

    @Override
    public void contextDestroyed( final ServletContextEvent sce )
    {
        // NOP
    }

}