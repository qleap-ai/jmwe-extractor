/*
 * MIT License
 *
 * Copyright (c) 2023.  qleap.ai
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

import ai.qleap.mwe.data.MWE;

import java.util.*;

import static ai.qleap.mwe.services.MWEExtractor.removePunctuationAtEnd;

public class MWEMatcher {


    private final Map<String, MWE> ngrams = new HashMap<>();
    private final int maxNgrams;

    public MWEMatcher(List<MWE> ngrams, int maxNgrams) {
        ngrams.forEach(n -> this.ngrams.put(n.getMwe(), n));
        this.maxNgrams = maxNgrams;
    }

    public List<MWE> match(String text) {
        String[] words = text.toLowerCase(Locale.GERMAN).split("\\s+");
        List<MWE> cands = new ArrayList<>();
        Arrays.stream(words).forEach(w -> cands.add(new MWE(w, Collections.singletonList(w))));
        for (int i = 0; i < words.length - 1; i++) {
            StringBuilder sb = new StringBuilder();
            String w = words[i];
//            System.out.println(w);
            sb.append(w);
            List<String> toks = new ArrayList<>();
            toks.add(w);
            for (int j = i + 1; j < Math.min(words.length - 1, i + maxNgrams); j++) {
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
        List<MWE> matched = new ArrayList<>();
        for (MWE cand : cands) {
            MWE nGram = this.ngrams.get(cand.getMwe());
            if (nGram != null) {
                matched.add(nGram);
            }
        }
        return matched;
    }

}

