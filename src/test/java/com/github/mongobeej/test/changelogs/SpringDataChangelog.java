package com.github.mongobeej.test.changelogs;

import com.github.mongobeej.changeset.ChangeLog;
import com.github.mongobeej.changeset.ChangeSet;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * @author abelski
 */
@ChangeLog
public class SpringDataChangelog {
  @ChangeSet(author = "abelski", id = "spring_test4", order = "04")
  public void testChangeSet(MongoTemplate mongoTemplate) {
    System.out.println("invoked  with mongoTemplate=" + mongoTemplate.toString());
    System.out.println("invoked  with mongoTemplate=" + mongoTemplate.getCollectionNames());
  }
}
