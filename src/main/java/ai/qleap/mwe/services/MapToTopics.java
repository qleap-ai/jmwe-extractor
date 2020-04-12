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
        Map<MWE, AtomicInteger> myMWEs = new HashMap<>();
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
                    for (MWE m : tmp) {
                        AtomicInteger ai = myMWEs.computeIfAbsent(m, k->new AtomicInteger(0));
                        ai.incrementAndGet();
                    }
//                    myMWEs.addAll();

                }
            }


        }
        double threshold = posCnt*0.001;
        myMWEs.entrySet().removeIf(e->e.getValue().get() < 100);
        System.out.println("Positive + Negative corpus size: " + topicDocs.size());
        System.out.println("Num MWEs: " + myMWEs.size());
        rankMWEs(topicDocs, myMWEs.keySet(),topic);
    }

    private void rankMWEs(List<Documents.Document> topicDocs, Set<MWE> myMWEs, String topic) {
        System.out.println("Preparing docs");
        List<String[]> allTokens = new ArrayList<>();
        for (Documents.Document doc : topicDocs) {
            allTokens.add(doc.getText().split("\\s+"));
        }


        int processed = 0;
        int next = 1;
        for (MWE mwe : myMWEs) {
            if (processed >= next) {
                next *= 2;
                System.out.println("MWEs processed " + processed + " out of " + myMWEs.size());
            }
            processed++;
            rankMWE(allTokens, mwe, topic);
        }
    }

    private void rankMWE(List<String[]> allTokens, MWE mwe, String topic) {
        List<String> mweToks = mwe.getToks();
        int ngram = mweToks.size();
//        if (ngram > 2 ){
//            return;
//        }
        List<AtomicLong> cnt = new ArrayList<>();
        IntStream.range(0, 1 << ngram).forEach(i -> cnt.add(new AtomicLong(0)));
        long totalToks =count(allTokens,mwe,cnt);


        double chi2 = computeChi2(cnt,totalToks,ngram);
        mwe.getTopicScores().put(topic, chi2);
        if (chi2 < 10){
            System.out.println(chi2);
        }
    }

    private double computeChi2(List<AtomicLong> cnt,long totalToks, int ngram) {
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
                E *= sum/totalToks;
            }
            double diff = E - cnt.get(i).get();
            chi2 += Math.pow(diff, 2)/E;

        }
        return chi2;
    }

    private long count(List<String[]> allTokens, MWE mwe, List<AtomicLong> cnt) {
        long totalToks = 0;
        List<String> mweToks = mwe.getToks();
        int ngram = mweToks.size();
        for (String[] tokens : allTokens) {
            for (int i = 0; i <= tokens.length - ngram; i++) {
                totalToks++;
                int addr = 0; // 0 0 0 ...
                for (int j = 0; j < ngram; j++) {
                    if (mweToks.get(j).equals(tokens[i + j])) {
                        int bit = 1 << j;
                        addr += bit;

                    }
                }
                cnt.get(addr).incrementAndGet();

            }
        }
        return totalToks;
    }
}
