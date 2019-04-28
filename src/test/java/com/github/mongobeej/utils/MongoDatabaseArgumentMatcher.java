package com.github.mongobeej.utils;

import com.mongodb.client.MongoDatabase;
import org.mockito.ArgumentMatcher;

import static java.util.Objects.requireNonNull;

public class MongoDatabaseArgumentMatcher extends ArgumentMatcher<MongoDatabase> {
    private final String dbName;

    public MongoDatabaseArgumentMatcher(String dbName) {
        this.dbName = requireNonNull(dbName);
    }

    @Override
    public boolean matches(Object object) {
        if (object instanceof MongoDatabase) {
            MongoDatabase mongoDatabase = (MongoDatabase) object;
            return mongoDatabase.getName().equals(dbName);
        }
        return false;
    }
}
