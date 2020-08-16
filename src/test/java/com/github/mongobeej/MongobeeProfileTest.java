package com.github.mongobeej;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.resources.EnvironmentMock;
import com.github.mongobeej.test.changelogs.AnotherMongobeeTestResource;
import com.github.mongobeej.test.profiles.def.UnProfiledChangeLog;
import com.github.mongobeej.test.profiles.dev.ProfiledDevChangeLog;
import org.bson.Document;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.mongobeej.utils.TestQueries.changeQuery;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeProfileTest extends MongobeeBaseTest {
    private static final int CHANGELOG_COUNT = 10;

    @Test
    public void shouldRunDevProfileAndNonAnnotated() throws Exception {
        // given
        mongobee.setSpringEnvironment(new EnvironmentMock("dev", "test"));
        mongobee.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long change1 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev1"));
        assertEquals(1, change1);  //  no-@Profile  should not match
        long change2 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev4"));
        assertEquals(1, change2);  //  @Profile("dev")  should not match
        long change3 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev3"));
        assertEquals(0, change3);  //  @Profile("default")  should not match
    }

    @Test
    public void shouldRunUnprofiledChangeLog() throws Exception {
        // given
        mongobee.setSpringEnvironment(new EnvironmentMock("test"));
        mongobee.setChangeLogsScanPackage(UnProfiledChangeLog.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long change1 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev1"));
        assertEquals(1, change1);
        long change2 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev2"));
        assertEquals(1, change2);
        long change3 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev3"));
        assertEquals(1, change3);  //  @Profile("dev")  should not match
        long change4 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev4"));
        assertEquals(0, change4);  //  @Profile("pro")  should not match
        long change5 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .count(changeQuery("Pdev5"));
        assertEquals(1, change5);  //  @Profile("!pro")  should match
    }

    @Test
    public void shouldNotRunAnyChangeSet() throws Exception {
        // given
        mongobee.setSpringEnvironment(new EnvironmentMock("foobar"));
        mongobee.setChangeLogsScanPackage(ProfiledDevChangeLog.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long changes = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
        assertEquals(0, changes);
    }

    @Test
    public void shouldRunChangeSetsWhenNoEnv() throws Exception {
        // given
        mongobee.setSpringEnvironment(null);
        mongobee.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long changes = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
        assertEquals(CHANGELOG_COUNT, changes);
    }

    @Test
    public void shouldRunChangeSetsWhenEmptyEnv() throws Exception {
        // given
        mongobee.setSpringEnvironment(new EnvironmentMock());
        mongobee.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long changes = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
        assertEquals(CHANGELOG_COUNT, changes);
    }

    @Test
    public void shouldRunAllChangeSets() throws Exception {
        // given
        mongobee.setSpringEnvironment(new EnvironmentMock("dev"));
        mongobee.setChangeLogsScanPackage(AnotherMongobeeTestResource.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long changes = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME).count(new Document());
        assertEquals(CHANGELOG_COUNT, changes);
    }

    @After
    public void cleanUp() {
        mongobee.setMongoTemplate(null);
    }
}
