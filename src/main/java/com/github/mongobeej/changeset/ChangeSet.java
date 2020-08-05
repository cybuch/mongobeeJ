package com.github.mongobeej.changeset;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set of changes to be added to the DB. Many changesets are included in one changelog.
 *
 * @author lstolowski
 * @see ChangeLog
 * @since 27/07/2014
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChangeSet {

    /**
     * Author of the changeset.
     * Obligatory
     *
     * @return author
     */
    String author();  // must be set

    /**
     * Unique ID of the changeset.
     * Obligatory
     *
     * @return unique id
     */
    String id();      // must be set

    /**
     * Sequence that provide correct order for changesets. Sorted alphabetically, ascending.
     * Obligatory.
     *
     * @return ordering
     */
    String order();   // must be set

    /**
     * Executes the change set on every mongobeej's execution, even if it has been run before.
     * Optional (default is false)
     *
     * @return should run always?
     */
    boolean runAlways() default false;
//
//  /**
//   * Executes the change the first time it is seen and each time the change set has been changed. <br/>
//   * Optional (default is false)
//   */
//  public boolean runOnChange() default false;
}
