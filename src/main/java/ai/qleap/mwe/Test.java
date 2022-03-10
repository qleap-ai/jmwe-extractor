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

package ai.qleap.mwe;

import ai.qleap.mwe.data.Documents;
import ai.qleap.mwe.io.LineBasedJsonReader;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Test {
    
    public static void main(String [] args) throws FileNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        LineBasedJsonReader parser = new LineBasedJsonReader(mapper);
        List<Documents.Document> docs = new ArrayList<>();
        Consumer<? super Object> consumer = new TestConsumer(docs);
        parser.parseAsStream(new FileInputStream(new File("business_journeys.jsonl")), Documents.Document.class,consumer);
        System.out.println(docs.size());
    }

    private static class TestConsumer implements Consumer<Object> {

        private final List<Documents.Document> docs;

        public TestConsumer(List<Documents.Document> docs) {
            this.docs = docs;
        }

        @Override
        public void accept(Object document) {
            this.docs.add((Documents.Document) document);
        }

    }
}
