package com.github.mongobeej.test.changelogs;

import com.github.mongobeej.changeset.ChangeLog;
import com.github.mongobeej.changeset.ChangeSet;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeLog(order = "3")
public class EnvironmentDependentTestResource {
    @ChangeSet(author = "testuser", id = "Envtest1", order = "01")
    public void testChangeSet7WithEnvironment(MongoTemplate template, Environment env) {
        System.out.println("invoked Envtest1 with mongotemplate=" + template.toString() + " and Environment " + env);
    }
}
