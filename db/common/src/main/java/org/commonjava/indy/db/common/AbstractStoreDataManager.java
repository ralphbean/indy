/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.db.common;

import org.commonjava.cdi.util.weft.Locker;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.change.event.ArtifactStoreUpdateType;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.data.StoreEventDispatcher;
import org.commonjava.indy.measure.annotation.Measure;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.commonjava.indy.model.core.StoreType.hosted;

public abstract class AbstractStoreDataManager
        implements StoreDataManager
{
    protected static final long LOCK_TIMEOUT_SECONDS = 10;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    protected final Locker<StoreKey> opLocks = new Locker<>(); // used internally

    abstract protected StoreEventDispatcher getStoreEventDispatcher();

    @Override
    public ArtifactStoreQuery<ArtifactStore> query()
    {
        return new DefaultArtifactStoreQuery<>( this );
    }

    protected abstract ArtifactStore getArtifactStoreInternal( final StoreKey key );

    @Override
    public ArtifactStore getArtifactStore( final StoreKey key )
            throws IndyDataException
    {
        return getArtifactStoreInternal( key );
    }

    @Override
    @Measure
    public boolean storeArtifactStore( final ArtifactStore store, final ChangeSummary summary,
                                       final boolean skipIfExists, final boolean fireEvents,
                                       final EventMetadata eventMetadata )
            throws IndyDataException
    {
        return store( store, summary, skipIfExists, fireEvents, eventMetadata );
    }

    protected void preStore( final ArtifactStore store, final ArtifactStore original, final ChangeSummary summary,
                             final boolean exists, final boolean fireEvents, final EventMetadata eventMetadata )
            throws IndyDataException
    {
        StoreEventDispatcher dispatcher = getStoreEventDispatcher();
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            logger.debug( "Firing store pre-update event for: {} (originally: {})", store, original );
            dispatcher.updating( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, eventMetadata,
                                 Collections.singletonMap( store, original ) );

            if ( exists )
            {
                if ( store.isDisabled() && !original.isDisabled() )
                {
                    dispatcher.disabling( eventMetadata, store );
                }
                else if ( !store.isDisabled() && original.isDisabled() )
                {
                    dispatcher.enabling( eventMetadata, store );
                }
            }
        }
    }

    protected void postStore( final ArtifactStore store, final ArtifactStore original, final ChangeSummary summary,
                              final boolean exists, final boolean fireEvents, final EventMetadata eventMetadata )
            throws IndyDataException
    {
        StoreEventDispatcher dispatcher = getStoreEventDispatcher();
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            logger.debug( "Firing store post-update event for: {} (originally: {})", store, original );
            dispatcher.updated( exists ? ArtifactStoreUpdateType.UPDATE : ArtifactStoreUpdateType.ADD, eventMetadata,
                                Collections.singletonMap( store, original ) );

            if ( exists )
            {
                if ( store.isDisabled() && !original.isDisabled() )
                {
                    dispatcher.disabled( eventMetadata, store );
                }
                else if ( !store.isDisabled() && original.isDisabled() )
                {
                    dispatcher.enabled( eventMetadata, store );
                }
            }
        }
    }

    protected void preDelete( final ArtifactStore store, final ChangeSummary summary, final boolean fireEvents,
                              final EventMetadata eventMetadata )
            throws IndyDataException
    {
        StoreEventDispatcher dispatcher = getStoreEventDispatcher();
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            dispatcher.deleting( eventMetadata, store );
        }
    }

    protected void postDelete( final ArtifactStore store, final ChangeSummary summary, final boolean fireEvents,
                               final EventMetadata eventMetadata )
            throws IndyDataException
    {
        StoreEventDispatcher dispatcher = getStoreEventDispatcher();
        if ( dispatcher != null && isStarted() && fireEvents )
        {
            dispatcher.deleted( eventMetadata, store );
        }
    }

    protected abstract ArtifactStore removeArtifactStoreInternal( StoreKey key );

    @Override
    public void deleteArtifactStore( final StoreKey key, final ChangeSummary summary,
                                     final EventMetadata eventMetadata )
            throws IndyDataException
    {
        AtomicReference<IndyDataException> error = new AtomicReference<>();
        opLocks.lockAnd( key, LOCK_TIMEOUT_SECONDS, k->{
            try
            {
                final ArtifactStore store = getArtifactStoreInternal( k );
                if ( store == null )
                {
                    logger.warn( "No store found for: {}", k );
                    return null;
                }

                if ( isReadonly( store ) )
                {
                    throw new IndyDataException( ApplicationStatus.METHOD_NOT_ALLOWED.code(),
                                                 "The store {} is readonly. If you want to delete this store, please modify it to non-readonly",
                                                 store.getKey() );
                }

                preDelete( store, summary, true, eventMetadata );

                ArtifactStore removed = removeArtifactStoreInternal( k );
                logger.info( "REMOVED store: {}", removed );

                postDelete( store, summary, true, eventMetadata );
            }
            catch ( IndyDataException e )
            {
                error.set( e );
            }

            return null;
        }, (k,lock)->{
            error.set( new IndyDataException( "Failed to lock: %s for DELETE after %d seconds.", k,
                                              LOCK_TIMEOUT_SECONDS ) );
            return false;
        } );

        IndyDataException ex = error.get();
        if ( ex != null )
        {
            throw ex;
        }
    }

    /**
     * TODO: currently we only check hosted readonly to prevent unexpected removing of both files and repo itself.
     * We may expand to remote or group in the future to support functions like remote repo "deploy-through".
     */
    @Override
    public boolean isReadonly( final ArtifactStore store )
    {
        if ( store != null )
        {
            if ( store.getKey().getType() == hosted && ( (HostedRepository) store ).isReadonly() )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public void install()
            throws IndyDataException
    {
    }

    @Override
    public abstract void clear( final ChangeSummary summary )
            throws IndyDataException;

    @Override
    public abstract Set<ArtifactStore> getAllArtifactStores()
            throws IndyDataException;

    @Override
    public Stream<ArtifactStore> streamArtifactStores()
            throws IndyDataException
    {
        return getAllArtifactStores().stream();
    }

    @Override
    public abstract Map<StoreKey, ArtifactStore> getArtifactStoresByKey();

    @Override
    public abstract boolean hasArtifactStore( final StoreKey key );

    @Override
    public void reload()
            throws IndyDataException
    {
    }

    @Override
    public abstract boolean isStarted();

    protected abstract ArtifactStore putArtifactStoreInternal(final StoreKey storeKey, ArtifactStore store);

    protected boolean store( final ArtifactStore store, final ChangeSummary summary, final boolean skipIfExists,
                           final boolean fireEvents, final EventMetadata eventMetadata )
            throws IndyDataException
    {
        if ( store == null )
        {
            logger.warn( "Tried to store null ArtifactStore!" );
            return false;
        }

        AtomicReference<IndyDataException> error = new AtomicReference<>();
        logger.trace( "Storing {} using operation lock: {}", store, opLocks );

        final StoreKey storeKey = store.getKey();

        Function<StoreKey, Boolean> lockHandler = k -> {
            try
            {
                ArtifactStore original = getArtifactStoreInternal( k );
                if ( original == store )
                {
                    // if they're the same instance, preUpdate events may not work correctly!
                    logger.warn( "Storing changes on existing instance of: {}! You forgot to call {}.copyOf().", store,
                                 store.getClass().getSimpleName() );
                }

                if ( skipIfExists && original != null )
                {
                    logger.info( "Skip storing for {} (repo exists)", original );
                    return true;
                }

                try
                {
                    if ( eventMetadata != null && summary != null )
                    {
                        eventMetadata.set( StoreDataManager.CHANGE_SUMMARY, summary );
                    }
                    logger.debug( "Starting pre-store actions for {}", k );
                    preStore( store, original, summary, original != null, fireEvents, eventMetadata );
                    logger.debug( "Pre-store actions complete for {}", k );
                }
                catch ( IndyDataException e )
                {
                    error.set( e );
                    return false;
                }

                logger.debug( "Put {} to stores map", k );
                final ArtifactStore old = putArtifactStoreInternal( store.getKey(), store );

                try
                {
                    logger.debug( "Starting post-store actions for {}", k );
                    postStore( store, original, summary, original != null, fireEvents, eventMetadata );
                    logger.debug( "Post-store actions complete for {}", k );
                }
                catch ( final IndyDataException e )
                {
                    if ( old != null )
                    {
                        logger.error( "postStore() failed for {}. Rollback to old value: {}", store, old );
                        putArtifactStoreInternal( old.getKey(), old );
                    }
                    error.set( e );
                    return false;
                }

                return true;
            }
            catch ( RuntimeException e )
            {
                //logger.error( "Runtime exception trying to store: " + k, e );
                error.set( new IndyDataException( "Runtime exception trying to store: " + k, e ) );
            }

            return false;
        };

        BiFunction<StoreKey, ReentrantLock, Boolean> lockFailedHandler = (k,lock) -> {
            error.set( new IndyDataException( "Failed to lock: %s for STORE after %d seconds.", k,
                                              LOCK_TIMEOUT_SECONDS ) );
            return false;
        };

        boolean result = opLocks.lockAnd( storeKey, LOCK_TIMEOUT_SECONDS, lockHandler, lockFailedHandler );

        IndyDataException ex = error.get();
        if ( ex != null )
        {
            throw ex;
        }

        return result;
    }

}
