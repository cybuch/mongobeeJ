package com.github.mongobeej.utils;

import com.github.mongobeej.changeset.ChangeEntry;
import com.github.mongobeej.exception.MongobeeChangeSetException;
import com.github.mongobeej.resources.EnvironmentMock;
import com.github.mongobeej.test.changelogs.AnotherMongobeeTestResource;
import com.github.mongobeej.test.changelogs.MongobeeTestResource;
import org.junit.Test;
import org.springframework.core.env.Environment;

import java.lang.reflect.Method;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ChangeServiceTest {

    Environment environment = new EnvironmentMock();
    String scanPackage = MongobeeTestResource.class.getPackage().getName();
    ChangeService changeService = new ChangeService(scanPackage, environment);

    @Test
    public void shouldFindChangeLogClasses() {
        List<Class<?>> foundClasses = changeService.fetchChangeLogs();
        // then
        assertTrue(foundClasses != null && foundClasses.size() > 0);
    }

    @Test
    public void shouldFindChangeSetMethods() throws MongobeeChangeSetException {
        // when
        List<Method> foundMethods = changeService.fetchChangeSets(MongobeeTestResource.class);

        // then
        assertTrue(foundMethods != null && foundMethods.size() == 5);
    }

    @Test
    public void shouldFindAnotherChangeSetMethods() throws MongobeeChangeSetException {
        // when
        List<Method> foundMethods = changeService.fetchChangeSets(AnotherMongobeeTestResource.class);

        // then
        assertTrue(foundMethods != null && foundMethods.size() == 6);
    }


    @Test
    public void shouldFindIsRunAlwaysMethod() throws MongobeeChangeSetException {
        // when
        List<Method> foundMethods = changeService.fetchChangeSets(AnotherMongobeeTestResource.class);

        // then
        for (Method foundMethod : foundMethods) {
            if (foundMethod.getName().equals("testChangeSetWithAlways")) {
                assertTrue(changeService.isRunAlwaysChangeSet(foundMethod));
            } else {
                assertFalse(changeService.isRunAlwaysChangeSet(foundMethod));
            }
        }
    }

    @Test
    public void shouldCreateEntry() throws MongobeeChangeSetException {
        // given
        List<Method> foundMethods = changeService.fetchChangeSets(MongobeeTestResource.class);

        for (Method foundMethod : foundMethods) {
            // when
            ChangeEntry entry = changeService.createChangeEntry(foundMethod);

            // then
            assertEquals("testuser", entry.getAuthor());
            assertEquals(MongobeeTestResource.class.getName(), entry.getChangeLogClass());
            assertNotNull(entry.getTimestamp());
            assertNotNull(entry.getChangeId());
            assertNotNull(entry.getChangeSetMethodName());
        }
    }

    @Test(expected = MongobeeChangeSetException.class)
    public void shouldFailOnDuplicatedChangeSets() throws MongobeeChangeSetException {
        // given
        String scanPackage = ChangeLogWithDuplicate.class.getPackage().getName();
        ChangeService service = new ChangeService(scanPackage, environment);

        // when
        service.fetchChangeSets(ChangeLogWithDuplicate.class);
    }
}
