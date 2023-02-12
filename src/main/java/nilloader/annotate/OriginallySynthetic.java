package nilloader.annotate;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a member or class as having originally been marked as synthetic, but it was untagged by
 * NilGradle's remapping to allow it to be referenced in source code.
 */
@Retention(CLASS)
@Target({ TYPE, FIELD, METHOD })
@Documented
public @interface OriginallySynthetic {}
