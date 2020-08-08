package com.github.mongobeej.test.changelogs;

import com.github.mongobeej.changeset.ChangeLog;
import com.github.mongobeej.changeset.ChangeSet;
import com.mongodb.client.MongoDatabase;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author lstolowski
 * @since 30.07.14
 */
@ChangeLog(order = "2")
public class AnotherMongobeeTestResource {

    @ChangeSet(author = "testuser", id = "Btest1", order = "01")
    public void testChangeSet() {
        System.out.println("invoked B1");
    }

    @ChangeSet(author = "testuser", id = "Btest2", order = "02")
    public void testChangeSet2() {
        System.out.println("invoked B2");
    }

    @ChangeSet(author = "testuser", id = "Btest5", order = "03", runAlways = true)
    public void testChangeSetWithAlways(MongoTemplate mongoTemplate) {
        System.out.println("invoked B5 with always + mongoTemplate=" + mongoTemplate.toString());
    }

    @ChangeSet(author = "testuser", id = "Btest6", order = "04")
    public void testChangeSet6(MongoDatabase mongoDatabase) {
        System.out.println("invoked B6 with db=" + mongoDatabase.toString());
    }

}
