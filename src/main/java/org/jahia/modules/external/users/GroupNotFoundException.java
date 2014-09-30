package org.jahia.modules.external.users;

/**
 * @author kevan
 */
public class GroupNotFoundException extends Exception{
    private static final long serialVersionUID = 1030373706107166645L;

    public GroupNotFoundException() {
    }

    public GroupNotFoundException(String message) {
        super(message);
    }

    public GroupNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public GroupNotFoundException(Throwable cause) {
        super(cause);
    }
}
