package zstreamer.http.entity;

/**
 * @author 张贝易
 * 请求/响应的状态
 */
public abstract class MessageState {
    public static WaitRequest initState() {
        return WaitRequest.WAIT_REQUEST;
    }

    public MessageState() {
    }

    /**
     * 让子类根据事件自己切换状态
     *
     * @param event 事件
     * @return 新的状态
     */
    public abstract MessageState changeState(HttpEvent event);

    public static class WaitRequest extends MessageState {
        private static final WaitRequest WAIT_REQUEST = new WaitRequest();

        private WaitRequest() {

        }

        @Override
        public MessageState changeState(HttpEvent event) {
            if (event == HttpEvent.RECEIVE_REQUEST) {
                return ReceivedRequest.RECEIVED_REQUEST;
            }
            return Error.ERROR;
        }
    }

    public static class ReceivedRequest extends MessageState {
        private static final ReceivedRequest RECEIVED_REQUEST = new ReceivedRequest();

        private ReceivedRequest() {
        }

        @Override
        public MessageState changeState(HttpEvent event) {
            switch (event) {
                case DISPATCH_REQUEST:
                    return WaitResponse.WAIT_RESPONSE;
                case NOT_FOUND:
                case EXCEPTION:
                case FAIL_FILTER:
                    return HandleException.HANDLE_EXCEPTION;
                default:
                    return Error.ERROR;
            }
        }
    }

    public static class WaitResponse extends MessageState {
        private static final WaitResponse WAIT_RESPONSE = new WaitResponse();

        private WaitResponse() {
        }

        @Override
        public MessageState changeState(HttpEvent event) {
            switch (event) {
                case SEND_HEAD:
                    return SendingResponse.SENDING_RESPONSE;
                case EXCEPTION:
                case WRONG_METHOD:
                    return HandleException.HANDLE_EXCEPTION;
                default:
                    return Error.ERROR;
            }
        }
    }

    public static class HandleException extends MessageState {
        private static final HandleException HANDLE_EXCEPTION = new HandleException();

        private HandleException() {
        }

        @Override
        public MessageState changeState(HttpEvent event) {
            if (event == HttpEvent.SEND_HEAD) {
                return SendingResponse.SENDING_RESPONSE;
            }
            return Error.ERROR;
        }
    }

    public static class SendingResponse extends MessageState {
        private static final SendingResponse SENDING_RESPONSE = new SendingResponse();

        @Override
        public MessageState changeState(HttpEvent event) {
            if (event == HttpEvent.FINISH_RESPONSE) {
                return WaitRequest.WAIT_REQUEST;
            }
            return Error.ERROR;
        }
    }

    public static class Error extends MessageState {
        private static final Error ERROR = new Error();

        private Error() {
        }

        @Override
        public MessageState changeState(HttpEvent event) {
            return null;
        }
    }
}
