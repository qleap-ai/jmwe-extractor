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
import ai.qleap.mwe.data.MWEs;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static ai.qleap.mwe.services.MWEExtractor.NGRAMM;


public class MapToTopics {
    private final MWEs mwes;
    private final Documents docs;
    private final MatchMWEs matcher;

    public MapToTopics(MWEs mwes, Documents docs) {
        this.mwes = mwes;
        this.docs = docs;
        this.matcher = new MatchMWEs(mwes);
    }

    public void run(String topic) {
        int processed = 0;
        int next = 1;
        System.out.println("extracting mwe from positive corpus");
        List<Documents.Document> topicDocs = new ArrayList<>();
        Set<MWE> matched = new HashSet<>();
        int posCnt = 0;
        for (Documents.Document doc : this.docs.getDocs()) {
            if (processed >= next) {
                next *= 2;
                System.out.println("Docs processed " + processed + " out of " + this.docs.getDocs().size());
            }
            processed++;
            if (doc.getClazz().equals(topic)) {
                topicDocs.add(doc);
                if (doc.getCorpus().equals("Positive")) {
                    posCnt++;
                    List<MWE> tmp = matcher.matchMWEs(doc.getText());
                    matched.addAll(tmp);

//                    myMWEs.addAll();

                }
            }


        }

        System.out.println("Counting matched MWEs in pos+neg corpus");
        processed = 0;
        next = 1;
        MatchMWEs myMatcher = new MatchMWEs(matched);
        Map<MWE, AtomicInteger> myMWEs = new HashMap<>();
        for (Documents.Document doc : this.docs.getDocs()) {
            if (processed >= next) {
                next *= 2;
                System.out.println("Docs processed " + processed + " out of " + this.docs.getDocs().size());
            }
            processed++;
            if (doc.getClazz().equals(topic)) {
                List<MWE> tmp = myMatcher.matchMWEs(doc.getText());
                for (MWE m : tmp) {
                    AtomicInteger ai = myMWEs.computeIfAbsent(m, k -> new AtomicInteger(0));
                    ai.incrementAndGet();
                }
            }
        }
//        myMWEs.entrySet().removeIf(e -> e.getValue().get() < 100);
        System.out.println("Positive + Negative corpus size: " + topicDocs.size());
        System.out.println("Num MWEs: " + myMWEs.size());
        rankMWEs(topicDocs, myMWEs, topic);
        normalizeChi2(myMWEs,topic);
    }

    private void normalizeChi2(Map<MWE, AtomicInteger> myMWEs, String topic) {
        System.out.println("Computing relevancy weights");
        for (int i = 2; i <= NGRAMM; i++) {
            double max = 0;
            double min = Double.POSITIVE_INFINITY;
            for (MWE mwe : myMWEs.keySet()){
                if (mwe.getToks().size() != i) {
                    continue;
                }
                if (mwe.getTopicScores().get(topic) > max) {
                    max = mwe.getTopicScores().get(topic);
                }
                if (mwe.getTopicScores().get(topic) < min) {
                    min = mwe.getTopicScores().get(topic);
                }
            }
            for (MWE mwe : myMWEs.keySet()) {
                if (mwe.getToks().size() != i) {
                    continue;
                }
                double chi2 = mwe.getTopicScores().get(topic);
                mwe.getTopicScores().put(topic, (chi2-min)/(max-min));
            }

        }
    }

    private void rankMWEs(List<Documents.Document> topicDocs, Map<MWE, AtomicInteger> myMWEs, String topic) {
        System.out.println("Preparing docs");
//        List<String[]> allTokens = new ArrayList<>();
        long totalTokens = 0;
        for (Documents.Document doc : topicDocs) {
//            allTokens.add(doc.getText().split("\\s+"));
            totalTokens+=doc.getText().split("\\s+").length;
        }
        System.out.println("Preparing mwes");
        List<Map<String, WordInMWEs>> wordsInMWEs = new ArrayList<>();
        IntStream.range(0, NGRAMM).forEach(i -> wordsInMWEs.add(new HashMap<>()));
        for (MWE mwe : myMWEs.keySet()) {
            for (int i = 0; i < mwe.getToks().size(); i++) {
                Map<String, WordInMWEs> map = wordsInMWEs.get(i);
                final int myInt = i;
                WordInMWEs wordInMWEs = map.computeIfAbsent(mwe.getToks().get(i), k -> new WordInMWEs(k,myInt));
                wordInMWEs.mwes.add(mwe);
            }
        }

        System.out.println("Computing chi2");

        int processed = 0;
        int next = 1;
        final long myTotal = totalTokens;
        myMWEs.entrySet().parallelStream().forEach(entry->rankMWE(wordsInMWEs, entry, topic,myMWEs,myTotal));
//        myMWEs.parallelStream().forEach(mwe -> rankMWE(allTokens, mwe, topic));
//        for (Map.Entry<MWE, AtomicInteger> entry : myMWEs.entrySet()) {
//            if (processed >= next) {
//                next *= 2;
//                System.out.println("MWEs processed " + processed + " out of " + myMWEs.size());
//            }
//            processed++;
//            rankMWE(wordsInMWEs, entry, topic,myMWEs,totalTokens);
//        }
    }

    private void rankMWE(List<Map<String, WordInMWEs>> wordsInMWEs, Map.Entry<MWE, AtomicInteger> entry, String topic, Map<MWE, AtomicInteger> myMWEs, long totalToks) {
        List<String> mweToks = entry.getKey().getToks();
        int ngram = mweToks.size();
//        if (ngram > 2 ){
//            return;
//        }
        List<AtomicLong> cnt = new ArrayList<>();
        IntStream.range(0, 1 << ngram).forEach(i -> cnt.add(new AtomicLong(0)));
        count(wordsInMWEs, entry, cnt,myMWEs);

        //estimate zero occ
        long sum = cnt.stream().map(AtomicLong::get).reduce(Long::sum).orElse(0L);
        cnt.get(0).set(totalToks-sum);

        double chi2 = computeChi2(cnt, totalToks, ngram);
        entry.getKey().getTopicScores().put(topic, chi2);
//        if (chi2 < 10) {
//            System.out.println(chi2 + " " + entry.getKey().getMwe());
//        }
    }

    private double computeChi2(List<AtomicLong> cnt, long totalToks, int ngram) {
        double chi2 = 0;
        for (int i = 0; i < cnt.size(); i++) {
            double E = totalToks;
            for (int nbit = 0; nbit < ngram; nbit++) {
                int val = (i >>> nbit) & 1;
                double sum = 0;
                for (int j = 0; j < cnt.size(); j++) {
                    if (val == ((j >>> nbit) & 1)) {
                        sum += cnt.get(j).get();
                    }
                }
                E *= sum / totalToks;
            }
            double diff = E - cnt.get(i).get();
            chi2 += Math.pow(diff, 2) / E;

        }
        return chi2;
    }

    private void count(List<Map<String, WordInMWEs>> wordsInMWEs, Map.Entry<MWE, AtomicInteger> entry, List<AtomicLong> cnt, Map<MWE, AtomicInteger> myMWEs) {
//        long totalToks = 0;
        List<String> mweToks = entry.getKey().getToks();
        int ngram = mweToks.size();
        List<MWE> partialMatches = new ArrayList<>();
        for (int i = 0; i < mweToks.size(); i++) {
            String tok = mweToks.get(i);
            Map<String, WordInMWEs> map = wordsInMWEs.get(i);
            partialMatches.addAll(map.get(tok).mwes);
        }

        for (MWE mwe : partialMatches) {
            if (mwe.getToks().size() != ngram){
                continue;
            }
            int addr = 0; // 0 0 0 ...
            for (int j = 0; j < ngram; j++) {

                if (mweToks.get(j).equals(mwe.getToks().get(j))){
                    int bit = 1 << j;
                    addr += bit;
                }
            }
            cnt.get(addr).addAndGet(myMWEs.get(mwe).get());
        }

//        for (String[] tokens : allTokens) {
//            for (int i = 0; i <= tokens.length - ngram; i++) {
//                totalToks++;
//                int addr = 0; // 0 0 0 ...
//                for (int j = 0; j < ngram; j++) {
//                    if (mweToks.get(j).equals(tokens[i + j])) {
//                        int bit = 1 << j;
//                        addr += bit;
//
//                    }
//                }
//                cnt.get(addr).incrementAndGet();
//
//            }
//        }
//        return totalToks;
    }

    private static final class WordInMWEs {
        final int pos;
        final String word;
        List<MWE> mwes = new ArrayList<>();

        public WordInMWEs(final String word,final int pos) {
            this.word = word;
            this.pos = pos;
        }
    }
}
