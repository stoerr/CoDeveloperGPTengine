package net.stoerr.chatgpt.devtoolbench;

/**
 * Thrown to abort an execution, but only when the error has been duly reported with sendError to ChatGPT.
 */
public class ExecutionAbortedException extends RuntimeException {
    public ExecutionAbortedException(String msg) {
        super(msg);
    }
}
