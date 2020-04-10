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
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MWEExtractor {

    private static final int NGRAMM = 4;
    private static final int MIN_OCC = 10;

    private final Documents docs;

    private final AtomicInteger toks = new AtomicInteger(0);

    private final Map<String, CandInfo> candMWEs = new ConcurrentHashMap<>(3 * 39479320);
    private final Map<String, AtomicInteger> unigrams = new ConcurrentHashMap<>();
    private double numToks;

    public MWEExtractor(Documents documents) {
        this.docs = documents;
    }

    public Map<String, CandInfo> run() {
        double total = this.docs.getDocs().size();
        AtomicInteger processed = new AtomicInteger(0);
        this.docs.getDocs().stream().map(Documents.Document::getText).peek(t -> {
            int pp = processed.incrementAndGet();
            if (pp % 1000 == 0) {
                System.out.println(pp + " of " + total + " cands thus far " + candMWEs.size());
            }
        })
//                .limit(10000)
                .forEach(this::handle);
        System.out.println("candidates: " + candMWEs.size());
        candMWEs.entrySet().removeIf(e -> e.getValue().count.get() < MIN_OCC || e.getValue().cand.length() < 7);
        System.out.println("filtered candidates: " + candMWEs.size());
        System.out.println("total unique unigrams: " + unigrams.size());
        System.out.println("total tokens: " + toks.get());
        this.numToks = toks.get();
        candMWEs.entrySet().stream().forEach(e->computePMI(e.getValue()));
        return candMWEs;
    }



    private void computePMI(CandInfo mwe) {
        double pmwe = (double)mwe.count.get()/numToks;
        double h = -Math.log(pmwe)/Math.log(2);

        for (String tok : mwe.toks) {
            double ptok = (double)unigrams.get(tok).get()/numToks;
            pmwe /= ptok;
        }
        double pmi = Math.log(pmwe);
        mwe.pmi = pmi;
        mwe.npmi = pmi/h;
    }

    private void handle(String t) {
        String[] words = t.split("\\s+");
        Arrays.stream(words).forEach(w -> unigrams.computeIfAbsent(w, k -> new AtomicInteger(0)).incrementAndGet());
        toks.addAndGet(words.length);
        List<CandInfo> cands = new ArrayList<>();
        for (int i = 0; i < words.length - 1; i++) {
            StringBuilder sb = new StringBuilder();
            String w = words[i];
            sb.append(w);
            List<String> toks = new ArrayList<>();
            toks.add(w);
            for (int j = i + 1; j < Math.min(words.length - 1, i + NGRAMM); j++) {
                sb.append("_");
                String w2 = words[j];
                sb.append(w2);
                toks.add(w2);
                CandInfo ci = new CandInfo(sb.toString(),new ArrayList<>(toks));
                cands.add(ci);
            }
        }
        handleCands(cands);
    }

    private void handleCands(List<CandInfo> cands) {
        for (CandInfo cand : cands) {
            CandInfo ci = candMWEs.computeIfAbsent(cand.cand, k -> new CandInfo(cand));
            ci.count.incrementAndGet();
        }
    }

    @JsonAutoDetect
    public static final class CandInfo {
        @JsonProperty("mwe")
        String cand;
        @JsonProperty("tokens")
        List<String> toks;
        @JsonProperty("count")
        AtomicInteger count = new AtomicInteger(0);
        @JsonProperty("pmi")
        double pmi;
        @JsonProperty("npmi")
        double npmi;

        public CandInfo(CandInfo ci){
            this.cand = ci.cand;
            this.toks = ci.toks;
        }

        public CandInfo(String cand, List<String> toks) {
            this.cand = cand;
            this.toks = toks;
        }
    }
}
