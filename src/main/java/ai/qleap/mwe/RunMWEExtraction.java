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
import ai.qleap.mwe.services.MWEMatcher;
import ai.qleap.mwe.services.MWERanker;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RunMWEExtraction {

    public static void main(String [] args) {

        ObjectMapper mapper = new ObjectMapper();
        // JSON file to Java object
        try {
            //positive corpus
//            LineBasedJsonReader parser = new LineBasedJsonReader(mapper);
//            Documents docs = parser.parseFile("positive.jsonl",1);
//            System.out.println(docs.getDocs().size());
//            Map<String, MWE> pos_map = new MWEExtractor(docs).run();
            Map<String, MWE> pos_map = loadAndExtractMWEs("positive.jsonl", mapper,1);
            Map<String, MWE> neg_map = loadAndExtractMWEs("negative.jsonl", mapper,1);
            //negative corpus
            new MWERanker(pos_map,neg_map).run();

//            map.values().stream().map(c-> new MWEs.MWE()).collect(Collectors.toList())
            List<MWE> mwes = pos_map.values().stream().filter(mwe -> mwe.getChi2() > 0.0).sorted(Comparator.comparing(MWE::getChi2)).collect(Collectors.toList());
            MWEs mweCont = new MWEs(new ArrayList<>(mwes),new ArrayList<>(), new ConcurrentHashMap<>());
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("mwes.json"), mweCont);

            List<MWE> filteredMWEs = mwes.stream().filter(mwe -> mwe.getChi2() > 0.0).collect(Collectors.toList());
            MWEMatcher matcher = new MWEMatcher(filteredMWEs,3);
            List<MWE> res = matcher.match("FA Oberteil, f. Selbstschlussvtl. DN15, duschen, fernbetätigt, 2000104403");
            res.stream().forEach(mwe -> System.out.println(mwe.getMwe() + " " + mwe.getChi2()));
            System.out.println("=====");
            res = matcher.match("Absperrklappe, als Endarmatur, Gehäuse aus Gusseisen EN-GJL-250, DN 15, Nenndruck 0,6 MPa (6 bar), für Trinkwasser DIN 1988-200, weich dichtend, geeignet für Fremdbetätigung.");
            res.stream().forEach(mwe -> System.out.println(mwe.getMwe() + " " + mwe.getChi2()));
//            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, MWE> loadAndExtractMWEs(String filename, ObjectMapper mapper, double sample) throws FileNotFoundException {
        LineBasedJsonReader parser = new LineBasedJsonReader(mapper);
        Documents docs = parser.parseFile(filename,sample);
        System.out.println(docs.getDocs().size());
        return new MWEExtractor(docs).run();
    }
}
