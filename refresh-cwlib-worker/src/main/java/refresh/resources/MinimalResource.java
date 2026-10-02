package refresh.resources;

public class MinimalResource<TResource> {
    public String Hash;
    public TResource Content;

    public MinimalResource(String hash, TResource content) {
        this.Content = content;
        this.Hash = hash;
    }
}
