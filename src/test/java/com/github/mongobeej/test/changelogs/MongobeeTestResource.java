package com.github.mongobeej.test.changelogs;

import com.github.mongobeej.changeset.ChangeLog;
import com.github.mongobeej.changeset.ChangeSet;
import com.mongodb.client.MongoDatabase;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author lstolowski
 * @since 27/07/2014
 */
@ChangeLog(order = "1")
public class MongobeeTestResource {

    @ChangeSet(author = "testuser", id = "test1", order = "01")
    public void testChangeSet() {
        System.out.println("invoked 1");
    }

    @ChangeSet(author = "testuser", id = "test2", order = "02")
    public void testChangeSet2() {
        System.out.println("invoked 2");
    }

    @ChangeSet(author = "testuser", id = "test3", order = "03")
    public void testChangeSet4(MongoTemplate mongoTemplate) {
        System.out.println("invoked 4 with mongoTemplate=" + mongoTemplate.toString());
    }

    @ChangeSet(author = "testuser", id = "test4", order = "04")
    public void testChangeSet5(MongoDatabase mongoDatabase) {
        System.out.println("invoked 5 with mongoDatabase=" + mongoDatabase.toString());
    }

}
