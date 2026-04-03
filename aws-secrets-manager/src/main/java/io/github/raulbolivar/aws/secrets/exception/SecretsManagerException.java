package io.github.raulbolivar.aws.secrets.exception;

/**
 * Unchecked exception thrown when an AWS Secrets Manager operation fails.
 */
public class SecretsManagerException extends RuntimeException {

    private final String secretName;
    private final String errorCode;

    public SecretsManagerException(String message, String secretName, String errorCode, Throwable cause) {
        super(message, cause);
        this.secretName = secretName;
        this.errorCode  = errorCode;
    }

    public SecretsManagerException(String message, String secretName, Throwable cause) {
        this(message, secretName, "UNKNOWN", cause);
    }

    public String getSecretName() { return secretName; }
    public String getErrorCode()  { return errorCode; }
}
