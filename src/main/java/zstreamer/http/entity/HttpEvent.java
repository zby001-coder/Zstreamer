package zstreamer.http.entity;

public enum HttpEvent {
    /**
     *
     */
    RECEIVE_REQUEST,
    DISPATCH_REQUEST,
    SEND_HEAD,
    FAIL_FILTER,
    FINISH_RESPONSE,
    NOT_FOUND,
    WRONG_METHOD,
    EXCEPTION
}
