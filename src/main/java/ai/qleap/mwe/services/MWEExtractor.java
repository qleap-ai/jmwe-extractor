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

package ai.qleap.mwe.services;

import ai.qleap.mwe.data.Documents;
import ai.qleap.mwe.data.MWE;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class MWEExtractor {

    public static final int NGRAMM = 4;
    private static final int MIN_OCC = 10;

    private final Documents docs;

    private final AtomicInteger toks = new AtomicInteger(0);

    private final Map<String, MWE> candMWEs = new ConcurrentHashMap<>(3 * 39479320);
    private final Map<String, AtomicInteger> unigrams = new ConcurrentHashMap<>();
    private double numToks;

    public MWEExtractor(Documents documents) {
        this.docs = documents;
    }

    public Map<String, MWE> run() {
        double total = this.docs.getDocs().size();
        AtomicInteger processed = new AtomicInteger(0);

        this.docs.getDocs().parallelStream().map(Documents.Document::getText).peek(t -> {
            int pp = processed.incrementAndGet();
            if (pp % 1000 == 0) {
                System.out.println(pp + " of " + total + " cands thus far " + candMWEs.size());
            }
        })
//                .limit(10000)
                .forEach(this::handle);
        System.out.println("candidates: " + candMWEs.size());
        candMWEs.entrySet().removeIf(e -> e.getValue().getNpmi() < 0 || e.getValue().getCount().get() < MIN_OCC || e.getValue().getMwe().length() < 7);
        System.out.println("filtered candidates: " + candMWEs.size());
        System.out.println("total unique unigrams: " + unigrams.size());
        System.out.println("total tokens: " + toks.get());
        this.numToks = toks.get();
        candMWEs.entrySet().parallelStream().forEach(e->computePMI(e.getValue()));

        return candMWEs;
    }



    private void computePMI(MWE mwe) {
        double pmwe = (double)mwe.getCount().get()/numToks;
        double h = -Math.log(pmwe)/Math.log(2);

        for (String tok : mwe.getToks()) {
            double ptok = (double)unigrams.get(tok).get()/numToks;
            pmwe /= ptok;
        }
        double pmi = Math.log(pmwe);
        mwe.setPmi(pmi);
        mwe.setNpmi(pmi/h);
    }

    private void handle(String t) {
        if (t==null){
            return;
        }
        String[] words = t.toLowerCase(Locale.GERMAN).split("\\s+");
        Arrays.stream(words).forEach(w -> unigrams.computeIfAbsent(w, k -> new AtomicInteger(0)).incrementAndGet());

        toks.addAndGet(words.length);
        List<MWE> cands = new ArrayList<>();
        Arrays.stream(words).forEach(w -> cands.add(new MWE(w, Collections.singletonList(w))));

        for (int i = 0; i < words.length - 1; i++) {
            StringBuilder sb = new StringBuilder();
            String w = words[i];
//            System.out.println(w);
            sb.append(w);
            List<String> toks = new ArrayList<>();
            toks.add(w);
            for (int j = i + 1; j < Math.min(words.length - 1, i + NGRAMM); j++) {
                String w2 = words[j];
                if (w2.equals("none")){
                    break;
                }

                sb.append("_");
                if (sb.toString().startsWith("_")){
                    sb.deleteCharAt(0);
                }
                sb.append(w2);
                toks.add(w2);
                MWE ci = new MWE(removePunctuationAtEnd(sb.toString()), new ArrayList<>(toks));
                cands.add(ci);
            }
        }
        handleCands(cands);
    }

    private void handleCands(List<MWE> cands) {
        for (MWE cand : cands) {
            MWE ci = candMWEs.computeIfAbsent(cand.getMwe(), k -> new MWE(cand));
            ci.getCount().incrementAndGet();
        }
    }

    public static void main(String ... args) {
        String text = "Absperrklappe, als Endarmatur, Gehäuse aus Gusseisen EN-GJL-250, DN 15, Nenndruck 0,6 MPa (6 bar), für Trinkwasser DIN 1988-200, weich dichtend, geeignet für Fremdbetätigung.";

        String[] words = text.toLowerCase(Locale.GERMAN).split("\\s+");


        List<MWE> cands = new ArrayList<>();
        for (int i = 0; i < words.length - 1; i++) {
            StringBuilder sb = new StringBuilder();
            String w = words[i];
            System.out.println(w);
            sb.append(w);
            List<String> toks = new ArrayList<>();
            toks.add(w);
            for (int j = i + 1; j < Math.min(words.length - 1, i + NGRAMM); j++) {
                String w2 = words[j];
                if (w2.equals("none")){
                    break;
                }

                sb.append("_");
                if (sb.toString().startsWith("_")){
                    sb.deleteCharAt(0);
                }
                sb.append(w2);
                toks.add(w2);
                MWE ci = new MWE(removePunctuationAtEnd(sb.toString()), new ArrayList<>(toks));
                cands.add(ci);
            }
        }
        System.out.println(cands);
    }

    public static String removePunctuationAtEnd(String input) {
        Pattern pattern = Pattern.compile("(.*?)[.,;:]$");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return input;
        }
    }
}
