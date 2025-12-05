package io.mo.cases;

import java.util.regex.Pattern;

/**
 * Represents a regex pattern with an include flag.
 * Used for checking if SQL output contains (include=true) or excludes (include=false) a pattern.
 * The pattern is compiled at parse time and stored for efficient matching.
 */
public class RegexPattern {
    private String pattern;
    private boolean include;
    private Pattern compiledPattern;

    public RegexPattern(String pattern, boolean include, Pattern compiledPattern) {
        this.pattern = pattern;
        this.include = include;
        this.compiledPattern = compiledPattern;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isInclude() {
        return include;
    }

    public void setInclude(boolean include) {
        this.include = include;
    }

    public Pattern getCompiledPattern() {
        return compiledPattern;
    }

    public void setCompiledPattern(Pattern compiledPattern) {
        this.compiledPattern = compiledPattern;
    }
}
