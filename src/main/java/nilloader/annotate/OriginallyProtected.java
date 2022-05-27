package nilloader.annotate;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a member or class as having originally been {@code protected}, but it was widened to
 * {@code public} by NilGradle's remapping. The access will automatically be widened if it is used.
 */
@Retention(CLASS)
@Target({ TYPE, FIELD, METHOD })
@Documented
public @interface OriginallyProtected {}
