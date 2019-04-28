package com.github.mongobeej.dao;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.utils.MongoEnvironmentCreator.MongoEnvironment;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.github.mongobeej.utils.MongoEnvironmentCreator.createMongoEnvironment;
import static org.junit.Assert.*;

public class ChangeEntryIndexDaoTest {
    private static final String CHANGEID_AUTHOR_INDEX_NAME = "changeId_1_author_1";
    private static final String CHANGELOG_COLLECTION_NAME = "dbchangelog";

    private ChangeEntryIndexDao dao = new ChangeEntryIndexDao();
    private MongoDatabase mongoDatabase;

    @Before
    public void setupMongoDatabase() {
        MongoEnvironment mongoEnvironment = createMongoEnvironment();
        mongoDatabase = mongoEnvironment.getMongoDatabase();
    }

    @Test
    public void shouldCreateRequiredUniqueIndex() {
        // when
        dao.createRequiredUniqueIndex(mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME));

        // then
        Document createdIndex = findIndex(mongoDatabase, CHANGEID_AUTHOR_INDEX_NAME);
        assertNotNull(createdIndex);
        assertTrue(dao.isUnique(createdIndex));
    }

    @Test
    @Ignore("Fongo has not implemented dropIndex for MongoCollection object (issue with mongo driver 3.x)")
    public void shouldDropWrongIndex() {
        MongoCollection<Document> collection = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME);
        collection.createIndex(new Document()
                .append(ChangeEntry.KEY_CHANGEID, 1)
                .append(ChangeEntry.KEY_AUTHOR, 1));
        Document index = new Document("name", CHANGEID_AUTHOR_INDEX_NAME);

        // given
        Document createdIndex = findIndex(mongoDatabase, CHANGEID_AUTHOR_INDEX_NAME);
        assertNotNull(createdIndex);
        assertFalse(dao.isUnique(createdIndex));

        // when
        dao.dropIndex(mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME), index);

        // then
        assertNull(findIndex(mongoDatabase, CHANGEID_AUTHOR_INDEX_NAME));
    }

    private Document findIndex(MongoDatabase db, String indexName) {
        for (Document index : db.getCollection(CHANGELOG_COLLECTION_NAME).listIndexes()) {
            String name = (String) index.get("name");
            if (indexName.equals(name)) {
                return index;
            }
        }
        return null;
    }
}
