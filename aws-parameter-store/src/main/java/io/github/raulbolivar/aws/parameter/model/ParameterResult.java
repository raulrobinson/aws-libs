package io.github.raulbolivar.aws.parameter.model;

import java.time.Instant;

/**
 * Represents a resolved parameter from AWS SSM Parameter Store.
 */
public class ParameterResult {

    public enum Type { STRING, STRING_LIST, SECURE_STRING }

    private final String  name;
    private final String  value;
    private final Type    type;
    private final String  version;
    private final Instant resolvedAt;

    private ParameterResult(Builder b) {
        this.name       = b.name;
        this.value      = b.value;
        this.type       = b.type;
        this.version    = b.version;
        this.resolvedAt = b.resolvedAt;
    }

    public String  getName()       { return name; }
    public String  getValue()      { return value; }
    public Type    getType()       { return type; }
    public String  getVersion()    { return version; }
    public Instant getResolvedAt() { return resolvedAt; }

    /** Splits a StringList parameter into individual values. */
    public String[] getValueAsList() {
        if (type != Type.STRING_LIST) throw new IllegalStateException("Parameter is not a StringList: " + name);
        return value.split(",");
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  name;
        private String  value;
        private Type    type       = Type.STRING;
        private String  version;
        private Instant resolvedAt = Instant.now();

        public Builder name(String v)       { this.name = v;       return this; }
        public Builder value(String v)      { this.value = v;      return this; }
        public Builder type(Type v)         { this.type = v;       return this; }
        public Builder version(String v)    { this.version = v;    return this; }
        public Builder resolvedAt(Instant v){ this.resolvedAt = v; return this; }

        public ParameterResult build() { return new ParameterResult(this); }
    }
}
