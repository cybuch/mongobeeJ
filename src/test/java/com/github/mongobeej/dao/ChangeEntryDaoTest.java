package com.github.mongobeej.dao;

import com.github.mongobeej.exception.MongobeeConfigurationException;
import com.github.mongobeej.exception.MongobeeLockException;
import com.github.mongobeej.utils.MongoDatabaseArgumentMatcher;
import com.github.mongobeej.utils.MongoEnvironmentCreator.MongoEnvironment;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import static com.github.mongobeej.utils.MongoEnvironmentCreator.DB_NAME;
import static com.github.mongobeej.utils.MongoEnvironmentCreator.createMongoEnvironment;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ChangeEntryDaoTest {
    private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
    private static final String LOCK_COLLECTION_NAME = "mongobeelock";
    private static final boolean WAIT_FOR_LOCK = false;
    private static final long CHANGE_LOG_LOCK_WAIT_TIME = 5L;
    private static final long CHANGE_LOG_LOCK_POLL_RATE = 10L;
    private static final boolean THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK = false;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private ChangeEntryDao changeEntryDao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME, WAIT_FOR_LOCK,
            CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE, THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);

    @Before
    public void setupMongoAndMock() {
        MongoEnvironment mongoEnvironment = createMongoEnvironment();
        mongoClient = mongoEnvironment.getMongoClient();
        mongoDatabase = mongoEnvironment.getMongoDatabase();
    }

    @Test
    public void shouldCreateChangeIdAuthorIndexIfNotFound() throws MongobeeConfigurationException {
        // given
        ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
        when(indexDaoMock.findRequiredChangeAndAuthorIndex(mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME))).thenReturn(null);
        changeEntryDao.setIndexDao(indexDaoMock);

        // when
        changeEntryDao.connectMongoDb(mongoClient, DB_NAME);

        //then
        verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(MongoCollection.class));
        // and not
        verify(indexDaoMock, times(0)).dropIndex(any(MongoCollection.class), any(Document.class));
    }

    @Test
    public void shouldNotCreateChangeIdAuthorIndexIfFound() throws MongobeeConfigurationException {
        // given
        ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
        when(indexDaoMock.findRequiredChangeAndAuthorIndex(mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME))).thenReturn(new Document());
        when(indexDaoMock.isUnique(any(Document.class))).thenReturn(true);
        changeEntryDao.setIndexDao(indexDaoMock);

        // when
        changeEntryDao.connectMongoDb(mongoClient, DB_NAME);

        //then
        verify(indexDaoMock, times(0)).createRequiredUniqueIndex(mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME));
        // and not
        verify(indexDaoMock, times(0)).dropIndex(mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME), new Document());
    }

    @Test
    public void shouldRecreateChangeIdAuthorIndexIfFoundNotUnique() throws MongobeeConfigurationException {
        // given
        ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
        when(indexDaoMock.findRequiredChangeAndAuthorIndex(any())).thenReturn(new Document());
        when(indexDaoMock.isUnique(any(Document.class))).thenReturn(false);
        changeEntryDao.setIndexDao(indexDaoMock);

        // when
        changeEntryDao.connectMongoDb(mongoClient, DB_NAME);

        //then
        verify(indexDaoMock, times(1)).dropIndex(any(MongoCollection.class), any(Document.class));
        // and
        verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(MongoCollection.class));
    }

    @Test
    public void shouldInitiateLock() throws MongobeeConfigurationException {
        // given
        ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME, WAIT_FOR_LOCK,
                CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE, THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
        ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
        dao.setIndexDao(indexDaoMock);
        LockDao lockDao = mock(LockDao.class);
        dao.setLockDao(lockDao);

        // when
        dao.connectMongoDb(mongoClient, DB_NAME);

        // then
        verify(lockDao).intitializeLock(argThat(new MongoDatabaseArgumentMatcher(DB_NAME)));

    }

    @Test
    public void shouldGetLockWhenLockDaoGetsLock() throws Exception {
        // given
        LockDao lockDao = mock(LockDao.class);
        when(lockDao.acquireLock(any(MongoDatabase.class))).thenReturn(true);
        changeEntryDao.setLockDao(lockDao);
        changeEntryDao.connectMongoDb(mongoClient, DB_NAME);

        // when
        boolean hasLock = changeEntryDao.acquireProcessLock();

        // then
        assertTrue(hasLock);
    }

    @Test
    public void shouldWaitForLockIfWaitForLockIsTrue() throws Exception {
        // given
        ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME, true,
                CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE, THROW_EXCEPTION_IF_CANNOT_OBTAIN_LOCK);
        LockDao lockDao = mock(LockDao.class);
        when(lockDao.acquireLock(any(MongoDatabase.class))).thenReturn(false, true);
        dao.setLockDao(lockDao);
        dao.connectMongoDb(mongoClient, DB_NAME);

        // when
        boolean hasLock = dao.acquireProcessLock();

        // then
        verify(lockDao, times(2)).acquireLock(any(MongoDatabase.class));
        assertTrue(hasLock);
    }

    @Test(expected = MongobeeLockException.class)
    public void shouldThrowLockExceptionIfThrowExceptionIsTrue() throws Exception {
        // given
        ChangeEntryDao dao = new ChangeEntryDao(CHANGELOG_COLLECTION_NAME, LOCK_COLLECTION_NAME, WAIT_FOR_LOCK,
                CHANGE_LOG_LOCK_WAIT_TIME, CHANGE_LOG_LOCK_POLL_RATE, true);
        LockDao lockDao = mock(LockDao.class);
        when(lockDao.acquireLock(any(MongoDatabase.class))).thenReturn(false);
        dao.setLockDao(lockDao);
        dao.connectMongoDb(mongoClient, DB_NAME);

        // when
        boolean hasLock = dao.acquireProcessLock();

        // then
        assertFalse(hasLock);
    }

    @Test
    public void shouldReleaseLockFromLockDao() throws Exception {
        // given
        LockDao lockDao = mock(LockDao.class);
        changeEntryDao.setLockDao(lockDao);
        changeEntryDao.connectMongoDb(mongoClient, DB_NAME);

        // when
        changeEntryDao.releaseProcessLock();

        // then
        verify(lockDao).releaseLock(any(MongoDatabase.class));
    }

    @Test
    public void shouldCheckLockHeldFromLockDao() throws Exception {
        // given
        LockDao lockDao = mock(LockDao.class);
        changeEntryDao.setLockDao(lockDao);
        changeEntryDao.connectMongoDb(mongoClient, DB_NAME);

        // when
        when(lockDao.isLockHeld(argThat(new MongoDatabaseArgumentMatcher(DB_NAME)))).thenReturn(true);
        boolean lockHeld = changeEntryDao.isProccessLockHeld();

        // then
        assertTrue(lockHeld);
    }
}
