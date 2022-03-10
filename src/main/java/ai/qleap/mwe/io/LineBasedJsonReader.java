/*
 * MIT License
 *
 * Copyright (c) 2022.  qleap.ai
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ai.qleap.mwe.io;

import ai.qleap.mwe.Test;
import ai.qleap.mwe.data.Documents;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Simple streamed reader to go through Lined JSON files, convert each line to POJO entry
 * and perform a specified action on every row.
 * @author Vladimir Salin @ SwiftDil
 */
public class LineBasedJsonReader {

    private static final Logger log = LoggerFactory.getLogger(LineBasedJsonReader.class);
    private final ObjectMapper objectMapper;

    public LineBasedJsonReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses a provided input in a streamed way. Converts each line in it
     * (which is supposed to be a JSON) to a specified POJO class
     * and performs an action provided as a Java 8 Consumer.
     *
     * @param stream lined JSON input
     * @param entryClass POJO class to convert JSON to
     * @param consumer action to perform on each entry
     * @return number of rows read
     */
    public int parseAsStream(final InputStream stream, final Class entryClass, final Consumer<? super Object> consumer) {
        long start = System.currentTimeMillis();

        final AtomicInteger total = new AtomicInteger(0);
        final AtomicInteger failed = new AtomicInteger(0);

        try (Stream<String> lines = new BufferedReader(new InputStreamReader(stream)).lines();) {
            lines
                    .map(line -> {
                        try {
                            total.incrementAndGet();
                            return objectMapper.readerFor(entryClass).readValue(line);
                        } catch (IOException e) {
                            log.error("Failed to parse a line {}. Reason: {}", total.get()-1, e.getMessage());
                            log.debug("Stacktrace: ", e);
                            failed.incrementAndGet();
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .forEach(consumer);
        }
        long took = System.currentTimeMillis() - start;
        log.info("Parsed {} lines with {} failures. Took {}ms", total.get(), failed.get(), took);

        return total.get() - failed.get();
    }

    public Documents parseFile(String fileName, double sample) throws FileNotFoundException {
        Documents docs = new Documents();
        Consumer<? super Object> consumer = new DocumentConsumer(docs,sample);
        parseAsStream(new FileInputStream(fileName), Documents.Document.class,consumer);
        System.out.println(docs.getDocs().size());
        return docs;
    }

    private static class DocumentConsumer implements Consumer<Object> {

        private final Documents docs;
        private final double sample;
//        private long cnt = 0;`
        private final Random random = new Random();

        public DocumentConsumer(Documents docs, double sample) {
            this.docs = docs;
            this.sample = sample;
        }

        @Override
        public void accept(Object document) {
            if (random.nextDouble() > sample) {
                return;
            }
            this.docs.getDocs().add((Documents.Document) document);
        }

    }
}