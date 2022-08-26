package zstreamer.http.entity;

/**
 * @author 张贝易
 * 请求/响应的状态
 */
public abstract class MessageState {
    public static Disabled initState() {
        return Disabled.DISABLED;
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

    public static class Disabled extends MessageState {
        private static final Disabled DISABLED = new Disabled();

        private Disabled() {

        }

        @Override
        public MessageState changeState(HttpEvent event) {
            switch (event) {
                case START:
                    return ReceivedHead.RECEIVED_HEAD;
                case EXCEPTION:
                    return DISABLED;
                default:
                    return Error.ERROR;
            }
        }
    }

    public static class ReceivedHead extends MessageState {
        private static final ReceivedHead RECEIVED_HEAD = new ReceivedHead();

        private ReceivedHead() {
        }

        @Override
        public MessageState changeState(HttpEvent event) {
            switch (event) {
                case FIND_SERVICE:
                    return ServiceFound.SERVICE_FOUND;
                case NOT_FOUND:
                case EXCEPTION:
                    return Disabled.DISABLED;
                default:
                    return Error.ERROR;
            }
        }
    }

    public static class ServiceFound extends MessageState {
        private static final ServiceFound SERVICE_FOUND = new ServiceFound();

        private ServiceFound() {
        }

        @Override
        public MessageState changeState(HttpEvent event) {
            switch (event) {
                case RESPOND_HEADER:
                    return RespondedHead.RESPONDED_HEAD;
                case FAIL_FILTER:
                case EXCEPTION:
                case WRONG_METHOD:
                    return Disabled.DISABLED;
                default:
                    return Error.ERROR;
            }
        }
    }

    public static class RespondedHead extends MessageState {
        private static final RespondedHead RESPONDED_HEAD = new RespondedHead();

        private RespondedHead() {
        }

        @Override
        public MessageState changeState(HttpEvent event) {
            switch (event) {
                case FINISH_RESPONSE:
                case EXCEPTION:
                    return Disabled.DISABLED;
                default:
                    return Error.ERROR;
            }
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
