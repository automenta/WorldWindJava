/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.event;

/**
 * General purpose message event.
 *
 * @author pabercrombie
 * @version $Id: Message.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class Message extends WWEvent {
    /**
     * Message name.
     */
    public final String name;
    /**
     * Time at which the message was sent.
     */
    public final long when;

    /**
     * Create a message. The message will be timestamped with the current system time.
     *
     * @param name   The name of the message.
     * @param source The object that generated the message.
     */
    public Message(String name, Object source) {
        this(name, source, System.currentTimeMillis());
    }

    /**
     * Create a message, with a timestamp.
     *
     * @param name   The name of the message.
     * @param source The object that generated the message.
     * @param when   The timestamp to apply to the message.
     */
    public Message(String name, Object source, long when) {
        super(source);
        this.name = name;
        this.when = when;
    }
}