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

import ai.qleap.mwe.data.MWE;
import ai.qleap.mwe.data.MWEs;

import java.util.*;

public class MatchMWEs {

    private final Map<String, MWE> mwes = new HashMap<>();

    public MatchMWEs(MWEs mwes) {
        this(mwes.getMwes());
    }

    public MatchMWEs(Collection<MWE> mwes) {
        mwes.stream().forEach(m -> this.mwes.put(m.getMwe(), m));
    }

    public List<MWE> matchMWEs(String text) {
        String[] words = text.split("\\s+");
        List<String> cands = new ArrayList<>();
        for (int i = 0; i < words.length - 1; i++) {
            StringBuilder sb = new StringBuilder();
            String w = words[i];
            sb.append(w);
            for (int j = i + 1; j < Math.min(words.length - 1, i + MWEExtractor.NGRAMM); j++) {
                sb.append("_");
                String w2 = words[j];
                sb.append(w2);
                cands.add(sb.toString());
            }
        }
        List<MWE> mwes = new ArrayList<>();
        for (String cand : cands) {
            MWE mwe = this.mwes.get(cand);
            if (mwe != null) {
                mwes.add(mwe);
            }
        }
        return mwes;
    }
}
