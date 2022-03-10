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
import ai.qleap.mwe.data.MWE;
import ai.qleap.mwe.data.MWEs;
import ai.qleap.mwe.io.LineBasedJsonReader;
import ai.qleap.mwe.services.MatchMWEs;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MWECounter {

    private final MatchMWEs matcher;
    Map<String, AtomicInteger> mweCount = new ConcurrentHashMap<>();

    public MWECounter(MatchMWEs matcher){
        this.matcher = matcher;
    }

    public static void main(String... args) {
        ObjectMapper mapper = new ObjectMapper();
        // JSON file to Java object
        try {
            Random r = new Random(42);
            MWEs mwes = mapper.readValue(new File("mwes.json"), MWEs.class);
            System.out.println("total mwes: " + mwes.getMwes().size());
            System.out.println("cleand mwes: "+ mwes.getMwes().size());
            MatchMWEs matcher = new MatchMWEs(mwes);
            LineBasedJsonReader parser = new LineBasedJsonReader(mapper);
            Documents docs = parser.parseFile("business_journeys.jsonl",1.);
            MWECounter counter = new MWECounter(matcher);
            docs.getDocs().parallelStream().forEach(counter::handle);
            mwes.setCounts(counter.mweCount);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("topics.json"), mwes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handle(Documents.Document d) {
        List<MWE> mwes = matcher.matchMWEs(d.getText());
        AtomicInteger ai = mweCount.computeIfAbsent(d.getMeta().getPileSet(), k -> new AtomicInteger(0));
        ai.addAndGet(mwes.size());
        mwes.forEach(m-> m.getTopicScores().computeIfAbsent(d.getMeta().getPileSet(), k -> new AtomicInteger(0)).incrementAndGet());
    }

}
