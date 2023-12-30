package net.stoerr.chatgpt.codevengine;

/**
 * Thrown to abort an execution, but only when the error has been duly reported with sendError to ChatGPT, so that the exception can be ignored.
 */
public class ExecutionAbortedException extends RuntimeException {
    // empty
}
