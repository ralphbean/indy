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

import org.commonjava.aprox.autoprox.data.*;
import java.net.MalformedURLException;
import org.commonjava.aprox.model.core.*;

class SonatypeRule extends AbstractAutoProxRule
{
    boolean matches( String named ){
        named.startsWith( "ST-" )
    }

    RemoteRepository createRemoteRepository( String named )
        throws MalformedURLException
    {
        def match = (named =~ /ST-(.+)/)[0]
        new RemoteRepository( name: named, url: "http://oss.sonatype.org/content/repositories/${match[1]}/" )
    }

    HostedRepository createHostedRepository( String named )
    {
/*        new HostedRepository( named )*/
        null
    }

    Group createGroup( String named )
    {
        Group g = new Group( named );
        g.addConstituent( new StoreKey( StoreType.remote, named ) )
/*        g.addConstituent( new StoreKey( StoreType.hosted, named ) )*/
        
        g
    }
}