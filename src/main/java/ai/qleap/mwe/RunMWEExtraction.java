/*
 * MIT License
 *
 * Copyright (c) 2020.  qleap.ai
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
import ai.qleap.mwe.services.MWEExtractor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RunMWEExtraction {

    public static void main(String [] args) {

        ObjectMapper mapper = new ObjectMapper();
        // JSON file to Java object
        try {
            LineBasedJsonReader parser = new LineBasedJsonReader(mapper);
            Documents docs = parser.parseFile("business_journeys.jsonl",0.03);

//                    mapper.readValue(new File("business_journeys.jsonl"), Documents.class);
            System.out.println(docs.getDocs().size());
            Map<String, MWE> map = new MWEExtractor(docs).run();
//            map.values().stream().map(c-> new MWEs.MWE()).collect(Collectors.toList())
            List<MWE> mwes = new ArrayList<>(map.values());
            mwes.sort(Comparator.comparing(MWE::getNpmi));
            MWEs mweCont = new MWEs(new ArrayList<>(mwes),new ArrayList<>(), new ConcurrentHashMap<>());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("mwes.json"), mweCont);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
