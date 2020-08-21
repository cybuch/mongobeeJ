package com.github.mongobeej;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.resources.EnvironmentMock;
import com.github.mongobeej.test.changelogs.EnvironmentDependentTestResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static com.github.mongobeej.utils.TestQueries.changeQuery;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MongobeeEnvTest extends MongobeeBaseTest {

    @Test
    public void shouldRunChangesetWithEnvironment() throws Exception {
        // given
        mongobee.setSpringEnvironment(new EnvironmentMock());
        mongobee.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long change1 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .countDocuments(changeQuery("Envtest1"));
        assertEquals(1, change1);

    }

    @Test
    public void shouldRunChangesetWithNullEnvironment() throws Exception {
        // given
        mongobee.setSpringEnvironment(null);
        mongobee.setChangeLogsScanPackage(EnvironmentDependentTestResource.class.getPackage().getName());
        when(changeEntryDao.isNewChange(any(ChangeEntry.class))).thenReturn(true);

        // when
        mongobee.execute();

        // then
        long change1 = mongoDatabase.getCollection(CHANGELOG_COLLECTION_NAME)
                .countDocuments(changeQuery("Envtest1"));
        assertEquals(1, change1);

    }
}
