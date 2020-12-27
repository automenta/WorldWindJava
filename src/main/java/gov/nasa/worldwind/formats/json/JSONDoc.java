/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.formats.json;

import com.fasterxml.jackson.core.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.util.*;

import java.io.*;
import java.util.logging.Level;

/**
 * @author dcollins
 * @version $Id: JSONDoc.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class JSONDoc {
    protected Object root;
    protected String name;

    public JSONDoc(Object source) throws Exception {
        if (WWUtil.isEmpty(source)) {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.name = WWIO.getSourcePath(source);

        InputStream s = WWIO.openStream(source);

        try (var json = new JsonFactory().createParser(s)) {

            JSONEventParserContext ctx = this.createEventParserContext(json);
            if (ctx == null) {
                Logging.logger().warning(Logging.getMessage("generic.CannotParse", this.name));
                return;
            }

            if (!ctx.hasNext())
                return;

            JSONEventParser rootParser = this.createRootObjectParser();
            if (rootParser == null) {
                Logging.logger().warning(Logging.getMessage("generic.CannotParse", this.name));
                return;
            }

            this.root = rootParser.parse(ctx, ctx.nextEvent());

        }
        catch (Exception e) {
            String message = Logging.getMessage("generic.ExceptionWhileReading", this.name);
            Logging.logger().log(Level.SEVERE, message, e);
            throw new WWRuntimeException(message, e);
        }
    }

    public Object getRoot() {
        return this.root;
    }

    protected JSONEventParserContext createEventParserContext(JsonParser parser) throws IOException {
        return new BasicJSONEventParserContext(parser);
    }

    protected JSONEventParser createRootObjectParser() {
        return new BasicJSONEventParser();
    }
}
