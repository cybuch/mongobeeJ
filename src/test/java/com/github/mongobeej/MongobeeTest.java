package com.github.mongobeej;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.dao.ChangeEntryDao;
import com.github.mongobeej.dao.ChangeEntryIndexDao;
import com.github.mongobeej.exception.MongobeeConfigurationException;
import com.github.mongobeej.exception.MongobeeException;
import com.github.mongobeej.test.changelogs.MongobeeTestResource;
import com.github.mongobeej.utils.MongoEnvironmentCreator.MongoEnvironment;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Collections;

import static com.github.mongobeej.utils.MongoEnvironmentCreator.DB_NAME;
import static com.github.mongobeej.utils.MongoEnvironmentCreator.createMongoEnvironment;
import static com.github.mongobeej.utils.TestQueries.changeQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeTest {
    private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
    MongoEnvironment mongoEnvironment = createMongoEnvironment();
    @InjectMocks
    private Mongobee mongobee = new Mongobee(mongoEnvironment.getMongoClient());
    @Mock
    private ChangeEntryDao changeEntryDao;
    @Mock
    private ChangeEntryIndexDao indexDao;
    private MongoDatabase mongoDatabase;

    @Before
    public void init() throws MongobeeException {
        mongoDatabase = mongoEnvironment.getMongoDatabase();
        when(changeEntryDao.connectMongoDb(any(ConnectionString.class), anyString()))
                .thenReturn(mongoDatabase);
        when(changeEntryDao.connectMongoDb(any(MongoClient.class), anyString()))
                .thenReturn(mongoDatabase);
        when(changeEntryDao.getMongoDatabase()).thenReturn(mongoDatabase);
        doCallRealMethod().when(changeEntryDao).save(any(ChangeEntry.class));
        doCallRealMethod().when(changeEntryDao).setChangelogCollectionName(anyString());
        doCallRealMethod().when(changeEntryDao).setIndexDao(any(ChangeEntryIndexDao.class));
        changeEntryDao.setIndexDao(indexDao);
        changeEntryDao.setChangelogCollectionName(CHANGELOG_COLLECTION_NAME);
        mongobee.setDbName(DB_NAME);
        mongobee.setEnabled(true);
        mongobee.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
    }

    @Test(expected = MongobeeConfigurationException.class)
    public void shouldThrowAnExceptionIfNoDbNameSet() throws Exception {
        Mongobee runner = new Mongobee(new ConnectionString("mongodb://localhost:27017/"));
        runner.setEnabled(true);
        runner.setChangeLogsScanPackage(MongobeeTestResource.class.getPackage().getName());
        runner.execute();
    }

    @Test
    public void shouldExecuteAllChangeSets() throws Exception {
        // given
        when(changeEntryDao.acquireProcessLock()).thenReturn(true);
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        verify(changeEntryDao, times(10)).save(any(ChangeEntry.class)); // 10 changesets saved to dbchangelog

        // dbchangelog collection checking
        long change1 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .countDocuments(changeQuery("test1"));
        assertEquals(1, change1);
        long change2 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .countDocuments(changeQuery("test2"));
        assertEquals(1, change2);
        long change3 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .countDocuments(changeQuery("test3"));
        assertEquals(1, change3);
        long change4 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .countDocuments(changeQuery("test4"));
        assertEquals(1, change4);
        long changeAll = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .countDocuments(new Document().append(ChangeEntry.KEY_AUTHOR, "testuser"));
        assertEquals(9, changeAll);
    }

    @Test
    public void shouldPassOverChangeSets() throws Exception {
        // given
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(false);

        // when
        mongobee.execute();

        // then
        verify(changeEntryDao, times(0)).save(any(ChangeEntry.class)); // no changesets saved to dbchangelog
    }

    @Test
    public void shouldUsePreConfiguredMongoTemplate() throws Exception {
        MongoTemplate mt = mock(MongoTemplate.class);
        when(mt.getCollectionNames()).thenReturn(Collections.EMPTY_SET);
        when(changeEntryDao.acquireProcessLock()).thenReturn(true);
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);
        mongobee.setMongoTemplate(mt);
        mongobee.afterPropertiesSet();
        verify(mt).getCollectionNames();
    }

    @Test
    public void shouldExecuteProcessWhenLockAcquired() throws Exception {
        // given
        when(changeEntryDao.acquireProcessLock()).thenReturn(true);

        // when
        mongobee.execute();

        // then
        verify(changeEntryDao, atLeastOnce()).isNewChange(any(ChangeEntry.class));
    }

    @Test
    public void shouldReleaseLockAfterWhenLockAcquired() throws Exception {
        // given
        when(changeEntryDao.acquireProcessLock()).thenReturn(true);

        // when
        mongobee.execute();

        // then
        verify(changeEntryDao).releaseProcessLock();
    }

    @Test
    public void shouldNotExecuteProcessWhenLockNotAcquired() throws Exception {
        // given
        when(changeEntryDao.acquireProcessLock()).thenReturn(false);

        // when
        mongobee.execute();

        // then
        verify(changeEntryDao, never()).isNewChange(any(ChangeEntry.class));
    }

    @Test
    public void shouldReturnExecutionStatusBasedOnDao() throws Exception {
        // given
        when(changeEntryDao.isProccessLockHeld()).thenReturn(true);

        //when
        boolean inProgress = mongobee.isExecutionInProgress();

        // then
        assertTrue(inProgress);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldReleaseLockWhenExceptionInMigration() throws Exception {
        // given
        // would be nicer with a mock for the whole execution, but this would mean breaking out to separate class..
        // this should be "good enough"
        when(changeEntryDao.acquireProcessLock()).thenReturn(true);
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenThrow(RuntimeException.class);

        // when
        // have to catch the exception to be able to verify after
        try {
            mongobee.execute();
        } catch (Exception e) {
            // do nothing
        }
        // then
        verify(changeEntryDao).releaseProcessLock();

    }

    @After
    public void cleanUp() {
        mongobee.setMongoTemplate(null);
    }
}
