package zstreamer.http.entity;

public enum HttpEvent {
    /**
     *
     */
    START,
    FIND_SERVICE,
    RESPOND_HEADER,
    FAIL_FILTER,
    NOT_FOUND,
    WRONG_METHOD,
    FINISH_RESPONSE,
    EXCEPTION
}
