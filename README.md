# aws-libs

Personal Java libraries for AWS service integration.
Published to Maven Central under `io.github.raulrobinson`.

## Modules

| Module                | Group ID                 | Artifact              | Description                                       |
|-----------------------|--------------------------|-----------------------|---------------------------------------------------|
| `aws-secrets-manager` | `io.github.raulrobinson` | `aws-secrets-manager` | AWS Secrets Manager client with TTL cache         |
| `aws-parameter-store` | `io.github.raulrobinson` | `aws-parameter-store` | AWS SSM Parameter Store client with TTL cache     |
| `aws-lambda-client`   | `io.github.raulrobinson` | `aws-lambda-client`   | AWS Lambda async invocation client                |
| `aws-eventbridge`     | `io.github.raulrobinson` | `aws-eventbridge`     | AWS EventBridge publish client with batch support |

All modules use **AWS SDK v2**, follow **hexagonal architecture** (port + adapter), and require **Java 17+**.

---

## Installation

### Gradle
```groovy
dependencies {
    implementation 'io.github.raulrobinson:aws-secrets-manager:1.0.0'
    implementation 'io.github.raulrobinson:aws-parameter-store:1.0.0'
    implementation 'io.github.raulrobinson:aws-lambda-client:1.0.0'
    implementation 'io.github.raulrobinson:aws-eventbridge:1.0.0'
}
```

### Maven
```xml
<dependency>
    <groupId>io.github.raulrobinson</groupId>
    <artifactId>aws-secrets-manager</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Usage Examples

### Secrets Manager
```java
SecretsManagerPort secrets = SecretsManagerAdapter.create(
    SecretsManagerProperties.builder()
        .region("us-east-1")
        .cacheTtlSeconds(3600)
        .build()
);

// Get full secret
SecretResult result = secrets.getSecret("prod/myapp/db");

// Parse JSON secret into map
Map<String, String> values = secrets.getSecretAsMap("prod/myapp/db");
String password = values.get("password");

// Get a single key from JSON secret
String apiKey = secrets.getSecretValue("prod/myapp/api", "key").orElseThrow();
```

### Parameter Store
```java
ParameterStorePort ssm = ParameterStoreAdapter.create(
    ParameterStoreProperties.builder()
        .region("us-east-1")
        .pathPrefix("/myapp/prod/")
        .cacheTtlSeconds(43200)   // 12h — intentional, no hot-reload
        .build()
);

// Get a single parameter
String dbUrl = ssm.getParameterValue("/myapp/prod/db-url").orElseThrow();

// Load all parameters under a path at once
Map<String, String> config = ssm.getParametersByPathAsMap("/myapp/prod/");
```

### Lambda Client
```java
LambdaClientPort lambda = LambdaClientAdapter.create(
    LambdaClientProperties.builder()
        .region("us-east-1")
        .build()
);

// Simple invocation
String response = lambda.invokeAndGetPayload("my-function", "{\"op\":\"ping\"}");

// Async invocation
CompletableFuture<String> future = lambda.invokeAndGetPayloadAsync("my-function", payload);

// Fire-and-forget (Event type)
lambda.invokeAsync("my-function", "{\"op\":\"process\"}");
```

### EventBridge
```java
EventBridgePort bus = EventBridgeAdapter.create(
    EventBridgeProperties.builder()
        .region("us-east-1")
        .defaultEventBus("my-app-bus")
        .defaultSource("com.myorg.myapp")
        .build()
);

// Simple publish
bus.publish("com.myorg.customers", "CustomerCreated", "{\"id\":\"abc-123\"}");

// Structured publish
EventBridgePublishResult result = bus.publish(
    EventBridgeEvent.builder()
        .source("com.myorg.orders")
        .detailType("OrderShipped")
        .detail("{\"orderId\":\"xyz\",\"tracking\":\"1Z999\"}")
        .eventBusName("my-app-bus")
        .build()
);

// Batch publish (auto-splits at 10 per AWS limit)
bus.publishBatch(List.of(event1, event2, event3));
```

### LocalStack (local development)
```java
// All clients accept an endpointOverride for LocalStack
SecretsManagerAdapter.create(
    SecretsManagerProperties.builder()
        .region("us-east-1")
        .endpointOverride("http://localhost:4566")
        .accessKeyId("test")
        .secretAccessKey("test")
        .build()
);
```

---

## Publishing to Maven Central

### One-time setup

1. Register at [central.sonatype.com](https://central.sonatype.com) and claim `io.github.raulrobinson`
2. Generate a GPG key:
   ```bash
   gpg --gen-key
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   gpg --export-secret-keys --armor YOUR_KEY_ID > private.key
   ```
3. Add credentials to `~/.gradle/gradle.properties`:
   ```properties
   ossrhUsername=YOUR_SONATYPE_USERNAME
   ossrhPassword=YOUR_SONATYPE_PASSWORD
   signingKey=<contents of private.key>
   signingPassword=YOUR_GPG_PASSPHRASE
   ```

### Publish locally
```bash
./gradlew publish
```

### Publish via GitHub (recommended)
Add the four secrets to your GitHub repo (`Settings → Secrets → Actions`):
- `OSSRH_USERNAME`
- `OSSRH_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

Then create a GitHub Release — the workflow triggers automatically.

---

## Architecture

Each module follows hexagonal architecture:

```
port/          → interface (what the module can do)
adapter/       → implementation (AWS SDK v2)
model/         → request/response value objects
config/        → properties builder
exception/     → typed unchecked exceptions
```

Consumers depend only on the **port** interface. The **adapter** is wired at startup.

---

## License

MIT © Raul Robinson Bolívar Navas
