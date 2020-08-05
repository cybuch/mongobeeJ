package com.github.mongobeej.changeset;

import org.bson.Document;

import java.util.Date;

/**
 * Entry in the changes collection log {@link com.github.mongobeej.Mongobee#DEFAULT_CHANGELOG_COLLECTION_NAME}
 * Type: entity class.
 */
public class ChangeEntry {
    public static final String KEY_CHANGEID = "changeId";
    public static final String KEY_AUTHOR = "author";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_CHANGELOGCLASS = "changeLogClass";
    private static final String KEY_CHANGESETMETHOD = "changeSetMethod";

    private final String changeId;
    private final String author;
    private final Date timestamp;
    private final String changeLogClass;
    private final String changeSetMethodName;

    public ChangeEntry(
            String changeId,
            String author,
            Date timestamp,
            String changeLogClass,
            String changeSetMethodName) {
        this.changeId = changeId;
        this.author = author;
        this.timestamp = new Date(timestamp.getTime());
        this.changeLogClass = changeLogClass;
        this.changeSetMethodName = changeSetMethodName;
    }

    public Document buildFullDBObject() {
        Document entry = new Document();
        entry.append(KEY_CHANGEID, this.changeId)
                .append(KEY_AUTHOR, this.author)
                .append(KEY_TIMESTAMP, this.timestamp)
                .append(KEY_CHANGELOGCLASS, this.changeLogClass)
                .append(KEY_CHANGESETMETHOD, this.changeSetMethodName);
        return entry;
    }

    public Document buildSearchQueryDBObject() {
        return new Document()
                .append(KEY_CHANGEID, this.changeId)
                .append(KEY_AUTHOR, this.author);
    }

    @Override
    public String toString() {
        return "[ChangeSet: id=" + this.changeId +
                ", author=" + this.author +
                ", changeLogClass=" + this.changeLogClass +
                ", changeSetMethod=" + this.changeSetMethodName + "]";
    }

    public String getChangeId() {
        return this.changeId;
    }

    public String getAuthor() {
        return this.author;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getChangeLogClass() {
        return this.changeLogClass;
    }

    public String getChangeSetMethodName() {
        return this.changeSetMethodName;
    }

}
