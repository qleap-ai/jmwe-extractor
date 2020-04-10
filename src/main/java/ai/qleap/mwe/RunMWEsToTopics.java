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
import ai.qleap.mwe.services.MapToTopics;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class RunMWEsToTopics {

    public static void main(String ... args) {
        ObjectMapper mapper = new ObjectMapper();
        // JSON file to Java object
        try {
//            Documents docs = mapper.readValue(new File("chunked_corpus_df_unique_IDs.json"), Documents.class);
//            System.out.println(docs.getDocs().size());

            MWEs mwes = mapper.readValue(new File("mwes_full_corpus_bi_to_4-grams.json"), MWEs.class);
            System.out.println("total mwes: "+ mwes.getMwes().size());
            mwes.getMwes().removeIf(next -> next.getNpmi() < 0.5);
            System.out.println("cleand mwes: "+ mwes.getMwes().size());
            Documents docs = mapper.readValue(new File("chunked_corpus_df_unique_IDs.json"), Documents.class);
            System.out.println("total docs: " + docs.getDocs().size());
            Set<String> classes = docs.getDocs().parallelStream().map(Documents.Document::getClazz).collect(Collectors.toSet());
            System.out.println("total classes: " + classes.size());
            System.out.println(classes);
            for (String cl : classes){
                System.out.println("running: "+cl);
                new MapToTopics(mwes, docs).run(cl);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
