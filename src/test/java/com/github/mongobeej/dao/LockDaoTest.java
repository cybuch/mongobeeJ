package com.github.mongobeej.dao;

import com.github.mongobeej.utils.MongoEnvironmentCreator;
import com.mongodb.client.MongoDatabase;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LockDaoTest {
    private static final String LOCK_COLLECTION_NAME = "mongobeelock";

    private MongoDatabase mongoDatabase;
    private LockDao lockDao = new LockDao(LOCK_COLLECTION_NAME);

    @Before
    public void initializeLock() {
        mongoDatabase = MongoEnvironmentCreator.createMongoEnvironment().getMongoDatabase();
        lockDao.intitializeLock(mongoDatabase);
    }

    @Test
    public void shouldGetLockWhenNotPreviouslyHeld() throws Exception {
        // when
        boolean hasLock = lockDao.acquireLock(mongoDatabase);

        // then
        assertTrue(hasLock);
    }

    @Test
    public void shouldNotGetLockWhenPreviouslyHeld() throws Exception {
        // when
        lockDao.acquireLock(mongoDatabase);
        boolean hasLock = lockDao.acquireLock(mongoDatabase);
        // then
        assertFalse(hasLock);
    }

    @Test
    public void shouldGetLockWhenPreviouslyHeldAndReleased() throws Exception {
        // when
        lockDao.acquireLock(mongoDatabase);
        lockDao.releaseLock(mongoDatabase);
        boolean hasLock = lockDao.acquireLock(mongoDatabase);
        // then
        assertTrue(hasLock);
    }

    @Test
    public void releaseLockShouldBeIdempotent() {
        // when
        lockDao.releaseLock(mongoDatabase);
        lockDao.releaseLock(mongoDatabase);
        boolean hasLock = lockDao.acquireLock(mongoDatabase);

        // then
        assertTrue(hasLock);
    }

    @Test
    public void whenLockNotHeldCheckReturnsFalse() {
        assertFalse(lockDao.isLockHeld(mongoDatabase));
    }

    @Test
    public void whenLockHeldCheckReturnsTrue() {
        lockDao.acquireLock(mongoDatabase);
        assertTrue(lockDao.isLockHeld(mongoDatabase));
    }
}
