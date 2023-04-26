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

import java.util.Map;

public class MWERanker {

    private final Map<String, MWE> pos;
    private final Map<String, MWE> neg;

    public MWERanker(Map<String, MWE> pos_map, Map<String, MWE> neg_map){
        this.pos = pos_map;
        this.neg = neg_map;
    }

    public void run() {
        double total_pos = 0;
        double total_neg = 0;
        for (MWE mwe : pos.values()) {
            total_pos += mwe.getCount().get();
        }
        for (MWE mwe : neg.values()) {
            total_neg += mwe.getCount().get();
        }
        System.out.println("Total pos: " + total_pos + " Total neg: " + total_neg);

        for (String ps : pos.keySet()) {
            MWE mwe = pos.get(ps);
            double p = mwe.getCount().get() / total_pos;
            double n = 0.000001;
            double nCnt = 0.000001;
            if (neg.containsKey(ps)) {
                nCnt = neg.get(ps).getCount().get();
                n = nCnt / total_neg;
            }
//            double actual = myPosCnt / posCount;
//            double expected = Math.max(myExpectedCnt / negCount, 0.000001);
            double diff = p - n;
            double sign = Math.signum(diff);
            double chi2 = mwe.getCount().get() * sign * Math.pow(diff, 2) / nCnt;
            mwe.setChi2(chi2);
        }

    }
}
