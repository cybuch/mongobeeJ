package com.github.mongobeej.utils;

import com.github.mongobeej.changeset.ChangeEntry;
import org.bson.Document;

public class TestQueries {
    private TestQueries() {
        throw new IllegalStateException("Can't create instance of thiss class");
    }

    public static Document changeQuery(String changeId) {
        return new Document()
                .append(ChangeEntry.KEY_CHANGEID, changeId)
                .append(ChangeEntry.KEY_AUTHOR, "testuser");
    }

    public static Document changeQuery(String changeId, String author) {
        return new Document()
                .append(ChangeEntry.KEY_CHANGEID, changeId)
                .append(ChangeEntry.KEY_AUTHOR, author);
    }
}
