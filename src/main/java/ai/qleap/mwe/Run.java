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
import ai.qleap.mwe.data.MWEs;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class Run {

    public static void main(String [] args) {

        ObjectMapper mapper = new ObjectMapper();
        // JSON file to Java object
        try {
            Documents docs = mapper.readValue(new File("chunked_corpus_df_unique_IDs_sample.json"), Documents.class);
            System.out.println(docs.getDocs().size());
            Map<String, MWEExtractor.CandInfo> map = new MWEExtractor(docs).run();
//            map.values().stream().map(c-> new MWEs.MWE()).collect(Collectors.toList())
            MWEs mwes = new MWEs(new ArrayList<>(map.values()));
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("mwes.json"), mwes);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
