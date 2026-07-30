package ai.firefly.docs;

/** Raw bytes for a documentation asset (e.g. a screenshot), plus its guessed content type. */
public record DocAsset(byte[] bytes, String contentType) {
}
