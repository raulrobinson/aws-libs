package io.github.raulbolivar.aws.lambda.exception;

/**
 * Unchecked exception thrown when a Lambda invocation fails at the transport or SDK level.
 * For function-level errors (handled/unhandled), check {@link io.github.raulbolivar.aws.lambda.model.LambdaInvocationResult#isError()}.
 */
public class LambdaClientException extends RuntimeException {

    private final String functionName;
    private final int    statusCode;

    public LambdaClientException(String message, String functionName, int statusCode, Throwable cause) {
        super(message, cause);
        this.functionName = functionName;
        this.statusCode   = statusCode;
    }

    public LambdaClientException(String message, String functionName, Throwable cause) {
        this(message, functionName, -1, cause);
    }

    public String getFunctionName() { return functionName; }
    public int    getStatusCode()   { return statusCode; }
}
