package com.fasterxml.jackson.core.json;

import java.io.*;

import com.fasterxml.jackson.core.*;

public class JsonFactoryTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testGeneratorFeatures() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(JsonGenerator.Feature.QUOTE_FIELD_NAMES)
                .build();
        assertTrue(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
        f = f.rebuild().configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false)
                .build();
        assertFalse(f.isEnabled(JsonGenerator.Feature.QUOTE_FIELD_NAMES));
    }

    public void testFactoryFeatures() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .enable(TokenStreamFactory.Feature.INTERN_FIELD_NAMES)
                .build();
        assertTrue(f.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));

        f = f.rebuild()
                .disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
                .build();
        assertFalse(f.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));

        // by default, should be enabled
        assertTrue(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));
        f = f.rebuild().disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING)
                .build();
        assertFalse(f.isEnabled(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING));
    }

    // for [core#189]: verify that it's ok to disable recycling
    // Basically simply exercises basic functionality, to ensure
    // there are no obvious problems; needed since testing never
    // disables this handling otherwise
    public void testDisablingBufferRecycling() throws Exception
    {
        JsonFactory f = JsonFactory.builder()
                .disable(JsonFactory.Feature.USE_THREAD_LOCAL_FOR_BUFFER_RECYCLING)
                .build();

        // First, generation
        for (int i = 0; i < 3; ++i) {
            StringWriter w = new StringWriter();
            JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), w);
            gen.writeStartObject();
            gen.writeEndObject();
            gen.close();
            assertEquals("{}", w.toString());
        }
    
        for (int i = 0; i < 3; ++i) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), bytes);
            gen.writeStartArray();
            gen.writeEndArray();
            gen.close();
            assertEquals("[]", bytes.toString("UTF-8"));
        }

        // Then parsing:
        for (int i = 0; i < 3; ++i) {
            JsonParser p = f.createParser(ObjectReadContext.empty(), "{}");
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
            p.close();

            p = f.createParser(ObjectReadContext.empty(), "{}".getBytes("UTF-8"));
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            assertNull(p.nextToken());
            p.close();
        }
    }
    
    public void testJsonWithFiles() throws Exception
    {
        File file = File.createTempFile("jackson-test", null);
        file.deleteOnExit();
        
        JsonFactory f = new JsonFactory();

        // First: create file via generator.. and use an odd encoding
        JsonGenerator g = f.createGenerator( ObjectWriteContext.empty(), file, JsonEncoding.UTF16_LE);
        g.writeStartObject();
        g.writeRaw("   ");
        g.writeEndObject();
        g.close();

        // Ok: first read file directly
        JsonParser jp = f.createParser(ObjectReadContext.empty(), file);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        // Then via URL:
        jp = f.createParser(ObjectReadContext.empty(), file.toURI().toURL());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        // ok, delete once we are done
        file.delete();
    }

    // #72
    public void testCopy() throws Exception
    {
        JsonFactory jf = new JsonFactory();
        // first, verify defaults

        // since 3.0:
        assertFalse(jf.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        assertFalse(jf.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertFalse(jf.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));

        jf = jf.rebuild()
                .enable(JsonFactory.Feature.INTERN_FIELD_NAMES)
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(JsonGenerator.Feature.ESCAPE_NON_ASCII)
                .build();
        // then change, verify that changes "stick"
        assertTrue(jf.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        assertTrue(jf.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertTrue(jf.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));

        JsonFactory jf2 = jf.copy();
        assertTrue(jf2.isEnabled(JsonFactory.Feature.INTERN_FIELD_NAMES));
        assertTrue(jf.isEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertTrue(jf.isEnabled(JsonGenerator.Feature.ESCAPE_NON_ASCII));
    }
}

