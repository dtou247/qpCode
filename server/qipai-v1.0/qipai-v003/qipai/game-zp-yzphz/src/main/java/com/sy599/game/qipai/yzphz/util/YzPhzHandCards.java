package com.sy599.game.qipai.yzphz.util;

import java.io.Serializable;
import java.util.*;

public class YzPhzHandCards implements Serializable {

    public final Map<Integer, List<Integer>> TI = Collections.synchronizedMap(new LinkedHashMap<Integer, List<Integer>>());

    public final Map<Integer, List<Integer>> PAO = Collections.synchronizedMap(new LinkedHashMap<Integer, List<Integer>>());

    public final Map<Integer, List<Integer>> KAN = Collections.synchronizedMap(new LinkedHashMap<Integer, List<Integer>>());

    public final Map<Integer, List<Integer>> WEI = Collections.synchronizedMap(new LinkedHashMap<Integer, List<Integer>>());

    public final Map<Integer, List<Integer>> WEI_CHOU = Collections.synchronizedMap(new LinkedHashMap<Integer, List<Integer>>());

    public final Map<Integer, List<Integer>> PENG = Collections.synchronizedMap(new LinkedHashMap<Integer, List<Integer>>());

    public final List<List<Integer>> CHI_JIAO = Collections.synchronizedList(new ArrayList<List<Integer>>());

    public final List<List<Integer>> CHI_COMMON = Collections.synchronizedList(new ArrayList<List<Integer>>());

    public final List<Integer> INS = Collections.synchronizedList(new ArrayList<Integer>());

    /**
     * 出的牌（未被吃、碰、跑）
     */
    public final Set<Integer> OUTS = Collections.synchronizedSet(new LinkedHashSet<Integer>());

    /**
     * 王牌
     */
    public final Set<Integer> WANGS = Collections.synchronizedSet(new HashSet<Integer>());

    /**
     * 不能碰的牌 val
     */
    public final Set<Integer> NOT_PENG = Collections.synchronizedSet(new LinkedHashSet<Integer>());

    /**
     * 不能吃的牌 val
     */
    public final Set<Integer> NOT_CHI = Collections.synchronizedSet(new LinkedHashSet<Integer>());

    /**
     * 初始牌
     */
    public final List<Integer> SRC = Collections.synchronizedList(new ArrayList<Integer>());

    public YzPhzHandCards() {
    }

    public void init(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> tempList = new ArrayList<>(list);
        SRC.addAll(tempList);
        YzPhzCardUtils.sort(tempList);
        int pre = tempList.get(0);

        if (YzPhzCardUtils.wangCard(pre)) {
            WANGS.add(pre);
        }

        int size = tempList.size();
        int count = 1;
        for (int i = 1; i < size; i++) {
            int cur = tempList.get(i);

            if (YzPhzCardUtils.wangCard(cur)) {
                WANGS.add(cur);
            } else if (YzPhzCardUtils.commonCard(cur) && YzPhzCardUtils.sameCard(pre, cur)) {
                count++;
                if (count == 3) {
                    KAN.put(YzPhzCardUtils.loadCardVal(cur), new ArrayList<>(tempList.subList(i - 2, i + 1)));
                } else if (count == 4) {
                    TI.put(YzPhzCardUtils.loadCardVal(cur), new ArrayList<>(tempList.subList(i - 3, i + 1)));
                    KAN.remove(YzPhzCardUtils.loadCardVal(cur));
                }
            } else {
                count = 1;
            }
            pre = cur;
        }
        for (List<Integer> temp : KAN.values()) {
            tempList.removeAll(temp);
        }
        for (List<Integer> temp : TI.values()) {
            tempList.removeAll(temp);
        }
        if (WANGS.size() > 0) {
            tempList.removeAll(WANGS);
        }

        INS.addAll(tempList);
    }

    public YzPhzHandCards(List<Integer> list) {
        init(list);
    }

    /**
     * 跑、碰、吃的总胡息
     *
     * @return
     */
    public int loadPPChuxi() {
        int total = 0;
        if (PAO.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : PAO.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.PAO.getBig() : YzPhzHuXiEnums.PAO.getSmall());
            }
        }
        if (PENG.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : PENG.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.PENG.getBig() : YzPhzHuXiEnums.PENG.getSmall());
            }
        }
        if (CHI_COMMON.size() > 0) {
            for (List<Integer> temp : CHI_COMMON) {
                Map<Integer, Integer> map = new HashMap<>();
                for (Integer tmp : temp) {
                    map.put(YzPhzCardUtils.loadCardVal(tmp), 1);
                }

                if (map.containsKey(1)) {
                    total += (YzPhzHuXiEnums.CHI123.getSmall());
                } else if (map.containsKey(101)) {
                    total += (YzPhzHuXiEnums.CHI123.getBig());
                } else if (map.containsKey(2) && map.containsKey(7) && map.containsKey(10)) {
                    total += (YzPhzHuXiEnums.CHI2710.getSmall());
                } else if (map.containsKey(102) && map.containsKey(107) && map.containsKey(110)) {
                    total += (YzPhzHuXiEnums.CHI2710.getBig());
                } else {
                    total += (YzPhzCardUtils.smallCard(temp.get(0)) ? YzPhzHuXiEnums.CHI.getSmall() : YzPhzHuXiEnums.CHI.getBig());
                }
            }
        }

        if (WEI_CHOU.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : WEI_CHOU.entrySet()) {
                total += (kv.getKey().intValue() > 100 ? YzPhzHuXiEnums.WEI_CHOU.getBig() : YzPhzHuXiEnums.WEI_CHOU.getSmall());
            }
        }
        return total;
    }

    public int loadTotalHuxiOutIns() {
        int total = 0;

        if (TI.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : TI.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.TI.getBig() : YzPhzHuXiEnums.TI.getSmall());
            }
        }

        if (PAO.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : PAO.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.PAO.getBig() : YzPhzHuXiEnums.PAO.getSmall());
            }
        }

        if (KAN.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : KAN.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.KAN.getBig() : YzPhzHuXiEnums.KAN.getSmall());
            }
        }

        if (WEI.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : WEI.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.WEI.getBig() : YzPhzHuXiEnums.WEI.getSmall());
            }
        }

        if (WEI_CHOU.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : WEI_CHOU.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.WEI_CHOU.getBig() : YzPhzHuXiEnums.WEI_CHOU.getSmall());
            }
        }

        if (PENG.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : PENG.entrySet()) {
                total += (kv.getKey() > 100 ? YzPhzHuXiEnums.PENG.getBig() : YzPhzHuXiEnums.PENG.getSmall());
            }
        }

        if (CHI_JIAO.size() > 0) {
            for (List<Integer> temp : CHI_JIAO) {
                int count = 0;
                for (Integer tmp : temp) {
                    if (tmp >= 41) {
                        count++;
                    }
                }
                total += (count == 2 ? YzPhzHuXiEnums.JIAO.getBig() : YzPhzHuXiEnums.JIAO.getSmall());
            }
        }

        if (CHI_COMMON.size() > 0) {
            for (List<Integer> temp : CHI_COMMON) {
                Map<Integer, Integer> map = new HashMap<>();
                for (Integer tmp : temp) {
                    map.put(YzPhzCardUtils.loadCardVal(tmp), 1);
                }

                if (map.containsKey(1)) {
                    total += (YzPhzHuXiEnums.CHI123.getSmall());
                } else if (map.containsKey(101)) {
                    total += (YzPhzHuXiEnums.CHI123.getBig());
                } else if (map.containsKey(2) && map.containsKey(7) && map.containsKey(10)) {
                    total += (YzPhzHuXiEnums.CHI2710.getSmall());
                } else if (map.containsKey(102) && map.containsKey(107) && map.containsKey(110)) {
                    total += (YzPhzHuXiEnums.CHI2710.getBig());
                } else {
                    total += (YzPhzCardUtils.smallCard(temp.get(0)) ? YzPhzHuXiEnums.CHI.getSmall() : YzPhzHuXiEnums.CHI.getBig());
                }
            }
        }

        return total;
    }

    public int loadRedCardCount(boolean all) {
        int count = 0;
        if (TI.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : TI.entrySet()) {
                if (YzPhzCardUtils.redCard(kv.getValue().get(0))) {
                    count += 4;
                }
            }
        }
        if (PAO.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : PAO.entrySet()) {
                if (YzPhzCardUtils.redCard(kv.getValue().get(0))) {
                    count += 4;
                }
            }
        }
        if (KAN.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : KAN.entrySet()) {
                if (YzPhzCardUtils.redCard(kv.getValue().get(0))) {
                    count += 3;
                }
            }
        }

        if (WEI.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : WEI.entrySet()) {
                if (YzPhzCardUtils.redCard(kv.getValue().get(0))) {
                    count += 3;
                }
            }
        }

        if (WEI_CHOU.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : WEI_CHOU.entrySet()) {
                if (YzPhzCardUtils.redCard(kv.getValue().get(0))) {
                    count += 3;
                }
            }
        }

        if (PENG.size() > 0) {
            for (Map.Entry<Integer, List<Integer>> kv : PENG.entrySet()) {
                if (YzPhzCardUtils.redCard(kv.getValue().get(0))) {
                    count += 3;
                }
            }
        }
        if (CHI_JIAO.size() > 0) {
            for (List<Integer> temp : CHI_JIAO) {
                if (YzPhzCardUtils.redCard(temp.get(0))) {
                    count += 3;
                }
            }
        }
        if (CHI_COMMON.size() > 0) {
            for (List<Integer> temp : CHI_COMMON) {
                for (Integer tmp : temp) {
                    if (YzPhzCardUtils.redCard(tmp)) {
                        count++;
                    }
                }
            }
        }
        if (all && INS.size() > 0) {
            for (Integer tmp : INS) {
                if (YzPhzCardUtils.redCard(tmp)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();

        strBuilder.append("WANGS:");
        if (WANGS.size() > 0) {
            strBuilder.append("{");
            strBuilder.append(WANGS);
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",TI:");
        if (TI.size() > 0) {
            strBuilder.append("{");
            for (Map.Entry<Integer, List<Integer>> kv : TI.entrySet()) {
                strBuilder.append(",").append(kv.getKey()).append("=").append(kv.getValue()).append(" huxi ").append(kv.getKey() > 100 ? YzPhzHuXiEnums.TI.getBig() : YzPhzHuXiEnums.TI.getSmall());
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",PAO:");
        if (PAO.size() > 0) {
            strBuilder.append("{");
            for (Map.Entry<Integer, List<Integer>> kv : PAO.entrySet()) {
                strBuilder.append(",").append(kv.getKey()).append("=").append(kv.getValue()).append(" huxi ").append(kv.getKey() > 100 ? YzPhzHuXiEnums.PAO.getBig() : YzPhzHuXiEnums.PAO.getSmall());
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",KAN:");
        if (KAN.size() > 0) {
            strBuilder.append("{");
            for (Map.Entry<Integer, List<Integer>> kv : KAN.entrySet()) {
                strBuilder.append(",").append(kv.getKey()).append("=").append(kv.getValue()).append(" huxi ").append(kv.getKey() > 100 ? YzPhzHuXiEnums.KAN.getBig() : YzPhzHuXiEnums.KAN.getSmall());
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",WEI:");
        if (WEI.size() > 0) {
            strBuilder.append("{");
            for (Map.Entry<Integer, List<Integer>> kv : WEI.entrySet()) {
                strBuilder.append(",").append(kv.getKey()).append("=").append(kv.getValue()).append(" huxi ").append(kv.getKey() > 100 ? YzPhzHuXiEnums.WEI.getBig() : YzPhzHuXiEnums.WEI.getSmall());
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",WEI_CHOU:");
        if (WEI_CHOU.size() > 0) {
            strBuilder.append("{");
            for (Map.Entry<Integer, List<Integer>> kv : WEI_CHOU.entrySet()) {
                strBuilder.append(",").append(kv.getKey()).append("=").append(kv.getValue()).append(" huxi ").append(kv.getKey() > 100 ? YzPhzHuXiEnums.WEI_CHOU.getBig() : YzPhzHuXiEnums.WEI_CHOU.getSmall());
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",PENG:");
        if (PENG.size() > 0) {
            strBuilder.append("{");
            for (Map.Entry<Integer, List<Integer>> kv : PENG.entrySet()) {
                strBuilder.append(",").append(kv.getKey()).append("=").append(kv.getValue()).append(" huxi ").append(kv.getKey() > 100 ? YzPhzHuXiEnums.PENG.getBig() : YzPhzHuXiEnums.PENG.getSmall());
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",CHI_JIAO:");
        if (CHI_JIAO.size() > 0) {
            strBuilder.append("{");
            for (List<Integer> temp : CHI_JIAO) {
                int count = 0;
                for (Integer tmp : temp) {
                    if (tmp >= 41) {
                        count++;
                    }
                }
                strBuilder.append(",").append(temp).append(" huxi ").append(count == 2 ? YzPhzHuXiEnums.JIAO.getBig() : YzPhzHuXiEnums.JIAO.getSmall());
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",CHI_COMMON:");
        if (CHI_COMMON.size() > 0) {
            strBuilder.append("{");
            for (List<Integer> temp : CHI_COMMON) {
                Map<Integer, Integer> map = new HashMap<>();
                for (Integer tmp : temp) {
                    map.put(YzPhzCardUtils.loadCardVal(tmp), 1);
                }

                if (map.containsKey(1)) {
                    strBuilder.append(",").append(temp).append(" huxi ").append(YzPhzHuXiEnums.CHI123.getSmall());
                } else if (map.containsKey(101)) {
                    strBuilder.append(",").append(temp).append(" huxi ").append(YzPhzHuXiEnums.CHI123.getBig());
                } else if (map.containsKey(2) && map.containsKey(7) && map.containsKey(10)) {
                    strBuilder.append(",").append(temp).append(" huxi ").append(YzPhzHuXiEnums.CHI2710.getSmall());
                } else if (map.containsKey(102) && map.containsKey(107) && map.containsKey(110)) {
                    strBuilder.append(",").append(temp).append(" huxi ").append(YzPhzHuXiEnums.CHI2710.getBig());
                } else {
                    strBuilder.append(",").append(temp).append(" huxi ").append(YzPhzCardUtils.smallCard(temp.get(0)) ? YzPhzHuXiEnums.CHI.getSmall() : YzPhzHuXiEnums.CHI.getBig());
                }
            }
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",INS:");
        if (INS.size() > 0) {
            strBuilder.append("{");
            strBuilder.append(INS);
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        strBuilder.append(",OUTS:");
        if (OUTS.size() > 0) {
            strBuilder.append("{");
            strBuilder.append(OUTS);
            strBuilder.append("}");
        } else {
            strBuilder.append("{}");
        }

        return strBuilder.toString();
    }

    public int fourSize() {
        return TI.size() + PAO.size();
    }

    public boolean hasFourSameCard() {
        return TI.size() > 0 || PAO.size() > 0;
    }

    public boolean hasThreeSameCard() {
        return KAN.size() > 0 || WEI.size() > 0 || WEI_CHOU.size() > 0;
    }

    public YzPhzHandCards copy() {
        YzPhzHandCards handCards = new YzPhzHandCards();
        for (Map.Entry<Integer, List<Integer>> kv : TI.entrySet()) {
            handCards.TI.put(kv.getKey(), new ArrayList<>(kv.getValue()));
        }
        for (Map.Entry<Integer, List<Integer>> kv : PAO.entrySet()) {
            handCards.PAO.put(kv.getKey(), new ArrayList<>(kv.getValue()));
        }
        for (Map.Entry<Integer, List<Integer>> kv : KAN.entrySet()) {
            handCards.KAN.put(kv.getKey(), new ArrayList<>(kv.getValue()));
        }
        for (Map.Entry<Integer, List<Integer>> kv : WEI.entrySet()) {
            handCards.WEI.put(kv.getKey(), new ArrayList<>(kv.getValue()));
        }
        for (Map.Entry<Integer, List<Integer>> kv : WEI_CHOU.entrySet()) {
            handCards.WEI_CHOU.put(kv.getKey(), new ArrayList<>(kv.getValue()));
        }
        for (Map.Entry<Integer, List<Integer>> kv : PENG.entrySet()) {
            handCards.PENG.put(kv.getKey(), new ArrayList<>(kv.getValue()));
        }
        for (List<Integer> temp : CHI_JIAO) {
            handCards.CHI_JIAO.add(new ArrayList<>(temp));
        }
        for (List<Integer> temp : CHI_COMMON) {
            handCards.CHI_COMMON.add(new ArrayList<>(temp));
        }

//        handCards.NOT_CHI.addAll(NOT_CHI);
//        handCards.NOT_PENG.addAll(NOT_PENG);
        handCards.INS.addAll(INS);
//        handCards.OUTS.addAll(OUTS);
//        handCards.SRC.addAll(SRC);
        handCards.WANGS.addAll(WANGS);
        return handCards;
    }

    public List<Integer> loadCards(){
        List<Integer> list=new ArrayList<>(26);
        for (List<Integer> tmp:TI.values()){
            list.addAll(tmp);
        }
        for (List<Integer> tmp:PAO.values()){
            list.addAll(tmp);
        }
        for (List<Integer> tmp:KAN.values()){
            list.addAll(tmp);
        }
        for (List<Integer> tmp:WEI.values()){
            list.addAll(tmp);
        }
        for (List<Integer> tmp:WEI_CHOU.values()){
            list.addAll(tmp);
        }
        for (List<Integer> tmp:PENG.values()){
            list.addAll(tmp);
        }
        for (List<Integer> tmp:CHI_COMMON){
            list.addAll(tmp);
        }
        for (List<Integer> tmp:CHI_JIAO){
            list.addAll(tmp);
        }
        list.addAll(INS);
        return list;
    }
}
