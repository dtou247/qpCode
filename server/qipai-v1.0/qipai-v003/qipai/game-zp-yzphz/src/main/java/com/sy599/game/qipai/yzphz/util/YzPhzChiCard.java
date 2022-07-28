package com.sy599.game.qipai.yzphz.util;

import com.sy599.game.util.JacksonUtil;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class YzPhzChiCard {
    private List<Integer> chi=new ArrayList<>();
    private List<List<Integer>> biList=new ArrayList<>();

    public List<Integer> getChi() {
        return chi;
    }

    public void setChi(List<Integer> chi) {
        this.chi = chi;
    }

    public List<List<Integer>> getBiList() {
        return biList;
    }

    public void setBiList(List<List<Integer>> biList) {
        this.biList = biList;
    }

    public YzPhzChiCard buildChi(List<Integer> chi){
        setChi(chi);
        return this;
    }

    public YzPhzChiCard buildBis(List<List<Integer>> biList){
        setBiList(biList);
        return this;
    }

    public YzPhzChiCard buildBi(List<Integer> bi){
        biList.add(bi);
        return this;
    }

    @Override
    public String toString() {
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("chi",chi.toString());
        jsonObject.put("bi", JacksonUtil.writeValueAsString(biList));
        return jsonObject.toString();
    }
}
