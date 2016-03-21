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
package org.commonjava.indy.core.change.event;

import org.commonjava.cdi.util.weft.ExecutorConfig;
import org.commonjava.cdi.util.weft.WeftManaged;
import org.commonjava.indy.change.event.ArtifactStoreDeletePostEvent;
import org.commonjava.indy.change.event.ArtifactStoreDeletePreEvent;
import org.commonjava.indy.change.event.ArtifactStorePostUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStorePreUpdateEvent;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.content.DownloadManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class DefaultStoreEventDispatcher
        implements StoreEventDispatcher
{

    //    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private Event<ArtifactStorePreUpdateEvent> updatePreEvent;

    @Inject
    private Event<ArtifactStorePostUpdateEvent> updatePostEvent;

    @Inject
    private Event<ArtifactStoreDeletePreEvent> preDelEvent;

    @Inject
    private Event<ArtifactStoreDeletePostEvent> postDelEvent;

    @Inject
    @WeftManaged
    @ExecutorConfig( named = CoreEventManagerConstants.DISPATCH_EXECUTOR_NAME,
                     threads = CoreEventManagerConstants.DISPATCH_EXECUTOR_THREADS,
                     priority = CoreEventManagerConstants.DISPATCH_EXECUTOR_PRIORITY )
    private Executor executor;

    @Inject
    private DownloadManager fileManager;

    @Override
    public void deleting( final EventMetadata eventMetadata, final ArtifactStore... stores )
    {
        if ( preDelEvent != null )
        {
            executor.execute( () -> {
                final Map<ArtifactStore, Transfer> storeRoots = new HashMap<>();
                for ( final ArtifactStore store : stores )
                {
                    if ( store == null )
                    {
                        continue;
                    }

                    final Transfer root = fileManager.getStoreRootDirectory( store );
                    storeRoots.put( store, root );
                }

                final ArtifactStoreDeletePreEvent event = new ArtifactStoreDeletePreEvent( eventMetadata, storeRoots );

                preDelEvent.fire( event );
            } );
        }
    }

    @Override
    public void deleted( final EventMetadata eventMetadata, final ArtifactStore... stores )
    {
        if ( postDelEvent != null )
        {
            executor.execute( () -> {
                final Map<ArtifactStore, Transfer> storeRoots = new HashMap<>();
                for ( final ArtifactStore store : stores )
                {
                    if ( store == null )
                    {
                        continue;
                    }

                    final Transfer root = fileManager.getStoreRootDirectory( store );
                    storeRoots.put( store, root );
                }

                final ArtifactStoreDeletePostEvent event =
                        new ArtifactStoreDeletePostEvent( eventMetadata, storeRoots );

                postDelEvent.fire( event );
            } );
        }
    }

    @Override
    public void updating( final ArtifactStoreUpdateType type, final EventMetadata eventMetadata,
                          final ArtifactStore... stores )
    {
        //        logger.debug( "Trying to fire pre-update event for: {}", new JoinString( ", ", stores ) );
        if ( updatePreEvent != null )
        {
            executor.execute( () -> {
                final ArtifactStorePreUpdateEvent event =
                        new ArtifactStorePreUpdateEvent( type, eventMetadata, stores );
                //            logger.debug( "Firing pre-update event: {} (for: {}) via:\n  {}", event, new JoinString( ", ", stores ),
                //                          new JoinString( "\n  ", Thread.currentThread()
                //                                                        .getStackTrace() ) );
                updatePreEvent.fire( event );
            } );
        }
    }

    @Override
    public void updated( final ArtifactStoreUpdateType type, final EventMetadata eventMetadata,
                         final ArtifactStore... stores )
    {
        if ( updatePostEvent != null )
        {
            executor.execute( () -> {
                final ArtifactStorePostUpdateEvent event =
                        new ArtifactStorePostUpdateEvent( type, eventMetadata, stores );
                updatePostEvent.fire( event );
            } );
        }
    }

}
