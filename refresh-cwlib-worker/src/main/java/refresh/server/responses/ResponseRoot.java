package refresh.server.responses;

public class ResponseRoot<TContent> {
    // error message if error, else optional notice (e.g. warning)
    public String message;
    public TContent content;

    public ResponseRoot(TContent content, String message) {
        this.content = content;
        this.message = message;
    }
}
