package io.mo.cases;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.regex.Pattern;

/**
 * Represents a regex pattern with an include flag.
 * Used for checking if SQL output contains (include=true) or excludes (include=false) a pattern.
 * The pattern is compiled at parse time and stored for efficient matching.
 */
@Getter
@Setter
@AllArgsConstructor
public class RegexPattern {
    private String pattern;
    private boolean include;
    private Pattern compiledPattern;
}
