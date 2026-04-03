package io.github.raulbolivar.aws.secrets.model;

import java.time.Instant;

/**
 * Represents a resolved secret value from AWS Secrets Manager.
 */
public class SecretResult {

    private final String  secretName;
    private final String  secretString;
    private final byte[]  secretBinary;
    private final String  versionId;
    private final Instant resolvedAt;

    private SecretResult(Builder b) {
        this.secretName   = b.secretName;
        this.secretString = b.secretString;
        this.secretBinary = b.secretBinary;
        this.versionId    = b.versionId;
        this.resolvedAt   = b.resolvedAt;
    }

    public String  getSecretName()   { return secretName; }
    public String  getSecretString() { return secretString; }
    public byte[]  getSecretBinary() { return secretBinary; }
    public String  getVersionId()    { return versionId; }
    public Instant getResolvedAt()   { return resolvedAt; }

    public boolean isString() { return secretString != null; }
    public boolean isBinary() { return secretBinary != null; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String  secretName;
        private String  secretString;
        private byte[]  secretBinary;
        private String  versionId;
        private Instant resolvedAt = Instant.now();

        public Builder secretName(String v)   { this.secretName = v;   return this; }
        public Builder secretString(String v) { this.secretString = v; return this; }
        public Builder secretBinary(byte[] v) { this.secretBinary = v; return this; }
        public Builder versionId(String v)    { this.versionId = v;    return this; }
        public Builder resolvedAt(Instant v)  { this.resolvedAt = v;   return this; }

        public SecretResult build() { return new SecretResult(this); }
    }
}
