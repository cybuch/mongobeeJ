package com.github.mongobeej;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.dao.ChangeEntryDao;
import com.github.mongobeej.dao.ChangeEntryIndexDao;
import com.github.mongobeej.utils.MongoEnvironmentCreator;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import org.junit.Before;
import org.mockito.Mock;

import static com.github.mongobeej.utils.MongoEnvironmentCreator.DB_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

abstract class MongobeeBaseTest {
    protected static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";
    protected Mongobee mongobee;
    @Mock
    protected ChangeEntryDao changeEntryDao;
    @Mock
    protected ChangeEntryIndexDao indexDao;
    protected MongoDatabase mongoDatabase;

    @Before
    public void init() throws Exception {
        MongoEnvironmentCreator.MongoEnvironment mongoEnvironment = MongoEnvironmentCreator.createMongoEnvironment();
        mongobee = new Mongobee(mongoEnvironment.getMongoClient());
        mongoDatabase = mongoEnvironment.getMongoDatabase();
        when(changeEntryDao.connectMongoDb(any(MongoClientURI.class), anyString()))
                .thenReturn(mongoDatabase);
        when(changeEntryDao.connectMongoDb(any(com.mongodb.client.MongoClient.class), anyString()))
                .thenReturn(mongoDatabase);
        when(changeEntryDao.connectMongoDb(any(MongoClient.class), anyString()))
                .thenReturn(mongoDatabase);
        when(changeEntryDao.getMongoDatabase()).thenReturn(mongoDatabase);
        when(changeEntryDao.acquireProcessLock()).thenReturn(true);
        doCallRealMethod().when(changeEntryDao).save(any(ChangeEntry.class));
        doCallRealMethod().when(changeEntryDao).setChangelogCollectionName(anyString());
        doCallRealMethod().when(changeEntryDao).setIndexDao(any(ChangeEntryIndexDao.class));
        changeEntryDao.setIndexDao(indexDao);
        changeEntryDao.setChangelogCollectionName(CHANGELOG_COLLECTION_NAME);
        mongobee.setDbName(DB_NAME);
        mongobee.setEnabled(true);
    }
}
