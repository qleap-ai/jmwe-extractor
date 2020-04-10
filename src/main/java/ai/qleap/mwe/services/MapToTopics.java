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

import java.util.List;

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
        for (Documents.Document doc : this.docs.getDocs()) {
            if (processed >= next){
                next *= 2;
                System.out.println("processed " + processed + " out of " + this.docs.getDocs().size());
            }
            processed++;
            if (doc.getClazz().equals(topic)){
                List<MWE> myMwes = matcher.matchMWEs(doc.getText());
//                System.out.println(myMwes.size());
            }
        }

    }
}
