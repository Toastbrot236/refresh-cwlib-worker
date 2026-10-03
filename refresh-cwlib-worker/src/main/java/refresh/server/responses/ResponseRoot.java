package refresh.server.responses;

public class ResponseRoot<TContent> {
    public int StatusCode;

    // error message if error, else optional notice (e.g. warning)
    public String message;
    public TContent content;

    public ResponseRoot(int statusCode, TContent content, String message) {
        this.StatusCode = statusCode;
        this.content = content;
        this.message = message;
    }
}
