package io.github.raulbolivar.aws.lambda.model;

/**
 * Represents the result of a Lambda function invocation.
 */
public class LambdaInvocationResult {

    private final String functionName;
    private final int    statusCode;
    private final String payload;        // JSON response string
    private final String functionError;  // null if successful
    private final String logResult;      // tail of execution log (base64)
    private final String executedVersion;

    private LambdaInvocationResult(Builder b) {
        this.functionName     = b.functionName;
        this.statusCode       = b.statusCode;
        this.payload          = b.payload;
        this.functionError    = b.functionError;
        this.logResult        = b.logResult;
        this.executedVersion  = b.executedVersion;
    }

    public String getFunctionName()    { return functionName; }
    public int    getStatusCode()      { return statusCode; }
    public String getPayload()         { return payload; }
    public String getFunctionError()   { return functionError; }
    public String getLogResult()       { return logResult; }
    public String getExecutedVersion() { return executedVersion; }

    /** Returns true when Lambda executed without a function-level error. */
    public boolean isSuccess()  { return functionError == null; }

    /** Returns true when Lambda returned a handled or unhandled error. */
    public boolean isError()    { return functionError != null; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String functionName;
        private int    statusCode;
        private String payload;
        private String functionError   = null;
        private String logResult       = null;
        private String executedVersion = null;

        public Builder functionName(String v)    { this.functionName = v;     return this; }
        public Builder statusCode(int v)         { this.statusCode = v;       return this; }
        public Builder payload(String v)         { this.payload = v;          return this; }
        public Builder functionError(String v)   { this.functionError = v;    return this; }
        public Builder logResult(String v)       { this.logResult = v;        return this; }
        public Builder executedVersion(String v) { this.executedVersion = v;  return this; }

        public LambdaInvocationResult build() { return new LambdaInvocationResult(this); }
    }
}
