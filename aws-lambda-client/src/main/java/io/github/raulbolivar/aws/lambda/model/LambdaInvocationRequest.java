package io.github.raulbolivar.aws.lambda.model;

/**
 * Represents a Lambda invocation request.
 */
public class LambdaInvocationRequest {

    public enum InvocationType { REQUEST_RESPONSE, EVENT, DRY_RUN }

    private final String         functionName;
    private final String         payload;           // JSON string
    private final InvocationType invocationType;
    private final String         qualifier;         // function alias or version

    private LambdaInvocationRequest(Builder b) {
        this.functionName   = b.functionName;
        this.payload        = b.payload;
        this.invocationType = b.invocationType;
        this.qualifier      = b.qualifier;
    }

    public String         getFunctionName()   { return functionName; }
    public String         getPayload()        { return payload; }
    public InvocationType getInvocationType() { return invocationType; }
    public String         getQualifier()      { return qualifier; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String         functionName;
        private String         payload        = "{}";
        private InvocationType invocationType = InvocationType.REQUEST_RESPONSE;
        private String         qualifier      = null;

        public Builder functionName(String v)         { this.functionName = v;   return this; }
        public Builder payload(String v)              { this.payload = v;        return this; }
        public Builder invocationType(InvocationType v){ this.invocationType = v; return this; }
        public Builder qualifier(String v)            { this.qualifier = v;      return this; }

        public LambdaInvocationRequest build() {
            if (functionName == null || functionName.isBlank())
                throw new IllegalArgumentException("functionName is required");
            return new LambdaInvocationRequest(this);
        }
    }
}
