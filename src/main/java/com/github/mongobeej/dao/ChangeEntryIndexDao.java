package com.github.mongobeej.dao;

import com.github.mongobeej.changeset.ChangeEntry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;

public class ChangeEntryIndexDao {

    void createRequiredUniqueIndex(MongoCollection<Document> collection) {
        collection.createIndex(new Document()
                        .append(ChangeEntry.KEY_CHANGEID, 1)
                        .append(ChangeEntry.KEY_AUTHOR, 1),
                new IndexOptions().unique(true)
        );
    }

    Document findRequiredChangeAndAuthorIndex(MongoCollection<Document> collection) {
        for (Document document : collection.listIndexes()) {
            Document indexKeys = ((Document) document.get("key"));
            if (indexKeys != null && isChangelogIndex(indexKeys)) {
                return document;
            }
        }
        return null;
    }

    private boolean isChangelogIndex(Document indexKeys) {
        return indexKeys.get(ChangeEntry.KEY_CHANGEID) != null && indexKeys.get(ChangeEntry.KEY_AUTHOR) != null;
    }

    boolean isUnique(Document index) {
        Object unique = index.get("unique");
        if (unique != null && unique instanceof Boolean) {
            return (Boolean) unique;
        } else {
            return false;
        }
    }

    void dropIndex(MongoCollection<Document> collection, Document index) {
        collection.dropIndex(index.get("name").toString());
    }
}
